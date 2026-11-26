# Design — C-17 `confirmar-cancelar-reprogramar`

## Context

- **Gobernanza ALTA:** modifica el ciclo de vida del turno desde rutas públicas sin autenticación, afectando la agenda y la ocupación de recursos. Requiere aprobación de Florencia antes de implementar código.
- **En `main`:**
  - `ServicioDeTurnos` (C-09): única vía de escritura de turnos. Expone `confirmar(turnoId, actor)`, `cancelar(turnoId, actor)`, `reprogramar(turnoId, solicitud, actor)` y `buscarPorToken(token)`. Aplica bloqueo pesimista ordenado (`profesional`, luego `sillón`) bajo aislamiento `READ COMMITTED`.
  - `ServicioDeEnlaces` (C-14): resuelve el token a `ResolucionDeEnlace` con su `SituacionDeEnlace` (`VIGENTE`, `CANCELADO`, `VENCIDO`, `INEXISTENTE`) y datos de `ResumenTurnoPaciente` sin exponer datos personales ni ids internos.
  - `MotorDeDisponibilidad` (C-10): ya soporta `turnoAExcluir` en `ConsultaDeHuecos` y en `candidatosPara(...)`, permitiendo calcular huecos excluyendo el turno actual.
  - `ParametrosDelConsultorio` (C-07): expone `anticipacionMinima()` devolviendo un `Duration`.
  - `Actor.paciente()` (C-08): actor específico para registrar los cambios en la auditoría sin requerir un id de usuario del personal.
- **Rutas públicas en frontend:** C-16 incorporó `/reservar` y componentes de fecha/horario en `frontend/src/features/reserva/`. C-17 reutiliza estos componentes y los estilos base.

## Goals / Non-Goals

**Goals**
- Confirmar asistencia en un toque (`pendiente` → `confirmado`), con idempotencia ante clics repetidos.
- Cancelar el turno liberando profesional y sillón, siempre que se cumpla la anticipación mínima configurada.
- Reprogramar transaccionalmente el turno utilizando los huecos válidos del motor (C-10) con exclusión del turno propio, sin solapamientos.
- Manejar con mensajes claros y orientativos los casos fuera de término, enlaces vencidos, turnos cancelados o conflictos por horarios tomados.
- Respetar la privacidad (RN-PR-03): respuestas públicas y logs sin DNI, teléfono, email ni identificadores innecesarios.
- Pantallas públicas mobile-first (390 px) y accesibles (WCAG 2.1 AA) navegables enteramente por teclado.

**Non-Goals**
- Envío automático de emails ante cancelación o reprogramación (C-14 cubre solo confirmación inicial; recordatorios en C-18).
- Notificación automática a lista de espera (diferenciador C-20).
- Inicio de sesión del paciente o creación de perfiles de usuario.
- Cancelación o reprogramación de sobreturnos desde la interfaz pública (los sobreturnos son de gestión exclusiva de recepción).

## Decisions

### D1. Capas y arquitectura
| Capa | Ubicación | Clases / Archivos |
|---|---|---|
| Aplicación | `aplicacion/turnos/` | `ServicioDeGestionTurnoPaciente` (caso de uso que orquesta enlaces, anticipación, turnos y disponibilidad), DTOs `AccionTurnoPacienteRespuesta`, `HuecosReprogramacionRespuesta` |
| Presentación | `presentacion/publico/` | `AccionesTurnoPublicoController` (o `TurnosPublicosController`) bajo `/api/publico/turnos/{token}/**`, DTOs `ReprogramarSolicitud`, `TurnoPublicoDetalleRespuesta` |
| Dominio | `dominio/configuracion/` | `ClaveParametro.ANTICIPACION_MINIMA_HORAS` actualizado con mínimo en 0 |
| Frontend | `frontend/src/features/reserva/` (o `features/paciente/`) | `ConfirmarTurnoPage.tsx`, `CancelarTurnoPage.tsx`, `ReprogramarTurnoPage.tsx`, `accionesTurnoApi.ts` |

### D2. Validación de situación del token
Toda operación pública comienza resolviendo el token mediante `ServicioDeEnlaces.resolver(token)`:
1. `INEXISTENTE`: responde inmediatamente con `404 Not Found` y ProblemDetail en español ("El enlace no es válido o ha expirado").
2. `CANCELADO`: para consulta devuelve 200 con situación `CANCELADO`; para confirmar, cancelar o reprogramar responde con `409 Conflict` ("El turno ya se encuentra cancelado").
3. `VENCIDO`: para consulta devuelve 200 con situación `VENCIDO`; para acciones responde con `409 Conflict` ("El turno ya ha vencido o fue atendido").
4. `VIGENTE`: procede con la validación de anticipación mínima y la acción requerida.

### D3. Validación de anticipación mínima (`anticipacion-minima-horas`)
- La condición para permitir cancelación o reprogramación es:
  $$\text{inicio del turno} - \text{ahora (reloj)} \ge \text{anticipacionMinima}$$
- Si faltan menos horas que la anticipación mínima:
  - En la consulta `GET /api/publico/turnos/{token}`: se devuelve `puedeModificar = false`.
  - En `POST /cancelar` o `POST /reprogramar`: se rechaza con `409 Conflict` con el mensaje: *"El plazo de cancelación o reprogramación online ha vencido. Por favor comunicate directamente con el consultorio."* Se incluye la información de contacto provista por `GET /api/publico/contacto` (o parámetro `contacto-consultorio`).

### D4. Búsqueda de huecos y reprogramación con `turnoAExcluir`
- Para reprogramar, el paciente consulta huecos en `GET /api/publico/turnos/{token}/huecos?desde=&hasta=`.
- El servicio consulta `MotorDeDisponibilidad.buscarHuecos(new ConsultaDeHuecos(prestacionId, profesionalId, desde, hasta, turno.getId()))`.
- Al confirmar el nuevo horario en `POST /reprogramar`:
  1. Se verifica anticipación mínima.
  2. Se buscan candidatos con `MotorDeDisponibilidad.candidatosPara(prestacionId, profesionalId, nuevoInicio, turno.getId())`. Si no hay candidatos, lanza `HuecoNoDisponibleException` (409).
  3. Se toma el candidato ordenado y se ejecuta `ServicioDeTurnos.reprogramar(turno.getId(), new SolicitudDeReprogramacion(candidato.profesionalId(), candidato.sillonId(), nuevoInicio), Actor.paciente())`.
  4. La transacción InnoDB con cerrojo pesimista garantiza que no habrá solapamiento. Si otra reserva compite y toma el sillón o profesional primero, se captura `TurnoSolapadoException` / `HuecoNoDisponibleException` devolviendo 409 con mensaje en español ("El horario elegido ya no está disponible; elegí otro").

### D5. Ampliación del rango del parámetro `anticipacion-minima-horas`
- En `ClaveParametro`:
  - `ANTICIPACION_MINIMA_HORAS("anticipacion-minima-horas", ..., TipoParametro.ENTERO, "horas", 0, 168, null, ...)`
  - Permite valor `0` (PA-04, DD-12), de modo que para demostraciones y pruebas locales se pueda cancelar o reprogramar hasta el mismo minuto del inicio sin trabas de tiempo.
- No requiere migración SQL de esquema ya que la columna `valor` en la tabla `parametro` es textual (`VARCHAR(4000)`), y el valor por defecto sigue siendo 24.

### D6. Pantallas públicas de frontend
- Rutas públicas agregadas a `routes/rutas.tsx`:
  - `/turno/:token/confirmar`
  - `/turno/:token/cancelar`
  - `/turno/:token/reprogramar`
- Experiencia de usuario:
  - Pantallas auto-contenidas, limpias, con foco en la acción principal.
  - Al cargar la página, se consulta `GET /api/publico/turnos/:token` para mostrar el resumen del turno (día, hora, profesional, prestación).
  - En `/confirmar`: botón destacado "Confirmar mi turno" (o confirmación con un toque y mensaje de confirmación visible).
  - En `/cancelar`: mensaje de advertencia y botón de confirmación de cancelación. Si `puedeModificar` es falso, muestra el cartel informativo con el contacto del consultorio.
  - En `/reprogramar`: selector de fecha y franja horaria (reutilizando la UI de selección de horarios de `/reservar`).
  - Navegación por teclado completa (`h1` y `h2` con foco programático al cambiar de paso, mensajes de error asociados mediante `aria-describedby` y `role="alert"`).

### D7. Privacidad y logs sanitizados
- En los controladores públicos y servicios de C-17, ninguna respuesta incluye DNI, nombre, email ni teléfono del paciente. Solo se exponen datos del turno (inicio, fin, estado, prestación y profesional).
- Los logs registran únicamente id de turno, acción y situación (por ejemplo: `Turno 123 confirmado por el paciente`), sin volcar el token en texto plano ni datos personales (`LogsDeAccionesPacienteTest`).

## Decisiones a aprobar (Q1..Q5)

| # | Decisión | Alternativas consideradas | Recomendación |
|---|---|---|---|
| Q1 | Modo de confirmación en `/turno/:token/confirmar` | A) Confirmación inmediata por GET al cargar la página.<br>B) Pantalla con resumen del turno y botón "Confirmar asistencia". | **Opción B (Recomendada)**: Evita falsas confirmaciones por scanners de seguridad y previsualizadores automáticos de clientes de email (Gmail/Outlook). |
| Q2 | Código de respuesta fuera de anticipación mínima | A) 400 Bad Request.<br>B) 409 Conflict.<br>C) 422 Unprocessable Entity. | **Opción B (409 Conflict)**: Alineado con el resto del proyecto para condiciones temporales o de estado que no permiten la operación. |
| Q3 | Mensaje ante cancelación fuera de término | A) Mensaje estático genérico.<br>B) Mensaje con el dato dinámico del parámetro `contacto-consultorio`. | **Opción B**: Reutiliza `contacto-consultorio` (C-16) para que el consultorio defina cómo comunicarse. |
| Q4 | Elección de profesional al reprogramar | A) Mantener siempre el profesional original.<br>B) Permitir cambiar a cualquier profesional que realice la prestación. | **Opción A (Recomendada para MVP)**: El paciente reprograma fecha/hora con el profesional que lo atiende; si requiere cambio de profesional, se canaliza por recepción. |
| Q5 | Límite anti-abuso en acciones públicas | A) Sin límite.<br>B) Límite por IP en memoria compartido con el limitador de reservas (5 req/10 min). | **Opción B**: Protege los endpoints públicos de escaneos y ataques de denegación. |

## Notas de implementación

- **Q5 (2026-11-26):** el límite por IP compartido con la reserva (5 pedidos cada 10 min → 429) se aplica solo a las acciones `POST` (`confirmar`, `cancelar`, `reprogramar`). Las lecturas (`GET` del turno y de huecos) no lo consumen, igual que `GET /api/publico/huecos` de C-16. Si lo consumieran, el paciente agotaría el cupo al recorrer días para reprogramar. Tests: `AccionesTurnoPublicoControllerTest.rateLimiterPorIpResponde429AlExcederLimiteEnAcciones` y `consultasDeLecturaNoConsumenElLimiteDeAcciones`.
