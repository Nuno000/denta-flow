## Purpose

Enviar al paciente los emails transaccionales de su turno (confirmación de reserva y, desde C-18, recordatorio) por un canal desacoplado del proveedor, dejando registrado el estado de cada envío y sin exponer datos personales fuera del mensaje.

## ADDED Requirements

### Requirement: Una notificación por turno y tipo
El sistema SHALL registrar cada email transaccional como una notificación con turno, tipo (`confirmacion` o `recordatorio`), canal `email`, estado (`en_curso`, `enviada`, `fallida` u `omitida`), cantidad de intentos e instantes en UTC. La base MUST rechazar una segunda notificación del mismo turno y tipo, aunque se escriba directo con SQL.

#### Scenario: Segunda confirmación del mismo turno
- **GIVEN** el turno 42 ya tiene una notificación `confirmacion`
- **WHEN** se pide enviar la confirmación del turno 42 otra vez
- **THEN** no se envía un segundo email y sigue existiendo una sola notificación `confirmacion` para ese turno

#### Scenario: Duplicado escrito directo
- **WHEN** se inserta por SQL una segunda fila `(turno 42, confirmacion)`
- **THEN** la base la rechaza por su clave única, en H2 `MODE=MySQL` y en MySQL 8

#### Scenario: Valores fuera del catálogo
- **WHEN** se inserta por SQL una notificación con tipo `reintento`, canal `sms` o estado `borrada`
- **THEN** la base la rechaza por una restricción con nombre propio

### Requirement: Email de confirmación de reserva
Después de crearse un turno, el sistema SHALL enviar al email del paciente un mensaje de confirmación con la fecha y hora del turno en `America/Argentina/Buenos_Aires`, la prestación, el profesional y los enlaces de confirmar, cancelar y reprogramar del turno. El mensaje MUST NOT incluir DNI ni teléfono.

#### Scenario: Turno a las 13:00 UTC
- **GIVEN** un turno pendiente del `2026-12-02T13:00:00Z` para "Limpieza" con la profesional "Ana Pérez"
- **WHEN** se envía su confirmación
- **THEN** el email va a la dirección del paciente y muestra `02/12/2026 10:00`, "Limpieza", "Ana Pérez" y los tres enlaces del turno

#### Scenario: Sin documento ni teléfono
- **WHEN** se envía la confirmación de un paciente con DNI `30111222` y teléfono `1155550000`
- **THEN** ni el asunto ni el cuerpo contienen esos valores

#### Scenario: Contenido escapado
- **GIVEN** el nombre del paciente es `<b>Ana</b>`
- **WHEN** se arma la versión HTML del email
- **THEN** el nombre aparece escapado y no como marcado HTML

### Requirement: Envío que no demora la reserva
El envío de la confirmación SHALL ocurrir después de confirmada la transacción que creó el turno y MUST NOT demorar ni revertir esa transacción: una falla de envío deja el turno creado y la falla registrada.

#### Scenario: Servidor de correo caído
- **GIVEN** el servidor de correo rechaza todas las conexiones
- **WHEN** se crea un turno y se pide su confirmación
- **THEN** el turno sigue existiendo y la notificación queda `fallida`

### Requirement: Reintento ante fallas transitorias
Ante una falla transitoria del envío (conexión, tiempo de espera o rechazo temporal del servidor), el sistema SHALL reintentar hasta completar 3 intentos en total. Ante una falla permanente (autenticación con el servidor o dirección rechazada) MUST NOT reintentar. Cada intento MUST sumarse en la notificación.

#### Scenario: Falla una vez y después anda
- **GIVEN** el primer intento de envío falla por conexión y el segundo funciona
- **WHEN** se envía la confirmación
- **THEN** la notificación queda `enviada` con 2 intentos y el instante de envío del reloj

#### Scenario: Falla siempre
- **GIVEN** todos los intentos fallan por conexión
- **WHEN** se envía la confirmación
- **THEN** se hacen exactamente 3 intentos y la notificación queda `fallida` con 3 intentos y motivo `conexion`

#### Scenario: Credenciales del servidor inválidas
- **GIVEN** el servidor de correo rechaza la autenticación
- **WHEN** se envía la confirmación
- **THEN** se hace 1 solo intento y la notificación queda `fallida` con motivo `autenticacion`

### Requirement: Estado del envío consultable
Cada notificación SHALL guardar su estado final, la cantidad de intentos, el instante de la última actualización y, si fue enviada, el instante de envío; si falló, un motivo de una lista cerrada (`conexion`, `autenticacion`, `destinatario`, `desconocido`) sin el texto de error del proveedor.

#### Scenario: Motivo sin datos del destinatario
- **GIVEN** el proveedor rechaza la dirección `paciente@ejemplo.com` con un mensaje que la repite
- **WHEN** se registra la falla
- **THEN** la notificación queda con motivo `destinatario` y ninguna columna contiene la dirección

### Requirement: Sin envío a pacientes anonimizados
El sistema MUST NOT enviar emails a un paciente anonimizado (C-15): la notificación queda `omitida` sin intentos.

#### Scenario: Paciente anonimizado
- **GIVEN** el paciente del turno 42 fue anonimizado
- **WHEN** se pide la confirmación del turno 42
- **THEN** no se llama al servidor de correo y la notificación queda `omitida` con 0 intentos

### Requirement: Canal desacoplado del proveedor
El envío SHALL pasar por un único punto de salida intercambiable por configuración entre un servidor SMTP real y un canal simulado que guarda los mensajes en memoria. Las pruebas automatizadas MUST usar el canal simulado y MUST NOT conectarse a un servidor de correo.

#### Scenario: Pruebas sin servidor de correo
- **WHEN** corren las pruebas del backend con el perfil `test`
- **THEN** los emails enviados quedan disponibles en el canal simulado y no se abre ninguna conexión SMTP

### Requirement: Sin datos personales ni enlaces en los logs
Los logs del envío SHALL identificar la notificación solo por su id, turno, tipo, estado e intentos. MUST NOT contener la dirección de email, el nombre del paciente, el token ni los enlaces.

#### Scenario: Log de un envío fallido
- **WHEN** falla el envío de la confirmación de un turno
- **THEN** el log registra id de notificación, turno, estado y motivo, y no contiene la dirección, el token ni el enlace
