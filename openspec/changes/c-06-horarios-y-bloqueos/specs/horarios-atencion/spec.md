# Spec Delta — horarios-atencion

## Purpose

Define el horario de atención semanal de cada profesional, expresado en la hora de reloj del consultorio, para que la reserva y la agenda solo ofrezcan y acepten turnos dentro de él (US-002, RN-TU-04).

## ADDED Requirements

### Requirement: Rango semanal en hora del consultorio
Un horario de atención SHALL pertenecer a un profesional existente y activo y tener un día de la semana (lunes a domingo), una hora de inicio y una hora de fin con precisión de minutos, interpretadas en la zona `America/Argentina/Buenos_Aires`. La hora de inicio MUST ser anterior a la de fin dentro del mismo día.

#### Scenario: Alta válida
- **WHEN** el administrador carga para un profesional activo el lunes de 09:00 a 13:00
- **THEN** el sistema guarda el rango y lo devuelve con su id, el día `LUNES`, `09:00` y `13:00`

#### Scenario: Inicio igual o posterior al fin
- **WHEN** el administrador carga un rango de 13:00 a 13:00 o de 18:00 a 09:00
- **THEN** el sistema lo rechaza como dato inválido y no guarda nada

#### Scenario: Hora con segundos
- **WHEN** el administrador carga un rango de 09:00:30 a 13:00
- **THEN** el sistema lo rechaza como dato inválido

#### Scenario: Profesional inexistente o inactivo
- **WHEN** el administrador carga un rango para un profesional que no existe
- **THEN** el sistema responde "no encontrado"
- **WHEN** el profesional existe pero está inactivo
- **THEN** el sistema rechaza el alta por conflicto y no guarda nada

### Requirement: Sin rangos superpuestos por profesional y día
El sistema MUST rechazar un alta o una edición que deje dos rangos del mismo profesional y el mismo día superpuestos en `[inicio, fin)`. Los rangos contiguos y los de otros profesionales o de otros días SHALL aceptarse.

#### Scenario: Superposición parcial rechazada
- **GIVEN** el profesional tiene el lunes de 09:00 a 13:00
- **WHEN** el administrador carga el lunes de 12:00 a 16:00
- **THEN** el sistema responde conflicto, indica el rango existente y no guarda nada

#### Scenario: Rango contenido rechazado
- **GIVEN** el profesional tiene el lunes de 09:00 a 13:00
- **WHEN** el administrador carga el lunes de 10:00 a 11:00
- **THEN** el sistema responde conflicto

#### Scenario: Rangos contiguos aceptados
- **GIVEN** el profesional tiene el lunes de 09:00 a 13:00
- **WHEN** el administrador carga el lunes de 13:00 a 17:00
- **THEN** el sistema guarda el segundo rango

#### Scenario: Otro día u otro profesional
- **GIVEN** el profesional A tiene el lunes de 09:00 a 13:00
- **WHEN** el administrador carga el martes de 09:00 a 13:00 para A, o el lunes de 09:00 a 13:00 para el profesional B
- **THEN** el sistema guarda ambos rangos

#### Scenario: Edición que no choca consigo misma
- **GIVEN** el profesional tiene el lunes de 09:00 a 13:00
- **WHEN** el administrador edita ese mismo rango a lunes de 09:00 a 14:00
- **THEN** el sistema guarda el cambio

#### Scenario: Edición que choca con otro rango
- **GIVEN** el profesional tiene el lunes de 09:00 a 13:00 y de 14:00 a 18:00
- **WHEN** el administrador edita el primero a lunes de 09:00 a 15:00
- **THEN** el sistema responde conflicto y el rango conserva 09:00 a 13:00

### Requirement: Altas simultáneas sin superposición
Cuando dos operaciones guardan a la vez rangos superpuestos para el mismo profesional y día, el sistema MUST aceptar como máximo una de ellas; la otra SHALL recibir conflicto. Operaciones sobre profesionales distintos no se bloquean entre sí.

#### Scenario: Dos altas superpuestas a la vez
- **WHEN** dos administradores cargan al mismo tiempo, para el mismo profesional, lunes de 09:00 a 13:00 y lunes de 12:00 a 16:00
- **THEN** exactamente una de las dos queda guardada y la otra recibe conflicto, verificado contra MySQL 8 real

### Requirement: Baja y consulta del horario
El administrador SHALL poder borrar un rango. La consulta del horario de un profesional SHALL devolver sus rangos ordenados por día (lunes primero) y hora de inicio.

#### Scenario: Borrar un rango
- **WHEN** el administrador borra un rango existente
- **THEN** el sistema responde sin contenido y el rango deja de aparecer en la consulta

#### Scenario: Borrar un rango inexistente
- **WHEN** el administrador borra un id que no existe
- **THEN** el sistema responde "no encontrado"

#### Scenario: Orden de la consulta
- **GIVEN** el profesional tiene martes 09:00–13:00, lunes 14:00–18:00 y lunes 09:00–13:00
- **WHEN** se consulta su horario
- **THEN** el sistema devuelve lunes 09:00–13:00, lunes 14:00–18:00 y martes 09:00–13:00, en ese orden

### Requirement: Permisos sobre el horario
Solo el administrador SHALL crear, editar y borrar rangos. Recepción SHALL leer el horario de cualquier profesional. Un usuario con rol profesional SHALL leer solo el horario del profesional vinculado a su usuario. Sin sesión, el sistema MUST responder 401.

#### Scenario: Recepción o profesional intentan modificar
- **WHEN** un usuario de recepción o con rol profesional intenta crear, editar o borrar un rango
- **THEN** el sistema responde 403 y no cambia nada

#### Scenario: Profesional consulta su propio horario
- **WHEN** un usuario con rol profesional vinculado al profesional A consulta el horario de A
- **THEN** el sistema devuelve los rangos de A

#### Scenario: Profesional consulta el horario de otro
- **WHEN** un usuario con rol profesional vinculado al profesional A consulta el horario de B
- **THEN** el sistema responde 403 sin datos

#### Scenario: Sin sesión
- **WHEN** se consulta o se modifica un horario sin token
- **THEN** el sistema responde 401
