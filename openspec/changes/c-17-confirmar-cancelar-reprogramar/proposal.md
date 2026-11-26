# Proposal — C-17 `confirmar-cancelar-reprogramar`

## Why

US-011 y US-012 junto a los Flujos 2 y 3 completan el ciclo de autoservicio del paciente en el MVP de DentaFlow: confirmar asistencia con un toque desde el email recibido (C-14), cancelar el turno liberando recursos cuando no pueda asistir, o reprogramarlo a otro horario disponible sin solapamientos (RN-TU-01/02, RN-CA-01..03).

C-17 (`CHANGES.md`, oleada 7, camino crítico, governance **ALTO**, en el corte del 26/11) conecta la infraestructura de enlaces y tokens no adivinables de C-14 con la lógica de negocio de los turnos (C-09), el catálogo de parámetros de consultorio (C-07) y el motor de disponibilidad (C-10). Todo se realiza de forma pública a través del token seguro del turno, sin requerir creación de cuenta ni autenticación previa por parte del paciente (SU-03), y asegurando el registro de auditoría con `Actor.paciente()` (RN-AU-01).

## What Changes

- **Consulta pública de turno por token** `GET /api/publico/turnos/{token}`: utiliza `ServicioDeEnlaces.resolver(token)` (C-14) para determinar la situación (`VIGENTE`, `CANCELADO`, `VENCIDO`, `INEXISTENTE`). Para turnos válidos, expone un resumen (`inicio`, `fin`, `prestacion`, `profesional`, `estado`) y el cálculo booleano `puedeModificar` basado en la anticipación mínima configurada (`anticipacion-minima-horas`). No expone DNI, teléfono, email, id de paciente ni sillón (RN-PR-03).
- **Confirmación con un toque** `POST /api/publico/turnos/{token}/confirmar`: cambia el estado de `pendiente` a `confirmado` mediante `ServicioDeTurnos.confirmar`, auditado con `Actor.paciente()`. La llamada es idempotente: si el turno ya está confirmado, responde con éxito sin registrar duplicados en la auditoría. Ante turnos vencidos o cancelados, responde con error explicativo.
- **Cancelación por el paciente** `POST /api/publico/turnos/{token}/cancelar`: cancela el turno (`ServicioDeTurnos.cancelar`), liberando inmediatamente el profesional y el sillón asignados para nuevas reservas (RN-TU-06). Solo permitido hasta la anticipación mínima configurada (`anticipacion-minima-horas`, RN-CA-01/02); si se intenta dentro de la ventana de anticipación, rechaza la operación con código 409/422 informando que debe contactar al consultorio (RN-CA-03).
- **Consulta de huecos para reprogramación** `GET /api/publico/turnos/{token}/huecos?desde=&hasta=`: consulta horarios alternativos usando `MotorDeDisponibilidad.buscarHuecos` con `turnoAExcluir = turnoId` (contrato de C-10), de modo que el horario actual del paciente figure disponible para su propia reprogramación si no hay otros bloqueos. Exige que el turno esté dentro del plazo de anticipación mínima.
- **Reprogramación transaccional** `POST /api/publico/turnos/{token}/reprogramar`: toma el nuevo inicio seleccionado y resuelve candidatos vía `MotorDeDisponibilidad.candidatosPara`, delegando la ejecución en `ServicioDeTurnos.reprogramar` con bloqueo pesimista ordenado (profesional y luego sillón). Garantiza ausencia de solapamientos (RN-TU-01/02) y audita con `AccionDeTurno.REPROGRAMACION` y `Actor.paciente()`.
- **Ampliación de rango en parámetro de anticipación mínima** (`ClaveParametro.ANTICIPACION_MINIMA_HORAS`): se actualiza el rango permitido de 1..168 a 0..168 horas (PA-04 resuelta, DD-12), permitiendo configurar 0 horas para habilitar cancelaciones y reprogramaciones inmediatas durante la demo y pruebas automatizadas.
- **Pantallas públicas en frontend** (`frontend/src/features/reserva/` o `features/paciente/`):
  - `/turno/:token/confirmar`: pantalla con datos del turno y confirmación de un toque o confirmación automática asistida con estado claro.
  - `/turno/:token/cancelar`: confirmación de cancelación, o pantalla informativa con datos de contacto del consultorio (`GET /api/publico/contacto`, C-16) si el turno está fuera de ventana.
  - `/turno/:token/reprogramar`: selección de nuevo día y horario disponible (reutilizando componentes de calendario de C-16) y confirmación de cambio.
  - Accesibilidad WCAG 2.1 AA, navegación por teclado y diseño responsive para móviles desde 390 px.

## Capabilities

### New Capabilities
- `gestion-turnos-paciente`: consulta pública de estado por token, confirmación de un toque, cancelación con anticipación mínima, consulta de huecos para reprogramar excluyendo el turno propio, reprogramación atómica anti-solapamiento y pantallas web públicas dedicadas.

### Modified Capabilities
- `parametros-consultorio`: ampliación del valor mínimo aceptado para `anticipacion-minima-horas` de 1 a 0 horas (PA-04, DD-12).

## Impact

- **Backend**:
  - `aplicacion/turnos/`: caso de uso de gestión de turnos para pacientes (`ServicioDeGestionTurnoPaciente` o métodos específicos), integrando `ServicioDeTurnos`, `ServicioDeEnlaces`, `MotorDeDisponibilidad` y `ParametrosDelConsultorio`.
  - `presentacion/publico/`: controlador `TurnosPublicosController` (o `AccionesTurnoPublicoController`) con endpoints para `/api/publico/turnos/{token}/**`.
  - `dominio/configuracion/ClaveParametro`: rango de `ANTICIPACION_MINIMA_HORAS` modificado a mínimo 0.
- **Frontend**:
  - `features/reserva/` (o módulo de gestión paciente): páginas para confirmar, cancelar y reprogramar, junto a hooks y API correspondientes.
  - `routes/rutas.tsx`: rutas públicas `/turno/:token/confirmar`, `/turno/:token/cancelar`, `/turno/:token/reprogramar`.
- **Dependencias**:
  - `C-07` (`parametros-consultorio`): ya en `main`.
  - `C-14` (`email-transaccional-y-enlaces`): ya en `main`.
  - `C-16` (`reserva-online-publica`): propuesta en `main`, PR #29 de implementación abierto.
- **Gobernanza y reglas**: ALTO. Cumple US-011, US-012, RN-CA-01..03, RN-NO-02, RN-TU-01/02/05/06, RN-AU-01, RN-PR-03; Flujos 2 y 3.
