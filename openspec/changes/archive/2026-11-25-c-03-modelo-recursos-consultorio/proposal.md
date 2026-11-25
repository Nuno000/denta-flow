# Proposal — C-03 `modelo-recursos-consultorio`

## Why

C-02 dejó el backend arrancando con Flyway y Hibernate en `validate`, pero todavía sin ninguna tabla de dominio. Profesional, sillón y prestación son los recursos que todo el resto del roadmap referencia: C-04 (`Usuario.profesional_id`), C-06 (horarios y bloqueos por profesional y por sillón), C-09 (turnos con bloqueo pesimista sobre profesional y sillón) y C-10 (motor de disponibilidad, que necesita la duración de cada prestación, RN-TU-03). C-03 es el tercer eslabón del camino crítico (`C-01 → C-02 → C-03 → C-04 → ...`) y, como C-05 quedó fuera del corte de entrega (DD-10: "alcanza el seed"), el seed de demostración de este change es la única fuente de recursos para la demo del 2026-11-26.

## What Changes

- **Modelo de dominio** (`ar.dentaflow.dominio.recursos`, Java puro, sin Spring ni JPA):
  - `Profesional` (nombre, apellido, matrícula opcional, email, activo) con las prestaciones que realiza.
  - `Sillon` (nombre, activo).
  - `Prestacion` (nombre, `duracion_minutos` obligatoria y mayor que cero, activa) — RN-TU-03.
  - Relación N:M Profesional–Prestación (US-001: "indicar qué prestaciones realiza cada profesional").
  - Activar / desactivar sin borrar en los tres recursos (US-001: "crear, editar y desactivar").
  - Puertos de repositorio del dominio, sin operación de borrado.
- **Persistencia** (`ar.dentaflow.infraestructura`):
  - Migración Flyway `V003_1__crear_recursos_consultorio.sql` (DD-09; reemplaza la "migración 001" del texto original de `CHANGES.md`): tablas `profesional`, `sillon`, `prestacion` y `profesional_prestacion`, con `NOT NULL`, `DEFAULT TRUE` para el estado, `CHECK (duracion_minutos > 0)`, unicidades y claves foráneas. SQL válido en MySQL 8 y en H2 `MODE=MySQL`.
  - Mapeo JPA fuera del paquete `dominio` y repositorios base (guardar, buscar por id, listar, listar activos, profesionales activos que realizan una prestación).
  - Tablas `profesional` y `sillon` bloqueables con `SELECT ... FOR UPDATE` para el servicio transaccional de C-09 (DD-09); los métodos de bloqueo los agrega C-09.
- **Seed de desarrollo/demo**: 3 profesionales, 2 sillones y 5 prestaciones con duraciones distintas, datos ficticios (Ley 25.326), idempotente y activado solo por configuración explícita; nunca corre en el perfil `test` ni por defecto.
- **Tests**: invariantes de modelo (unitarios), duración obligatoria y positiva (dominio y base), desactivar sin borrar (persistencia sobre H2 `MODE=MySQL`), restricciones verificadas también contra MySQL 8 real (`@Tag("mysql")`, perfil `it-mysql`).
- **Fuera de alcance**: endpoints REST, DTOs, pantallas y RBAC de la gestión de recursos (C-05, US-001); horarios y bloqueos (C-06); métodos de bloqueo pesimista y turnos (C-09); usuario administrador (C-04). Este change no expone endpoints, por lo que no hay superficie RBAC, y no maneja fechas ni horas (la duración se expresa en minutos, independiente de la zona horaria).

## Capabilities

### New Capabilities
- `recursos-consultorio`: los recursos del consultorio (profesionales, sillones y prestaciones), sus datos obligatorios, la duración de cada prestación, qué prestaciones realiza cada profesional, el estado activo/inactivo sin borrado y los datos de demostración.

### Modified Capabilities
- Ninguna. `configuracion-entorno` y `zona-horaria-consultorio` no cambian sus requisitos: la migración nueva se aplica con el mecanismo ya especificado ("Migraciones aplicadas al arrancar").

## Impact

- **Código nuevo**: `backend/src/main/java/ar/dentaflow/dominio/recursos/`, `backend/src/main/java/ar/dentaflow/infraestructura/persistencia/recursos/`, `backend/src/main/java/ar/dentaflow/infraestructura/seed/`, `backend/src/main/resources/db/migration/V003_1__crear_recursos_consultorio.sql`, el archivo de mapeo JPA de recursos y sus tests.
- **Archivos compartidos que se tocan** (una línea cada uno): `application.yml` (archivo de mapeo), `application-dev.yml` (activar el seed), `.env.example` (variable del seed), `db/migration/README.md` (nota del seed). Una excepción de dominio común en `dominio/comun/`.
- **Changes posteriores**: C-04 crea la FK `usuario.profesional_id → profesional.id`; C-05 construye la API y la UI de administración sobre los puertos de este change; C-06 referencia `profesional` y `sillon`; C-09 agrega los métodos `FOR UPDATE` (profesional y después sillón) y la FK `turno.prestacion_id`; C-19 decide si el despliegue de demo activa el seed.
- **Dependencias externas**: ninguna nueva en el `pom.xml`. MySQL 8.0.16 o superior para que el `CHECK` se aplique (las versiones anteriores lo ignoran).
- **Gobernanza**: CRÍTICO. El diseño (`design.md`) requiere aprobación explícita de Renzo antes de implementar; el PR lo aprueba Florencia.
