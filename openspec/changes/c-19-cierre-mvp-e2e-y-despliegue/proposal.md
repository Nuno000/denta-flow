# Proposal

## Why

C-19 cierra el corte de entrega del jueves 2026-11-26 a las 12:00 (DD-10): demostrar de punta a punta los flujos del corte (reserva, confirmación, cancelación y reprogramación, más ingreso y configuración del administrador) en un entorno accesible. PA-14 quedó resuelta el 2026-11-25 (DD-12): MySQL en **Aiven** (plan gratuito) como opción principal y, si a las **11:00 del 26/11** no alcanza el tiempo, **demo local completa funcionando al 100%**. Preparar ahora el despliegue y los E2E evita que el cierre dependa de lo que se descubra a último momento.

## What Changes

- **Perfil de despliegue `prod`** del backend: conexión a Aiven con TLS obligatorio, sin seeds de demo salvo que se pidan (`APP_SEED_DEMO`), programador local apagado, HTTPS detrás del proxy (C-15).
- **API en Render** (plan gratuito) construida desde el repositorio, con health check en `/actuator/health` y todas las variables de `.env.example` cargadas como secretos de Render. Como Render no tiene runtime nativo de Java, se usa un `backend/Dockerfile` que **solo** construye Render en sus servidores, nunca en local, tests ni CI (design D1, aprobado el 2026-11-25 como excepción DD-13).
- **Frontend en Vercel** (plan gratuito): build de Vite con `VITE_API_BASE_URL` apuntando a Render, reescritura de rutas de la SPA a `index.html` (`frontend/vercel.json`) y CORS de la API con la URL de Vercel en `APP_BASE_URL`.
- **Base en Aiven**: servicio MySQL 8 gratuito, base `dentaflow`, usuario de la aplicación; Flyway aplica las migraciones al arrancar.
- **Cron externo de recordatorios** (solo si C-18 entra): cron-job.org cada 15 minutos contra `POST /api/tareas/recordatorios` con `X-Tarea-Token`.
- **E2E con Playwright** (`frontend/e2e/`) de los flujos del corte: reserva online completa (con aviso de privacidad de C-15), confirmación y cancelación por enlace, reprogramación, ingreso del personal y edición de un parámetro. Corren contra el entorno local y, con una variable, contra el desplegado.
- **Prueba de concurrencia explícita** (`@Tag("mysql")`): dos reservas simultáneas por la API pública sobre el mismo hueco; solo una gana.
- **Revisión de seguridad** con lista de verificación (RBAC en backend, rutas públicas, secretos solo en variables, HTTPS, sin datos personales en logs, límite de intentos) y sus hallazgos.
- **Plan B — demo local**: guía y script de arranque (`scripts/demo-local.ps1`) con MySQL nativo, backend en `dev` con seed de demo y frontend con `npm run dev`; checklist de ensayo de la demo.
- **README** con la puesta en marcha local, el despliegue y las URLs de la demo.

## Capabilities

### New Capabilities
- `despliegue-publico`: entorno público accesible (Vercel + Render + Aiven), configuración por variables, conexión cifrada a la base, rutas de la SPA, arranque en frío, cron externo y demo local equivalente como plan B.

### Modified Capabilities
- `configuracion-entorno`: suma el perfil de despliegue `prod` (sin seeds de demo por defecto, sin programador local, TLS a la base) al conjunto de perfiles definidos.

## Impact

- **Backend**: `application-prod.yml`, `backend/Dockerfile` (si se aprueba D1), test de concurrencia de reserva en `it-mysql`.
- **Frontend**: `frontend/vercel.json`, `frontend/playwright.config.ts`, `frontend/e2e/`, dependencia de desarrollo `@playwright/test`.
- **CI**: job `e2e` en `.github/workflows/ci.yml` con el MySQL preinstalado del runner (sin contenedores), backend y frontend levantados en el job.
- **Cuentas externas** (las crea el equipo, no el agente): Aiven, Render, Vercel y cron-job.org.
- **Depende de** C-16 y C-17 (flujos del corte), C-14 (email), C-15 (aviso) y, si entra, C-18. El bloque A de `tasks.md` (perfil `prod`, Vercel, Render, Aiven, plan B, esqueleto de Playwright) se puede hacer ya con lo que hay en `main`.
- **Fuera del corte**: E2E de carga manual con sobreturno, asistencia y agenda (C-11..C-13), validación con un consultorio real (SU-01/SU-02), permisos de base que impidan `UPDATE`/`DELETE` en `turno_historial` (se documenta como mejora).
