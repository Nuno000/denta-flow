# Proposal — C-11 `agenda-interna-vistas`

## Why

Recepción y el administrador no tienen dónde ver los turnos del día ni de la semana, y el profesional no puede ver a quién atiende (US-020, US-023, Flujo 5). La agenda es la pantalla central del panel interno. Para la entrega del 2026-11-26 conviene tenerla lista sin esperar a que C-09 (`modelo-turno-paciente`, Renzo) llegue a `main`: el frontend se construye y se prueba ahora contra la API simulada, y el endpoint de lectura se conecta cuando exista el modelo de turnos.

## What Changes

- Nueva sección **Agenda** del panel en `/panel/agenda`, para los tres roles internos:
  - Vista **diaria** y vista **semanal** (lunes a domingo), con navegación a la fecha anterior, a la siguiente y a "Hoy" en la zona del consultorio (`America/Argentina/Buenos_Aires`).
  - Filtros por **profesional**, **sillón** y **estado**, guardados en la URL para que el botón atrás y un enlace compartido conserven la vista.
  - Cada turno muestra su horario, paciente (nombre y apellido), prestación, profesional, sillón, estado en texto y una marca de sobreturno.
  - Diseño mobile-first (390 px) y WCAG 2.1 AA.
- **Mi agenda** para el rol `profesional` (US-023): la misma pantalla, siempre limitada a sus propios turnos y sin filtro de profesional.
- **Contrato de lectura de la agenda**: `GET /api/agenda/turnos` con rango de fechas del consultorio (máximo 7 días) y filtros opcionales. El backend restringe al profesional a sus turnos y devuelve el mínimo de datos personales del paciente (Ley 25.326). Se define en este change y la UI lo consume por un único módulo cliente (`features/agenda/api.ts`).
- **Implementación del endpoint**: se hace en este mismo change, después del merge de C-09, sobre el modelo `Turno` que este aporta. Hasta entonces, la UI se valida con `simularApi`.
- **Menú del panel**: se agrega el enlace "Agenda" para los tres roles internos.

Fuera de alcance: crear o cargar turnos (C-12), cambiar estados o registrar asistencia (C-13), sobreturnos (C-12), horarios de atención y bloqueos (C-06) y una grilla horaria con arrastrar y soltar.

Referencias: `CHANGES.md` §C-11. Historias: US-020 y US-023. Reglas: RN-TU-05 (estados), RN-AU-02 (verificación de rol en backend), SU-09 (zona horaria del consultorio).

## Capabilities

### New Capabilities
- `agenda-interna`: lectura de los turnos del consultorio por rango y filtros, aislamiento del profesional en sus propios turnos, y la sección Agenda del panel con vistas diaria y semanal.

### Modified Capabilities
- `autorizacion-por-rol`: el requisito "Menú del panel según el rol" suma el enlace "Agenda" para los tres roles internos.

## Impact

- **Frontend**: nueva carpeta `frontend/src/features/agenda/` (página, vistas, filtros, `api.ts`, utilidades de fecha) y ruta `/panel/agenda` en `routes/rutas.tsx`. Se agrega la sección en `BarraDelPanel.tsx` y datos de prueba ficticios en `frontend/src/test/datosAgenda.ts`. Sin dependencias nuevas: las fechas se formatean con `Intl.DateTimeFormat` y `timeZone`.
- **Backend** (después de C-09): `aplicacion/agenda/` (consulta), `presentacion/agenda/` (controller y DTOs records) y una consulta de solo lectura sobre el repositorio de turnos de C-09. No hay migración nueva: usa los índices `(profesional_id, estado, inicio, fin)`, `(sillon_id, estado, inicio, fin)` y `(estado, inicio)` que crea C-09.
- **API**: nuevo `GET /api/agenda/turnos` (personal interno), documentado en OpenAPI.
- **Coordinación**: depende de que C-09 exponga la entidad `Turno` y la de `Paciente`. Si los nombres de C-09 difieren del contrato, solo cambia el mapeo del backend, no `api.ts` ni la UI.
