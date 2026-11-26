# Design — C-19 `cierre-mvp-e2e-y-despliegue`

## Context

- Entrega: jueves 2026-11-26, 12:00 (DD-10). **Punto de decisión a las 11:00**: si el despliegue no muestra los flujos del corte funcionando, se presenta la demo local (DD-12).
- Stack de hosting: Render (API), Vercel (frontend), Aiven MySQL gratuito (PA-14, DD-12). Autenticación JWT Bearer entre dominios (DD-11), sin cookies.
- Gobernanza MEDIA: se implementa por pasos con puntos de control; la creación de cuentas y la carga de secretos en las plataformas las hace el equipo.
- Depende de C-14..C-17 para los E2E del corte. Lo de infraestructura se puede preparar ya con lo que hay en `main` (C-02..C-08).

## Goals / Non-Goals

**Goals**: entorno público reproducible; plan B local ensayado; E2E de los flujos del corte; concurrencia verificada contra MySQL real; checklist de seguridad.

**Non-Goals**: dominio propio, monitoreo, backups automáticos, escalado, E2E de C-11..C-13 y de los diferenciales, validación con un consultorio real.

## Decisions

### D1. Cómo construir la API en Render — **aprobado: opción A (2026-11-25, Bruno; DD-13)**
Render no tiene runtime nativo de Java: las opciones para Spring Boot son una imagen Docker construida por Render o no usar Render.
- **Opción A (recomendada)**: `backend/Dockerfile` multi-etapa (`eclipse-temurin:21-jdk` para `./mvnw -B -DskipTests package`, `eclipse-temurin:21-jre` para correr el jar con `-XX:MaxRAMPercentage=75`). Lo construye **solo Render, en sus servidores**. No se usa en desarrollo, tests ni CI; no hay `docker-compose` ni Testcontainers. Requiere dejar escrita una excepción acotada a la regla dura "NUNCA usar Docker" de `CLAUDE.md` (cuyo motivo es la RAM local).
- **Opción B**: otro hosting con buildpacks de Java sin Dockerfile (por ejemplo Koyeb). Cambia DD-08 a horas de la entrega y suma una cuenta nueva.
- **Opción C**: saltar el despliegue y presentar directo el plan B local.

### D2. Perfil `prod`
`application-prod.yml`:
- `spring.datasource.*` desde variables; `spring.jpa.show-sql: false`.
- `app.seed.demo: ${APP_SEED_DEMO:false}`; `app.recordatorios.programador-habilitado: false`.
- `server.forward-headers-strategy: framework` (si C-15 no lo dejó global).
- `server.error.include-message: never`, `include-stacktrace: never`.
- Pool Hikari chico (`maximum-pool-size: 5`): los planes gratuitos limitan conexiones y memoria.
- Render arranca con `SPRING_PROFILES_ACTIVE=prod` y `PORT`; `server.port: ${PORT:8080}`.
- El seed de demo de C-03 se verifica idempotente (no duplica al reiniciar); si no lo es, se corrige acá.

### D3. Aiven MySQL
- Servicio MySQL 8 gratuito; se crea la base `dentaflow` y un usuario de la aplicación distinto del administrador del servicio.
- `SPRING_DATASOURCE_URL=jdbc:mysql://<host>:<puerto>/dentaflow?sslMode=REQUIRED` (Aiven exige TLS). Si la conexión pide validar el certificado de la CA de Aiven, se usa `sslMode=VERIFY_CA` con la CA cargada por variable; se decide al probar la conexión.
- Antes de crear el servicio, revisar en la consola de Aiven los límites vigentes del plan gratuito (almacenamiento, conexiones, apagado por inactividad) y anotarlos en la guía.
- Usuario con permisos de DDL porque Flyway migra al arrancar. Separar un usuario sin `UPDATE`/`DELETE` sobre `turno_historial` (pendiente de C-08) queda como mejora documentada.

### D4. Vercel
- Proyecto con raíz `frontend/`, comando `npm run build`, salida `dist/`.
- `VITE_API_BASE_URL=https://<api>.onrender.com` (es pública por definición; no es un secreto).
- `frontend/vercel.json`: `{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }` (Vercel sirve primero los archivos estáticos que existen).
- En Render, `APP_BASE_URL=https://<frontend>.vercel.app` (CORS y enlaces del email de C-14).

### D5. Arranque en frío
- Render suspende la API sin tráfico y la primera respuesta puede tardar cerca de un minuto. El cliente API del frontend usa un timeout de 70 s para la primera carga y TanStack Query muestra "Estamos despertando el sistema, puede tardar hasta un minuto" mientras espera.
- Checklist de demo: abrir `/actuator/health` 5 minutos antes. El cron de C-18 (si entra) mantiene la API despierta como efecto secundario.

### D6. E2E con Playwright
- `@playwright/test` como devDependency; `frontend/playwright.config.ts` con `baseURL` de `E2E_BASE_URL` (por defecto `http://localhost:5173`), solo Chromium, proyecto móvil (390×844) para la reserva y desktop para el panel.
- `frontend/e2e/`: `reserva.spec.ts`, `confirmar-cancelar.spec.ts`, `reprogramar.spec.ts`, `panel-ingreso.spec.ts`, `parametros.spec.ts`.
- Los enlaces del email se obtienen sin leer el buzón: en el perfil `dev`/`test` el adaptador simulado de C-14 los expone por un mecanismo solo de pruebas que se acuerda con Jessica (por ejemplo, un endpoint `/api/dev/ultimos-enlaces` activo solo en `dev`). Contra el despliegue, esos dos flujos se saltean.
- Locators por rol y etiqueta accesible (no por clases CSS), sin `waitForTimeout`.
- CI: job `e2e` en `ubuntu-latest` con el MySQL preinstalado (`sudo systemctl start mysql.service`, como el job `it-mysql`), backend con `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` en segundo plano, frontend con `npm run build && npx vite preview`, espera a `/actuator/health`, `npx playwright install --with-deps chromium`, `npx playwright test`, reporte HTML como artefacto.

### D7. Concurrencia de reserva
`ReservaPublicaConcurrenciaMySqlTest` (`@Tag("mysql")`, perfil `it-mysql`): dos hilos con `CountDownLatch` envían a la vez la misma reserva por MockMvc o `TestRestTemplate`; se espera un 201 y un 409 y una sola fila activa. Complementa el test de concurrencia del servicio de C-09 a nivel API pública.

### D8. Plan B local
- `scripts/demo-local.ps1`: verifica Java 21, Node y MySQL; crea la base si no existe; levanta backend (`dev`, seed de demo) y frontend en dos terminales; abre el navegador.
- `docs/demo.md`: guion de la demo (paso a paso de los flujos del corte), credenciales de demo ficticias tomadas de `.env.example`, y checklist de ensayo.
- Email en la demo local: el adaptador simulado de C-14 o una cuenta SMTP de prueba del equipo, según lo que deje C-14.

### D9. Revisión de seguridad
`openspec/changes/c-19-cierre-mvp-e2e-y-despliegue/evidencia/seguridad.md` con la lista: rutas públicas vs. `autorizacion-por-rol`; cada endpoint con rol verificado; secretos solo en variables (búsqueda en el repo y en `dist/`); HTTPS y HSTS; CORS; límite de intentos de ingreso; logs sin datos personales; validación de input en `/api/publico/**`. Cada hallazgo con severidad y, si no se corrige, motivo.

## Risks / Trade-offs

- **D1 aprobado (opción A)** → el Dockerfile solo lo construye Render; si alguien lo usa en local o en CI, viola la excepción de DD-13 y es un defecto.
- **Límites del plan gratuito de Aiven** desconocidos hasta crear el servicio → se anotan en la guía; si apaga el servicio por inactividad, se agrega al checklist de demo.
- **Arranque en frío de Render** durante la presentación → paso previo de despertar y mensaje de espera.
- **E2E dependen de C-14..C-17**, que todavía no existen → el bloque B se escribe a medida que se mergean; los selectores se acuerdan con Jessica.
- **Tiempo**: si a las 11:00 el despliegue no está, se presenta el plan B sin seguir depurando la nube.

## Migration Plan

1. Bloque A (ya): perfil `prod`, Vercel, Aiven, plan B, esqueleto de Playwright, y Render si se aprueba D1.
2. Con C-14..C-17 en `main`: E2E de los flujos, concurrencia de reserva, cron de C-18 si entra.
3. 26/11 antes de las 11:00: redeploy desde `main`, smoke test, revisión de seguridad.
4. 11:00: decisión nube vs. plan B.
5. Rollback: en Render y Vercel, volver al deploy anterior desde el panel; la base no se toca porque Flyway solo agrega migraciones.

## UI Mockups

- Design system revision: no aplica
- Decision: no (project policy) — 2026-11-25 (`visual_design.mode = off`). El único texto nuevo de UI es el mensaje de arranque en frío.
