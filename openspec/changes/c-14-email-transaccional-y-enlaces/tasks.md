# Tasks — C-14 `email-transaccional-y-enlaces`

> **Gobernanza ALTA:** no empezar hasta que Florencia apruebe `design.md` y Bruno confirme el contrato de `notificacion` con C-18 (design D2 y Q5).
>
> TDD estricto en cada tarea: **RED** (test que falla y se ve fallar), **GREEN** (código mínimo), **REFACTOR** (con tests en verde). Nada requiere Docker, Testcontainers ni un servidor SMTP de prueba. Backend desde `backend/` con `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local). Evidencia en `openspec/changes/c-14-email-transaccional-y-enlaces/evidencia/`.
>
> Rama: `feat/C-14-email-transaccional-y-enlaces`. Commits en Conventional Commits en castellano, sin `Co-Authored-By`.

## 1. Línea base

- [ ] 1.1 Correr `./mvnw verify` y `./mvnw verify -Pit-mysql` antes de tocar código y anotar los resultados en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa

## 2. Dependencia y configuración (spec `configuracion-entorno`)

- [ ] 2.1 Agregar `spring-boot-starter-mail` a `pom.xml` en un commit aparte (`chore: agregar spring-boot-starter-mail`), y `management.health.mail.enabled=false` (design D7)
- [ ] 2.2 RED: `PropiedadesEmailTest` (`ApplicationContextRunner`): sin `APP_MAIL_MODO` el modo es `simulado`; con `modo=smtp` y host vacío el contexto no arranca y el mensaje nombra `SPRING_MAIL_HOST` sin mostrar la contraseña; con `APP_MAIL_FROM` inválido no arranca. GREEN: `PropiedadesEmail` + `ValidadorConfiguracionEmail`
- [ ] 2.3 RED: `PropiedadesEnlacesTest`: `APP_BASE_URL=turnos.example` no arranca; `https://turnos.example/` queda sin barra final. GREEN: `PropiedadesEnlaces` (`app.base-url`) reutilizando `APP_BASE_URL`
- [ ] 2.4 Completar `application.yml` (design D7) y `.env.example` con `SPRING_MAIL_*`, `APP_MAIL_FROM`, `APP_MAIL_MODO`, sin credenciales reales. Verificar que el perfil `test` queda en `simulado`

## 3. Esquema `notificacion` (spec "Una notificación por turno y tipo")

- [ ] 3.1 RED: `RestriccionesNotificacionBaseTest` (H2, SQL directo con un turno real): segunda fila `(turno, confirmacion)` rechazada por `uk_notificacion_turno_tipo`; tipo `reintento`, canal `sms`, estado `borrada`, `intentos = -1`, `enviada` sin `enviada_en`, `fallida` sin motivo y turno inexistente rechazados por su restricción. GREEN: `V014_1__crear_notificacion.sql` (design D2) y `db/migration/README.md`
- [ ] 3.2 Mismo test con `@Tag("mysql")` en `it-mysql`; guardar la salida en `evidencia/it-mysql.txt`

## 4. Dominio `dominio/notificaciones/` (Java puro)

- [ ] 4.1 RED: `NotificacionTest`: nace `en_curso` con 0 intentos; `registrarIntento` suma; `marcarEnviada` fija `enviada_en`; `marcarFallida` exige motivo; `omitir` deja 0 intentos; desde `enviada` no se puede volver a `fallida`. GREEN: `Notificacion` y enums (design D3)
- [ ] 4.2 RED: `PoliticaDeReintentoTest`: `conexion` reintenta con 1 y 2 intentos y no con 3; `autenticacion` y `destinatario` nunca reintentan. GREEN: `PoliticaDeReintento`
- [ ] 4.3 RED: `SituacionDeEnlaceTest` (spec "Resolución de un token"): pendiente futuro → `VIGENTE`; confirmado futuro → `VIGENTE`; cancelado → `CANCELADO`; `ahora == inicio` → `VENCIDO`; `asistio`, `ausente`, `ausente_con_aviso` → `VENCIDO`. GREEN: `SituacionDeEnlace.de(turno, ahora)`
- [ ] 4.4 RED: `ArquitecturaNotificacionesTest`: `dominio.notificaciones` no importa Spring, JPA ni `jakarta.mail`. GREEN: ajustar si hiciera falta

## 5. Persistencia

- [ ] 5.1 RED: `NotificacionRepositorioJpaTest` (`@DataJpaTest`, H2): guardar y `buscarPorTurnoYTipo`; los enums se guardan con su código en minúscula. GREEN: `META-INF/orm/notificaciones.xml`, conversores, `NotificacionRepositorioJpa` (sin `delete*`), alta en `spring.jpa.mapping-resources`

## 6. Enlaces (spec `enlaces-paciente`)

- [ ] 6.1 RED: `EnlacesDeTurnoTest`: con base `https://turnos.example` y un turno con token conocido arma los tres enlaces; con base terminada en `/` no hay barra doble; dos llamadas dan lo mismo; no se invoca `GeneradorDeTokens`. GREEN: `EnlacesDeTurno` (design D6)
- [ ] 6.2 RED: `ServicioDeEnlacesTest` (`@SpringBootTest`, `Clock` fijo, turnos reales vía `ServicioDeTurnos`): vigente, cancelado, vencido por hora, inexistente y mal formado (`../x`, vacío) con la misma respuesta; resolver dos veces no cambia el estado ni agrega historial; el resumen no tiene DNI, teléfono, email ni id del paciente. GREEN: `ServicioDeEnlaces` + `ResolucionDeEnlace` + `ResumenTurnoPaciente`

## 7. Email: plantilla y adaptadores (spec `notificaciones-email`)

- [ ] 7.1 RED: `PlantillaConfirmacionReservaTest`: turno `2026-12-02T13:00:00Z` → `02/12/2026 10:00`, prestación, profesional y los tres enlaces en texto y HTML; no aparecen DNI ni teléfono; `<b>Ana</b>` sale escapado. GREEN: `PlantillaConfirmacionReserva`
- [ ] 7.2 RED: `EnviadorDeEmailSimuladoTest`: guarda el mensaje; respeta el máximo de 50; configurado para fallar 1 vez con `conexion` lanza `FallaDeEnvio` y después envía. GREEN: `EnviadorDeEmailSimulado`
- [ ] 7.3 RED: `EnviadorDeEmailSmtpTest` (Mockito sobre `JavaMailSender`): arma un `MimeMessage` multiparte UTF-8 con el remitente configurado; traduce `MailAuthenticationException` → `autenticacion`, direcciones inválidas → `destinatario`, conexión → `conexion`, otra → `desconocido`; `MensajeEmail.toString()` no muestra destinatario ni cuerpo. GREEN: `EnviadorDeEmailSmtp` (design D4)

## 8. Servicio de notificaciones (spec `notificaciones-email`)

- [ ] 8.1 RED: `ServicioDeNotificacionesTest` (`@SpringBootTest`, H2, `Clock` fijo, ejecutor síncrono, espera nula, adaptador simulado): crear un turno y `enviarConfirmacion` → un email al paciente y notificación `enviada` con 1 intento y `enviada_en` del reloj. GREEN: `ServicioDeNotificaciones` (design D5)
- [ ] 8.2 RED: llamar dos veces → un solo email y una sola notificación. GREEN: idempotencia por `(turno, tipo)`, incluida la `DataIntegrityViolationException` concurrente
- [ ] 8.3 RED: falla 1 vez por `conexion` → `enviada` con 2 intentos; falla siempre → exactamente 3 intentos y `fallida` con motivo `conexion`; `autenticacion` → 1 intento y `fallida`. GREEN: bucle con `PoliticaDeReintento`
- [ ] 8.4 RED: con el SMTP caído el turno sigue existiendo y en su estado; el envío no corre dentro de la transacción de la reserva (`TransactionSynchronizationManager.isActualTransactionActive()` falso durante `enviar`). GREEN: transacciones cortas separadas
- [ ] 8.5 RED: paciente anonimizado (C-15) → ningún email y notificación `omitida` con 0 intentos. GREEN: verificar `Paciente.puedeRecibirEmails()`
- [ ] 8.6 RED: `LogsDeNotificacionesTest` (`OutputCaptureExtension`): un envío exitoso y uno fallido no dejan en el log el email, el nombre, el token ni el enlace, y sí el id de notificación, el turno, el estado y el motivo. GREEN: ajustar los mensajes de log
- [ ] 8.7 Configurar el `ejecutorDeEmails` (2 hilos, cola de 100) y su reemplazo síncrono en el perfil `test`; documentar en el Javadoc de `ServicioDeNotificaciones` el contrato con C-16 (llamar después del commit)

## 9. Verificación y cierre

- [ ] 9.1 `./mvnw verify` y `./mvnw verify -Pit-mysql` en verde; salidas en `evidencia/verificacion-h2.txt` y `evidencia/it-mysql.txt`
- [ ] 9.2 Prueba manual con Brevo (o Gmail): `APP_MAIL_MODO=smtp` en local, crear un turno de prueba con datos ficticios y un email propio del equipo, y enviar la confirmación. Anotar en `evidencia/envio-real.md` el resultado, sin credenciales ni direcciones reales
- [ ] 9.3 `evidencia/trazabilidad.md`: cada requisito y escenario de las tres specs → test que lo cubre. Proponer la corrección de `knowledge-base/04_modelo_de_datos.md` §Notificacion (tipo `reintento` → `intentos`) para aplicarla al archivar
- [ ] 9.4 Avisar a Bruno (C-18) y al equipo el contrato final: tabla `notificacion`, `EnviadorDeEmail`, `EnlacesDeTurno`, `ServicioDeEnlaces.resolver`, y que la plantilla de recordatorio la agrega C-18 (design Q5)
