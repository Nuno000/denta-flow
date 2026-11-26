# Evidencia del bloque A de C-19 (sin C-14..C-17)

- **Fecha**: 2026-11-26, madrugada · **Base**: `main` en `de47e9a` · Windows 11, Temurin 21, Node 22.15.0

## Ciclo TDD

| Tarea | Test | RED | GREEN |
|---|---|---|---|
| 2.1 | `PerfilProdTest` (sin seed / con seed una sola vez) | Sin `application-prod.yml` la URL no se tomaba de `SPRING_DATASOURCE_URL` | `application-prod.yml` |
| 2.2 | `PerfilProdTest` (falta `SPRING_DATASOURCE_URL` / `AUTH_SECRET`) | El arranque no fallaba: levantaba una H2 embebida | Placeholders sin default en `prod` |
| 3.4 | `src/despliegue/vercel.test.ts` | `vercel.json` no existía | `frontend/vercel.json` |
| 3.6 | `apiClient.test.ts` (timeout 70 s) y `AvisoDeArranque.test.tsx` (2) | Sin timeout ni aviso | `TIEMPO_MAXIMO_PEDIDO_MS` + `AvisoDeArranque` global en `Proveedores` |
| 5.1 | `e2e/panel-ingreso.spec.ts` contra el stack local (`scripts/demo-local.ps1`) | — | 2 pasan (credenciales inválidas, móvil y escritorio); 6 omitidos sin `E2E_ADMIN_*` |

**Gotcha**: surefire activa `spring.profiles.active=test` como propiedad de sistema; para probar `prod` hay que pasar el perfil como argumento de línea de comandos (más prioridad), o `test` sigue activo y aporta la base y la clave.

## Resultados

| Verificación | Resultado |
|---|---|
| `npm test` | 17 archivos, 100 tests en verde |
| `npm run lint` / `npm run build` | OK |
| `npx playwright test` (local, Chrome instalado) | 2 pasan, 6 omitidos (faltan `E2E_ADMIN_EMAIL`/`E2E_ADMIN_PASSWORD`) |
| `./mvnw clean verify` | 413 tests en verde (incluye 4 de `PerfilProdTest`); ver `backend.txt` |
| Job `e2e` de CI | se verifica en el PR (#17) |

## Sin verificar acá

- `backend/Dockerfile`: por DD-13 no se construye localmente; se verifica en el primer deploy de Render (tarea 3.3).
- Tareas del equipo: 3.2 (Aiven), 3.3 (Render), 3.5 (verificación en la URL pública), 7.1 (cron).
- 2.3 (`.env.example`): el agente no tiene permiso de lectura sobre ese archivo; hay que agregar a mano `SPRING_PROFILES_ACTIVE=prod`, `APP_SEED_DEMO=false` y el ejemplo de URL de Aiven con `sslMode=REQUIRED`.
