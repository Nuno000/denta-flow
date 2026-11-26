# Prueba manual en `dev` con backend real — C-17 (tarea 6.6)

**Estado: PENDIENTE.** No se hizo en esta sesión.

**Motivo:** el token de un turno llega solo por el email de confirmación (C-14) o se lee de la tabla `turno`. Las dos vías necesitan las credenciales locales (`.env`, la base `dentaflow` o la casilla del equipo), y el agente no las usó.

**Lo que ya cubre la base real:** `AccionesTurnoPublicoControllerTest` (H2 `MODE=MySQL`) y `ReprogramacionConcurrenciaMySqlTest` (MySQL 8 real) ejercitan los mismos endpoints de punta a punta. La revisión visual y de teclado está en `accesibilidad.md`.

## Pasos para completarla

1. `powershell -ExecutionPolicy Bypass -File scripts\demo-local.ps1`.
2. En el panel, dejar `anticipacion-minima-horas` en `0`.
3. Reservar en `/reservar` con datos ficticios y abrir el enlace del email, o leer `token_acceso` del turno en la base local.
4. Probar `/turno/<token>/confirmar` → "¡Turno confirmado!" y confirmar de nuevo (es idempotente).
5. Probar `/turno/<token>/reprogramar` → elegir un horario → "¡Reprogramaste tu turno!".
6. Forzar un 409: tomar ese mismo horario desde otra reserva y reprogramar a él. Tiene que aparecer el aviso y los horarios tienen que actualizarse.
7. Probar `/turno/<token>/cancelar` → "Cancelaste tu turno". Al volver a abrir cualquier enlace del turno aparece "Este turno fue cancelado".
8. Con `anticipacion-minima-horas=24` y un turno para dentro de menos de 24 h, cancelar tiene que mostrar el aviso con el contacto.
