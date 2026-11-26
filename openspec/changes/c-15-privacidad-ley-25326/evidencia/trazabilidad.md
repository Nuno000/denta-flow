# Trazabilidad C-15: escenario → test

Spec `privacidad-datos-personales`.

| Requisito / escenario | Test |
|---|---|
| Aviso público · Consulta sin token | `AvisoPrivacidadControllerTest.consultaSinTokenDevuelveSoloTextoYLeyenda` |
| Aviso público · Cambio visible de inmediato | `AvisoPrivacidadControllerTest.cambioDelTextoVisibleDeInmediato` |
| Aceptación obligatoria · Reserva sin aceptar | `ServicioDePacientesTest.desdeLaReservaOnlineSinConsentimientoSeRechazaSinTocarLaBase` (backend); `AvisoPrivacidad.test.tsx` "sin marcar no envía…" y `avisoPrivacidadApi.test.ts` (UI). El 400 por HTTP lo cubre C-16 con su endpoint de reserva |
| Aceptación obligatoria · Reserva aceptando | `ServicioDePacientesTest.unPacienteNuevoConConsentimientoLoGuarda`; `AvisoPrivacidad.test.tsx` "marcada, envía…" |
| Instante del consentimiento · Paciente nuevo | `ServicioDePacientesTest.unDniNuevoCreaAlPacienteConLaFechaDelReloj` y `unPacienteNuevoConConsentimientoLoGuarda` |
| Instante del consentimiento · Paciente que vuelve a reservar | `ServicioDePacientesTest` (consentimiento que se reemplaza, C-09) y `unDniExistenteConOtroFormatoDevuelveAlMismoPacienteSinAltaNueva` |
| Instante del consentimiento · Reserva que falla por solapamiento | Por diseño de C-09 el paciente se registra en su propia transacción antes del turno: `ServicioDePacientesIntegracionTest` (registro independiente). El flujo completo reserva → conflicto lo cubre C-16 |
| Instante del consentimiento · Registro online sin aceptación | `ServicioDePacientesTest.desdeLaReservaOnlineSinConsentimientoSeRechazaSinTocarLaBase`, `PrivacidadPacienteIntegracionTest.elRegistroOnlineSinAceptacionNoCreaPacienteNiDejaDatosEnElLog` |
| Datos mínimos · Campos del paciente | `PrivacidadPacienteIntegracionTest.elPacienteSoloTieneLasColumnasPermitidas` |
| Anonimización · Exitosa | `PacienteAdminControllerTest.elAdministradorAnonimizaYLosTurnosPasadosQuedan`, `PacienteAnonimizarTest`, `ServicioDeAnonimizacionTest` |
| Anonimización · El DNI original queda libre | `PrivacidadPacienteIntegracionTest.despuesDeLaBajaElMismoDniCreaUnPacienteNuevo`, `RestriccionesPrivacidadPaciente{H2,MySql}Test.aceptaVariosPacientesAnonimizadosSinDni` |
| Anonimización · Con turnos futuros activos | `PacienteAdminControllerTest.conUnTurnoFuturoActivoEsConflictoYNoCambiaNada`, `unTurnoFuturoCanceladoNoImpideLaBaja`, `ServicioDeAnonimizacionTest` |
| Anonimización · Dos veces | `PacienteAdminControllerTest.anonimizarDosVecesDevuelveLoMismo`, `PacienteAnonimizarTest.anonimizarDosVecesNoCambiaNada` |
| Anonimización · Paciente inexistente | `PacienteAdminControllerTest.pacienteInexistenteEs404` |
| Solo administrador · Recepción / sin token | `PacienteAdminControllerTest.recepcionRecibe403YSinTokenRecibe401SinCambios` |
| Sin emails a anonimizados | `PacienteAnonimizarTest.soloRecibeEmailsSiNoEstaAnonimizado` (los destinatarios reales los eligen C-14/C-18) |
| Sin datos personales en logs · Reserva rechazada | `PrivacidadPacienteIntegracionTest.elRegistroOnlineSinAceptacionNoCreaPacienteNiDejaDatosEnElLog`, `ErroresSinDatosDelInputTest`, `SinValoresDeLaBaseEnLogsTest` |
| Sin datos personales en logs · Anonimización | `PacienteAdminControllerTest.elAdministradorAnonimizaYLosTurnosPasadosQuedan` |
| Transporte cifrado · Request HTTPS a través del proxy | `HstsDetrasDelProxyTest` |

Escenarios cubiertos solo en parte acá (dependen de C-16): el 400 por HTTP de una reserva sin aceptar y el flujo reserva → conflicto de turno.
