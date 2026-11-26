# Spec Delta — auditoria-turnos

## ADDED Requirements

### Requirement: Historial ligado a un turno existente
Cada registro del historial SHALL referenciar un turno existente. El sistema MUST rechazar, sin guardar nada, un registro de un turno inexistente, también si se escribe directo en la base (restricción `fk_turno_historial_turno`, en H2 `MODE=MySQL` y MySQL 8).

#### Scenario: Registro de un turno inexistente
- **WHEN** se registra un cambio del turno 9999, que no existe, dentro de una transacción
- **THEN** el registro se rechaza y la transacción no confirma nada

#### Scenario: Inserción directa con turno inexistente
- **WHEN** se inserta por SQL un registro de historial con `turno_id` 9999 inexistente
- **THEN** la base lo rechaza por la restricción `fk_turno_historial_turno`

#### Scenario: Registro de un turno real
- **GIVEN** existe el turno 42
- **WHEN** se registra un cambio del turno 42 dentro de una transacción que termina bien
- **THEN** el registro queda guardado
