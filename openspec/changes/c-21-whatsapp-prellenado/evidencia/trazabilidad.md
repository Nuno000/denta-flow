# Trazabilidad requisito → tests — C-21

Backend: `backend/src/test/java/ar/dentaflow/...`. Frontend: `frontend/src/...`.

## `mensajes-whatsapp`

| Requisito / escenario | Tests |
|---|---|
| Mensaje a pedido: turno pendiente con teléfono válido, sin cambios en el historial | `WhatsappControllerTest.recepcionRecibeElMensajeYElEnlaceSinCacheYSinTocarElHistorial`, `ServicioDeMensajesWhatsappTest.armaElMensajeYElEnlace...` |
| Turno inexistente → 404 | `WhatsappControllerTest.unTurnoInexistenteDa404`, `ServicioDeMensajesWhatsappTest.unTurnoInexistenteEsRecursoNoEncontrado` |
| Turno cancelado o atendido → 409 | `WhatsappControllerTest.unTurnoCanceladoDa409`, `ServicioDeMensajesWhatsappTest.unTurnoNoVigenteEsUnConflicto` (4 estados) |
| Solo administrador y recepción: profesional → 403 | `WhatsappControllerTest.elProfesionalNoPuedeNiSiquieraConUnTurnoPropio`, `elAdministradorTambienPuede` |
| Sin sesión → 401 | `WhatsappControllerTest.sinSesionDa401` |
| Plantilla: fecha y hora en la zona del consultorio | `PlantillaWhatsappTest.fechaYHoraEnLaZonaDelConsultorio` |
| Plantilla: marcador desconocido literal | `PlantillaWhatsappTest.marcadorDesconocidoQuedaLiteral` (más `marcadorRepetido...` y `unValorConLlaves...`) |
| Plantilla: enlace de confirmación | `PlantillaWhatsappTest.reemplazaLosSeisMarcadores`, `WhatsappControllerTest` (el mensaje contiene `/turno/`) |
| Teléfono normalizado (Buenos Aires, Mendoza, internacional con y sin 9) | `TelefonoWhatsappTest.normalizaTelefonosArgentinosAlFormatoDeWhatsapp` (10 casos) |
| Teléfono no argentino o paciente anonimizado → `enlace` nulo | `TelefonoWhatsappTest.rechaza...` (9 casos + nulo/vacío), `ServicioDeMensajesWhatsappTest.sinTelefonoUtilizable...`, `WhatsappControllerTest.sinTelefonoValidoElEnlaceEsNulo` |
| Mensaje codificado (acentos, `+`, `&`, `?`, `#`, `%20`) | `EnlaceWhatsappTest` (4 tests) |
| Sin datos personales en logs | `LogsDeWhatsappTest.pedirElMensajeNoDejaTelefonoNombreNiTokenEnElLog` |
| Botón y diálogo: recepción/administrador, solo pendiente/confirmado | `AgendaPage.test.tsx` "%s ve WhatsApp solo en turnos pendientes y confirmados..." |
| Botón: profesional o turno no vigente no lo ven | `AgendaPage.test.tsx` "el profesional no ve WhatsApp", "un turno cancelado no muestra WhatsApp" |
| Diálogo con enlace (`target`, `rel`) | `WhatsappDialogo.test.tsx` "muestra la vista previa y el enlace..." |
| Diálogo sin teléfono válido (aviso `role="alert"`) | `WhatsappDialogo.test.tsx` "sin teléfono válido avisa..." |
| Cierre con teclado y foco devuelto | `WhatsappDialogo.test.tsx` "Cerrar y Escape cierran", `Dialogo.test.tsx`, `accesibilidad.md` (Chromium) |

## `parametros-consultorio`

| Requisito / escenario | Tests |
|---|---|
| Catálogo con cinco claves | `ClaveParametroTest.desdeCodigoDevuelveLaClaveDelCatalogo`, `ParametrosControllerTest.elAdministradorListaLosCincoParametros...`, `ServicioDeParametrosTest.listar...` |
| Valores por defecto provisorios (cinco) | `RestriccionesParametrosH2Test` / `RestriccionesParametrosMySqlTest` (`laBaseRecienMigrada...`, `losValoresInicialesSonLosDelDiseno`), `CatalogoDeParametrosConsistenciaTest` |
| Plantilla vacía, solo espacios o de 1001 caracteres → 400 sin cambios | `ParametrosControllerTest.laPlantillaDeWhatsappEsObligatoriaYAcotada` |
| Plantilla válida → 200 y deja de ser provisoria | `ParametrosControllerTest.laPlantillaDeWhatsappEsObligatoriaYAcotada`, `ServicioDeParametrosTest.laPlantillaDeWhatsappSeLeeAunqueSeaProvisoria` |
