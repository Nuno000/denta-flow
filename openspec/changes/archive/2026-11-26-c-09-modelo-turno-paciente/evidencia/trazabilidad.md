# Trazabilidad requisito -> test (C-09)

Nivel requisito (94 escenarios agrupados). Estado: H2 verde (560 tests); los marcados MySQL quedan pendientes de `.\mvnw.cmd verify -Pit-mysql` (lo corre Renzo).

| Requisito | Tests |
|---|---|
| auditoria: Historial ligado a un turno existente | RestriccionesHistorialBase (rechazaUnTurnoInexistente, aceptaUnTurnoExistente), ServicioDeAuditoriaIntegracionTest (registrarUnCambioDeUnTurnoInexistenteRevierteTodo) |
| pacientes: Datos obligatorios / Formato email / Teléfono / Consentimiento | PacienteTest |
| pacientes: DNI normalizado | DniTest |
| pacientes: DNI único con deduplicación | ServicioDePacientesTest, PacienteRepositorioTest, ServicioDePacientesIntegracionTest, AltaSimultaneaPacienteMySqlTest (MySQL) |
| pacientes: Actualización según el origen | ServicioDePacientesTest |
| pacientes: Fecha de consentimiento | PacienteTest, ServicioDePacientesTest, PacienteRepositorioTest |
| pacientes: Integridad en la base | RestriccionesPacienteH2Test / RestriccionesPacienteMySqlTest (MySQL) |
| pacientes: Datos personales fuera de logs y auditoría | DatosPaciente/Paciente.toString sin datos; ServicioDeTurnosIntegracionTest (laAltaDejaUnTurnoPendiente...) |
| turnos: Duración / Estado inicial / Origen y actor / Sobreturno (dominio) | TurnoTest, ServicioDeTurnosCrearTest |
| turnos: Recursos válidos / Inicio no anterior | ServicioDeTurnosCrearTest |
| turnos: Sin solapamiento (profesional, sillón) / instantes absolutos / cancelación libera | TurnoRepositorioTest, ServicioDeTurnosCrearTest, ServicioDeTurnosIntegracionTest |
| turnos: Sobreturno explícito | TurnoTest, ServicioDeTurnosCrearTest, TurnoRepositorioTest, ServicioDeTurnosIntegracionTest |
| turnos: Reservas simultáneas / Bloqueo en orden fijo | CreacionSimultaneaTurnosMySqlTest (MySQL), ServicioDeTurnosCrearTest (InOrder), CerrojoDeRecursosTest |
| turnos: Transiciones / Idempotente | EstadoTurnoTest (36), TurnoTest, ServicioDeTurnosEstadoTest, ServicioDeTurnosIntegracionTest |
| turnos: Reprogramación verificada | TurnoTest, ServicioDeTurnosReprogramarTest, ServicioDeTurnosIntegracionTest, CreacionSimultaneaTurnosMySqlTest (e, MySQL) |
| turnos: Cambios auditados en la misma transacción | ServicioDeTurnosCrearTest/EstadoTest/ReprogramarTest, ServicioDeTurnosIntegracionTest |
| turnos: Única vía de escritura | UnicaViaDeEscrituraDeTurnosTest |
| turnos: Token de acceso | GeneradorDeTokensSeguroTest, TurnoRepositorioTest |
| turnos: Integridad en la base | RestriccionesTurnoH2Test / RestriccionesTurnoMySqlTest / IndicesTurnoMySqlTest (MySQL) |
