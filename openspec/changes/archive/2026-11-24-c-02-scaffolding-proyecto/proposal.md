# Proposal — C-02 `scaffolding-proyecto`

## Why

C-01 dejó cerrado el stack (DD-09) y el plazo (DD-10), pero el repositorio todavía no tiene código. Todo el resto del roadmap (C-03 en adelante) necesita un `backend/` y un `frontend/` que compilen, arranquen y se prueben igual en cada máquina y en CI, sin Docker. C-02 es el segundo eslabón del camino crítico y abre la oleada 1 (C-03, Renzo). Además, dejar ahora en `pom.xml` y `package.json` las dependencias comunes evita conflictos de merge cuando las oleadas trabajen en paralelo.

## What Changes

- **Backend** (`backend/`, Maven Wrapper, Java 21, Spring Boot 3):
  - Paquetes `ar.dentaflow.{dominio,aplicacion,infraestructura,presentacion}` según `knowledge-base/08_arquitectura_propuesta.md`.
  - Dependencias base: web, data JPA, validation, Flyway (+ `flyway-mysql`), `mysql-connector-j`, H2 (solo tests), springdoc-openapi y actuator (health). Security entra en C-04 y Mail en C-14, que son sus dueños.
  - Perfiles: `dev` (MySQL 8 local), `test` (H2 `MODE=MySQL;DATABASE_TO_LOWER=TRUE`, perfil por defecto de los tests) e `it-mysql` (MySQL real; tests `@Tag("mysql")`, excluidos del build por defecto). Hibernate en `validate`, tiempos en UTC, sin `open-in-view`.
  - Configuración solo por variables de entorno, con `.env.example` y sin secretos en el repo.
  - Utilidad de zona horaria del consultorio (`America/Argentina/Buenos_Aires`, SU-09) en `dominio` (Java puro) y un `Clock` inyectable.
- **Frontend** (`frontend/`, Vite + React + TypeScript `strict`):
  - React Router, TanStack Query, React Hook Form + Zod y Tailwind CSS instalados y configurados.
  - Cliente API en `src/lib/` que toma `VITE_API_BASE_URL`.
  - Vitest + Testing Library y ESLint con `no-explicit-any` en error.
  - Una página inicial mínima, sin diseño de pantallas.
- **CI** (`.github/workflows/ci.yml`): jobs `backend` (`./mvnw verify` sobre H2), `it-mysql` (MySQL preinstalado del runner, sin contenedores) y `frontend` (lint, test, build).
- **Documentación**: `README.md` en la raíz con la puesta en marcha local, incluida la creación de la base y el usuario MySQL.

## Capabilities

### New Capabilities
- `configuracion-entorno`: cómo arranca el sistema según el entorno (dev / test / it-mysql), toma su configuración de variables de entorno, aplica migraciones e informa su estado de salud.
- `zona-horaria-consultorio`: interpretación única de fechas y horas en la zona del consultorio y almacenamiento en UTC.

### Modified Capabilities
- Ninguna (`openspec/specs/` está vacío).

## Impact

- **Código nuevo**: `backend/`, `frontend/`, `.github/workflows/ci.yml`, `README.md`, `.env.example` en la raíz y en `frontend/`. No se modifica la KB.
- **Dependencias externas**: Spring Initializr para generar el esqueleto y el Maven Wrapper (Maven no está instalado localmente), npm. MySQL 8 nativo en cada máquina para `dev` e `it-mysql`.
- **Changes posteriores**: C-03 en adelante agregan migraciones `V0NN_<n>__...` y código en los paquetes creados acá; C-04 agrega Security y C-14 agrega Mail al `pom.xml`.
- **Diseño visual**: sin pantallas en este change. El primer change visual (C-04, login) dispara la decisión de política de diseño del proyecto, que hoy no está registrada.
- **Gobernanza**: BAJO.
