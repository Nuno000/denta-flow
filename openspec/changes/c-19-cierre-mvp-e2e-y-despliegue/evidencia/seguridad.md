# Revisión de seguridad — C-19 (design D9 y tarea 3.7)

- **Fecha**: 2026-11-26 · **Rama**: `feat/C-19-cierre-mvp-e2e-y-despliegue` (base `main` `34f368e`)

## Búsqueda de secretos en build y repo (tarea 3.7)

| Comprobación | Comando | Resultado |
|---|---|---|
| Bundle de producción | `Select-String -Path "frontend/dist/assets/*.js" -Pattern "AUTH_SECRET\|SPRING_DATASOURCE_PASSWORD\|SPRING_MAIL_PASSWORD\|APP_ADMIN_PASSWORD\|APP_TAREAS_TOKEN\|jdbc:mysql"` | **0 coincidencias** |
| Código fuente del repo | `git grep` por nombres de variables secretas fuera de docs/tests/plantillas | Solo referencias a nombres de variables en `SeedAdministradorInicial`, `ConfiguracionTokens` y `PropiedadesAuth`; ningún valor secreto embebido |

## Lista de verificación (design D9)

| Ítem | Estado | Notas / Evidencia |
|---|---|---|
| Secretos solo en variables de entorno | ✅ Verificado (local) | `.env` ignorado por Git; `.env.example` con plantillas no reales; `frontend/dist/` sin secretos (tarea 3.7). Pendiente verificar secretos cargados en Render/Vercel (tareas 3.2/3.3/3.4). |
| Rutas públicas vs. `autorizacion-por-rol` | ⏳ Parcial | Cubierto para `C-04`, `C-05`, `C-07`, `C-15` (`/api/publico/aviso-privacidad`). Pendiente revisar rutas públicas de `C-10`, `C-16`, `C-17` cuando entren a `main`. |
| Cada endpoint con rol verificado en backend | ⏳ Parcial | Verificado por tests de `C-04`, `C-05`, `C-07`, `C-15` (anonimización solo `ADMINISTRADOR`). Revisión final al cerrar Bloque B. |
| HTTPS y HSTS | ✅ En código | `server.forward-headers-strategy: framework` y HSTS configurados en `C-15`; perfil `prod` exige TLS en `SPRING_DATASOURCE_URL` (`sslMode=REQUIRED`). Pendiente validar en URL pública de Render/Vercel. |
| CORS | ⏳ Parcial | Configurado con `APP_BASE_URL`; falta verificación en navegador desde otro origen contra Render (tarea 3.5). |
| Límite de intentos de ingreso | ✅ Verificado | Rate limiting en login (`C-04`, verificado en tests backend y frontend `IngresarPage.test.tsx`). |
| Logs sin datos personales | ✅ Verificado (actual) | `SinValoresDeLaBaseEnLogsTest` y `SinDatosPersonalesEnLogsTest` (`C-09`/`C-15`), `SqlExceptionHelper` en `OFF`. Re-verificar tras `C-14`/`C-16`/`C-17`. |
| Validación de input en `/api/publico/**` | ⏳ Parcial | DTOs con Bean Validation en endpoints actuales; pendiente cubrir `/api/publico/huecos` (`C-10`) y reserva/confirmación/cancelación (`C-16`/`C-17`). |

## Hallazgos y mejoras documentadas

- *(Sin hallazgos bloqueantes en el corte actual de `main` `34f368e`.)*
- **Mejora documentada (D3)**: el usuario de MySQL en Aiven tiene permisos de DDL para que Flyway migre al arrancar; separar un usuario sin `UPDATE`/`DELETE` sobre `turno_historial` (pendiente de `C-08`) queda como mejora post-MVP.
