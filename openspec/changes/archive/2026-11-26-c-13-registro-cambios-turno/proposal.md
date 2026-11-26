# Proposal — C-13 parte 2 `registro-cambios-turno` (US-025)

## Why

La primera parte de C-13 (`c-13-registro-asistencia-turno`) implementó el registro de asistencia (US-024), pero dejó pendiente **US-025**: que el administrador pueda consultar desde la aplicación quién modificó cada turno, cuándo y qué cambió (`RN-AU-01`). Hoy `turno_historial` ya guarda todos los eventos de creación, confirmación, cancelación, reprogramación y cambio de estado (C-08 y C-09), pero no existe endpoint ni pantalla para verlos. Este change completa el 100% de **C-13** en `CHANGES.md`.

## What Changes

- **Nuevo endpoint de solo lectura `GET /api/turnos/{id}/historial`:**
  - Protegido con `@SoloAdministrador` (matriz RBAC de `03_actores_y_roles.md`: `recepcion` y `profesional` reciben `403`, sin sesión `401`).
  - Valida que el turno exista (`404` si no existe) y devuelve la lista cronológica de `ServicioDeAuditoria.historialDe(id)` enriquecida con el email del usuario interno cuando `actor_tipo = usuario`.
  - Solo consulta: no expone ningún endpoint de edición ni borrado (spec `auditoria-turnos`), ni datos personales del paciente (DNI, teléfono, email del paciente o token de acceso — Ley 25.326).
- **Diálogo de historial en la agenda (`/panel/agenda`):**
  - En cada `TarjetaDeTurno` (incluidos los turnos `cancelado`), el rol `administrador` ve el botón **"Ver historial"**; recepción y profesional no lo ven.
  - Reutiliza el componente accesible `Dialogo` para listar cada cambio en orden cronológico con fecha y hora en `America/Argentina/Buenos_Aires`, actor (email del personal, "Paciente" o "Sistema"), acción en español y valores anterior/nuevo legibles, sin controles de edición ni borrado.

Fuera de alcance:
- Edición o eliminación de registros de auditoría (prohibido por RN-AU-01).
- Panel global de auditoría multi-turno o exportación CSV.
- Panel de ausentismo (C-23).

Referencias: `CHANGES.md` §C-13. Historia: US-025. Reglas: RN-AU-01, RN-AU-02, RN-SO-02, RN-PR-03.

## Capabilities

### New Capabilities
<!-- Ninguna: se amplía la capability existente `auditoria-turnos` (C-08) con la consulta por API y UI para el administrador. -->

### Modified Capabilities
- `auditoria-turnos`: agrega la consulta HTTP `GET /api/turnos/{id}/historial` restringida a `administrador` y la visualización de solo lectura desde la agenda del panel (US-025).

## Impact

- **Backend:**
  - `aplicacion/auditoria/ConsultaDeHistorialDeTurno.java` y `EntradaDeHistorial.java`.
  - `presentacion/auditoria/HistorialTurnoController.java` y `CambioDeTurnoRespuesta.java`.
  - Sin migraciones de Flyway ni cambios en `ServicioDeTurnos` (C-09) ni en `ServicioDeAuditoria` (C-08).
- **Frontend:**
  - `frontend/src/features/agenda/api.ts` (`useHistorialDeTurno`), `HistorialDeTurnoDialogo.tsx` y botón condicional en `TarjetaDeTurno.tsx`.
- **API:** nuevo `GET /api/turnos/{id}/historial` documentado en OpenAPI bajo el tag `Auditoría`.
- **Roadmap:** al archivar este change, **C-13** pasa a `[x]` completo en `CHANGES.md` (US-024 + US-025).
