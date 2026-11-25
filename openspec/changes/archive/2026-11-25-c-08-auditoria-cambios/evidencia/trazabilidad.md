# Trazabilidad escenario → test — C-08 `auditoria-cambios`

Spec: `specs/auditoria-turnos/spec.md`. Clases de test en `backend/src/test/java/ar/dentaflow/` (abreviadas).

| Requisito | Escenario | Test |
|---|---|---|
| Registro de quién y cuándo | Cambio de estado hecho por recepción | `ServicioDeAuditoriaTest.registraElCambioConLaHoraDelSistema`, `HistorialDeTurnoRepositorioTest.agregaYDevuelveElRegistroConSuId` |
| Registro de quién y cuándo | (la hora sale del sistema) | `ServicioDeAuditoriaTest.laHoraSaleDelRelojYNoDeQuienLlama` |
| Registro de quién y cuándo | Alta de un turno | `CambioDeTurnoTest.laCreacionAdmiteValorAnteriorVacio`, `HistorialDeTurnoRepositorioTest.laCreacionGuardaElValorAnteriorVacio` |
| Registro de quién y cuándo | Sobreturno registra quién y cuándo | `CambioDeTurnoTest.losValoresRespetanElOrdenYNoSePuedenModificar` (creación con `es_sobreturno` y actor usuario) |
| Actor identificado | Cambio hecho por el paciente | `ActorTest.elPacienteNoTieneIdDeUsuario`, `RestriccionesHistorial*.aceptaUnRegistroValidoDeCadaTipoDeActor` |
| Actor identificado | Cambio hecho por una tarea automática | `ActorTest.elSistemaNoTieneIdDeUsuario`, `RestriccionesHistorial*.aceptaUnRegistroValidoDeCadaTipoDeActor` |
| Actor identificado | Actor usuario sin id | `ActorTest.elActorUsuarioSinIdSeRechaza`, `RestriccionesHistorial*.rechazaActorUsuarioSinId` |
| Actor identificado | Usuario inexistente | `RestriccionesHistorial*.rechazaUnUsuarioInexistente`, `ServicioDeAuditoriaIntegracionTest.unUsuarioInexistenteFallaEnElRegistroYNoAlConfirmar` |
| Actor identificado | (paciente o sistema con id) | `ActorTest.noSePuedeArmarUnActorIncoherente`, `RestriccionesHistorial*.rechazaPacienteOSistemaConIdDeUsuario` |
| Acción de un catálogo cerrado | Acción desconocida | `AccionDeTurnoTest.unaAccionDesconocidaSeRechaza`, `RestriccionesHistorial*.rechazaUnaAccionFueraDelCatalogo` |
| Valores legibles | Reprogramación guarda los instantes en UTC | `ConversorValoresJsonTest.respetaElOrdenDeLosCamposEnIdaYVuelta`, `HistorialDeTurnoRepositorioTest.agregaYDevuelveElRegistroConSuId` (instante exacto en UTC) |
| Valores legibles | Cambio sin valores | `CambioDeTurnoTest.unCambioSinValoresSeRechaza`, `RestriccionesHistorial*.rechazaUnRegistroSinValores` |
| Valores legibles | Valores demasiado largos | `ConversorValoresJsonTest.rechazaValoresQueNoEntranEnLaColumnaEnVezDeTruncarlos`, `ServicioDeAuditoriaIntegracionTest.valoresDemasiadoLargosSeRechazanComoValidacionYRevierteTodo` |
| Registro atómico | Se confirman juntos | `ServicioDeAuditoriaIntegracionTest.siLaOperacionTerminaBienElRegistroQueda` |
| Registro atómico | Falla el cambio después de registrar | `ServicioDeAuditoriaIntegracionTest.siLaOperacionFallaDespuesDeRegistrarNoQuedaNada` |
| Registro atómico | Registro suelto sin transacción | `ServicioDeAuditoriaIntegracionTest.sinTransaccionSeRechazaYNoSeGuardaNada` |
| Solo inserción | Intento de modificar un registro | `HistorialDeTurnoRepositorioTest.unRegistroGuardadoNoSeModificaAunqueSeCambieLaEntidad` (con prueba de mutación, ver `backend.txt`) |
| Solo inserción | Sin operación de borrado | `HistorialSoloInsercionTest` (2 tests) |
| Consulta cronológica | Historial ordenado | `HistorialDeTurnoRepositorioTest.devuelveElHistorialDeUnTurnoEnOrdenCronologicoYSinOtrosTurnos` |
| Consulta cronológica | Misma fecha y hora | `HistorialDeTurnoRepositorioTest.aIgualInstanteRespetaElOrdenDeAlta` |
| Consulta cronológica | Turno sin historial | `HistorialDeTurnoRepositorioTest.unTurnoSinRegistrosDevuelveListaVacia` |

`RestriccionesHistorial*` corre en H2 (`RestriccionesHistorialH2Test`) y en MySQL 8 real (`RestriccionesHistorialMySqlTest`, perfil `it-mysql`).

Ningún escenario queda sin test. Mostrar los instantes en hora de `America/Argentina/Buenos_Aires` es responsabilidad de quien consulta (C-13), con `ZonaHorariaConsultorio`, que ya tiene sus propios tests.
