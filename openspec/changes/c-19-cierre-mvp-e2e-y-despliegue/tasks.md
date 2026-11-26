# Tasks — C-19 `cierre-mvp-e2e-y-despliegue`

> **Gobernanza MEDIA**, con **design D1 aprobado el 2026-11-25** (opción A: Dockerfile solo para el build remoto de Render; excepción registrada como DD-13 y en `CLAUDE.md`).
>
> TDD donde hay código (perfil `prod`, cliente API, concurrencia). Nada local requiere Docker ni Testcontainers. Las cuentas de Aiven, Render, Vercel y cron-job.org y la carga de secretos las hace el equipo; el agente no escribe credenciales en ningún lado. Evidencia en `openspec/changes/c-19-cierre-mvp-e2e-y-despliegue/evidencia/` (sin URLs con credenciales ni secretos).
>
> **Bloque A** se puede hacer ya. **Bloque B** espera C-14..C-17 en `main`. **Hito 11:00 del 26/11**: nube o plan B.

## 1. Línea base

- [ ] 1.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm test` y `npm run build` y anotar los resultados en `evidencia/linea-base.txt`

## 2. Bloque A — Perfil `prod` (spec `configuracion-entorno` modificada)

- [x] 2.1 RED: `PerfilProdTest` (`@SpringBootTest` con `prod` y H2 por propiedades de prueba): sin `APP_SEED_DEMO` no hay profesionales ni sillones; con `true` los hay; reiniciar el contexto no los duplica; no existe el bean del programador de recordatorios. GREEN: `application-prod.yml` (design D2) y, si hace falta, seed idempotente
- [x] 2.2 RED: `PerfilProdVariablesFaltantesTest`: sin `SPRING_DATASOURCE_URL` o sin `AUTH_SECRET` el contexto no arranca y el mensaje nombra la variable sin mostrar valores. GREEN: ajustar
- [ ] 2.3 Agregar a `.env.example` `SPRING_PROFILES_ACTIVE`, `APP_SEED_DEMO` y un ejemplo de URL de Aiven con `sslMode=REQUIRED` (sin host real)

## 3. Bloque A — Hosting (spec `despliegue-publico`)

- [x] 3.1 Agregar `backend/Dockerfile` multietapa y `backend/.dockerignore` (design D1, opción A aprobada). La excepción ya está registrada en `CLAUDE.md` y en DD-13. Verificar que ni el CI ni los scripts locales lo usan
- [ ] 3.2 (Equipo) Crear el servicio MySQL en Aiven, la base `dentaflow` y el usuario de la aplicación; anotar en `docs/despliegue.md` los límites del plan gratuito vistos en la consola. Verificar la conexión con `./mvnw verify -Pit-mysql` apuntando a Aiven (solo una vez, con las variables en la terminal, sin guardarlas)
- [ ] 3.3 (Equipo) Crear el servicio web en Render desde el repo (raíz `backend/`, health check `/actuator/health`) y cargar las variables de `.env.example` con `SPRING_PROFILES_ACTIVE=prod`. Verificar que `/actuator/health` responde `UP` y que Flyway aplicó todas las migraciones
- [x] 3.4 RED: test de Vitest de `vercel.json` (las rutas de la app reescriben a `index.html`). GREEN: `frontend/vercel.json` (design D4). (Equipo) Crear el proyecto en Vercel con `VITE_API_BASE_URL`; poner su URL en `APP_BASE_URL` de Render
- [ ] 3.5 Verificar desde el navegador: ingreso del administrador en la URL pública, recarga en una ruta profunda sin 404, y que otra página de otro origen no recibe CORS. Guardar capturas en `evidencia/`
- [x] 3.6 RED: `apiClient.test.ts`: la primera llamada usa un timeout de 70 s y, mientras espera, el estado de carga muestra el mensaje de arranque en frío; al vencer, el mensaje de reintento. GREEN: cliente API y componente de carga (design D5)
- [ ] 3.7 Buscar en `frontend/dist/` y en el repo (`git grep`) los valores secretos configurados; anotar en `evidencia/seguridad.md` que no aparecen

## 4. Bloque A — Plan B local (spec "Demo local equivalente como plan B")

- [x] 4.1 Escribir `scripts/demo-local.ps1` y `docs/demo.md` (design D8). Ensayarlo en una máquina del equipo desde cero, cronometrando; anotar el tiempo y los problemas en `evidencia/ensayo-plan-b.md`

## 5. Bloque A — Esqueleto E2E (spec "Flujos del corte verificados de punta a punta")

- [x] 5.1 Agregar `@playwright/test`, `frontend/playwright.config.ts` (design D6) y el script `npm run e2e`. Primera prueba: `panel-ingreso.spec.ts` (ingreso del administrador y menú según rol) en verde contra local
- [ ] 5.2 ~~`parametros.spec.ts`~~: no hay pantalla de parámetros en `main` (C-07 es solo backend). En su lugar `panel-ingreso.spec.ts` cubre recursos de demo y la recarga de una ruta profunda. Queda pendiente si se agrega la pantalla
- [x] 5.3 Agregar el job `e2e` a `.github/workflows/ci.yml` con el MySQL del runner (design D6). Verificar en verde en el PR

## 6. Bloque B — E2E del corte (requiere C-14..C-17)

- [ ] 6.1 Acordar con Jessica cómo obtener los enlaces del email en `dev` (design D6) y el texto accesible de los botones de la reserva
- [ ] 6.2 `reserva.spec.ts` (móvil): elegir prestación, profesional o "cualquiera", hueco, datos ficticios, aceptar el aviso (sin aceptarlo no deja seguir), reservar y ver la confirmación
- [ ] 6.3 `confirmar-cancelar.spec.ts`: confirmar por enlace → estado `confirmado`; cancelar por enlace → hueco liberado; enlace vencido o turno cancelado → mensaje correspondiente
- [ ] 6.4 `reprogramar.spec.ts`: reprogramar por enlace respetando la anticipación mínima (con el parámetro en 0 h para la demo, PA-04)
- [ ] 6.5 RED: `ReservaPublicaConcurrenciaMySqlTest` (`@Tag("mysql")`, design D7) falla sin la garantía o pasa con ella; anotar el resultado en `evidencia/it-mysql.txt`
- [ ] 6.6 Correr la suite E2E con `E2E_BASE_URL` del despliegue (saltando los flujos que leen enlaces) y guardar el reporte en `evidencia/`

## 7. Bloque B — Recordatorios (solo si C-18 entra)

- [ ] 7.1 (Equipo) Cargar `APP_TAREAS_TOKEN` en Render y crear el cron en cron-job.org cada 15 minutos con el header. Verificar en el historial del cron un 200 y el resumen de la ejecución

## 8. Cierre (26/11, antes de las 11:00)

- [ ] 8.1 Completar `evidencia/seguridad.md` con la lista de design D9 y los hallazgos
- [ ] 8.2 Redeploy desde `main`, smoke test con `docs/demo.md` y decisión de las 11:00 (nube o plan B) anotada en `evidencia/decision-1100.md`
- [ ] 8.3 Actualizar el `README.md` (puesta en marcha local, despliegue y URLs de la demo) y `CHANGES.md` (estado de C-19)
- [ ] 8.4 Correr `openspec validate c-19-cierre-mvp-e2e-y-despliegue --strict` y anotar la tabla escenario → prueba en `evidencia/trazabilidad.md`
