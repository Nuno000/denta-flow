# Trazabilidad — C-14 `email-transaccional-y-enlaces`

Cada requisito y escenario de las tres specs, con el test que lo cubre. Rutas de test relativas a `backend/src/test/java/ar/dentaflow/`.

## `notificaciones-email`

| Requisito / escenario | Test |
|---|---|
| Una notificación por turno y tipo — Segunda confirmación del mismo turno | `aplicacion/notificaciones/ServicioDeNotificacionesTest#pedirlaDosVecesEnviaUnSoloEmail` |
| — Duplicado escrito directo (H2 y MySQL 8) | `infraestructura/persistencia/notificaciones/RestriccionesNotificacionBase#rechazaUnaSegundaNotificacionDelMismoTurnoYTipo` (`…H2Test`, `…MySqlTest`) |
| — Valores fuera del catálogo | `RestriccionesNotificacionBase#rechazaUnTipoFueraDelCatalogo`, `#rechazaUnCanalFueraDelCatalogo`, `#rechazaUnEstadoFueraDelCatalogo` (+ intentos, motivo, enviada, fallida, FK) |
| Email de confirmación de reserva — Turno a las 13:00 UTC | `aplicacion/notificaciones/PlantillaConfirmacionReservaTest#muestraLaHoraDelConsultorioLaPrestacionElProfesionalYLosEnlaces`, `ServicioDeNotificacionesTest#enviaLaConfirmacionAlPacienteYLaRegistraEnviada`, `#losEnlacesDelEmailUsanElTokenDelTurno` |
| — Sin documento ni teléfono | `PlantillaConfirmacionReservaTest#noIncluyeDniNiTelefono` |
| — Contenido escapado | `PlantillaConfirmacionReservaTest#escapaElContenidoEnLaVersionHtml` |
| Envío que no demora la reserva — Servidor de correo caído | `ServicioDeNotificacionesTest#conElServidorCaidoElTurnoSigueIgualYElEnvioNoCorreDentroDeUnaTransaccion`, `#pedidaDentroDeUnaTransaccionSeEnviaRecienDespuesDelCommit`, `#siLaTransaccionSeRevierteNoSeEnviaNada` |
| Reintento ante fallas transitorias — Falla una vez y después anda | `ServicioDeNotificacionesTest#siFallaUnaVezPorConexionReintentaYQuedaEnviadaConDosIntentos` |
| — Falla siempre | `ServicioDeNotificacionesTest#siFallaSiempreHaceTresIntentosYQuedaFallidaPorConexion`, `dominio/notificaciones/PoliticaDeReintentoTest` |
| — Credenciales del servidor inválidas | `ServicioDeNotificacionesTest#conCredencialesInvalidasHaceUnSoloIntento`, `infraestructura/email/EnviadorDeEmailSmtpTest#autenticacionRechazadaEsPermanente` |
| Estado del envío consultable — Motivo sin datos del destinatario | `EnviadorDeEmailSmtpTest#direccionRechazadaEsDeDestinatarioYNoRepiteLaDireccion`, `infraestructura/persistencia/notificaciones/NotificacionRepositorioTest#guardaLosCodigosEnMinuscula`, `dominio/notificaciones/NotificacionTest` |
| Sin envío a pacientes anonimizados — Paciente anonimizado | `ServicioDeNotificacionesTest#aUnPacienteAnonimizadoNoLeEnviaYQuedaOmitida` |
| Canal desacoplado del proveedor — Pruebas sin servidor de correo | `infraestructura/email/PropiedadesEmailTest#sinModoUsaElCanalSimulado`, `EnviadorDeEmailSimuladoTest`, `dominio/DominioSinFrameworksTest` (sin `jakarta.mail` en el dominio) |
| Sin datos personales ni enlaces en los logs — Log de un envío fallido | `aplicacion/notificaciones/LogsDeNotificacionesTest#unEnvioFallidoDejaElMotivoSinDatosPersonales`, `#unEnvioExitosoSoloDejaIdsEstadoEIntentos` |

## `enlaces-paciente`

| Requisito / escenario | Test |
|---|---|
| Enlaces armados con la URL pública y el token del turno — Enlaces de un turno | `aplicacion/notificaciones/EnlacesDeTurnoTest#armaLosTresEnlacesConElTokenDelTurno` |
| — URL base con barra final | `EnlacesDeTurnoTest#conBaseTerminadaEnBarraNoQuedanBarrasDobles`, `PropiedadesEnlacesTest#quitaLaBarraFinal` |
| — Mismos enlaces en confirmación y recordatorio | `EnlacesDeTurnoTest#dosLlamadasDanLosMismosEnlaces` |
| Resolución de un token — Turno pendiente futuro | `dominio/notificaciones/SituacionDeEnlaceTest#pendienteFuturoEsVigente`, `aplicacion/notificaciones/ServicioDeEnlacesTest#unTurnoPendienteFuturoEsVigenteYTraeSuResumen` |
| — Turno ya cancelado | `SituacionDeEnlaceTest#canceladoEsCancelado`, `ServicioDeEnlacesTest#unTurnoCanceladoEsCancelado` |
| — Turno cuyo inicio ya pasó | `SituacionDeEnlaceTest#cuandoLlegaElInicioVence`, `ServicioDeEnlacesTest#unTurnoCuyoInicioYaLlegoEstaVencido` |
| — Turno con asistencia registrada | `SituacionDeEnlaceTest#conAsistenciaRegistradaVence` |
| — Token inexistente o mal formado | `ServicioDeEnlacesTest#unTokenInexistenteOMalFormadoDaLaMismaRespuestaSinDatos` |
| La resolución no expone ni modifica el turno — Resolver no confirma | `ServicioDeEnlacesTest#resolverDosVecesNoCambiaElEstadoNiAgregaHistorial` |
| — Sin datos personales en la resolución | `ServicioDeEnlacesTest#elResumenNoTieneDatosDelPaciente` |

## `configuracion-entorno` (delta)

| Requisito / escenario | Test |
|---|---|
| Configuración del email por variables de entorno — Desarrollo local sin servidor de correo | `infraestructura/email/PropiedadesEmailTest#sinModoUsaElCanalSimulado` |
| — Ejemplo de entorno completo | **Pendiente**: `.env.example` está bloqueado por los permisos de la sesión de Claude Code; hay que agregar las variables a mano (ver `tasks.md` 2.4) |
| Configuración de email inválida detiene el arranque — Modo SMTP sin servidor | `PropiedadesEmailTest#enModoSmtpSinServidorNoArrancaYNombraLaVariableSinMostrarLaContrasena` |
| — URL pública relativa | `aplicacion/notificaciones/PropiedadesEnlacesTest#unaUrlRelativaNoArranca` |

## Corrección propuesta a la KB (aplicar al archivar)

`knowledge-base/04_modelo_de_datos.md` §Notificacion dice `tipo (recordatorio | confirmación | reintento)`. Con la clave única `(turno_id, tipo)` que pide C-18, un reintento no es una fila nueva. Texto propuesto:

> - id, turno_id, tipo (`confirmacion` | `recordatorio`), canal (`email`), estado (`en_curso` | `enviada` | `fallida` | `omitida`), intentos, motivo de falla (`conexion` | `autenticacion` | `destinatario` | `desconocido`), creada_en, actualizada_en, enviada_en. Una por turno y tipo: los reintentos suman en `intentos` (C-14, design D2).
