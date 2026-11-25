# auditoria-turnos Specification

## Purpose

Deja constancia permanente de cada cambio de un turno (quién lo hizo, cuándo, qué acción y qué valores cambiaron), registrada en la misma operación que el cambio y sin posibilidad de editarla ni borrarla, para cumplir RN-AU-01 y RN-SO-02.

## Requirements

### Requirement: Registro de quién y cuándo en cada cambio de turno
Cada cambio de un turno SHALL generar un registro con el turno, el actor, la fecha y hora del cambio, la acción y los valores anterior y nuevo de los campos que cambiaron (RN-AU-01). La fecha y hora MUST salir de la hora actual del sistema, no de un dato enviado por quien llama.

#### Scenario: Cambio de estado hecho por recepción
- **GIVEN** la hora actual del sistema es `2026-12-01T13:15:00Z`
- **WHEN** el usuario de recepción con id 7 cambia el turno 42 de `pendiente` a `confirmado`
- **THEN** queda un registro del turno 42 con actor usuario 7, fecha y hora `2026-12-01T13:15:00Z`, acción `cambio_estado`, valor anterior `{"estado":"pendiente"}` y valor nuevo `{"estado":"confirmado"}`

#### Scenario: Alta de un turno
- **WHEN** se registra la creación del turno 43
- **THEN** el registro tiene acción `creacion`, valor anterior vacío y valor nuevo con los campos iniciales del turno

#### Scenario: Sobreturno registra quién y cuándo
- **WHEN** el usuario de recepción con id 7 crea el sobreturno 44
- **THEN** el registro tiene acción `creacion`, actor usuario 7, la fecha y hora del alta y `"es_sobreturno":"true"` en el valor nuevo (RN-SO-02)

### Requirement: Actor identificado
Todo registro SHALL tener exactamente un tipo de actor: `usuario` (personal autenticado, con su id), `paciente` (desde su enlace del turno) o `sistema` (tareas automáticas). El sistema MUST rechazar un registro con actor `usuario` sin id, con un id de usuario inexistente o con id en un actor `paciente` o `sistema`.

#### Scenario: Cambio hecho por el paciente
- **WHEN** el paciente cancela su turno desde el enlace
- **THEN** el registro tiene actor `paciente` y ningún id de usuario

#### Scenario: Cambio hecho por una tarea automática
- **WHEN** una tarea programada cambia el estado de un turno
- **THEN** el registro tiene actor `sistema` y ningún id de usuario

#### Scenario: Actor usuario sin id
- **WHEN** se intenta registrar un cambio con actor `usuario` y sin id de usuario
- **THEN** el registro se rechaza con un error de validación y no se guarda nada

#### Scenario: Usuario inexistente
- **WHEN** se intenta registrar un cambio con actor usuario 9999, que no existe
- **THEN** el registro se rechaza y no se guarda nada

### Requirement: Acción de un catálogo cerrado
La acción SHALL ser una de `creacion`, `confirmacion`, `cancelacion`, `reprogramacion` o `cambio_estado`. El sistema MUST rechazar cualquier otra acción, también si se escribe directo en la base.

#### Scenario: Acción desconocida
- **WHEN** se intenta guardar un registro con acción `borrado`
- **THEN** el registro se rechaza y no se guarda nada

### Requirement: Valores anterior y nuevo legibles
Los valores anterior y nuevo SHALL guardarse como texto JSON con pares campo-valor de texto, solo de los campos que cambiaron. Al menos uno de los dos MUST tener contenido; el anterior MAY quedar vacío solo en la acción `creacion`. El sistema MUST rechazar valores que no entren en el largo máximo de la columna en lugar de truncarlos.

#### Scenario: Reprogramación guarda los instantes en UTC
- **WHEN** se reprograma el turno 42 de `2026-12-01T12:00:00Z` a `2026-12-02T15:30:00Z`
- **THEN** el valor anterior es `{"inicio":"2026-12-01T12:00:00Z"}` y el nuevo `{"inicio":"2026-12-02T15:30:00Z"}`, en UTC, que se muestran en hora de `America/Argentina/Buenos_Aires` al consultarlos

#### Scenario: Cambio sin valores
- **WHEN** se intenta registrar un `cambio_estado` sin valor anterior ni nuevo
- **THEN** el registro se rechaza con un error de validación

#### Scenario: Valores demasiado largos
- **WHEN** los valores de un cambio superan el largo máximo permitido
- **THEN** el registro se rechaza con un error de validación y el cambio auditado tampoco se confirma

### Requirement: Registro atómico con el cambio auditado
El registro SHALL confirmarse o revertirse junto con el cambio de turno que audita, en la misma transacción. El sistema MUST rechazar un intento de registrar un cambio fuera de una transacción en curso.

#### Scenario: Se confirman juntos
- **WHEN** un cambio de turno y su registro se escriben en la misma operación y la operación termina bien
- **THEN** tanto el cambio como el registro quedan guardados

#### Scenario: Falla el cambio después de registrar
- **WHEN** se escribe el registro y después la operación falla (por ejemplo, por un solapamiento detectado)
- **THEN** no queda guardado ni el cambio ni el registro

#### Scenario: Registro suelto sin transacción
- **WHEN** se intenta registrar un cambio sin una transacción abierta
- **THEN** se rechaza con un error y no se guarda nada

### Requirement: Historial solo de inserción
Un registro guardado MUST NOT poder modificarse ni borrarse a través de la aplicación. El sistema SHALL ofrecer únicamente agregar registros y consultarlos.

#### Scenario: Intento de modificar un registro
- **GIVEN** un registro guardado del turno 42 con acción `confirmacion`
- **WHEN** se intenta cambiar su acción o sus valores desde la aplicación y se confirma la transacción
- **THEN** el registro sigue igual en la base

#### Scenario: Sin operación de borrado
- **WHEN** se revisan las operaciones que la aplicación ofrece sobre el historial
- **THEN** no existe ninguna operación para modificar ni para borrar registros

### Requirement: Consulta cronológica por turno
El sistema SHALL devolver el historial de un turno ordenado del cambio más antiguo al más reciente, con desempate estable por orden de alta cuando dos registros tienen la misma fecha y hora. Un turno sin cambios devuelve una lista vacía.

#### Scenario: Historial ordenado
- **GIVEN** el turno 42 tiene registros de `creacion` a las `12:00:00Z`, `confirmacion` a las `12:05:00Z` y `cancelacion` a las `12:30:00Z`
- **WHEN** se consulta el historial del turno 42
- **THEN** se devuelven los tres en ese orden y ninguno de otro turno

#### Scenario: Misma fecha y hora
- **WHEN** dos registros del turno 42 tienen la misma fecha y hora
- **THEN** aparecen en el orden en que se agregaron

#### Scenario: Turno sin historial
- **WHEN** se consulta el historial de un turno sin registros
- **THEN** se devuelve una lista vacía
