# Prueba visual de C-11 — agenda interna (grupos 1 a 5)

- **Fecha:** 2026-11-26, rama `feat/C-11-agenda-interna-vistas`.
- **Herramienta:** Playwright MCP (Chromium) contra `vite` local en el puerto 5199. El servidor se detuvo al terminar.
- **API:** simulada en el navegador por intercepción de red (`context.route('**/api/**')`), con datos **ficticios** (nombres genéricos, sin DNI ni contacto). La app no tiene ningún modo demo ni datos falsos: la simulación vive solo en el script de prueba, fuera del repositorio.
- **Hora fija:** `page.clock.setFixedTime('2026-12-01T15:00:00Z')`, es decir, martes 1/12 a las 12:00 en el consultorio.
- **Sesión:** usuarios ficticios de recepción y de profesional (Laura Ejemplo, id 10) cargados en `sessionStorage`.

## Capturas (`capturas/`)

| Archivo | Rol | Ancho | Escenario |
|---|---|---|---|
| `recepcion-dia-390.png` | recepción | 390 | Vista diaria de hoy: seis turnos, los seis estados, sobreturno, turno de 23:30 local (02:30Z) |
| `recepcion-semana-390.png` | recepción | 390 | Vista semanal con los días apilados |
| `recepcion-dia-1440.png` | recepción | 1440 | Vista diaria en dos columnas |
| `recepcion-semana-1440.png` | recepción | 1440 | Vista semanal en siete columnas, hoy marcado |
| `recepcion-filtros-1440.png` | recepción | 1440 | Filtros desde la URL: `sillonId=2&estado=pendiente` |
| `profesional-mi-agenda-1440.png` | profesional | 1440 | "Mi agenda", sin filtro de profesional, solo sus turnos |
| `profesional-mi-agenda-390.png` | profesional | 390 | Lo mismo en el celular |
| `recepcion-dia-vacio-390.png` | recepción | 390 | "No hay turnos para este día" |
| `recepcion-error-390.png` | recepción | 390 | API con 500: "No se pudo cargar la agenda." y "Reintentar" |
| `recepcion-foco-teclado-390.png` | recepción | 390 | Foco de teclado sobre el selector de estado |

## Resultados

- **Sin desplazamiento horizontal:** `scrollWidth` ≤ ancho visible en los diez escenarios (390 → 375/390 con barra de desplazamiento; 1440 → 1425/1440).
- **Teclado:** el recorrido con Tab sigue el orden visual: logo, Panel, Agenda, Recursos, Cerrar sesión, Día, Semana, Día anterior, Hoy, Día siguiente, Profesional, Sillón, Estado. Los 13 controles muestran anillo de foco.
- **Consola:** sin errores ni advertencias de la app. Los únicos errores son los dos `500` provocados a propósito en el escenario de error: el pedido original y el reintento automático de TanStack Query.
- **Zona horaria:** el turno de `2026-12-02T02:30:00Z` aparece como "23:30–00:00" en el martes 1/12, tanto en la vista diaria como en la semanal.

## Hallazgo corregido durante la prueba

- **[mayor] Vista semanal a 1440 px:** en columnas de unos 150 px, la grilla de rótulos y valores de cada tarjeta se apretaba, partía los nombres en dos líneas y "Extracción" se salía del borde de la tarjeta.
  - *Corrección:* `TarjetaDeTurno` tiene una variante `compacta`, que usa la vista semanal. Los valores van apilados y los rótulos quedan solo para lectores de pantalla (`sr-only`), así que la lista de definiciones sigue siendo semántica.
  - *Test:* `TarjetaDeTurno.test.tsx`.
  - *Nueva medición:* 0 desbordes en 390 y 1440 (se compara el `scrollWidth` de cada `dd` y su borde derecho con el de la tarjeta). Las capturas de la semana son las posteriores a la corrección.

## Observación (no es un defecto de la UI)

- El orden de los turnos a igual hora (por profesional y después por sillón) es responsabilidad del backend (spec "Orden de los turnos"). La UI respeta el orden recibido. Se verifica en el grupo 6 con `@DataJpaTest`.
