# Migraciones Flyway

Única ubicación de las migraciones del esquema (`classpath:db/migration`). Flyway las aplica al arrancar y Hibernate solo valida (`ddl-auto=validate`).

## Convención de nombres

`V0NN_<n>__descripcion.sql`

- `NN`: número del change de `CHANGES.md` que agrega la migración (C-09 → `V009_...`).
- `<n>`: número correlativo dentro de ese change, desde 1.
- `descripcion`: en minúsculas, con guiones bajos.

Ejemplos: `V003_1__crear_recursos_consultorio.sql` (existe: profesionales, sillones, prestaciones y su relación), `V004_1__crear_usuarios.sql` (existe: catálogo `rol` y usuarios del personal), `V006_1__crear_horarios_y_bloqueos.sql` (existe: horario de atención semanal por profesional y bloqueos de agenda por profesional, sillón o consultorio), `V007_1__crear_parametros.sql` (existe: parámetros del consultorio con sus valores iniciales), `V008_1__crear_turno_historial.sql` (existe: historial solo-inserción de los cambios de turno), `V009_1__crear_paciente_y_turno.sql` (pacientes y turnos con sus restricciones e índices de solapamiento), `V009_2__fk_turno_historial_turno.sql` (FK del historial al turno).

> **Resuelto en `V009_2`:** `turno_historial.turno_id` no tenía FK porque la tabla `turno` no existía en C-08; `V009_2__fk_turno_historial_turno.sql` agrega `fk_turno_historial_turno`. La migración **no borra** historial huérfano (la auditoría nunca se borra desde una migración): en una base local de desarrollo con filas de prueba sueltas (por ejemplo `turno_id` 42 de los tests viejos de C-08) falla de forma visible. Antes de aplicarla, mirar `SELECT COUNT(*) FROM turno_historial` y, si hay filas huérfanas, limpiarlas a mano en esa base (solo en desarrollo, nunca en producción).

Los datos de demostración no son una migración: los carga `SeedRecursosDemo` (componente condicionado por `app.seed.demo`, solo en `dev`). Tampoco lo es el administrador inicial: lo crea `SeedAdministradorInicial` al arrancar, con `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`. Sí van en la migración los datos de referencia fijos que deben existir en todo entorno, como los tres roles de la tabla `rol`.

Los valores iniciales de los parámetros del consultorio (`V007_1`) son datos de referencia en la migración y nacen con `provisorio = TRUE`. Para reemplazar uno por su valor definitivo (por ejemplo la anticipación mínima de PA-04) se agrega una `V007_n` nueva con `UPDATE parametro SET valor = ... WHERE clave = ... AND provisorio = TRUE`, para no pisar un valor que el administrador ya confirmó; nunca se edita `V007_1`.

Así los changes que trabajan en paralelo no chocan de versión. En el perfil `dev` está activado `spring.flyway.out-of-order=true`.

## Reglas (DD-09)

- SQL válido en MySQL 8 **y** en H2 `MODE=MySQL`: `BIGINT AUTO_INCREMENT`, `DATETIME(6)` en UTC, estados como `VARCHAR` + `CHECK` (sin `ENUM`), motor InnoDB.
- Nunca se edita una migración ya mergeada: se agrega una nueva.
- Nunca se cambia el esquema por fuera de Flyway.
- Sin datos personales reales en los seeds (Ley 25.326).
