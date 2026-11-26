# Revisión de accesibilidad y mobile-first — C-17 (tarea 6.7)

- **Fecha:** 2026-11-26
- **Cómo:** Playwright (Chromium) contra `vite` en dev (puerto 5179), con la API pública simulada con `page.route` en el navegador (mismo contrato que `TurnoPublicoDetalleRespuesta`). No usa el backend real (ver `verificacion-dev.md`).
- **Viewports:** 390×844 (todas las pantallas) y 1440×900 (reprogramar).
- **Capturas:** [`capturas/`](capturas/)

| Pantalla | Captura | Desborde horizontal a 390 px | Foco al cargar | Tab |
|---|---|---|---|---|
| `/turno/:token/confirmar` | `confirmar-390.png` | No | `h2` "Revisá los datos de tu turno" | 1.º Tab → "Confirmar mi turno" (anillo visible, alto 44 px) |
| Confirmado (Enter con teclado) | `confirmado-390.png` | No | `h2` "¡Turno confirmado!" | — |
| `/turno/:token/cancelar` | `cancelar-390.png` | No | `h2` "¿Querés cancelar tu turno?" | "Confirmar cancelación" → "Prefiero reprogramarlo" (anillo visible, 44 px cada uno) |
| Cancelar fuera de término | `cancelar-fuera-de-termino-390.png` | No | `h2` "No podés cancelar online" | Sin acciones; alerta `role="alert"` con el contacto |
| `/turno/:token/reprogramar` | `reprogramar-390.png`, `reprogramar-1440.png` | No | `h2` "Elegí un nuevo día y horario" | Recorre los días en orden (anillo visible, alto ≥ 44 px); Enter elige el día y carga los horarios |
| Reprogramar con 409 | `reprogramar-409-390.png` | No | Vuelve a `h2` "Elegí un nuevo día y horario" | Alerta `role="alert"` con el motivo del backend |

## Criterios WCAG 2.1 AA revisados

- **1.4.3 Contraste:** se reutilizan las clases ya revisadas en C-16 (`slate-900` sobre blanco, `red-800` sobre `red-50`, botón primario blanco sobre `sky-800`).
- **2.1.1 Teclado:** todas las acciones se hacen con Tab + Enter. Lo cubren los tests (`CancelarTurnoPage.test.tsx`, "con teclado se llega al botón...") y esta revisión.
- **2.4.3 Orden del foco:** el foco pasa al `h2` de cada paso (`TituloDePaso`, con `key` por paso para que se vuelva a montar) y Tab sigue el orden visual.
- **2.4.7 Foco visible:** `focus-visible:ring-2 ring-sky-700` en botones, chips y enlaces (se ve `box-shadow` en todos los elementos recorridos).
- **2.5.5 Tamaño del objetivo:** botones y enlaces de 44 px de alto (`min-h-11`).
- **4.1.3 Mensajes de estado:** los errores y el plazo vencido usan `role="alert"`, y la carga usa `role="status"`.

Consola del navegador: sin errores, salvo el 409 que se simuló a propósito.
