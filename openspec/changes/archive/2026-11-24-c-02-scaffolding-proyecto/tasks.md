# Tasks — C-02 `scaffolding-proyecto`

> TDD estricto en todo el código con comportamiento: primero el test que falla, después el código mínimo, después el refactor. Nada de esto requiere Docker ni Testcontainers.

## 1. Esqueleto del backend

- [x] 1.1 Generar `backend/` con Spring Initializr (Maven, Java 21, Spring Boot 3.x estable, `ar.dentaflow` / `dentaflow-backend`, dependencias de design D2) y commitear el Maven Wrapper. Verificar que `./mvnw -v` funciona sin Maven instalado y que `./mvnw -q -DskipTests package` compila
- [x] 1.2 Crear los paquetes `ar.dentaflow.{dominio,aplicacion,infraestructura,presentacion}` (con `package-info.java`) y `backend/src/main/resources/db/migration/README.md` con la convención `V0NN_<n>__descripcion.sql`. Verificar que el árbol coincide con `knowledge-base/08` §Estructura de directorios

## 2. Configuración por entorno (spec `configuracion-entorno`)

- [x] 2.1 Test que falla: `@SpringBootTest` con perfil `test` que arranca el contexto sin servicios externos y comprueba que la base es H2 en modo MySQL. Implementar `application.yml` + `application-test.yml` (design D3) y el perfil por defecto de Surefire. Verificar que `./mvnw verify` pasa en una máquina sin MySQL
- [x] 2.2 Test que falla: con MockMvc, `GET /actuator/health` responde `200` con `status: UP` y sin `components` ni `details`. Configurar actuator para exponer solo `health` sin detalles. Verificar que pasa
- [x] 2.3 Test que falla: el contexto de `test` tiene `ddl-auto=validate`, `open-in-view=false` y `hibernate.jdbc.time_zone=UTC`. Ajustar `application.yml`. Verificar que pasa
- [x] 2.4 Agregar `application-dev.yml` y `application-it-mysql.yml` (datasource desde `SPRING_DATASOURCE_*`, `out-of-order` en dev) y el perfil Maven `it-mysql` con `excludedGroups=mysql` por defecto (design D4). Test `@Tag("mysql")` que verifica la conexión y que el motor es MySQL 8. Verificar que `./mvnw verify` no lo ejecuta y que `./mvnw verify -Pit-mysql` lo ejecuta en verde contra el MySQL80 local
- [x] 2.5 Crear `.env.example` en la raíz con todas las variables de `knowledge-base/08` §Variables de entorno, sin valores sensibles. Verificar a mano contra la tabla de `08` y con `git check-ignore .env` que `.env` queda ignorado

## 3. Zona horaria (spec `zona-horaria-consultorio`)

- [x] 3.1 Tests unitarios que fallan para `ZonaHorariaConsultorio` (Java puro): hora local del consultorio a instante con JVM en `UTC`, instante a hora local con cambio de día, "hoy" y "ahora" con `Clock.fixed` (escenarios de la spec). Implementar en `ar.dentaflow.dominio.comun`. Verificar que pasan y que el paquete `dominio` no importa Spring ni JPA
- [x] 3.2 Bean `Clock` en `ar.dentaflow.infraestructura.tiempo` (design D5), con un test que lo reemplaza por un reloj fijo. Verificar que pasa
- [x] 3.3 Test de integración de ida y vuelta UTC con `JdbcTemplate` sobre una tabla temporal: versión H2 (perfil `test`) y versión `@Tag("mysql")`. Verificar que ambos pasan (`./mvnw verify` y `./mvnw verify -Pit-mysql`)

## 4. Frontend (spec `configuracion-entorno`, requisito de frontend)

- [x] 4.1 Generar `frontend/` con Vite (`react-ts`), `strict: true` y las dependencias de design D6; regla `@typescript-eslint/no-explicit-any: error`; `engines.node >= 22`. Verificar que `npm run lint` y `npm run build` pasan
- [x] 4.2 Configurar Vitest + Testing Library (jsdom) y Tailwind (`@tailwindcss/vite`). Test que falla: la ruta `/` renderiza el nombre "DentaFlow" dentro de `QueryClientProvider` y el router. Implementar `src/routes/`, `src/lib/queryClient.ts` y la página inicial mínima. Verificar que `npm test` pasa
- [x] 4.3 Test que falla: `apiClient` arma la URL con `VITE_API_BASE_URL`, devuelve el JSON tipado y lanza un error tipado ante una respuesta no 2xx (con `fetch` simulado). Implementar `src/lib/apiClient.ts`. Crear `frontend/.env.example` y las carpetas `src/features/{reserva,agenda,auth}`. Verificar que `npm test` pasa

## 5. CI y documentación

- [x] 5.1 Crear `.github/workflows/ci.yml` con los jobs `backend`, `it-mysql` y `frontend` (design D7). Verificar la sintaxis localmente y, después del push, que los tres jobs corren en verde en el PR
- [x] 5.2 Escribir el `README.md` de la raíz: requisitos (Java 21, Node 22+, MySQL 8 nativo), creación de la base y el usuario `dentaflow`, `.env`, comandos para levantar backend (`dev`) y frontend, pruebas (`verify` y `-Pit-mysql`) y enlaces a la KB y al plan del equipo. Verificar siguiendo el README desde cero que el backend arranca en `dev` y responde `/actuator/health`, y que el frontend muestra la página inicial

## 6. Verificación integral

- [x] 6.1 Ejecutar `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm run lint`, `npm test` y `npm run build`; guardar los resúmenes en `openspec/changes/c-02-scaffolding-proyecto/evidencia/`. Verificar con una búsqueda que no hay `Dockerfile`, `docker-compose*`, `testcontainers` ni secretos commiteados
- [x] 6.2 Marcar C-02 `[x]` en la ficha y en la fila de `CHANGES.md`. Verificar que solo cambió C-02

## Workflow follow-up

- Commit(s) en `feat/C-02-scaffolding-proyecto` (Conventional Commits en castellano, sin `Co-Authored-By`), push y PR a `main`; lo aprueba Bruno (PR de Florencia). El PR incluye también el archivado de C-01 (`56c2e5d`).
- Pedir a Bruno que marque los jobs `backend`, `it-mysql` y `frontend` como checks obligatorios en la protección de `main`.
- Con el PR aprobado: `/openspec-archive-change c-02-scaffolding-proyecto`, merge y avisar a Renzo que C-03 queda desbloqueado.
