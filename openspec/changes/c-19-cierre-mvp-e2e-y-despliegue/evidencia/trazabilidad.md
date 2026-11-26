# Trazabilidad escenario → prueba — C-19 (tarea 8.4)

- **Fecha**: 2026-11-26 · **Base**: `main` `d9bf158`
- `openspec validate c-19-cierre-mvp-e2e-y-despliegue --strict` → `Change 'c-19-cierre-mvp-e2e-y-despliegue' is valid`

| Spec | Escenario | Prueba o evidencia | Estado |
|---|---|---|---|
| configuracion-entorno | Arranque en prod sin seed de demo | `backend/src/test/java/ar/dentaflow/configuracion/PerfilProdTest.java` | ✅ Cubierto (tarea 2.1) |
| configuracion-entorno | Demo con datos ficticios | `PerfilProdTest` (`APP_SEED_DEMO=true`, idempotente) | ✅ Cubierto (tarea 2.1) |
| configuracion-entorno | Variable obligatoria faltante | `PerfilProdTest` (tarea 2.2: el mensaje nombra la variable sin mostrar valores) | ✅ Cubierto |
| configuracion-entorno | URL de conexión de producción | `.env.example` con `sslMode=REQUIRED` y perfil `prod` | ✅ Configuración; ⏳ sin validar contra Aiven (3.2) |
| despliegue-publico | Ingreso al panel desplegado | — | ⏳ Pendiente: no hay despliegue (`decision-1100.md`) |
| despliegue-publico | Salud de la API desplegada | — | ⏳ Pendiente (3.3) |
| despliegue-publico | Enlace de confirmación abierto directo | `frontend/src/despliegue/vercel.test.ts` | ✅ Cubierto en la configuración; ⏳ sin validar en Vercel |
| despliegue-publico | Bundle del frontend | `evidencia/seguridad.md` (búsqueda en `dist/` y `git grep`) | ✅ Cubierto (3.7) |
| despliegue-publico | Llamada desde el frontend desplegado | — | ⏳ Pendiente (3.5) |
| despliegue-publico | Llamada desde otro origen | — | ⏳ Pendiente (3.5) |
| despliegue-publico | Primera llamada con la API dormida | `frontend/src/lib/apiClient.test.ts` | ✅ Cubierto (3.6) |
| despliegue-publico | Ensayo del plan B | `evidencia/ensayo-plan-b.md` (15 s hasta `UP`) y `decision-1100.md` | ✅ Cubierto (4.1) |
| despliegue-publico | Suite E2E local | `frontend/e2e/panel-ingreso.spec.ts` (ingreso y menú según rol); job `e2e` en CI | ⏳ Parcial: faltan los E2E de reserva, confirmación y reprogramación (6.2 a 6.4; C-16 y C-17 ya están en `main` desde las 11:15, pero no hubo tiempo de escribir los E2E) |
| despliegue-publico | Suite E2E contra el despliegue | — | ⏳ Pendiente (6.6, sin despliegue) |
| despliegue-publico | Dos pacientes eligen el mismo hueco a la vez | `backend/src/test/java/ar/dentaflow/presentacion/publico/ReservaPublicaConcurrenciaMySqlTest.java` (C-16, `@Tag("mysql")`) y `aplicacion/agenda/ReservaSimultaneaDeHuecosMySqlTest.java` (C-10) | ✅ Cubierto por C-16 y C-10 en el perfil `it-mysql` (tarea 6.5); C-19 no volvió a correr la suite |

**Resumen** (15 escenarios):
- 7 cubiertos;
- 2 cubiertos solo en la configuración, sin validar en la nube;
- 6 pendientes, que dependen del despliegue en la nube o de los E2E del bloque B. La suite E2E local cuenta como parcial. La entrega usa el plan B local.
