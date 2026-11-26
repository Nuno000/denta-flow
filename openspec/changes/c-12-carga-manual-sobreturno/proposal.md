# Proposal — C-12 `carga-manual-sobreturno`

## Why

La recepción atiende turnos por teléfono y en persona, pero hoy solo puede verlos en la agenda (C-11) y registrar la asistencia (C-13): no tiene cómo cargarlos. El servicio de turnos de C-09 ya sabe crear turnos de origen `recepcion` y sobreturnos con todas las garantías (solapamiento, horario, bloqueos, auditoría), y el motor de C-10 sabe calcular huecos; falta exponerlos de forma segura y llevarlos a una pantalla del panel (US-021, US-022, Flujo 4). Sin esto, el MVP no cubre el canal principal de un consultorio chico.

## What Changes

- **Carga manual de turno — `POST /api/turnos`** (solo `administrador` y `recepcion`, verificado en el backend):
  - Recibe los datos del paciente (DNI, nombre, apellido, teléfono y email), la prestación, el profesional, el sillón y el inicio.
  - Registra o actualiza al paciente por DNI con la política de recepción de C-09 y crea el turno `pendiente` de origen `recepcion` por la única vía de escritura (`ServicioDeTurnos`, RN-GL-02).
  - Respeta el horario de atención y los bloqueos (RN-TU-04) y **rechaza con 409** cualquier superposición de profesional o de sillón (RN-TU-01, RN-TU-02): no es una alerta.
  - Ante un conflicto, la respuesta 409 sugiere hasta 3 horarios alternativos cercanos del mismo profesional, calculados con el motor de C-10 (Flujo 4, caso de error).
- **Sobreturno explícito — `POST /api/turnos/sobreturnos`**, endpoint separado (US-022, RN-SO-01, DD-03):
  - Solo `administrador` y `recepcion`; el `profesional` recibe 403.
  - Marca `es_sobreturno`, se audita con quién y cuándo en el historial de C-08 (RN-SO-02) y solo exime del solapamiento: sigue respetando horario y bloqueos.
  - No existe en la reserva online (RN-DI-02): ninguna ruta pública lo ofrece.
- **Búsqueda de paciente por DNI — `POST /api/pacientes/busqueda`** (solo `administrador` y `recepcion`), para precargar los datos de un paciente existente. El DNI viaja en el cuerpo, no en la URL (Ley 25.326).
- **Pantalla "Nuevo turno"** en `/panel/agenda/nuevo-turno`, a la que se entra desde la agenda (solo administrador y recepción): buscar paciente por DNI, completar o corregir sus datos, elegir prestación, profesional, sillón, fecha y hora del consultorio, y cargar el turno o, con una acción separada y explícita, el sobreturno. Muestra el conflicto y las alternativas sugeridas, y permite elegir una.

**Desvío de `CHANGES.md`:** el alcance de C-12 dice "email opcional", pero DD-12 / PA-13 (2026-11-25) y la spec `pacientes` lo hacen **obligatorio**. Se sigue la KB: el email es obligatorio también en la carga manual.

Fuera de alcance:
- Reprogramar, confirmar o cancelar desde el panel (C-17 y posteriores).
- Enviar el email de confirmación al paciente cargado por recepción (ver "Preguntas abiertas" en `design.md`).
- Sugerir otros profesionales como alternativa.
- Cualquier cambio a `ServicioDeTurnos`, al motor de C-10 o al esquema de la base.

Referencias: `CHANGES.md` §C-12. Historias: US-021, US-022. Reglas: RN-TU-01, RN-TU-02, RN-TU-04, RN-SO-01, RN-SO-02, RN-GL-02, RN-DI-02, RN-AU-01. Decisiones: DD-03, DD-09, DD-12. Flujo 4.

## Capabilities

### New Capabilities
- `carga-manual-turnos`: carga de turnos y sobreturnos por el personal del consultorio. Cubre quién puede cargar, la búsqueda y el alta del paciente por DNI, el rechazo de conflictos con alternativas sugeridas, el sobreturno explícito y auditado, y la pantalla del panel.

### Modified Capabilities
<!-- Ninguna: las garantías de creación (solapamiento, horario, bloqueos, sobreturno, auditoría) ya están en `turnos` (C-09, C-10) y la deduplicación por DNI en `pacientes` (C-09); no cambian. -->

## Impact

- **Backend:**
  - `aplicacion/turnos/`: caso de uso `CargaManualDeTurnos` que registra al paciente y delega en `ServicioDeTurnos.crear`; ante un conflicto pide alternativas a `MotorDeDisponibilidad.buscarHuecos`.
  - `presentacion/turnos/`: controller `CargaManualController` con DTO records y Bean Validation.
  - `presentacion/pacientes/`: endpoint de búsqueda por DNI con DTO propio.
  - `presentacion/errores/ManejadorDeErrores`: un handler más para el 409 con alternativas.
  - Sin migraciones: `es_sobreturno` ya existe desde `V009_1__crear_paciente_y_turno.sql`.
- **Frontend:** `features/agenda/` suma la pantalla y sus hooks en `api.ts` (TanStack Query + React Hook Form + Zod), una ruta nueva en `routes/rutas.tsx` y un botón "Nuevo turno" en `AgendaPage` solo para administrador y recepción.
- **API:** `POST /api/turnos`, `POST /api/turnos/sobreturnos`, `POST /api/pacientes/busqueda`, documentados en OpenAPI.
- **Coordinación:** solo usa la API pública de `ServicioDeTurnos`, `ServicioDePacientes` y `MotorDeDisponibilidad`. No toca C-16, C-17, C-18 ni C-19.
