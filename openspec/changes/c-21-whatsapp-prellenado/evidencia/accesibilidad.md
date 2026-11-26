# Revisión de accesibilidad y mobile-first — C-21 (tarea 6.2)

- **Fecha:** 2026-11-26
- **Cómo:** Playwright (Chromium) contra `vite` en dev (puerto 5179), con sesión de recepción ficticia en `sessionStorage` y la API simulada con `page.route` en el navegador (mismos contratos que `GET /api/agenda/turnos` y `GET /api/turnos/{id}/whatsapp`). Datos ficticios; el token del mensaje es `TOKEN-FICTICIO`.
- **Capturas:** [`capturas/`](capturas/)

| Caso | Captura | Resultado |
|---|---|---|
| Agenda a 390 px (recepción) | `agenda-390.png` | "WhatsApp" aparece en el turno confirmado y en el pendiente; no aparece en el turno `asistio` |
| Diálogo con enlace, 390 px | `dialogo-con-enlace-390.png` | Sin desborde horizontal; el mensaje largo hace saltos de línea; "Abrir WhatsApp" mide 44 px de alto y tiene anillo de foco |
| Diálogo sin teléfono válido, 390 px | `dialogo-sin-telefono-390.png` | Aviso `role="alert"` con el texto explicativo; sin enlace; el mensaje queda visible para copiarlo |
| Diálogo con enlace, 1440 px | `dialogo-con-enlace-1440.png` | Diálogo centrado de ancho máximo `max-w-md` |

## Teclado

- Enter sobre "WhatsApp" abre el diálogo y el foco pasa a "Cerrar", el primer control (mismo patrón que el historial de C-13).
- Shift+Tab desde "Cerrar" va a "Abrir WhatsApp", que muestra un anillo de foco visible.
- **Escape** y **"Cerrar"** cierran el diálogo y el foco **vuelve al botón "WhatsApp"**.
  - Antes de la corrección de `Dialogo` quedaba en `BODY` en desarrollo (StrictMode). Ver `frontend-red.txt`.

## Criterios WCAG 2.1 AA

- **1.4.3 Contraste:** se reutilizan `CLASES_BOTON_PRIMARIO`, `CLASES_BOTON_SECUNDARIO` y la alerta `red-800` sobre `red-50`, ya revisadas en C-05 y C-16.
- **2.1.1 Teclado y 2.4.3 Orden del foco:** todo el recorrido se hace sin mouse.
- **2.4.4 Propósito de los enlaces:** el enlace dice "Abrir WhatsApp" y se abre en una pestaña nueva (`target="_blank"`, `rel="noopener noreferrer"`).
- **4.1.2 Nombre, función y valor:** el `<dialog>` toma su nombre del título ("WhatsApp — <paciente> (<hora>)").
- **4.1.3 Mensajes de estado:** el aviso de teléfono inválido y los errores usan `role="alert"`.

Consola del navegador: sin errores.
