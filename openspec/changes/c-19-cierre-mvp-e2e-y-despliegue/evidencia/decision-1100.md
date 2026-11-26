# Decisión de las 11:00 — C-19 (tarea 8.2, design D8)

- **Fecha**: 2026-11-26, 11:00 · **Decidió**: Bruno · **Base**: `main` `d9bf158` (incluye C-16)

## Decisión

Se presenta el **plan B local** (`scripts/demo-local.ps1` + `docs/demo.md`). No se sigue depurando la nube.

## Motivo

- A las 11:00 el entorno en la nube no estaba armado. Falta crear el MySQL en Aiven (tarea 3.2), el servicio web en Render (3.3) y el proyecto en Vercel con sus variables (3.4). Son tareas del equipo con cuentas propias y no se hicieron.
- El plan B ya estaba ensayado y llega a `UP` en unos 15 s (`evidencia/ensayo-plan-b.md`).
- El código de despliegue queda en el repo y probado: perfil `prod`, `backend/Dockerfile` (DD-13), `frontend/vercel.json` y la espera del arranque en frío en el cliente API. El equipo puede desplegar después de la entrega sin cambiar código.

## Consecuencias

- La demo corre en la máquina del equipo que presenta. Hace falta MySQL 8 nativo, JDK 21, Node 22 y los `.env` locales (ver `docs/demo.md` §1).
- No hay URL pública. Las tareas 3.2, 3.3, 3.5, 6.6 y 7.1 quedan pendientes y fuera de la entrega.
- En el guion de `docs/demo.md` §3 hay pasos que dependen de changes que no están en `main` y se tachan antes del ensayo: C-17 (confirmar, cancelar y reprogramar por enlace, pasos 4 a 6) y C-07 (no tiene pantalla de parámetros, paso 11). C-16 sí está en `main`, así que la reserva online (pasos 2, 3 y 12) se muestra.
