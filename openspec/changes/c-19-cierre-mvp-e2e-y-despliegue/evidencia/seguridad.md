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

## Cierre de la revisión (tarea 8.1) — `main` `d9bf158`, 2026-11-26 11:10

Esta revisión es posterior al merge de C-16 y se hizo con el plan B local (`evidencia/decision-1100.md`).

| Ítem | Estado final | Notas |
|---|---|---|
| Rutas públicas vs. `autorizacion-por-rol` | ✅ Revisado | `ConfiguracionSeguridad` deja abiertas solo estas rutas: `/actuator/health` (GET), `/api/auth/login` (POST), la documentación OpenAPI, `OPTIONS` y `/api/publico/**`. Bajo `/api/publico/**` están aviso de privacidad (C-15), catálogo, contacto, huecos (C-10) y reservas (C-16). Todo lo demás exige JWT. |
| Cada endpoint con rol verificado en backend | ✅ Revisado | Los endpoints internos usan guardas de rol y tienen tests de 401 y 403. C-12 (PR #31, todavía no está en `main`) agrega `@AdministradorORecepcion` en la carga manual, en el sobreturno y en la búsqueda por DNI. |
| HTTPS y HSTS | ⏳ Sin validar en URL pública | Está en el código (C-15) y el perfil `prod` exige TLS hacia MySQL. No se valida en una URL pública porque no hubo despliegue: es riesgo aceptado para la entrega. |
| CORS | ⏳ Sin validar en navegador contra Render | `APP_BASE_URL` restringe el origen. La tarea 3.5 queda pendiente porque no hay despliegue. |
| Validación de input en `/api/publico/**` | ✅ Revisado | `POST /api/publico/reservas` valida el cuerpo con `@Valid` y limita los pedidos por IP con 429 (C-16). `GET /api/publico/huecos` valida los parámetros en `MotorDeDisponibilidad`, que responde 400 ante un rango inválido o demasiado largo. |
| Logs sin datos personales | ✅ Sin cambios | Siguen vigentes los tests de C-09 y C-15. C-12 no escribe el DNI en logs y lo recibe en el cuerpo, nunca en la URL. |

## Hallazgos y mejoras documentadas

- *(Sin hallazgos bloqueantes en `main` `d9bf158`.)*
- **Fuera de esta revisión**: C-17 (`f07ba45`) y C-12 (`e0b4573`) entraron a `main` después de la base revisada. Sus rutas no se revisaron en este cierre: queda para el próximo pase de seguridad. C-12 solo agrega rutas internas, protegidas con `@AdministradorORecepcion` y con tests de 401 y 403.
- **Riesgo aceptado (entrega)**: HSTS y CORS no se validaron contra una URL pública porque se usa el plan B local. Hay que verificarlos cuando se despliegue (tareas 3.3 y 3.5).
- **Mejora documentada (D3)**: el usuario de MySQL en Aiven tiene permisos de DDL para que Flyway migre al arrancar; separar un usuario sin `UPDATE`/`DELETE` sobre `turno_historial` (pendiente de `C-08`) queda como mejora post-MVP.
