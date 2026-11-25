# Design — C-02 `scaffolding-proyecto`

## Context

Ver `proposal.md` (Why). El repositorio solo tiene documentación. Entorno observado en la máquina de Florencia: Java 21 (OpenJDK 21.0.12) y Node 24 / npm 12 instalados; **Maven no está instalado**; MySQL 8.0 instalado como servicio `MySQL80` y en ejecución, con `mysql` fuera del `PATH`. Las reglas del stack están en `CLAUDE.md`, `openspec/config.yaml` y `knowledge-base/08` (DD-09).

## Goals / Non-Goals

**Goals:**
- Un `backend/` y un `frontend/` que compilen, pasen sus pruebas y corran en CI sin Docker.
- Dejar fijos los puntos compartidos (`pom.xml`, `package.json`, perfiles, cliente API, router) para que los changes paralelos solo agreguen.

**Non-Goals:**
- Seguridad, login, CORS definitivo (C-04), email (C-14), Playwright/E2E y despliegue (C-19).
- Tablas de dominio: la primera migración real es de C-03.
- Diseño visual o design system (no hay pantallas en este change).

## Decisions

### D1. Generar el esqueleto con Spring Initializr y commitear el Maven Wrapper
Maven no está instalado. El proyecto Initializr (`start.spring.io`, Maven, Java 21, última versión 3.x estable de Spring Boot) incluye `mvnw`/`mvnw.cmd`, así que nadie necesita instalar Maven y CI usa la misma versión.
- Coordenadas: `groupId` `ar.dentaflow`, `artifactId` `dentaflow-backend`, paquete base `ar.dentaflow`, clase `DentaFlowApplication`.
- Alternativa descartada: pedir a cada integrante que instale Maven (más pasos y versiones distintas).

### D2. Dependencias del `pom.xml` en C-02, salvo Security y Mail
Incluidas: `spring-boot-starter-web`, `-data-jpa`, `-validation`, `-actuator`, `flyway-core` + `flyway-mysql`, `mysql-connector-j` (runtime), `springdoc-openapi-starter-webmvc-ui`, `h2` (test), `spring-boot-starter-test`.
- **Security** queda para C-04: agregarla ahora bloquearía todos los endpoints y obligaría a decidir una configuración de seguridad provisoria en una zona CRÍTICA.
- **Mail** queda para C-14: sin host SMTP configurado, el indicador de salud de mail marcaría la app como caída.
- Cada uno de esos changes toca el `pom.xml` una sola vez.

### D3. Perfiles y configuración
- `application.yml` (común): `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, Flyway en `classpath:db/migration`. Actuator expone solo `health`, sin detalles (`show-details: never`).
- `application-dev.yml`: datasource desde `SPRING_DATASOURCE_*`, con valores por defecto `jdbc:mysql://localhost:3306/dentaflow` y usuario `dentaflow` (la contraseña siempre del entorno); `spring.flyway.out-of-order=true`.
- `application-test.yml`: `jdbc:h2:mem:dentaflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`. Los tests lo activan con `@ActiveProfiles("test")` y además es el perfil por defecto de Surefire (`spring.profiles.active=test`).
- `application-it-mysql.yml`: datasource desde `SPRING_DATASOURCE_*` (en CI, la base del runner).
- Sin migraciones en C-02: Flyway arranca sin scripts. La carpeta `db/migration/` queda creada con un `README.md` que explica la convención `V0NN_<n>__descripcion.sql`.

### D4. Separar las pruebas `mysql` con Surefire y un perfil Maven
- Surefire por defecto: `excludedGroups=mysql`.
- Perfil Maven `it-mysql`: `groups=mysql` y `spring.profiles.active=it-mysql`.
- Comandos: `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL).
- Alternativa descartada: Failsafe con sufijo `*IT`. Agrega una convención de nombres más; el tag ya expresa la intención.

### D5. Zona horaria
- `ar.dentaflow.dominio.comun.ZonaHorariaConsultorio`: Java puro (sin Spring, regla dura). Expone la `ZoneId` fija `America/Argentina/Buenos_Aires` y conversiones `Instant` ↔ `LocalDateTime`/`LocalDate` del consultorio, con `Clock` como parámetro donde se necesita "ahora".
- `ar.dentaflow.infraestructura.tiempo.RelojConfig`: bean `Clock` (`Clock.systemUTC()`), reemplazable en tests por `Clock.fixed`.
- No se depende de `TZ` ni de `user.timezone`. La variable `TZ` del entorno solo afecta los logs.
- Prueba de ida y vuelta UTC: test de integración con `JdbcTemplate` sobre una tabla temporal creada por el propio test, en H2 y también con `@Tag("mysql")`.

### D6. Frontend
- `npm create vite@latest frontend -- --template react-ts`; `tsconfig` con `strict: true`.
- Dependencias: `react-router-dom`, `@tanstack/react-query`, `react-hook-form`, `zod`, `@hookform/resolvers`, `tailwindcss` + `@tailwindcss/vite`; dev: `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`.
- Estructura: `src/lib/apiClient.ts` (wrapper de `fetch` con base `import.meta.env.VITE_API_BASE_URL`, errores tipados), `src/lib/queryClient.ts`, `src/routes/` (router con una ruta `/`), `src/features/{reserva,agenda,auth}/` vacías con `.gitkeep`, `src/components/`.
- ESLint (config del template) más la regla `@typescript-eslint/no-explicit-any: error`.
- Scripts: `dev`, `build`, `lint`, `test` (`vitest run`).

### D7. CI sin contenedores
`.github/workflows/ci.yml`, en push y PR a `main`:
- `backend`: `actions/setup-java` (Temurin 21, caché Maven), `./mvnw -B verify`.
- `it-mysql`: `sudo systemctl start mysql.service`, crear la base `dentaflow` con el usuario `root/root` preinstalado del runner, `./mvnw -B verify -Pit-mysql` con `SPRING_DATASOURCE_*` apuntando a esa base.
- `frontend`: `actions/setup-node` (Node 22 LTS, caché npm), `npm ci`, `npm run lint`, `npm test`, `npm run build`.
- Pedir a Bruno que marque los tres jobs como checks obligatorios en la protección de `main`.

### D8. Archivos de entorno
- `.env.example` en la raíz (variables del backend de `08` §Variables de entorno) y `frontend/.env.example` (`VITE_API_BASE_URL=http://localhost:8080`).
- `.gitignore` ya ignora `.env`, `.env.*` (salvo `.env.example`), `target/`, `node_modules/`, `dist/`.

### Ajustes durante el apply (2026-11-24)
- **D1 — Spring Boot 3.5.16 en lugar de la versión del Initializr.** `start.spring.io` ya solo ofrece Spring Boot 4.x. Se usó para generar el Maven Wrapper (Maven 3.10.1) y el `pom.xml` se escribió con el parent `spring-boot-starter-parent` **3.5.16**, la última 3.x de Maven Central, para respetar el stack decidido (Spring Boot 3, DD-08/DD-09). springdoc fijado en **2.10.1**, que se compila contra Boot 3.5.16. La clase principal es `DentaFlowApplication`.
- **D6 — oxlint en lugar de ESLint.** El template `react-ts` actual de `create-vite` (9.x) trae oxlint. Se mantiene el linter del template con la regla equivalente `typescript/no-explicit-any: error` y `--deny-warnings`. `strict: true` se declara explícitamente en `tsconfig.app.json` y `tsconfig.node.json`.
- **D3/D8 — Lectura del `.env`.** Spring Boot no lee `.env` por sí mismo. Los perfiles `dev` e `it-mysql` lo importan con `spring.config.import=optional:file:../.env[.properties]` (y `optional:file:.env[.properties]`), y la contraseña se resuelve con `${SPRING_DATASOURCE_PASSWORD:}`. Una variable de entorno real tiene prioridad sobre el archivo (así funciona CI).

## Risks / Trade-offs

- [La versión de Spring Boot 3.x que ofrezca Initializr puede tener incompatibilidades con springdoc] → Fijar la versión de springdoc compatible indicada en su documentación y verificar `/swagger-ui.html` en la prueba de humo.
- [H2 `MODE=MySQL` acepta SQL que MySQL rechaza (y al revés)] → Job `it-mysql` obligatorio en CI desde C-03 (primera migración real).
- [MySQL del runner de GitHub podría cambiar de versión o de credenciales] → El job imprime `mysql --version` y falla de forma explícita si no arranca.
- [Node 24 local frente a Node 22 en CI] → `engines` en `package.json` `>=22`; el lockfile se genera localmente y CI usa `npm ci`.
- [Plazo DD-10] → Change chico y mecánico; sin decisiones de dominio.

## Migration Plan

No hay datos ni código previos. Cada integrante, una sola vez:
1. Crear en su MySQL local la base y el usuario (pasos en el `README.md` raíz).
2. Copiar `.env.example` a `.env` y completar la contraseña.

Rollback: revertir el PR.

## Open Questions

- Ninguna que cambie el alcance. El esquema de autenticación y CORS se decide en C-04.

## UI Mockups

- Design system revision: — (no existe `design-system/`)
- Decision: no (no UI) — 2026-11-24. El change solo crea una página inicial de prueba de arranque, sin pantallas de producto.
