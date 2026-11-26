# Spec Delta — turnos

## Purpose

Define el turno del consultorio y su única vía de escritura: duración por prestación, estados y transiciones, y la garantía transaccional de que ningún flujo superpone un profesional ni un sillón salvo el sobreturno explícito (DD-02, DD-09, RN-TU-01..07, RN-GL-02).

## ADDED Requirements

### Requirement: Duración del turno definida por la prestación
Al crear un turno el sistema SHALL fijar `fin = inicio + duración de la prestación` (RN-TU-03) y guardarlo junto al turno; quien llama MUST NOT poder elegir el fin. Inicio y fin SHALL guardarse como instantes UTC con precisión de minuto.

#### Scenario: Turno de una prestación de 30 minutos
- **GIVEN** la prestación `Limpieza` dura 30 minutos
- **WHEN** se crea un turno de `Limpieza` el 2026-12-01 a las 09:00 de `America/Argentina/Buenos_Aires`
- **THEN** el turno guarda inicio `2026-12-01T12:00:00Z` y fin `2026-12-01T12:30:00Z`

#### Scenario: Inicio con segundos
- **WHEN** se intenta crear un turno con inicio `2026-12-01T12:00:30Z`
- **THEN** se rechaza con un error de validación

### Requirement: Recursos válidos para el turno
El sistema SHALL crear un turno solo si el paciente existe, la prestación, el profesional y el sillón existen y están activos, y el profesional realiza esa prestación; si no, MUST rechazarlo sin guardar nada. Un recurso inexistente MUST informarse como no encontrado.

#### Scenario: Profesional que no realiza la prestación
- **WHEN** se crea un turno de `Ortodoncia` con un profesional que no la realiza
- **THEN** se rechaza con un error de validación y no se guarda ningún turno ni registro de historial

#### Scenario: Sillón inactivo
- **WHEN** se crea un turno en un sillón desactivado
- **THEN** se rechaza con un error de validación

#### Scenario: Profesional inexistente
- **WHEN** se crea un turno con el profesional 9999, que no existe
- **THEN** se rechaza como recurso no encontrado

### Requirement: Inicio no anterior a la hora actual
El sistema MUST rechazar crear o reprogramar un turno cuyo inicio sea anterior a la hora actual del sistema.

#### Scenario: Turno en el pasado
- **GIVEN** la hora actual del sistema es `2026-12-01T13:00:00Z`
- **WHEN** se crea un turno con inicio `2026-12-01T12:00:00Z`
- **THEN** se rechaza con un error de validación

### Requirement: Estado inicial pendiente
Todo turno SHALL nacer en estado `pendiente`, tanto el de origen `online` como el de `recepcion` (PA-10).

#### Scenario: Reserva online
- **WHEN** se crea un turno de origen `online`
- **THEN** su estado es `pendiente`

#### Scenario: Carga de recepción
- **WHEN** recepción crea un turno
- **THEN** su estado es `pendiente`

### Requirement: Origen y actor coherentes
Un turno de origen `online` SHALL crearse con actor paciente y sin usuario creador; uno de origen `recepcion` MUST tener como creador al usuario del personal que lo carga. El sistema MUST rechazar cualquier otra combinación.

#### Scenario: Recepción sin usuario
- **WHEN** se crea un turno de origen `recepcion` con actor paciente
- **THEN** se rechaza con un error de validación

#### Scenario: Creador registrado
- **WHEN** el usuario 7 crea un turno de origen `recepcion`
- **THEN** el turno guarda como creador al usuario 7

### Requirement: Sin solapamiento por profesional
El sistema MUST rechazar con un conflicto crear o reprogramar un turno no sobreturno cuyo rango `[inicio, fin)` se superponga con otro turno no cancelado y no sobreturno del mismo profesional (RN-TU-01). Rangos contiguos MUST NOT considerarse superpuestos.

#### Scenario: Superposición parcial
- **GIVEN** el profesional 1 tiene un turno `confirmado` de 12:00Z a 12:30Z
- **WHEN** se crea otro turno del profesional 1 de 12:15Z a 12:45Z en otro sillón
- **THEN** se rechaza con un conflicto que indica que el profesional está ocupado y no se guarda nada

#### Scenario: Turnos contiguos
- **GIVEN** el profesional 1 tiene un turno de 12:00Z a 12:30Z
- **WHEN** se crea otro turno del profesional 1 de 12:30Z a 13:00Z
- **THEN** el turno se crea

#### Scenario: Otro profesional en el mismo horario
- **GIVEN** el profesional 1 tiene un turno de 12:00Z a 12:30Z en el sillón 1
- **WHEN** se crea un turno del profesional 2 de 12:00Z a 12:30Z en el sillón 2
- **THEN** el turno se crea

### Requirement: Sin solapamiento por sillón
El sistema MUST rechazar con un conflicto crear o reprogramar un turno no sobreturno cuyo rango `[inicio, fin)` se superponga con otro turno no cancelado y no sobreturno en el mismo sillón, aunque el profesional esté libre (RN-TU-02, RN-TU-07).

#### Scenario: Sillón ocupado con profesional libre
- **GIVEN** el sillón 1 tiene un turno del profesional 1 de 12:00Z a 12:30Z
- **WHEN** se crea un turno del profesional 2 en el sillón 1 de 12:10Z a 12:40Z
- **THEN** se rechaza con un conflicto que indica que el sillón está ocupado

### Requirement: Solapamiento evaluado sobre instantes absolutos
La superposición SHALL evaluarse sobre los instantes UTC, de modo que los turnos que cruzan la medianoche de `America/Argentina/Buenos_Aires` se comparen correctamente.

#### Scenario: Turno que cruza la medianoche local
- **GIVEN** el profesional 1 tiene un turno el 2026-12-01 de 23:45 a 00:15 del 2026-12-02 (hora local)
- **WHEN** se crea un turno del profesional 1 el 2026-12-02 a las 00:00 (hora local)
- **THEN** se rechaza con un conflicto

### Requirement: Cancelación libera profesional y sillón
Un turno `cancelado` MUST NOT ocupar su profesional ni su sillón: su rango SHALL quedar disponible para nuevos turnos (RN-TU-06).

#### Scenario: Reserva sobre un turno cancelado
- **GIVEN** el turno 42 del profesional 1 y el sillón 1 de 12:00Z a 12:30Z fue cancelado
- **WHEN** se crea un turno del profesional 1 en el sillón 1 de 12:00Z a 12:30Z
- **THEN** el turno se crea

### Requirement: Sobreturno explícito
Un turno marcado como sobreturno SHALL crearse aunque se superponga con otros turnos, y MUST NOT impedir la creación de otros turnos (RN-TU-07, RN-SO-01). Solo el origen `recepcion` con un usuario del personal MAY marcarlo; el origen `online` MUST NOT crear sobreturnos (RN-DI-02).

#### Scenario: Sobreturno superpuesto
- **GIVEN** el profesional 1 tiene un turno de 12:00Z a 12:30Z en el sillón 1
- **WHEN** el usuario 7 de recepción crea un sobreturno del profesional 1 en el sillón 1 de 12:00Z a 12:30Z
- **THEN** el sobreturno se crea con `es_sobreturno` verdadero y su historial registra al usuario 7, la hora y `"es_sobreturno":"true"` (RN-SO-02)

#### Scenario: Sin la marca no hay excepción
- **GIVEN** el profesional 1 tiene un turno de 12:00Z a 12:30Z
- **WHEN** recepción crea un turno no marcado como sobreturno del profesional 1 en ese horario
- **THEN** se rechaza con un conflicto

#### Scenario: Sobreturno desde la reserva online
- **WHEN** se intenta crear un sobreturno de origen `online`
- **THEN** se rechaza con un error de validación y no se guarda nada

#### Scenario: El sobreturno no ocupa el horario
- **GIVEN** existe solo un sobreturno del profesional 1 en el sillón 1 de 12:00Z a 12:30Z
- **WHEN** se crea un turno común del profesional 1 en el sillón 1 de 12:00Z a 12:30Z
- **THEN** el turno se crea

### Requirement: Reservas simultáneas sin doble ocupación
Ante creaciones o reprogramaciones simultáneas que compiten por el mismo profesional o el mismo sillón en rangos superpuestos, el sistema SHALL confirmar como máximo una y rechazar las demás con un conflicto, verificado contra MySQL 8 real (perfil `it-mysql`).

#### Scenario: Mismo profesional, mismo horario
- **WHEN** dos operaciones crean a la vez un turno del profesional 1 de 12:00Z a 12:30Z, en los sillones 1 y 2
- **THEN** exactamente una se confirma, la otra recibe un conflicto y queda un solo turno activo del profesional 1 en ese rango

#### Scenario: Mismo sillón, distinto profesional
- **WHEN** dos operaciones crean a la vez un turno en el sillón 1 de 12:00Z a 12:30Z, con los profesionales 1 y 2
- **THEN** exactamente una se confirma y la otra recibe un conflicto

#### Scenario: Recursos distintos no se bloquean entre sí
- **WHEN** dos operaciones crean a la vez un turno del profesional 1 en el sillón 1 y otro del profesional 2 en el sillón 2 en el mismo horario
- **THEN** ambas se confirman sin interbloqueo

### Requirement: Bloqueo de recursos en orden fijo
Toda creación o reprogramación SHALL bloquear para escritura la fila del profesional y después la del sillón, en ese orden, antes de verificar el solapamiento, y la verificación MUST leer el último estado confirmado (DD-09).

#### Scenario: Orden de bloqueo
- **WHEN** se crea un turno del profesional 1 en el sillón 2
- **THEN** primero se bloquea el profesional 1, después el sillón 2, y recién entonces se verifica el solapamiento y se escribe

#### Scenario: Operaciones cruzadas sin interbloqueo
- **WHEN** a la vez una operación crea un turno del profesional 1 en el sillón 2 y otra uno del profesional 2 en el sillón 2 en rangos superpuestos
- **THEN** ninguna falla por interbloqueo: una se confirma y la otra recibe un conflicto

### Requirement: Transiciones de estado explícitas
El estado SHALL cambiar solo por transiciones válidas: `pendiente` → `confirmado`, `cancelado`, `asistio`, `ausente`, `ausente_con_aviso`; `confirmado` → `cancelado`, `asistio`, `ausente`, `ausente_con_aviso`; entre `asistio`, `ausente` y `ausente_con_aviso` para corregir la asistencia. `cancelado` es final. Toda otra transición MUST rechazarse con un conflicto sin cambiar nada (RN-TU-05).

#### Scenario: Confirmación de un pendiente
- **WHEN** se confirma un turno `pendiente`
- **THEN** queda `confirmado`

#### Scenario: Turno cancelado no se reactiva
- **WHEN** se intenta confirmar un turno `cancelado`
- **THEN** se rechaza con un conflicto y el turno sigue `cancelado`

#### Scenario: Asistencia no vuelve a confirmado
- **WHEN** se intenta pasar a `confirmado` un turno `asistio`
- **THEN** se rechaza con un conflicto

#### Scenario: Corrección de asistencia
- **WHEN** se pasa a `ausente` un turno marcado `asistio`
- **THEN** queda `ausente` y el cambio queda auditado

### Requirement: Repetir el estado actual es idempotente
Pedir para un turno el estado que ya tiene SHALL terminar sin error, sin modificar el turno y sin agregar registros al historial.

#### Scenario: Doble confirmación
- **GIVEN** el turno 42 está `confirmado`
- **WHEN** se lo confirma otra vez
- **THEN** la operación termina bien, el turno sigue `confirmado` y el historial no suma registros

### Requirement: Reprogramación verificada
Reprogramar SHALL mover un turno `pendiente` o `confirmado` a un nuevo inicio, profesional y sillón, recalculando el fin por la prestación, conservando estado, paciente y token, y aplicando las mismas verificaciones que la creación sin contar al propio turno. Otros estados MUST rechazarse con un conflicto.

#### Scenario: Correr el turno 15 minutos
- **GIVEN** el turno 42 del profesional 1 está de 12:00Z a 12:30Z
- **WHEN** se lo reprograma a las 12:15Z con el mismo profesional y sillón
- **THEN** queda de 12:15Z a 12:45Z sin conflicto consigo mismo

#### Scenario: Destino ocupado
- **WHEN** se reprograma un turno a un rango donde el sillón destino tiene otro turno activo
- **THEN** se rechaza con un conflicto y el turno queda como estaba

#### Scenario: Reprogramar un cancelado
- **WHEN** se intenta reprogramar un turno `cancelado`
- **THEN** se rechaza con un conflicto

### Requirement: Cambios de turno auditados en la misma transacción
Cada creación, cambio de estado y reprogramación SHALL registrar en el historial (C-08) al actor, la acción y los valores que cambiaron, en la misma transacción: si la operación falla, MUST NOT quedar ni el cambio ni el registro (RN-AU-01). Confirmar usa `confirmacion`, cancelar `cancelacion`, reprogramar `reprogramacion`, crear `creacion` y el resto `cambio_estado`.

#### Scenario: Cancelación por el paciente
- **WHEN** el paciente cancela el turno 42 `confirmado` desde su enlace
- **THEN** el historial registra acción `cancelacion`, actor `paciente`, anterior `{"estado":"confirmado"}` y nuevo `{"estado":"cancelado"}`

#### Scenario: Conflicto sin rastros
- **WHEN** una creación se rechaza por solapamiento
- **THEN** no queda ningún turno nuevo ni registro de historial

#### Scenario: Reprogramación auditada en UTC
- **WHEN** se reprograma el turno 42 de `2026-12-01T12:00:00Z` a `2026-12-02T15:30:00Z`
- **THEN** el historial registra `reprogramacion` con los valores anteriores y nuevos de inicio y fin en UTC

### Requirement: Única vía de escritura de turnos
Toda creación y modificación de turnos SHALL pasar por el servicio transaccional de turnos (RN-GL-02); ningún otro componente de la aplicación MUST escribir turnos directamente. Este change no expone endpoints: cada consumidor aplica su control de rol (C-04) antes de invocar el servicio.

#### Scenario: Revisión de escritores de turnos
- **WHEN** se revisa qué componentes de la aplicación guardan turnos
- **THEN** el único es el servicio transaccional de turnos

### Requirement: Token de acceso del paciente
Cada turno SHALL tener un token de acceso único, generado con un generador criptográficamente seguro y con al menos 256 bits de entropía, apto para URL. El sistema SHALL encontrar un turno por su token y MUST NOT incluir el token en logs ni en el historial.

#### Scenario: Tokens distintos
- **WHEN** se crean dos turnos
- **THEN** sus tokens son distintos, de 43 caracteres de `[A-Za-z0-9_-]`

#### Scenario: Búsqueda por token
- **WHEN** se busca un turno con el token de un turno existente
- **THEN** se obtiene ese turno; con un token inexistente o mal formado no se obtiene ninguno

#### Scenario: Token fuera del historial
- **WHEN** se crea un turno
- **THEN** el valor nuevo del historial no contiene el token

### Requirement: Integridad de turnos en la base
La base SHALL rechazar, aunque se escriba directo con SQL, un turno con estado u origen fuera del catálogo, con fin no posterior al inicio, sobreturno de origen `online`, origen incoherente con el creador, token repetido o referencias inexistentes, por restricciones con nombre propio en H2 `MODE=MySQL` y MySQL 8.

#### Scenario: Estado desconocido escrito directo
- **WHEN** se inserta por SQL un turno con estado `borrado`
- **THEN** la base lo rechaza por `ck_turno_estado`

#### Scenario: Sobreturno online escrito directo
- **WHEN** se inserta por SQL un turno de origen `online` con `es_sobreturno` verdadero
- **THEN** la base lo rechaza

#### Scenario: Fin igual al inicio
- **WHEN** se inserta por SQL un turno con fin igual al inicio
- **THEN** la base lo rechaza por `ck_turno_rango`
