# Proposal — C-13 `registro-asistencia-turno`

## Why

La agenda interna (C-11) ya muestra los turnos del día, pero ni el profesional ni la recepción pueden registrar si el paciente vino (US-024, Flujo 5). Sin ese dato no hay control de asistencia ni base para el panel de ausentismo (C-23). El modelo de turnos de C-09 ya tiene los estados y las transiciones de asistencia con auditoría; falta exponerlos de forma segura y llevarlos a la pantalla.

Este change cubre **solo la asistencia (US-024)** del ítem C-13 de `CHANGES.md`. La pantalla del registro de cambios por turno (US-025, solo administrador) queda para un change posterior, por decisión de la usuaria del 2026-11-26, para llegar con tiempo a la entrega.

## What Changes

- **Nuevo endpoint `POST /api/turnos/{id}/asistencia`** con `{ "estado": "asistio" | "ausente" | "ausente_con_aviso" }`.
  - Usa la única vía de escritura de turnos de C-09 (`ServicioDeTurnos.cambiarEstado`), que valida las transiciones de RN-TU-05 y audita en la misma transacción quién y cuándo (RN-AU-01).
  - **Permisos:** administrador y recepción, sobre cualquier turno; el profesional, solo sobre sus propios turnos (matriz RBAC de `03_actores_y_roles.md`). El rol se verifica en el backend.
  - **Momento:** `asistio` y `ausente` solo se registran desde el inicio del turno, en hora del consultorio. `ausente_con_aviso` se acepta en cualquier momento, porque el paciente puede avisar antes.
  - **Repetir el mismo estado** no cambia nada ni genera auditoría (idempotente, como en C-09). Corregir entre los tres estados de asistencia está permitido. Un turno `cancelado` responde 409.
- **Acciones en la agenda (`/panel/agenda`):** cada turno que admite asistencia muestra "Asistió", "Ausente" y "Ausente con aviso". Al registrar, la agenda se refresca y el resultado se anuncia a lectores de pantalla. Los errores 403 y 409 se muestran en la tarjeta.

Fuera de alcance:
- Consulta del historial de cambios del turno (US-025).
- Confirmar, cancelar o reprogramar desde el panel (C-12 y C-17).
- El panel de ausentismo (C-23).

Referencias: `CHANGES.md` §C-13. Historias: US-024. Reglas: RN-TU-05, RN-AU-01 y RN-AU-02. Flujo 5.

## Capabilities

### New Capabilities
- `asistencia-turnos`: registro de asistencia de un turno (asistió, ausente, ausente con aviso). Cubre quién puede registrarla, desde cuándo, la corrección, los errores y las acciones en la agenda del panel.

### Modified Capabilities
<!-- Ninguna: las transiciones y la auditoría ya están en `turnos` y `auditoria-turnos` (C-09, C-08) y no cambian. La agenda de C-11 suma acciones nuevas, que se especifican en la capability nueva. -->

## Impact

- **Backend:**
  - `aplicacion/turnos/`: un caso de uso de asistencia que verifica rol, profesional propio y momento, y delega en `ServicioDeTurnos.cambiarEstado`.
  - `presentacion/turnos/`: controller nuevo con un DTO record.
  - Sin migraciones: usa el estado y la auditoría existentes.
- **Frontend:**
  - `features/agenda/`: mutación `useRegistrarAsistencia` en `api.ts` y acciones en `TarjetaDeTurno`, sin dependencias nuevas.
- **API:** nuevo `POST /api/turnos/{id}/asistencia`, documentado en OpenAPI.
- **Coordinación:** no toca C-10 (Renzo), C-16 ni C-17 (Jessica), ni C-19 (Bruno). Solo usa la API pública de `ServicioDeTurnos`.
