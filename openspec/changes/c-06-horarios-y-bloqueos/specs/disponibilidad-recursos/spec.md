# Spec Delta — disponibilidad-recursos

## Purpose

Responde de una única manera si un profesional o un sillón está disponible en un rango de tiempo según los horarios de atención y los bloqueos, para que la reserva online, la carga manual y el motor de disponibilidad apliquen RN-TU-04 sin duplicar reglas.

## ADDED Requirements

### Requirement: Profesional disponible dentro de su horario
Un profesional SHALL estar disponible en `[inicio, fin)` solo si está activo, el rango cae en un único día del consultorio y queda contenido en sus horarios de atención de ese día. Los rangos contiguos del mismo día SHALL tratarse como continuos.

#### Scenario: Dentro del horario
- **GIVEN** el profesional atiende los lunes de 09:00 a 13:00 y no hay bloqueos
- **WHEN** se consulta el lunes 2026-11-30 de 10:00 a 10:30 (hora del consultorio)
- **THEN** el profesional está disponible

#### Scenario: Bordes exactos del horario
- **GIVEN** el profesional atiende los lunes de 09:00 a 13:00
- **WHEN** se consulta de 09:00 a 09:30 o de 12:30 a 13:00
- **THEN** el profesional está disponible
- **WHEN** se consulta de 08:45 a 09:15 o de 12:45 a 13:15
- **THEN** el profesional no está disponible

#### Scenario: Rangos contiguos
- **GIVEN** el profesional atiende los lunes de 09:00 a 13:00 y de 13:00 a 17:00
- **WHEN** se consulta de 12:30 a 13:30
- **THEN** el profesional está disponible

#### Scenario: Hueco entre rangos
- **GIVEN** el profesional atiende los lunes de 09:00 a 13:00 y de 14:00 a 18:00
- **WHEN** se consulta de 12:30 a 14:30
- **THEN** el profesional no está disponible

#### Scenario: Día sin horario
- **GIVEN** el profesional no tiene horario los domingos
- **WHEN** se consulta un domingo
- **THEN** el profesional no está disponible

#### Scenario: Profesional inactivo
- **GIVEN** el profesional tiene horario pero está inactivo
- **WHEN** se consulta un rango dentro del horario
- **THEN** el profesional no está disponible

### Requirement: Día del consultorio y no día UTC
La consulta SHALL determinar el día de la semana y la hora en la zona `America/Argentina/Buenos_Aires`, aunque el rango en UTC caiga en otro día. Un rango que cruza la medianoche del consultorio MUST considerarse no disponible para el profesional.

#### Scenario: Turno nocturno que en UTC es del día siguiente
- **GIVEN** el profesional atiende los martes de 14:00 a 22:00
- **WHEN** se consulta de `2026-12-01T23:30Z` a `2026-12-02T00:30Z` (martes 20:30 a 21:30 en el consultorio)
- **THEN** el profesional está disponible

#### Scenario: Cruce de medianoche local
- **WHEN** se consulta del martes 23:30 al miércoles 00:30 del consultorio
- **THEN** el profesional no está disponible

### Requirement: Bloqueos que quitan disponibilidad
Un profesional MUST NOT estar disponible si se superpone con el rango algún bloqueo suyo o del consultorio. Un sillón SHALL estar disponible si está activo y no se superpone ningún bloqueo suyo ni del consultorio; los sillones no tienen horario propio. La superposición se evalúa con intervalos semiabiertos `[inicio, fin)`.

#### Scenario: Bloqueo parcial del profesional
- **GIVEN** el profesional atiende de 09:00 a 13:00 y tiene un bloqueo de 11:00 a 12:00
- **WHEN** se consulta de 10:30 a 11:30
- **THEN** el profesional no está disponible

#### Scenario: Bloqueo que termina justo al inicio
- **GIVEN** el profesional tiene un bloqueo de 09:00 a 10:00
- **WHEN** se consulta de 10:00 a 10:30
- **THEN** el profesional está disponible

#### Scenario: Bloqueo del consultorio
- **GIVEN** existe un bloqueo del consultorio el 25/12 todo el día
- **WHEN** se consulta cualquier profesional o sillón ese día
- **THEN** ninguno está disponible

#### Scenario: Bloqueo de otro recurso
- **GIVEN** existe un bloqueo del profesional B y otro del sillón 2 en el rango
- **WHEN** se consulta el profesional A y el sillón 1
- **THEN** ambos están disponibles si no tienen otro impedimento

#### Scenario: Sillón bloqueado con profesional libre
- **GIVEN** el sillón 1 tiene un bloqueo de 10:00 a 12:00 y el profesional A no
- **WHEN** se consulta de 10:00 a 10:30
- **THEN** el profesional A está disponible y el sillón 1 no

#### Scenario: Sillón inactivo
- **WHEN** se consulta un sillón inactivo
- **THEN** no está disponible

### Requirement: Rango de consulta válido
La consulta MUST exigir un inicio anterior al fin y un recurso existente.

#### Scenario: Rango vacío o invertido
- **WHEN** se consulta con inicio igual o posterior al fin
- **THEN** la consulta se rechaza como dato inválido

#### Scenario: Recurso inexistente
- **WHEN** se consulta un profesional o sillón que no existe
- **THEN** la consulta responde "no encontrado"

### Requirement: Datos para el cálculo de huecos
El sistema SHALL ofrecer a los procesos de agenda, para un profesional y un día del consultorio, sus rangos de atención (con los contiguos unidos) y los bloqueos que lo afectan en ese día, con la misma interpretación de zona y de intervalos que la consulta de disponibilidad.

#### Scenario: Franjas de un día
- **GIVEN** el profesional atiende los lunes de 09:00 a 13:00 y de 13:00 a 15:00, y de 16:00 a 18:00
- **WHEN** se piden sus franjas del lunes 2026-11-30
- **THEN** el sistema entrega 09:00–15:00 y 16:00–18:00 de ese día en la hora del consultorio
