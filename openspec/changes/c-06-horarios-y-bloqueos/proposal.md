# Proposal — C-06 `horarios-y-bloqueos`

## Why

US-002 pide que el administrador defina el **horario de atención semanal de cada profesional** y **bloquee franjas** (vacaciones, feriados, ausencias) por profesional, sillón o consultorio, para que nadie pueda reservar fuera de horario ni en una franja bloqueada (RN-TU-04). Hoy el backend tiene los recursos (C-03) y la autenticación (C-04), pero ningún dato de "cuándo" atiende cada recurso: sin esto el motor de disponibilidad (C-10, camino crítico) no tiene de dónde calcular huecos. C-06 está en el corte de entrega del 26/11 (DD-10) y RN-GL-01 advierte que, si cargar horarios y bloqueos es difícil, la reserva online ofrecerá huecos que no existen.

## What Changes

- **Horarios de atención por profesional** (`CHANGES.md` C-06, US-002): rangos semanales `día de la semana + hora de inicio + hora de fin` en la hora de reloj del consultorio (`America/Argentina/Buenos_Aires`, SU-09). Un profesional puede tener varios rangos por día (p. ej. 09:00–13:00 y 14:00–18:00), pero **no superpuestos** para el mismo profesional y día; los rangos contiguos se permiten. Dos altas simultáneas que se superponen no pueden quedar ambas guardadas.
- **Bloqueos** con ámbito `profesional | sillón | consultorio`, inicio y fin absolutos (guardados en UTC, `DATETIME(6)`), motivo (`vacaciones`, `feriado`, `ausencia`, `otro`) y un detalle opcional; se registra quién y cuándo lo grabó por última vez.
- **Alta, edición, baja y consulta** por API REST (US-002, matriz RBAC de `03_actores_y_roles.md`): el **administrador** crea, edita y borra; **recepción** lee todo; el **profesional** lee solo sus horarios y los bloqueos que lo afectan (los suyos y los del consultorio). Cada permiso se verifica en el backend.
- **Consulta de disponibilidad de un recurso** ("¿está disponible este profesional / sillón en este rango?" = horario de atención − bloqueos), como servicio interno reutilizable por C-09 y C-10, sin duplicar reglas. Intervalos semiabiertos `[inicio, fin)`; los horarios se evalúan en el día del consultorio, no en el día UTC.
- **Aviso de turnos afectados** al crear o editar un bloqueo: la respuesta incluye la lista de turnos activos que caen en la franja. Como la tabla de turnos llega con C-09, en C-06 la lista sale siempre vacía a través de un puerto que C-09 implementa. **No se codifica ninguna resolución de PA-11**: el bloqueo se guarda y los turnos existentes no se tocan.
- **Migración** `V006_1__crear_horarios_y_bloqueos.sql` (convención DD-09; la ficha dice "Migración 002", numeración anterior a la convención vigente) y horarios de demostración en el seed de `dev`.
- **Tests** (TDD estricto, sin Docker): horarios superpuestos rechazados, bloqueo de cada ámbito, borde exacto de horario y de bloqueo, cambio de día entre la hora local y UTC, permisos por rol, restricciones de la tabla en H2 `MODE=MySQL` y en MySQL 8 real (`@Tag("mysql")`), y altas simultáneas de horarios contra MySQL.
- **Fuera de alcance**:
  - Pantalla de administración en el frontend: la ficha marca solo zona backend; se opera desde Swagger UI (ver design, Q1).
  - Resolución de turnos afectados por un bloqueo (cancelar, reprogramar, notificar): PA-11 sigue abierta.
  - Cálculo de huecos por prestación y asignación de profesional/sillón: C-10.
  - Validar horario y bloqueos al crear un turno: lo hace C-09/C-10 consumiendo la consulta de este change.
  - Excepciones de horario por fecha (p. ej. "este sábado atiende de 9 a 12"): se modelan con bloqueos o quedan como extensión.

## Capabilities

### New Capabilities
- `horarios-atencion`: horario de atención semanal de cada profesional en la hora del consultorio, sin rangos superpuestos por profesional y día, con su alta, edición, baja y consulta según el rol.
- `bloqueos-agenda`: franjas bloqueadas por profesional, sillón o consultorio, con motivo, su alta, edición, baja y consulta según el rol, y el aviso de los turnos que quedan dentro de la franja.
- `disponibilidad-recursos`: respuesta única a "¿el profesional / sillón está disponible en este rango?" combinando horarios de atención y bloqueos, para los procesos de reserva y agenda.

### Modified Capabilities
- Ninguna. `recursos-consultorio` ya exige que profesional y sillón se puedan bloquear para escritura ("Recursos bloqueables para la reserva concurrente"); C-06 usa ese bloqueo sin cambiar el requisito. `autorizacion-por-rol` no cambia: los endpoints nuevos nacen protegidos y declaran su guarda. `zona-horaria-consultorio` se aplica tal cual.

## Impact

- **Código nuevo**: `backend/src/main/java/ar/dentaflow/dominio/disponibilidad/` (modelo, reglas y puertos en Java puro, zona de la ficha), `aplicacion/disponibilidad/` (servicios y puerto de turnos afectados), `infraestructura/persistencia/disponibilidad/` (repositorios Spring Data), `presentacion/disponibilidad/` (controladores y DTOs), `META-INF/orm/disponibilidad.xml`, `db/migration/V006_1__crear_horarios_y_bloqueos.sql` y sus tests.
- **Archivos compartidos** (cambios mínimos y aditivos, `docs/plan-trabajo-equipo.md` §4): `application.yml` (una línea en `spring.jpa.mapping-resources`), `db/migration/README.md` (ejemplo), los puertos `ProfesionalRepositorio` y `SillonRepositorio` de C-03 (un método para bloquear la fila, que C-09 también va a usar) con sus implementaciones Spring Data, y `SeedRecursosDemo` (horarios de demostración, solo `dev`). No se tocan `ConfiguracionSeguridad` ni `ManejadorDeErrores`.
- **Base de datos**: tablas `horario_atencion` y `bloqueo` con FK a `profesional`, `sillon` (C-03) y `usuario` (C-04). MySQL 8.0.16 o superior para los `CHECK`.
- **API**: endpoints nuevos bajo `/api/profesionales/{id}/horarios`, `/api/horarios/{id}` y `/api/bloqueos`, documentados en OpenAPI.
- **Changes posteriores**: C-09 implementa el puerto de turnos afectados y consulta la disponibilidad dentro de su transacción; C-10 calcula huecos sobre los mismos horarios y bloqueos; cuando se resuelva PA-11, un change nuevo agrega la resolución.
- **Dependencias externas**: ninguna nueva en el `pom.xml`.
- **Gobernanza**: MEDIO (`CHANGES.md`): implementación con checkpoints; las decisiones Q1–Q6 de `design.md` se informan para que el equipo las confirme. Responsable: Jessica. Revisor del PR: Florencia.
