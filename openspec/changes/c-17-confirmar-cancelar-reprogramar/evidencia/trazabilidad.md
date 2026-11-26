# Trazabilidad requisito → tests — C-17

Backend: `backend/src/test/java/ar/dentaflow/...`. Frontend: `frontend/src/features/turno-paciente/` y `frontend/src/routes/`.

## `parametros-consultorio`: anticipación mínima 0..168

| Escenario | Tests |
|---|---|
| Valor válido / límites 0 y 168 | `ClaveParametroTest`, `ParametroTest`, `ServicioDeParametrosTest` (0 → `Duration.ZERO`), `ParametrosControllerTest` (PUT `"0"` → 200) |
| Fuera de rango (-1, -5, 169) / no entero | `ClaveParametroTest`, `ParametrosControllerTest` (400 en español, sin cambiar el valor) |

## `gestion-turnos-paciente`

| Requisito / escenario | Tests backend | Tests frontend |
|---|---|---|
| Consulta: vigente dentro de ventana | `ServicioDeGestionTurnoPacienteTest.consultarTurnoVigenteDentroDeAnticipacion...`, `AccionesTurnoPublicoControllerTest.consultarTurnoValidoDevuelveDatosSinExponerDatosSensibles` | `ConfirmarTurnoPage.test` "muestra los datos del turno..." |
| Consulta: fuera de ventana (`puedeModificar=false`) | `...consultarTurnoVigenteFueraDeAnticipacion...` | `CancelarTurnoPage.test` / `ReprogramarTurnoPage.test` "fuera de término..." |
| Consulta: cancelado o vencido | `...consultarPorTokenCanceladoOVencido...` | `ConfirmarTurnoPage.test` "turno cancelado/vencido..." |
| Token inválido → 404 | `...consultarPorTokenInexistente...`, `...consultarTokenInvalidoDevuelve404` | `accionesTurnoApi.test` (404 sin reintento), `ConfirmarTurnoPage.test` "enlace inválido" |
| Confirmación exitosa con `Actor.paciente()` | `...confirmarPendienteVigenteLlamaAServicioDeTurnosConActorPaciente...`, `...confirmarTurnoPasaAConfirmadoYSegundaLlamadaEsIdempotente` | `ConfirmarTurnoPage.test` "confirma con un toque..." |
| Confirmación idempotente | `...confirmarTurnoYaConfirmadoEsIdempotente...` | `ConfirmarTurnoPage.test` "ya confirmado..." |
| Confirmar cancelado o vencido → 409 | `...confirmarCanceladoOVencidoRechazaConConflictoException` | `ConfirmarTurnoPage.test` "si la confirmación falla..." |
| Cancelación dentro de término (libera recursos) | `...cancelarTurnoDentroDeAnticipacion...`, `...cancelarTurnoValidoPasaACanceladoYLiberaRecursos` | `CancelarTurnoPage.test` "cancela, informa el éxito..." |
| Cancelación con anticipación 0 h | `...cancelarConAnticipacionCeroPermiteCancelarHastaElMismoInicio` | — |
| Cancelación tardía → 409 con contacto | `...cancelarFueraDeTerminoLanzaCancelacionFueraDeTermino...`, `...cancelarTurnoFueraDeTerminoDevuelve409ConMensajeYContacto` | `CancelarTurnoPage.test` "fuera de término..." y "si el backend rechaza..." |
| Cancelar ya cancelado → 409 | `AccionesTurnoPublicoControllerTest.cancelarTurnoYaCanceladoDevuelve409` | — |
| Huecos con `turnoAExcluir` | `...consultarHuecosParaReprogramarLlamaAMotorConTurnoAExcluir`, `...listarHuecosExcluyeElPropioTurnoYDevuelve200` | `accionesTurnoApi.test` "consulta huecos de un día..." |
| Huecos fuera de ventana → rechazo | `...reprogramarFueraDeVentanaLanza...` (misma validación) | `ReprogramarTurnoPage.test` "fuera de término..." (no pide huecos) |
| Reprogramación exitosa (`REPROGRAMACION`, paciente) | `...reprogramarDentroDeVentanaBuscaCandidatos...`, `...reprogramarTurnoValidoActualizaHorarioYAudita` | `ReprogramarTurnoPage.test` "elige día y horario..." |
| Horario tomado → 409 | `...reprogramarSinCandidatosDisponibles...`, `...reprogramarAHorarioTomadoDevuelve409` | `ReprogramarTurnoPage.test` "ante 409 vuelve a la elección...", `accionesTurnoApi.test` |
| Concurrencia reserva contra reprogramación (MySQL 8) | `ReprogramacionConcurrenciaMySqlTest` (`@RepeatedTest(5)`, `it-mysql.txt`) | — |
| Límite por IP (Q5) → 429 | `...rateLimiterPorIpResponde429AlExcederLimiteEnAcciones`, `...consultasDeLecturaNoConsumenElLimiteDeAcciones` | `accionesTurnoApi.test` (429 → `demasiados-pedidos`) |
| Privacidad en logs (D7) | `LogsDeAccionesPacienteTest` | — |
| Pantallas públicas sin sesión | — | `rutas.test` "/turno/:token/{confirmar,cancelar,reprogramar} es pública" |
| Mobile-first 390 px + teclado | — | Tests de foco en cada página + `accesibilidad.md` |
