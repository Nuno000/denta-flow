# Spec Delta — bloqueos-agenda

## Purpose

Permite bloquear franjas de la agenda (vacaciones, feriados, ausencias) para un profesional, un sillón o todo el consultorio, de modo que esas franjas no se ofrezcan ni se acepten para turnos (US-002, RN-TU-04, RN-GL-01).

## ADDED Requirements

### Requirement: Ámbito del bloqueo
Un bloqueo SHALL tener exactamente un ámbito: `PROFESIONAL` (con un profesional), `SILLON` (con un sillón) o `CONSULTORIO` (sin recurso, afecta a todos). El recurso indicado MUST existir y estar activo, y MUST corresponder al ámbito.

#### Scenario: Bloqueo de profesional
- **WHEN** el administrador crea un bloqueo `PROFESIONAL` para un profesional activo
- **THEN** el sistema lo guarda con ese profesional y sin sillón

#### Scenario: Bloqueo de sillón
- **WHEN** el administrador crea un bloqueo `SILLON` para un sillón activo
- **THEN** el sistema lo guarda con ese sillón y sin profesional

#### Scenario: Bloqueo del consultorio
- **WHEN** el administrador crea un bloqueo `CONSULTORIO` sin recurso
- **THEN** el sistema lo guarda sin profesional ni sillón

#### Scenario: Ámbito y recurso incoherentes
- **WHEN** el administrador crea un bloqueo `PROFESIONAL` sin profesional, o con sillón, o un bloqueo `CONSULTORIO` con un recurso
- **THEN** el sistema lo rechaza como dato inválido y no guarda nada

#### Scenario: Recurso inexistente o inactivo
- **WHEN** el recurso indicado no existe
- **THEN** el sistema responde "no encontrado"
- **WHEN** el recurso existe pero está inactivo
- **THEN** el sistema rechaza el bloqueo por conflicto

### Requirement: Franja absoluta con zona del consultorio
Inicio y fin del bloqueo SHALL recibirse como fecha y hora con desplazamiento ISO-8601, guardarse como instantes UTC y devolverse en la hora del consultorio (`America/Argentina/Buenos_Aires`). El inicio MUST ser anterior al fin. La franja es semiabierta `[inicio, fin)` y puede abarcar varios días.

#### Scenario: Conversión a UTC
- **WHEN** el administrador crea un bloqueo de `2026-12-24T08:00-03:00` a `2026-12-24T13:00-03:00`
- **THEN** el sistema guarda `2026-12-24T11:00Z` a `2026-12-24T16:00Z` y lo devuelve como `2026-12-24T08:00-03:00` a `2026-12-24T13:00-03:00`

#### Scenario: Entrada en UTC
- **WHEN** el administrador envía `2026-12-24T11:00Z` como inicio
- **THEN** el sistema lo devuelve como `2026-12-24T08:00-03:00`

#### Scenario: Cambio de día entre hora local y UTC
- **WHEN** el administrador bloquea el consultorio de `2026-12-31T21:00-03:00` a `2027-01-01T00:00-03:00`
- **THEN** el sistema guarda `2027-01-01T00:00Z` a `2027-01-01T03:00Z` y el bloqueo corresponde al 31 de diciembre del consultorio

#### Scenario: Fecha y hora sin desplazamiento
- **WHEN** el administrador envía `2026-12-24T08:00` sin desplazamiento
- **THEN** el sistema lo rechaza como dato inválido

#### Scenario: Inicio igual o posterior al fin
- **WHEN** inicio y fin son iguales, o el fin es anterior
- **THEN** el sistema lo rechaza como dato inválido

### Requirement: Motivo del bloqueo
Cada bloqueo SHALL tener un motivo `VACACIONES`, `FERIADO`, `AUSENCIA` u `OTRO` y MAY tener un detalle de hasta 200 caracteres. Bloqueos superpuestos entre sí SHALL permitirse.

#### Scenario: Motivo desconocido
- **WHEN** el administrador envía el motivo `PARO`
- **THEN** el sistema lo rechaza como dato inválido

#### Scenario: Detalle demasiado largo
- **WHEN** el detalle tiene 201 caracteres
- **THEN** el sistema lo rechaza indicando el máximo

#### Scenario: Bloqueos superpuestos
- **GIVEN** existe un bloqueo de consultorio el 25/12 todo el día
- **WHEN** el administrador crea un bloqueo del profesional A el 25/12 de 09:00 a 12:00
- **THEN** el sistema guarda ambos

### Requirement: Registro de quién grabó el bloqueo
El sistema SHALL guardar en cada bloqueo el usuario que lo creó o editó por última vez y el instante UTC, tomado del reloj del sistema.

#### Scenario: Alta y edición registradas
- **WHEN** el administrador 7 crea un bloqueo y después el administrador 9 lo edita
- **THEN** el bloqueo muestra al usuario 9 y el instante de la edición

### Requirement: Edición y baja del bloqueo
El administrador SHALL poder editar inicio, fin, motivo y detalle de un bloqueo con las mismas validaciones del alta; el ámbito y el recurso no se editan. El administrador SHALL poder borrar un bloqueo, y la franja vuelve a estar disponible.

#### Scenario: Edición válida
- **WHEN** el administrador cambia el fin de un bloqueo a una hora posterior al inicio
- **THEN** el sistema guarda el nuevo fin

#### Scenario: Intento de cambiar el ámbito
- **WHEN** el administrador envía en la edición un ámbito o un recurso distinto
- **THEN** el sistema lo rechaza como dato inválido y el bloqueo no cambia

#### Scenario: Borrado
- **WHEN** el administrador borra un bloqueo existente
- **THEN** el sistema responde sin contenido y el bloqueo deja de aparecer en las consultas

#### Scenario: Bloqueo inexistente
- **WHEN** se edita, borra o consulta un id que no existe
- **THEN** el sistema responde "no encontrado"

### Requirement: Consulta de bloqueos por rango
La consulta SHALL exigir un rango `desde`–`hasta` (con desplazamiento, `desde` anterior a `hasta`, como máximo 366 días) y devolver los bloqueos que se superponen con él, ordenados por inicio, con filtros opcionales por profesional y por sillón.

#### Scenario: Superposición con el rango
- **GIVEN** un bloqueo de 08:00 a 10:00 y otro de 10:00 a 12:00 del mismo día
- **WHEN** se consultan bloqueos desde 10:00 hasta 11:00
- **THEN** el sistema devuelve solo el de 10:00 a 12:00

#### Scenario: Rango faltante o inválido
- **WHEN** falta `desde` o `hasta`, `desde` no es anterior a `hasta`, o el rango supera 366 días
- **THEN** el sistema lo rechaza como dato inválido

### Requirement: Aviso de turnos afectados
Al crear o editar un bloqueo, la respuesta SHALL incluir la lista de turnos activos que se superponen con la franja y el ámbito. El sistema MUST NOT cancelar, mover ni modificar esos turnos (PA-11 abierta). Mientras no existan turnos en el sistema la lista SHALL estar vacía.

#### Scenario: Sin turnos en el sistema
- **WHEN** el administrador crea un bloqueo
- **THEN** la respuesta incluye el bloqueo guardado y `turnosAfectados` vacío

#### Scenario: Turnos existentes no se tocan
- **GIVEN** el proceso de turnos informa un turno activo dentro de la franja
- **WHEN** el administrador crea el bloqueo
- **THEN** el bloqueo se guarda, la respuesta lista ese turno y el turno conserva su estado

### Requirement: Permisos sobre los bloqueos
Solo el administrador SHALL crear, editar y borrar bloqueos. Recepción SHALL consultar todos. Un usuario con rol profesional SHALL consultar solo los bloqueos de su profesional y los del consultorio. Sin sesión, el sistema MUST responder 401.

#### Scenario: Recepción o profesional intentan modificar
- **WHEN** un usuario de recepción o con rol profesional crea, edita o borra un bloqueo
- **THEN** el sistema responde 403 y no cambia nada

#### Scenario: Vista del profesional
- **GIVEN** existen un bloqueo del profesional A, uno del profesional B, uno de un sillón y uno del consultorio en el rango
- **WHEN** un usuario con rol profesional vinculado a A consulta bloqueos del rango
- **THEN** el sistema devuelve solo el de A y el del consultorio

#### Scenario: Profesional pide un bloqueo ajeno por id
- **WHEN** un usuario con rol profesional vinculado a A consulta por id un bloqueo de B o de un sillón
- **THEN** el sistema responde 403 sin datos

#### Scenario: Sin sesión
- **WHEN** se consulta o modifica un bloqueo sin token
- **THEN** el sistema responde 401
