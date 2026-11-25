# Migraciones Flyway

Única ubicación de las migraciones del esquema (`classpath:db/migration`). Flyway las aplica al arrancar y Hibernate solo valida (`ddl-auto=validate`).

## Convención de nombres

`V0NN_<n>__descripcion.sql`

- `NN`: número del change de `CHANGES.md` que agrega la migración (C-09 → `V009_...`).
- `<n>`: número correlativo dentro de ese change, desde 1.
- `descripcion`: en minúsculas, con guiones bajos.

Ejemplos: `V003_1__crear_recursos_consultorio.sql` (existe: profesionales, sillones, prestaciones y su relación), `V009_1__crear_turnos.sql`, `V009_2__indices_solapamiento.sql`.

Los datos de demostración no son una migración: los carga `SeedRecursosDemo` (componente condicionado por `app.seed.demo`, solo en `dev`).

Así los changes que trabajan en paralelo no chocan de versión. En el perfil `dev` está activado `spring.flyway.out-of-order=true`.

## Reglas (DD-09)

- SQL válido en MySQL 8 **y** en H2 `MODE=MySQL`: `BIGINT AUTO_INCREMENT`, `DATETIME(6)` en UTC, estados como `VARCHAR` + `CHECK` (sin `ENUM`), motor InnoDB.
- Nunca se edita una migración ya mergeada: se agrega una nueva.
- Nunca se cambia el esquema por fuera de Flyway.
- Sin datos personales reales en los seeds (Ley 25.326).
