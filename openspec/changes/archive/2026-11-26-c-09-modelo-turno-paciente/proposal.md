# Proposal — C-09 `modelo-turno-paciente`

## Why

El diferencial central de DentaFlow (DD-02) es que **ningún flujo** pueda superponer un profesional ni un sillón (RN-TU-01, RN-TU-02, RN-TU-07, RN-GL-02). Hoy no existe el turno ni el paciente: C-09 (`CHANGES.md`, oleada 4, camino crítico, governance CRÍTICO) crea ese modelo y la **única vía de escritura de turnos**, con bloqueo pesimista en MySQL 8 (DD-09) y auditoría en la misma transacción (C-08, RN-AU-01). Sin C-09 no pueden arrancar C-10, C-11, C-12, C-13, C-14, C-15, C-16, C-17 ni C-18, y el corte del 2026-11-26 prioriza C-09 + C-10.

## What Changes

- **Paciente** (dominio puro `dominio/pacientes/`): DNI, nombre, apellido, teléfono y email **obligatorios**, y fecha de consentimiento del aviso de privacidad (la completa C-15). **PA-13 resuelta por el equipo el 2026-11-25**: el email del paciente es obligatorio (formato validado) y el DNI es obligatorio y único (`paciente.dni NOT NULL` + `uk_paciente_dni`). Deduplicación por DNI (SU-03): el mismo DNI reutiliza siempre el mismo paciente y nunca genera una segunda fila; la política de actualización depende del origen (online no pisa datos existentes; recepción sí).
- **Turno** (dominio puro `dominio/turnos/`): paciente, profesional, sillón, prestación, `inicio`, `fin = inicio + duración de la prestación` (RN-TU-03, guardado), estado, origen `online | recepcion`, `es_sobreturno`, creado por, creado en y token de acceso del paciente no adivinable (SU-03, DD-04).
- **Máquina de estados** explícita: `pendiente`, `confirmado`, `cancelado`, `asistio`, `ausente`, `ausente_con_aviso` (RN-TU-05). Nace `pendiente` (PA-10). Cancelar libera profesional y sillón (RN-TU-06). Repetir el estado actual es idempotente.
- **Servicio de aplicación transaccional** `ServicioDeTurnos` (única vía de escritura): crear, confirmar, cancelar, cambiar estado y reprogramar. Toma `SELECT ... FOR UPDATE` sobre el profesional y después sobre el sillón, verifica el solapamiento `[inicio, fin)` con aislamiento `READ COMMITTED`, devuelve conflicto (409 vía `ManejadorDeErrores`) y llama a `ServicioDeAuditoria.registrarCambio` de C-08 en la misma transacción.
- **Sobreturno** explícito: solo origen `recepcion` con actor usuario; toma los bloqueos pero no se verifica ni bloquea a otros turnos (RN-SO-01/02, RN-DI-02).
- **Migraciones Flyway** (MySQL 8 + H2 `MODE=MySQL`): `V009_1__crear_paciente_y_turno.sql` (tablas, `CHECK`, FK e índices `(profesional_id, estado, inicio, fin)`, `(sillon_id, estado, inicio, fin)`, `(paciente_id)`, `(estado, inicio)`) y `V009_2__fk_turno_historial_turno.sql` (cierra el pendiente de C-08).
- **Punto de extensión** `RestriccionDeAgenda` evaluado dentro de la transacción con los bloqueos tomados, para que C-06/C-10 agreguen horarios y bloqueos (RN-TU-04) sin reescribir el servicio.
- **Sin endpoints REST ni pantallas**: la Zona de la ficha es dominio + migración; los endpoints son de C-11, C-12, C-13, C-16 y C-17.
- Tests de concurrencia `@Tag("mysql")` en el perfil `it-mysql` contra MySQL 8 nativo. Sin Docker ni Testcontainers.

## Capabilities

### New Capabilities
- `turnos`: modelo del turno, duración por prestación, estado inicial, máquina de estados, anti-solapamiento por profesional y por sillón con bloqueo pesimista, sobreturno explícito, reprogramación, token de acceso del paciente, única vía de escritura auditada e integridad en la base.
- `pacientes`: datos mínimos obligatorios del paciente (DNI, nombre, apellido, teléfono y email, PA-13), validación y normalización del DNI, DNI único con deduplicación (SU-03) y política de actualización por origen, fecha de consentimiento e integridad en la base.

### Modified Capabilities
- `auditoria-turnos`: se agrega el requisito de que cada registro del historial referencie un turno existente (FK `fk_turno_historial_turno`, pendiente declarado por C-08).

## Impact

- **Backend nuevo**: `dominio/turnos/`, `dominio/pacientes/`, `aplicacion/turnos/`, `aplicacion/pacientes/`, `infraestructura/persistencia/turnos/`, `infraestructura/persistencia/pacientes/`, `infraestructura/turnos/` (generador de tokens); mapeos `META-INF/orm/turnos.xml` y `META-INF/orm/pacientes.xml`.
- **Archivos compartidos (cambio mínimo y aditivo, commit aparte)**: `application.yml` (`mapping-resources`), `db/migration/README.md` (ejemplos y cierre del pendiente de C-08).
- **Reutiliza**: C-03 (`Profesional`, `Sillon`, `Prestacion` y sus repositorios; patrón `Restricciones*Base` H2/MySQL), C-04 (actor usuario, guardas para los consumidores), C-07 (nada en C-09; C-17 usa `anticipacionMinima()`), C-08 (`ServicioDeAuditoria`, `Actor`, `AccionDeTurno`), zona horaria (`Instant` UTC, `Clock`, `America/Argentina/Buenos_Aires`), excepciones de `dominio/comun` y `ManejadorDeErrores`.
- **Tests de C-08 afectados**: con la FK nueva, los tests que insertan historial con `turno_id` 42 sin turno deben crear un turno real (ajuste de fixtures, sin cambiar su intención; avisar a Bruno).
- **Consumidores**: C-10 (crear por candidato y capturar `TurnoSolapadoException`), C-11 (consultas por rango), C-12 (sobreturno, recepción), C-13 (asistencia), C-14/C-18 (token y email), C-15 (consentimiento), C-16 (reserva), C-17 (confirmar, cancelar, reprogramar).
- **Historias y reglas**: US-010, US-011, US-012, US-021, US-022, US-024 (base de dominio); RN-TU-01..07, RN-GL-02, RN-SO-01/02, RN-DI-02, RN-AU-01, RN-PR-03; PA-10 y PA-13 (ambas resueltas); SU-03, SU-07; DD-02, DD-04, DD-09.
- **Fuera de alcance de C-09**: actualizar `knowledge-base/` y `CHANGES.md` con PA-13 resuelta (lo hace el orquestador).
- **Governance**: CRÍTICO. No se implementa sin la aprobación explícita de Renzo de las decisiones abiertas Q1..Qn de `design.md`; revisa y aprueba la PR Florencia.
