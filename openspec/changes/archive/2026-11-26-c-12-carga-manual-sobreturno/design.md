# Design — C-12 `carga-manual-sobreturno`

## Context

Ver `proposal.md` (Why) y la spec `carga-manual-turnos`. Lo que ya existe en `main` y condiciona el enfoque:

- **`ServicioDeTurnos.crear(SolicitudDeTurno)`** (C-09, C-10) es la única vía de escritura.
  - Acepta origen `RECEPCION` con `Actor.usuario(id)` y la marca `esSobreturno`; rechaza el sobreturno `ONLINE`.
  - Bloquea profesional y después sillón (`CerrojoDeRecursos`), aplica las `RestriccionDeAgenda` (`RestriccionDeHorarioYBloqueos` → `AgendaNoDisponibleException`, 409) y, **solo si no es sobreturno**, verifica el solapamiento (`TurnoSolapadoException` con `RecursoOcupado`, 409).
  - Audita `creacion` con `valoresDeAlta()` (incluye `"es_sobreturno"`, sin token ni datos personales) en la misma transacción.
  - Exige ser el borde transaccional con `READ COMMITTED`; el rol lo verifica quien llama.
- **`ServicioDePacientes.registrarDesdeRecepcion(DatosPaciente)`** registra o reemplaza el contacto por DNI en **su propia transacción** (`REQUIRES_NEW`), con un reintento ante la carrera de `uk_paciente_dni`. `buscarPorDni` normaliza el DNI.
- **`MotorDeDisponibilidad.buscarHuecos(ConsultaDeHuecos)`** devuelve `Hueco(inicio, fin, candidatos)` ordenados por inicio, con grilla `app.disponibilidad.paso-minutos` (15) y rango máximo `dias-maximos` (14). Solo lectura, sin bloqueos de fila; ya descarta el pasado.
- **Guardas:** `@AdministradorORecepcion` (`hasAnyRole('ADMINISTRADOR','RECEPCION')`); `AccessDeniedException` → 403 en `ManejadorDeErrores`; sin token → 401 por `RespuestasDeSeguridad`.
- **Esquema:** `turno.es_sobreturno` y `ck` del sobreturno `online` ya existen (`V009_1__crear_paciente_y_turno.sql`). No hace falta migración.
- **Frontend:** `features/agenda/api.ts` concentra la API de la agenda; `lib/apiClient.ts` + `lib/erroresApi.ts` (`leerErrorDeApi` lee `detail` y `errores`); `RutaProtegida` acepta `roles`; `components/formulario/Dialogo` y `Campo`; `react-hook-form`, `@hookform/resolvers` y `zod` ya son dependencias. Listas de prestaciones, profesionales (con `prestacionIds`) y sillones: `GET /api/prestaciones|profesionales|sillones` con `@PersonalInterno`.
- **Discrepancia KB vs `CHANGES.md`:** `CHANGES.md` §C-12 dice "email opcional"; DD-12 / PA-13 y la spec `pacientes` lo hacen obligatorio. Se sigue la KB (ver Hallazgos).

## Goals / Non-Goals

**Goals:**
- Exponer la carga manual y el sobreturno sin tocar `ServicioDeTurnos`, `MotorDeDisponibilidad`, `ServicioDePacientes` ni el esquema.
- Que el sobreturno sea imposible de pedir por accidente: endpoint propio, no un flag en el cuerpo.
- Sugerir alternativas con un costo acotado (una consulta de solo lectura, solo ante conflicto).

**Non-Goals:**
- Reintentar automáticamente con otro candidato (eso es la reserva online de C-10/C-16; la recepción eligió sillón y profesional).
- Alternativas con otros profesionales.
- Email de confirmación al paciente (Preguntas abiertas, P1).
- Editar o cancelar turnos desde el panel.

## Decisions

### D1. Dos endpoints en `presentacion/turnos/CargaManualController`

| Método | Ruta | Guarda | Éxito | Errores |
|---|---|---|---|---|
| POST | `/api/turnos` | `@AdministradorORecepcion` | 201 `TurnoCargadoRespuesta` | 400, 401, 403, 404, 409 (con `alternativas`) |
| POST | `/api/turnos/sobreturnos` | `@AdministradorORecepcion` | 201 `TurnoCargadoRespuesta` | 400, 401, 403, 404, 409 (sin `alternativas`) |

- **Mismo cuerpo** `SolicitudDeCargaManual` (record con Bean Validation):
  `{ paciente: { dni, nombre, apellido, telefono, email }, prestacionId, profesionalId, sillonId, inicio }`.
  - `paciente` es un record anidado `@Valid @NotNull`, con `@NotBlank` en los cinco campos y `@Size` (nombre/apellido 100, email 254). El formato fino (DNI, teléfono, email) lo valida el dominio (`Dni`, `Paciente`) y sale como 400.
  - Ids `@NotNull @Positive`; `inicio` es `OffsetDateTime @NotNull`. Sin desplazamiento, Jackson no lo deserializa → `handleHttpMessageNotReadable` → 400. Se convierte a `Instant` en el controller.
  - El record **no tiene** campo `esSobreturno`: un campo desconocido se ignora y la carga común nunca crea sobreturnos (escenario "La carga común nunca crea sobreturnos").
- **Respuesta** `TurnoCargadoRespuesta(id, inicio, fin, estado, esSobreturno, pacienteId, prestacionId, profesionalId, sillonId)`, con `inicio`/`fin` como `OffsetDateTime` en `ZonaHorariaConsultorio.ZONA` (como `HuecosPublicosController`). Sin token ni contacto. Sin header `Location`: no existe `GET /api/turnos/{id}`.
- **Por qué un endpoint separado para el sobreturno y no un flag:** decisión del usuario (2026-11-26). Además deja el permiso, la documentación OpenAPI y la auditoría de la operación separados, y evita que un cliente envíe el flag por error. Alternativa descartada: `POST /api/turnos?sobreturno=true`.
- **Por qué los dos endpoints con la misma guarda:** la matriz RBAC de `03_actores_y_roles.md` no da "crear turnos" al profesional, así que también la carga común es solo de recepción y administrador.
- **Convivencia con `AsistenciaController`:** ambos mapean bajo `/api/turnos`; no hay colisión (`POST /api/turnos/{id}/asistencia` vs `POST /api/turnos` y `/api/turnos/sobreturnos`; `sobreturnos` no es numérico y `{id}` es `long`, pero además las rutas tienen distinta cantidad de segmentos).

### D2. Caso de uso `aplicacion/turnos/CargaManualDeTurnos`

Sin `@Transactional` (como `ServicioDeReservaDeHuecos` y `RegistroDeAsistencia`): `ServicioDeTurnos` debe ser el borde transaccional. Métodos `cargar(PedidoDeCargaManual, long usuarioId)` y `cargarSobreturno(...)`, que hacen:

1. `ServicioDePacientes.registrarDesdeRecepcion(new DatosPaciente(..., consentimientoEn = null))` → paciente (transacción propia).
2. `ServicioDeTurnos.crear(new SolicitudDeTurno(pacienteId, profesionalId, sillonId, prestacionId, inicio, RECEPCION, esSobreturno, Actor.usuario(usuarioId)))`.
3. Solo en la carga común: si `crear` lanza `TurnoSolapadoException` o `AgendaNoDisponibleException`, calcula alternativas (D3) y lanza `ConflictoConAlternativasException(mensajeOriginal, alternativas)`, que extiende `ConflictoException`. En el sobreturno, la excepción se propaga tal cual (409 sin alternativas).

- `PedidoDeCargaManual` es un record de aplicación (no el DTO de presentación) para no acoplar `aplicacion` a Jackson ni a Bean Validation.
- `consentimientoEn = null`: la carga por recepción no registra aceptación del aviso de privacidad (spec `pacientes`, "Paciente cargado sin aceptación"); se conserva la fecha previa si la había.
- **El paciente queda registrado aunque el turno falle** (por conflicto, 404 de un recurso o inicio pasado). Es el contrato de C-09 (design D8 de C-09) y está en la spec. Alternativa descartada: validar recursos antes de registrar al paciente; duplica las verificaciones del servicio CRÍTICO y no evita la carrera.
- `PessimisticLockingFailureException` se propaga (500 por el handler genérico), igual que en C-10; no hay reintento.

### D3. Alternativas: hasta 3, mismo profesional, 7 días del consultorio

`SugeridorDeAlternativas` (en `aplicacion/turnos/`, o método privado de `CargaManualDeTurnos` si queda corto):

- Llama a `motor.buscarHuecos(new ConsultaDeHuecos(prestacionId, profesionalId, diaLocal, diaLocal + 6, null))`, con `diaLocal = ZonaHorariaConsultorio.aHoraLocal(inicio).toLocalDate()`. 7 días ≤ `dias-maximos` (14) por defecto; si `dias-maximos` fuera menor, se usa `min(7, diasMaximos)` días.
- Ordena por `|hueco.inicio - inicio|` y, a igual distancia, por inicio más temprano; toma **3**.
- Sillón de cada alternativa: el pedido si está entre los `candidatos` del hueco; si no, el primero.
- El motor ya excluye el pasado y respeta horario, bloqueos y ocupaciones; como no toma bloqueos, una alternativa puede ocuparse antes del siguiente intento: el nuevo intento simplemente vuelve a pasar por `crear`.
- Si el motor falla (p. ej. el profesional no realiza la prestación → 400 desde `crear` antes, así que no llega acá), se devuelve lista vacía y se registra en `DEBUG` sin datos del paciente: una falla al sugerir no debe tapar el 409 original.
- **Por qué 3 y 7 días:** suficiente para ofrecer una opción por teléfono sin que la respuesta crezca, y una sola consulta acotada. Alternativa descartada: buscar hasta encontrar (sin límite), costo impredecible.
- **Por qué solo el mismo profesional:** el paciente suele pedir a su profesional; ofrecer otro es una decisión de la recepción. Queda como mejora (P2).

### D4. 409 con alternativas en `ManejadorDeErrores`

Nuevo `@ExceptionHandler(ConflictoConAlternativasException.class)` (más específico que el de `ConflictoException`, Spring elige el más cercano en la jerarquía): `ProblemDetail` 409 "Conflicto" con el `detail` original y la propiedad `alternativas: [{ inicio, fin, profesionalId, sillonId }]`, instantes con `-03:00`. El mapeo a `OffsetDateTime` usa `ZonaHorariaConsultorio`. La excepción vive en `aplicacion/turnos` y lleva una lista de un record de aplicación; el handler la convierte a un record de presentación `AlternativaRespuesta`.

### D5. Búsqueda de paciente `POST /api/pacientes/busqueda`

- En un controller nuevo `presentacion/pacientes/PacienteRecepcionController` con `@AdministradorORecepcion` (no se mezcla con `PacienteAdminController`, que es `@SoloAdministrador`).
- Cuerpo `BusquedaDePacienteSolicitud(@NotBlank String dni)`; respuesta 200 `PacienteRespuesta(id, dni, nombre, apellido, telefono, email)` con `Cache-Control: no-store`; 404 "No hay un paciente con ese DNI"; DNI mal formado → 400 desde `Dni.normalizar` (mensaje sin repetir el valor).
- **Por qué POST y no `GET /api/pacientes?dni=`:** el DNI es un dato personal (Ley 25.326) y en la URL queda en logs de acceso, historial del navegador y proxies. Se acepta el costo de no ser cacheable (tampoco se quiere cachear).
- Un paciente anonimizado (C-15) tiene DNI nulo: nunca se encuentra por DNI, que es lo esperado.

### D6. Frontend: pantalla `CargaDeTurnoPage` en `features/agenda/`

- **Ruta** `/panel/agenda/nuevo-turno` con `RutaProtegida roles={['administrador','recepcion']}`. `AgendaPage` muestra el enlace "Nuevo turno" solo a esos roles (lee el rol de la sesión). No se agrega entrada al menú del panel (spec `autorizacion-por-rol` no cambia).
- **`api.ts`** suma: `useBuscarPacientePorDni()` (`useMutation`, porque es POST y se dispara por acción del usuario), `useCargarTurno()` y `useCargarSobreturno()` (`useMutation`; al terminar bien invalidan `['agenda']`). Para prestaciones, profesionales y sillones se reutilizan `usePrestaciones`, `useProfesionales` y `useSillones` de `features/recursos/api.ts` (C-05), filtrando los activos en la pantalla.
- **Formulario** con React Hook Form + Zod (`esquemaCargaDeTurno`): DNI 7–8 dígitos tras quitar `.`, ` ` y `-`; teléfono 8–15 dígitos con `+` opcional; email `algo@dominio.ext`; ids requeridos; fecha `AAAA-MM-DD` y hora `HH:mm` (paso 5 minutos en el input; el backend decide). Profesionales filtrados por `prestacionIds`; solo recursos activos.
- **Hora del consultorio:** el inicio se arma como `${fecha}T${hora}:00-03:00` con una función nueva `inicioEnConsultorio(fecha, hora)` en `fechas.ts`. Argentina no tiene horario de verano desde 2009 y `ZonaHorariaConsultorio` del backend usa la zona real; si eso cambiara, el backend sigue interpretando bien el desplazamiento recibido. Riesgo anotado.
- **Errores:** 400 → `leerErrorDeApi(...).campos` se asignan con `setError` (claves `paciente.email`, etc.); 409 → `detail` + lista de alternativas como botones "Usar mar 01/12 10:00 — Sillón 2" que hacen `setValue` de fecha, hora y sillón sin enviar; 403 → "No tenés permiso para cargar turnos".
- **Sobreturno:** botón secundario "Cargar sobreturno", que abre `Dialogo` con el texto "El sobreturno se superpone con otros turnos del profesional o del sillón. Queda registrado quién lo cargó." y "Confirmar sobreturno" / "Cancelar". Valida el formulario antes de abrir el diálogo.
- **Anuncios:** región `aria-live="polite"` con "Turno cargado" / "Sobreturno cargado"; errores con `role="alert"`. Tras el éxito se ofrece "Volver a la agenda" (enlace a la fecha del turno) y se limpia el formulario.

### D7. Tests y concurrencia

- **Unitarios (Mockito):** `CargaManualDeTurnosTest`: registra al paciente antes de crear; arma la `SolicitudDeTurno` con `RECEPCION`, `Actor.usuario(id)` y el flag correcto; ante `TurnoSolapadoException`/`AgendaNoDisponibleException` en la carga común lanza la excepción con alternativas; en el sobreturno no consulta al motor. `SugeridorDeAlternativas` (o el método equivalente): orden por distancia, desempate, tope 3, preferencia del sillón, ventana de 7 días por día local (incluido el caso 23:30 −03:00), lista vacía si el motor falla.
- **Integración (MockMvc, H2, perfil `test`):** `CargaManualControllerTest` con datos ficticios: 201 y fila de historial `creacion`; sobreturno superpuesto 201 con `"es_sobreturno":"true"` y usuario en el historial; 409 por profesional, por sillón, fuera de horario y bloqueo, con `alternativas`; sobreturno fuera de horario 409 sin `alternativas`; 403 profesional en los tres endpoints sin crear paciente ni turno; 401; 400 por email faltante, inicio sin desplazamiento, inicio pasado; 404 prestación inexistente; respuesta sin token ni contacto; rutas en `/v3/api-docs`. `PacienteRecepcionControllerTest`: 200, 404, 400, 403, `no-store`.
- **MySQL 8 (`@Tag("mysql")`, `it-mysql`):** el camino de escritura no cambia (`ServicioDeTurnos.crear`, ya cubierto por `CreacionSimultaneaTurnosMySqlTest`), pero governance CRÍTICO y el paso nuevo "paciente en otra transacción + turno" justifican **un** test de punta a punta barato: `CargaManualSimultaneaMySqlTest`, dos hilos con `CountDownLatch` llaman a `CargaManualDeTurnos.cargar` para el mismo profesional y horario en sillones distintos, con pacientes distintos → exactamente un éxito y un `ConflictoException`, un solo turno activo; `@RepeatedTest(5)`; limpia lo que crea.
- **Frontend (Vitest + Testing Library + `simularApi`):** `api.test.tsx` (método, ruta y cuerpo de los tres pedidos; invalidación de `['agenda']`), `CargaDeTurnoPage.test.tsx` (precarga por DNI, paciente nuevo, validación Zod, error de campo del backend, 409 con y sin alternativas, elegir alternativa sin pedido, diálogo de sobreturno confirmar/cancelar, anuncios), `fechas.test.ts` (`inicioEnConsultorio`), `AgendaPage.test.tsx` y `rutas.test.tsx` (enlace y ruta por rol).

## UI Mockups

- Design system revision: no aplica (política del proyecto `off`).
- Decision: no (project policy) — 2026-11-26.
- Mockups: —
- Approval: —
- Preview: —
- Visual verify: se hace la prueba visual con Playwright MCP a 390x844 y 1440x900 como en C-13 (tareas 5.x), sin gate de mockups.

## Risks / Trade-offs

- **[Paciente registrado aunque el turno se rechace]** → Es el contrato de C-09 y no deja datos inválidos: los datos los cargó el personal autenticado. Queda explícito en la spec.
- **[Alternativa ocupada entre la sugerencia y el reintento]** → El motor no bloquea; el reintento vuelve a pasar por `crear`, que garantiza el no solapamiento y devuelve nuevas alternativas.
- **[Consulta extra del motor en cada 409]** → Acotada a un profesional y 7 días, solo lectura, solo en el camino de error.
- **[Desplazamiento fijo `-03:00` en el frontend]** → Si Argentina volviera a tener horario de verano, la UI enviaría un instante corrido una hora. Mitigación: función única `inicioEnConsultorio` fácil de cambiar; el backend no asume el desplazamiento.
- **[El sobreturno no notifica a nadie de la superposición]** → La agenda ya lo muestra marcado (C-11, "Estado y sobreturno visibles sin depender del color").
- **[`PessimisticLockingFailureException` responde 500]** → Igual que el resto de los flujos (C-10); se documenta, sin reintento.

## Migration Plan

Sin migraciones Flyway ni configuración nueva (se reutilizan `app.disponibilidad.*`). Despliegue con el resto del MVP (C-19). Rollback: revertir el PR; la agenda vuelve a no tener carga manual y ningún dato queda inconsistente (los turnos creados son turnos válidos de origen `recepcion`).

## Hallazgos

- **H1 (KB vs `CHANGES.md`):** `CHANGES.md` §C-12 dice "email opcional"; DD-12, PA-13, SU-03 y la spec `pacientes` dicen obligatorio. Se sigue la KB. Conviene corregir el texto de `CHANGES.md` al archivar.
- **H2:** `es_sobreturno` ya existe en `V009_1`; no se requiere migración.

## Preguntas abiertas

- **P1. Email de confirmación para turnos de recepción.** `ServicioDeNotificaciones.enviarConfirmacion(turnoId)` (C-14) existe, pero ni US-021 ni el Flujo 4 lo piden. Propuesta por defecto: **no enviarlo en C-12** (se mantiene el alcance acotado); si el equipo lo quiere, es una línea después de `crear` en `CargaManualDeTurnos`, sin cambiar la spec de C-12 más que con un requisito nuevo. A confirmar con Bruno/Renzo.
- **P2. Alternativas con otros profesionales.** Hoy solo el mismo profesional (D3). Se puede ampliar después sin cambiar el formato de la respuesta (cada alternativa ya trae `profesionalId`).
- **P3. Rol de administrador en el sobreturno.** La matriz RBAC lo marca "C (supuesto)"; la decisión del 2026-11-26 lo incluye. Si se validara que solo recepción, basta con cambiar la guarda del endpoint de sobreturno.
