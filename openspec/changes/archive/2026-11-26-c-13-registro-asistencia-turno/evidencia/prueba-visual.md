# Prueba visual de C-13 — registro de asistencia en la agenda (tarea 5.1)

- **Fecha:** 2026-11-26, rama `feat/C-13-registro-asistencia-turno`.
- **Herramienta:** Playwright MCP (Chromium) contra `vite` local en el puerto 5199. El servidor y `.playwright-mcp/` se limpiaron al terminar.
- **API:** simulada en el navegador por intercepción de red (`context.route('**/api/**')`) con datos ficticios (sin DNI, teléfono, email real ni token).
- **Hora fija:** `page.clock.setFixedTime('2026-12-01T15:00:00Z')` (martes 1/12 a las 12:00 en `America/Argentina/Buenos_Aires`).
- **Sesión:** usuario ficticio de recepción cargado en `sessionStorage` (`dentaflow.sesion`).

## Capturas (`capturas/`)

| Archivo | Ancho | Escenario |
|---|---|---|
| `asistencia-dia-390.png` | 390×844 | Agenda diaria: turno `asistio` (botón "Asistió" presionado), turno `confirmado` iniciado (3 acciones habilitadas) y turno futuro 14:00 ("Asistió"/"Ausente" deshabilitados con "Disponible desde las 14:00", "Ausente con aviso" habilitado) |
| `asistencia-dia-1440.png` | 1440×900 | Mismo escenario en escritorio (grilla de dos columnas) |
| `asistencia-error-409-390.png` | 390×844 | Al tocar "Asistió" con respuesta 409 (`application/problem+json`), la alerta aparece en la tarjeta sin alterar el estado |
| `asistencia-error-409-1440.png` | 1440×900 | Mismo error 409 en escritorio |

## Resultados

- **Sin desborde horizontal:** `scrollWidth <= clientWidth` en las cuatro capturas (390 → 375/375; 1440 → 1440/1440).
- **Estados de controles verificados:** `aria-pressed="true"` en el estado actual (`asistio`), botones `"Asistió"` y `"Ausente"` deshabilitados en el turno futuro (`14:00`) con `"Ausente con aviso"` habilitado.
- **Error 409:** muestra el `detail` de la API (`"La asistencia se registra desde el inicio del turno"`) en `role="alert"` dentro de la tarjeta y conserva el estado `confirmado`.
- **Consola:** 0 errores de la aplicación (solo el 409 provocado intencionalmente).
