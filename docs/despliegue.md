# Despliegue de DentaFlow (plan A)

> Plan A de la entrega (DD-12): frontend en **Vercel**, API en **Render**, MySQL 8 en **Aiven**, todo en planes gratuitos. Si a las **11:00 del 26/11** esto no muestra los flujos del corte, se pasa al plan B ([demo.md](demo.md)).
>
> Las cuentas y los secretos los crea y carga el equipo en cada plataforma. **Nunca** se escriben contraseñas, claves ni URLs con credenciales en el repo, en los PR ni en el chat.

Orden: 1 → 2 → 3 → 4. Cada paso termina con una verificación.

## 1. Base en Aiven

1. Crear un servicio **MySQL** con el plan gratuito. Anotar abajo, en "Límites vistos", lo que la consola diga del plan (almacenamiento, conexiones, apagado por inactividad).
2. En el servicio, crear la base `dentaflow` y un usuario de la aplicación (no usar `avnadmin` para la app).
3. Armar la URL JDBC con el host y el puerto que muestra Aiven, **con TLS**:
   `jdbc:mysql://<host>:<puerto>/dentaflow?sslMode=REQUIRED`
4. **Verificar** desde una máquina del equipo (variables solo en esa terminal, sin guardarlas):
   ```powershell
   $env:SPRING_DATASOURCE_URL = '<url de arriba>'; $env:SPRING_DATASOURCE_USERNAME = '<usuario>'; $env:SPRING_DATASOURCE_PASSWORD = '<contraseña>'
   cd backend; .\mvnw.cmd verify -Pit-mysql
   ```
   Si falla por el certificado, probar `sslMode=VERIFY_CA` con la CA de Aiven y anotarlo acá.

Límites vistos: _(completar)_

## 2. API en Render

1. **New → Web Service** desde el repo `denta-flow/denta-flow`, rama `main`.
2. **Root Directory**: `backend` · **Runtime**: Docker (usa `backend/Dockerfile`, que solo construye Render; DD-13) · **Instance**: Free.
3. **Health Check Path**: `/actuator/health`.
4. **Environment** (como secretos):

   | Variable | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | la de Aiven, con `sslMode=REQUIRED` |
   | `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | usuario de la app en Aiven |
   | `AUTH_SECRET` | 32 caracteres o más, generada para producción (distinta de la local) |
   | `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | administrador de la demo |
   | `APP_BASE_URL` | URL de Vercel del paso 3 (se completa después) |
   | `APP_SEED_DEMO` | `true` para la demo (datos ficticios, no se duplican al reiniciar) |
   | `SPRING_MAIL_*`, `APP_MAIL_FROM` | los que defina C-14 |

   `SPRING_PROFILES_ACTIVE=prod` ya lo fija la imagen.
5. **Verificar**: `https://<api>.onrender.com/actuator/health` responde `{"status":"UP"}` y en el log de Render aparece que Flyway aplicó las migraciones. La primera respuesta después de un rato sin uso puede tardar cerca de un minuto.

## 3. Frontend en Vercel

1. **Add New → Project** desde el mismo repo. **Root Directory**: `frontend` (Vercel detecta Vite: build `npm run build`, salida `dist`).
2. **Environment Variables**: `VITE_API_BASE_URL=https://<api>.onrender.com` (es pública, no es secreto).
3. Deploy. Copiar la URL (`https://<proyecto>.vercel.app`) y ponerla en `APP_BASE_URL` de Render; redeploy de Render para que tome el CORS.
4. **Verificar** en el navegador:
   - Ingreso del administrador y menú.
   - Recargar estando en `/panel/recursos`: carga la app (no un 404 de Vercel, gracias a `vercel.json`).
   - Desde la consola de otra página (por ejemplo `https://example.com`), un `fetch` a la API no recibe CORS.

## 4. Antes de la demo

- Abrir `/actuator/health` de Render 5 minutos antes para despertar la API.
- Correr los E2E contra el despliegue:
  ```powershell
  cd frontend; $env:E2E_BASE_URL = 'https://<proyecto>.vercel.app'; $env:E2E_ADMIN_EMAIL = '<admin>'; $env:E2E_ADMIN_PASSWORD = '<contraseña>'; npm run e2e
  ```
- Si C-18 entra: cargar `APP_TAREAS_TOKEN` en Render y crear el cron en cron-job.org (cada 15 min, `POST /api/tareas/recordatorios` con el header `X-Tarea-Token`).
- **11:00**: decidir nube o plan B y anotarlo en `openspec/changes/c-19-cierre-mvp-e2e-y-despliegue/evidencia/decision-1100.md`.

## Rollback

Render y Vercel permiten volver al deploy anterior desde su panel. La base no se toca: Flyway solo agrega migraciones.
