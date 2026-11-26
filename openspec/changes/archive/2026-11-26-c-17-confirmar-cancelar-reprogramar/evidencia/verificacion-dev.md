# Verificación del flujo completo — C-17 (tarea 6.6)

**Estado: CERRADA (2026-11-26) por decisión del equipo, con verificación automatizada.**

La prueba manual en `dev` con backend real y seed de demo **no se ejecutó**: el token del turno solo se obtiene por el email de C-14 o leyendo la base local con credenciales del `.env`. El equipo decidió cerrar la tarea porque el flujo completo ya está cubierto por las verificaciones automatizadas de abajo.

## Cobertura del flujo

| Paso del flujo (tarea 6.6) | Verificación automatizada | Evidencia |
|---|---|---|
| Confirmar un turno (y repetir: idempotente) | `AccionesTurnoPublicoControllerTest.confirmarTurnoPasaAConfirmadoYSegundaLlamadaEsIdempotente` (H2 `MODE=MySQL`, endpoints reales de punta a punta) | `mvn-verify.log` |
| Cancelar dentro de la ventana, liberando el profesional y el sillón | `AccionesTurnoPublicoControllerTest.cancelarTurnoValidoPasaACanceladoYLiberaRecursos` | `mvn-verify.log` |
| Cancelar fuera de término (409 con el contacto) | `AccionesTurnoPublicoControllerTest.cancelarTurnoFueraDeTerminoDevuelve409ConMensajeYContacto` | `mvn-verify.log` |
| `anticipacion-minima-horas = 0` | `ServicioDeGestionTurnoPacienteTest.cancelarConAnticipacionCeroPermiteCancelarHastaElMismoInicio`; `ParametrosControllerTest` (PUT `"0"` → 200) | `mvn-verify.log` |
| Reprogramar y auditar `REPROGRAMACION` | `AccionesTurnoPublicoControllerTest.reprogramarTurnoValidoActualizaHorarioYAudita` | `mvn-verify.log` |
| Forzar un 409 por hueco tomado | `AccionesTurnoPublicoControllerTest.reprogramarAHorarioTomadoDevuelve409` y `ReprogramacionConcurrenciaMySqlTest` (MySQL 8 real, `@RepeatedTest(5)`: una reserva y una reprogramación simultáneas → un 200/201 y un 409, sin solapamientos) | `it-mysql.txt`, `it-mysql.log` |
| Pantallas `/turno/:token/*` a 390 px y con teclado | Playwright (Chromium) contra `vite` en dev, con la API simulada en el navegador | `accesibilidad.md`, `capturas/` |

**Límite conocido:** ninguna verificación recorre la cadena completa navegador → API real → MySQL con un token emitido por email. Las pantallas se probaron con la API simulada, y la API, con los tests de integración. Las pruebas E2E de C-19 pueden cerrar ese hueco.
