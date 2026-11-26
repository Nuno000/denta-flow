# Prueba visual de C-12 — pantalla "Nuevo turno" (tarea 7.1)

- **Fecha:** 2026-11-26, rama `feat/C-12-carga-manual-sobreturno`.
- **Herramienta:** Playwright MCP (Chromium) contra `vite` local en el puerto 5199. El servidor y `.playwright-mcp/` se limpiaron al terminar.
- **API:** simulada en el navegador por intercepción de red (`context.route('**/api/**')`) con datos ficticios (sin DNI, teléfono ni email reales).
- **Sesión:** usuario ficticio de recepción en `sessionStorage` (`dentaflow.sesion`).

## Capturas (`capturas/`), a 390x844 y 1440x900

| Archivo (`-390` / `-1440`) | Escenario |
|---|---|
| `nuevo-turno-vacio` | Formulario vacío |
| `nuevo-turno-paciente-precargado` | Búsqueda por DNI con paciente encontrado y turno completo (Limpieza, Laura Ejemplo, Sillón 1, 01/12/2026 09:00) |
| `nuevo-turno-conflicto-alternativas` | 409 con motivo y dos alternativas ("Usar martes 1/12 10:00 — Sillón 2", "... 10:15 — Sillón 1") |
| `nuevo-turno-dialogo-sobreturno` | Diálogo "¿Cargar un sobreturno?" con Confirmar sobreturno / Cancelar |

## Resultados

- Sin desborde horizontal: `scrollWidth == clientWidth` a 390 (390/390) y a 1440 (1440/1440).
- Se inspeccionó visualmente la captura del 409 a 390: el aviso, las alternativas, los campos y los botones se leen y caben en pantalla.
- Consola: solo los dos 409 provocados a propósito (uno por viewport); sin otros errores de la aplicación.
