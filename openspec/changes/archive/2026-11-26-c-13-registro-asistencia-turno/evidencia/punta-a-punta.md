# Prueba de punta a punta de C-13 contra el backend real (tarea 5.2)

- **Fecha:** 2026-11-26, rama `feat/C-13-registro-asistencia-turno` (con `origin/main` mergeado).
- **Backend real:** `./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true -Dspring-boot.run.profiles=test` en el puerto `8080`, con base H2 en archivo `backend/target/e2e-asistencia` (`MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE`), `APP_SEED_DEMO=true`, `APP_SEED_ADMIN=true` y credenciales ficticias por variables de entorno del proceso (sin `.env` ni MySQL local).
- **Frontend real:** `vite` en el puerto `5199` con `VITE_API_BASE_URL=http://localhost:8080`, sin `simularApi` ni intercepción de red. Ingreso real por `/ingresar`.
- **Turnos ficticios cargados en H2:**
  - Turno `1`: Ana Prueba (`08:30–09:00`, `confirmado`, ya iniciado).
  - Turno `2`: Bruno Muestra (`15:00–15:30`, `confirmado`, futuro).
- Al terminar se detuvieron backend y frontend y se borraron `backend/target/e2e-asistencia*`, `backend/target/e2e-seed.sql` y `.playwright-mcp/`.

## Resultados

| Verificación | Resultado |
|---|---|
| Ingreso real por `/ingresar` y carga de `/panel/agenda?fecha=2026-11-26` | 200 OK, ambos turnos visibles |
| Turno futuro (Bruno Muestra, 15:00–15:30) | `"Asistió"` y `"Ausente"` deshabilitados con `"Disponible desde las 15:00"`; `"Ausente con aviso"` habilitado |
| Marcar `"Asistió"` en Ana Prueba (`POST /api/turnos/1/asistencia`) | 200 OK, estado actualizado, botón con `aria-pressed="true"`, anuncio `"Asistencia registrada"` |
| Corregir a `"Ausente"` en Ana Prueba | 200 OK, estado pasa a `ausente`, botón `"Ausente"` presionado |
| Repetir `"Ausente"` en Ana Prueba (idempotencia) | 200 OK, sin fila duplicada en `turno_historial` |
| Marcar `"Ausente con aviso"` en turno futuro (Bruno Muestra) | 200 OK, estado pasa a `ausente_con_aviso` |
| Auditoría confirmada en `turno_historial` (3 filas exactas) | Fila 1: `turno_id=1`, `usuario_id=1`, `cambio_estado`, `confirmado -> asistio`; Fila 2: `turno_id=1`, `usuario_id=1`, `cambio_estado`, `asistio -> ausente`; Fila 3: `turno_id=2`, `usuario_id=1`, `cambio_estado`, `confirmado -> ausente_con_aviso` |
| Errores de consola y pedidos fallidos | 0 y 0 |
| Cambios necesarios en `frontend/src/features/agenda/api.ts` | **Ninguno**: el contrato simulado coincidió con el backend real |

## Captura

- `capturas/e2e-real-asistencia-1440.png`: agenda real tras registrar y corregir asistencia de ambos turnos.
