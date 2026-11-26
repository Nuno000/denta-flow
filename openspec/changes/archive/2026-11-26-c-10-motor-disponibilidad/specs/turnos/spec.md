# Spec Delta — turnos

## ADDED Requirements

### Requirement: Turno dentro del horario de atención y fuera de bloqueos
Crear o reprogramar un turno, incluido el sobreturno, SHALL exigir que el profesional esté disponible según su horario de atención y sus bloqueos y los del consultorio, y que el sillón no tenga bloqueos propios ni del consultorio en `[inicio, fin)` (RN-TU-04). La verificación MUST hacerse dentro de la misma transacción, con los bloqueos de profesional y sillón tomados y antes de verificar el solapamiento; si falla, SHALL responder un conflicto y no guardar nada.

#### Scenario: Turno fuera del horario de atención
- **GIVEN** el profesional 1 atiende los lunes de 09:00 a 13:00 (hora del consultorio)
- **WHEN** se crea un turno del profesional 1 el lunes 2026-11-30 de 12:45 a 13:15
- **THEN** se rechaza con un conflicto y no se crea ningún turno ni registro de historial

#### Scenario: Turno dentro de un bloqueo del profesional
- **GIVEN** el profesional 1 atiende de 09:00 a 13:00 y tiene un bloqueo de 11:00 a 12:00
- **WHEN** se crea un turno del profesional 1 de 11:30 a 12:00
- **THEN** se rechaza con un conflicto

#### Scenario: Sillón bloqueado
- **GIVEN** el sillón 1 tiene un bloqueo de 10:00 a 12:00 y el profesional 1 está en horario
- **WHEN** se crea un turno del profesional 1 en el sillón 1 de 10:00 a 10:30
- **THEN** se rechaza con un conflicto

#### Scenario: Borde exacto del horario
- **GIVEN** el profesional 1 atiende los lunes de 09:00 a 13:00
- **WHEN** se crea un turno de 12:30 a 13:00
- **THEN** el turno se crea

#### Scenario: Sobreturno fuera de horario
- **GIVEN** el profesional 1 atiende los lunes de 09:00 a 13:00
- **WHEN** recepción crea un sobreturno del profesional 1 de 13:00 a 13:30
- **THEN** se rechaza con un conflicto (el sobreturno solo exime del solapamiento)

#### Scenario: Reprogramar a una franja bloqueada
- **GIVEN** el turno 42 está en horario y existe un bloqueo del consultorio de 15:00 a 16:00
- **WHEN** se reprograma el turno 42 a las 15:00
- **THEN** se rechaza con un conflicto y el turno queda como estaba

#### Scenario: Bloqueo creado después del turno
- **GIVEN** el turno 42 fue creado de 10:00 a 10:30 y después se cargó un bloqueo del profesional de 10:00 a 12:00
- **WHEN** se consulta el turno 42
- **THEN** el turno sigue activo y sin cambios (DD-12)
