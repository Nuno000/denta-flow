# motor-disponibilidad Specification

## Purpose

Calcula de una única manera los huecos que se pueden ofrecer para una prestación y asigna profesional y sillón, para que la reserva online, la carga manual, la reprogramación y la lista de espera reutilicen las mismas reglas (RN-DI-01, RN-TU-04, RN-TU-07, PA-05, PA-06) y confirmen el hueco elegido por la única vía de escritura de turnos.

## Requirements

### Requirement: Huecos donde entra la duración completa
El sistema SHALL ofrecer como hueco un inicio solo si el rango `[inicio, inicio + duración de la prestación)` queda completo dentro de una franja de atención del profesional en un único día del consultorio (RN-DI-01). Los inicios SHALL generarse en una grilla que parte del comienzo de cada franja con un paso configurable (15 minutos por defecto).

#### Scenario: La duración no entra al final de la franja
- **GIVEN** el profesional A atiende los lunes de 09:00 a 10:00, la prestación dura 45 minutos y el paso es 15 minutos
- **WHEN** se piden los huecos del lunes 2026-11-30
- **THEN** se ofrecen 09:00 y 09:15, y no se ofrecen 09:30 ni 09:45

#### Scenario: Borde exacto de la franja
- **GIVEN** el profesional A atiende los lunes de 09:00 a 13:00 y la prestación dura 30 minutos
- **WHEN** se piden los huecos del lunes 2026-11-30
- **THEN** el último hueco ofrecido es 12:30 a 13:00 y no hay ninguno que termine después de las 13:00

#### Scenario: Franjas contiguas se tratan como continuas
- **GIVEN** el profesional A atiende los lunes de 09:00 a 13:00 y de 13:00 a 17:00, y la prestación dura 60 minutos
- **WHEN** se piden los huecos del lunes 2026-11-30
- **THEN** se ofrece el hueco de 12:30 a 13:30

#### Scenario: Día sin horario
- **GIVEN** el profesional A no atiende los domingos
- **WHEN** se piden los huecos de un domingo
- **THEN** no se ofrece ningún hueco de A ese día

### Requirement: Bloqueos que quitan huecos
El sistema MUST NOT ofrecer un hueco que se superponga con un bloqueo del profesional, de todos los sillones candidatos o del consultorio, con intervalos semiabiertos. Un bloqueo de un sillón SHALL quitar solo ese sillón de la asignación.

#### Scenario: Bloqueo parcial del profesional
- **GIVEN** el profesional A atiende de 09:00 a 13:00, tiene un bloqueo de 11:00 a 12:00 y la prestación dura 30 minutos
- **WHEN** se piden los huecos de ese día
- **THEN** no se ofrecen 10:45, 11:00, 11:15, 11:30 ni 11:45, y se ofrecen 10:30 y 12:00

#### Scenario: Bloqueo del consultorio
- **GIVEN** existe un bloqueo del consultorio el 2026-12-25 todo el día
- **WHEN** se piden los huecos de ese día para cualquier profesional
- **THEN** no se ofrece ningún hueco

#### Scenario: Sillón bloqueado con otro sillón libre
- **GIVEN** hay dos sillones activos 1 y 2, y el sillón 1 tiene un bloqueo de 10:00 a 12:00
- **WHEN** se piden los huecos de 10:00
- **THEN** se ofrece el hueco de 10:00 con el sillón 2 asignado

### Requirement: Turnos activos de profesional y sillón
El sistema MUST NOT ofrecer un hueco que se superponga con un turno activo (no cancelado) del profesional, ni asignar un sillón con un turno activo superpuesto (RN-TU-07). Un hueco SHALL existir solo si hay a la vez profesional y sillón libres. Los turnos cancelados y los sobreturnos MUST NOT ocupar el horario, igual que en la verificación de la creación.

#### Scenario: Hueco entre dos turnos donde no entra la duración
- **GIVEN** el profesional A tiene turnos de 09:00 a 09:30 y de 10:00 a 10:30, y la prestación dura 45 minutos
- **WHEN** se piden los huecos de ese día
- **THEN** no se ofrece ningún hueco que empiece entre 09:15 y 09:45

#### Scenario: Hueco que empieza justo cuando termina un turno
- **GIVEN** el profesional A tiene un turno de 09:00 a 09:30
- **WHEN** se piden los huecos de ese día con una prestación de 30 minutos
- **THEN** se ofrece el hueco de 09:30 a 10:00

#### Scenario: Sillón ocupado con profesional libre
- **GIVEN** hay un único sillón activo, con un turno del profesional B de 10:00 a 10:30, y el profesional A está libre
- **WHEN** se piden los huecos de A a las 10:00
- **THEN** no se ofrece el hueco de 10:00

#### Scenario: Turno cancelado y sobreturno no ocupan
- **GIVEN** el profesional A tuvo un turno de 10:00 a 10:30 que fue cancelado y tiene un sobreturno de 11:00 a 11:30
- **WHEN** se piden los huecos de ese día
- **THEN** se ofrecen los huecos de 10:00 y de 11:00

### Requirement: Profesional elegido o cualquiera
La consulta SHALL aceptar un profesional opcional (PA-05). Con profesional, los huecos son solo suyos y el profesional MUST existir, estar activo y realizar la prestación. Sin profesional ("cualquiera"), SHALL considerar a todos los profesionales activos que realizan la prestación y ofrecer cada inicio una sola vez.

#### Scenario: Cualquiera une los horarios de varios profesionales
- **GIVEN** los profesionales A y B realizan la prestación, A está libre a las 09:00 y B a las 09:00 y a las 10:00
- **WHEN** se piden los huecos sin profesional
- **THEN** se ofrecen 09:00 y 10:00, cada uno una sola vez

#### Scenario: Profesional que no realiza la prestación
- **WHEN** se piden huecos con un profesional que no realiza la prestación o está inactivo
- **THEN** la consulta se rechaza como dato inválido

#### Scenario: Profesional inactivo excluido de cualquiera
- **GIVEN** el profesional C realiza la prestación pero está inactivo
- **WHEN** se piden los huecos sin profesional
- **THEN** ningún hueco se asigna a C

### Requirement: Candidatos ordenados de asignación
Cada hueco SHALL incluir sus candidatos (profesional, sillón) ordenados: primero por profesional (el elegido, o los de "cualquiera" por id ascendente) y, para cada profesional, por sillón libre en id ascendente (PA-06). El paciente MUST NOT elegir el sillón.

#### Scenario: Primer sillón libre
- **GIVEN** los sillones activos 1, 2 y 3, con el 1 ocupado a las 10:00
- **WHEN** se pide el hueco de las 10:00 del profesional A
- **THEN** sus candidatos son (A, 2) y luego (A, 3)

#### Scenario: Orden de profesionales en cualquiera
- **GIVEN** los profesionales con id 4 y 7 realizan la prestación y ambos están libres a las 10:00 con el sillón 1 libre
- **WHEN** se piden los huecos sin profesional
- **THEN** el hueco de las 10:00 tiene como primer candidato al profesional 4

### Requirement: Recursos y prestación activos
El sistema MUST considerar solo sillones activos y una prestación activa. Una prestación inexistente o un profesional inexistente SHALL responder "no encontrado"; una prestación inactiva SHALL rechazarse como dato inválido.

#### Scenario: Sillón inactivo
- **GIVEN** el único sillón libre a las 10:00 está inactivo
- **WHEN** se piden los huecos de las 10:00
- **THEN** no se ofrece el hueco

#### Scenario: Prestación inexistente
- **WHEN** se piden huecos de una prestación que no existe
- **THEN** la consulta responde "no encontrado"

### Requirement: Rango de fechas del consultorio acotado
La consulta SHALL recibir fechas `desde` y `hasta` del consultorio (inclusive), con `desde` no posterior a `hasta` y como máximo 14 días (configurable). Las fechas anteriores a hoy en el consultorio SHALL ignorarse. Un rango inválido MUST rechazarse como dato inválido.

#### Scenario: Rango demasiado largo
- **WHEN** se piden huecos del 2026-12-01 al 2026-12-31
- **THEN** la consulta se rechaza como dato inválido

#### Scenario: Rango invertido
- **WHEN** se piden huecos con `desde` posterior a `hasta`
- **THEN** la consulta se rechaza como dato inválido

### Requirement: Sin huecos en el pasado
El sistema MUST NOT ofrecer huecos cuyo inicio sea anterior al instante actual del reloj del sistema.

#### Scenario: Huecos de hoy ya pasados
- **GIVEN** son las 10:10 del lunes 2026-11-30 en el consultorio y el profesional A atiende de 09:00 a 13:00
- **WHEN** se piden los huecos de ese día con paso de 15 minutos
- **THEN** el primer hueco ofrecido es el de 10:15

### Requirement: Zona horaria del consultorio
Los días, franjas y fechas de la consulta SHALL interpretarse en `America/Argentina/Buenos_Aires` y los huecos SHALL expresarse como instantes absolutos, aunque el día UTC sea otro.

#### Scenario: Hueco nocturno que en UTC es del día siguiente
- **GIVEN** el profesional A atiende los martes de 20:00 a 22:00 y la prestación dura 30 minutos
- **WHEN** se piden los huecos del martes 2026-12-01
- **THEN** se ofrece el hueco de `2026-12-01T21:30-03:00` (`2026-12-02T00:30Z`) y ninguno del miércoles 2026-12-02

#### Scenario: Límite del rango en día del consultorio
- **GIVEN** el profesional A tiene un turno el miércoles 2026-12-02 a las 00:30Z (martes 21:30 en el consultorio)
- **WHEN** se piden los huecos solo del martes 2026-12-01
- **THEN** ese turno se tiene en cuenta y el hueco de las 21:30 no se ofrece

### Requirement: Excluir un turno propio al reprogramar
La consulta SHALL aceptar opcionalmente un turno a excluir, que no ocupa el horario, para ofrecer huecos de reprogramación sin que el turno se bloquee a sí mismo.

#### Scenario: Correr el propio turno
- **GIVEN** el turno 42 del profesional A está de 10:00 a 10:30 y es el único turno del día
- **WHEN** se piden los huecos excluyendo el turno 42
- **THEN** se ofrecen los huecos de 10:00 y de 10:15

### Requirement: Reserva por candidato con una transacción por intento
Al reservar un horario, el sistema SHALL recalcular sus candidatos con datos actuales y crear el turno con el primero por la única vía de escritura de turnos, en una transacción propia por candidato. Si un intento falla por solapamiento, agenda no disponible o interbloqueo, SHALL probar el siguiente sin retener bloqueos del intento anterior.

#### Scenario: El primer candidato fue tomado en el medio
- **GIVEN** el hueco de las 10:00 tiene candidatos (A, 1) y (B, 1), y entre el cálculo y la reserva otro paciente tomó a A a las 10:00
- **WHEN** se reserva el hueco de las 10:00 sin profesional
- **THEN** el turno se crea con el profesional B y el sillón 1

#### Scenario: Errores que no son de disponibilidad
- **WHEN** se reserva con un paciente inexistente
- **THEN** se responde "no encontrado" sin probar otros candidatos

### Requirement: Hueco tomado se informa como conflicto
Si el horario elegido ya no tiene candidatos o todos los intentos fallan, el sistema SHALL responder un conflicto (409) sin crear ningún turno, para que el consumidor ofrezca otros huecos (Flujo 1, caso de error).

#### Scenario: Hueco de un único profesional tomado
- **GIVEN** el profesional A tiene libre las 10:00 y otro paciente lo reserva primero
- **WHEN** se reserva A a las 10:00
- **THEN** se responde conflicto y no se crea ningún turno

#### Scenario: Horario fuera de la franja o bloqueado
- **WHEN** se reserva un inicio fuera del horario de atención o dentro de un bloqueo
- **THEN** se responde conflicto y no se crea ningún turno

### Requirement: Reservas simultáneas del mismo hueco
Ante reservas simultáneas del mismo horario, el sistema SHALL confirmar como máximo un turno por profesional y por sillón, y las demás reservas SHALL caer en otro candidato o recibir un conflicto, verificado contra MySQL 8 real (perfil `it-mysql`).

#### Scenario: Un solo profesional y un solo sillón
- **WHEN** dos reservas del profesional A a las 10:00 llegan a la vez con un único sillón
- **THEN** exactamente una crea el turno y la otra recibe conflicto

#### Scenario: Cualquiera con dos profesionales y dos sillones
- **GIVEN** los profesionales A y B y los sillones 1 y 2 están libres a las 10:00
- **WHEN** dos reservas sin profesional de las 10:00 llegan a la vez
- **THEN** ambas crean turno, con profesionales distintos y sillones distintos

### Requirement: Consulta pública de huecos
El sistema SHALL exponer sin autenticación la consulta de huecos por prestación, profesional opcional y rango de fechas, devolviendo por hueco solo inicio y fin con el desplazamiento del consultorio y, si se pidió un profesional, ese profesional. La respuesta MUST NOT incluir sillones, pacientes ni otros datos personales.

#### Scenario: Visitante consulta huecos
- **WHEN** un visitante sin token pide los huecos de una prestación del 2026-11-30 al 2026-12-06
- **THEN** recibe 200 con la lista ordenada por inicio, cada hueco como `inicio` y `fin` con `-03:00`

#### Scenario: Parámetros inválidos
- **WHEN** falta la prestación, una fecha no tiene formato `AAAA-MM-DD` o el rango supera el máximo
- **THEN** recibe 400 con el detalle del error en español

#### Scenario: Sin huecos
- **GIVEN** no hay huecos en el rango
- **WHEN** un visitante los pide
- **THEN** recibe 200 con una lista vacía
