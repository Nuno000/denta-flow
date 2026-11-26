# asistencia-turnos Specification

## Purpose

Permite que el personal interno registre si el paciente asistió, faltó o avisó que faltaba a su turno, con auditoría y permisos por rol, desde la agenda del panel (US-024, Flujo 5).

## Requirements

### Requirement: Registro de asistencia
El sistema SHALL permitir registrar la asistencia de un turno como `asistio`, `ausente` o `ausente_con_aviso`. El cambio MUST quedar auditado con el usuario y el instante (RN-AU-01) en la misma transacción. Cualquier otro valor MUST rechazarse con 400 sin cambiar el turno.

#### Scenario: Marcar asistió
- **WHEN** recepción registra `asistio` en un turno `confirmado` que ya empezó
- **THEN** el turno queda `asistio` y el historial registra el cambio de `confirmado` a `asistio` con su usuario

#### Scenario: Valor que no es de asistencia
- **WHEN** se intenta registrar `confirmado` o `cancelado` como asistencia
- **THEN** el sistema responde 400 y el turno no cambia

#### Scenario: Turno inexistente
- **WHEN** se registra asistencia sobre un turno que no existe
- **THEN** el sistema responde 404

### Requirement: Quién puede registrar asistencia
Administrador y recepción SHALL poder registrar la asistencia de cualquier turno. El profesional MUST poder registrarla solo en sus propios turnos: si el turno es de otro profesional o su usuario no está vinculado, MUST recibir 403 sin cambios. Sin sesión, la respuesta MUST ser 401.

#### Scenario: Profesional en su turno
- **WHEN** un profesional registra `ausente` en un turno suyo que ya empezó
- **THEN** el turno queda `ausente`

#### Scenario: Profesional en un turno ajeno
- **WHEN** un profesional registra asistencia en un turno de otro profesional
- **THEN** el sistema responde 403 y el turno no cambia ni se audita

#### Scenario: Sin sesión
- **WHEN** se registra asistencia sin token
- **THEN** el sistema responde 401

### Requirement: Asistencia desde el inicio del turno
`asistio` y `ausente` MUST registrarse solo cuando el turno ya empezó, comparando su inicio con la hora actual. Antes, MUST rechazarse con 409 sin cambios. `ausente_con_aviso` SHALL aceptarse en cualquier momento, porque el paciente puede avisar antes.

#### Scenario: Asistió antes de empezar
- **WHEN** a las 08:50 del consultorio se registra `asistio` en un turno de las 09:00
- **THEN** el sistema responde 409 y el turno sigue como estaba

#### Scenario: Aviso previo
- **WHEN** el día anterior se registra `ausente_con_aviso` en un turno `confirmado`
- **THEN** el turno queda `ausente_con_aviso`

#### Scenario: Justo al inicio
- **WHEN** a las 09:00 en punto se registra `asistio` en un turno de las 09:00
- **THEN** se acepta

### Requirement: Corrección e idempotencia
La asistencia SHALL poder corregirse entre `asistio`, `ausente` y `ausente_con_aviso`, auditando cada corrección. Registrar el mismo estado que ya tiene MUST responder 200 sin cambios ni auditoría. Un turno `cancelado` MUST rechazarse con 409 (RN-TU-05).

#### Scenario: Corregir una asistencia
- **WHEN** se registra `ausente` en un turno marcado `asistio`
- **THEN** queda `ausente` y el historial registra la corrección

#### Scenario: Repetir el mismo estado
- **WHEN** se registra `asistio` en un turno que ya está `asistio`
- **THEN** responde 200 y no se agrega ningún registro al historial

#### Scenario: Turno cancelado
- **WHEN** se registra asistencia en un turno `cancelado`
- **THEN** el sistema responde 409 y el turno sigue `cancelado`

### Requirement: Respuesta del registro
La respuesta exitosa SHALL devolver el identificador del turno y su estado resultante, sin datos personales del paciente.

#### Scenario: Respuesta mínima
- **WHEN** se registra `asistio` con éxito
- **THEN** la respuesta contiene `id` y `estado` `asistio`, y no contiene DNI, teléfono, email ni token

### Requirement: Acciones de asistencia en la agenda
En la agenda del panel, cada turno no cancelado SHALL mostrar las acciones "Asistió", "Ausente" y "Ausente con aviso" a los tres roles internos (el profesional solo ve sus turnos). "Asistió" y "Ausente" MUST mostrarse deshabilitadas, con su motivo, mientras el turno no empezó. La acción del estado actual MUST indicarse como seleccionada.

#### Scenario: Turno que ya empezó
- **WHEN** recepción ve un turno `confirmado` que empezó hace 10 minutos
- **THEN** ve los botones "Asistió", "Ausente" y "Ausente con aviso" habilitados

#### Scenario: Turno futuro
- **WHEN** recepción ve un turno que empieza en una hora
- **THEN** "Asistió" y "Ausente" están deshabilitados con el texto "Disponible desde las 10:00" y "Ausente con aviso" está habilitado

#### Scenario: Turno cancelado
- **WHEN** la agenda muestra un turno `cancelado`
- **THEN** no ofrece acciones de asistencia

#### Scenario: Estado actual indicado
- **WHEN** un turno está `ausente`
- **THEN** el botón "Ausente" figura como presionado

### Requirement: Resultado del registro en la pantalla
Al registrar asistencia, la pantalla SHALL actualizar la agenda y anunciar el resultado a lectores de pantalla. Un error MUST mostrarse en la tarjeta del turno sin romper la pantalla: 403 como falta de permiso y 409 con el motivo de la API.

#### Scenario: Registro exitoso
- **WHEN** recepción toca "Asistió" en un turno que empezó
- **THEN** el turno pasa a mostrarse "Asistió" y se anuncia "Asistencia registrada"

#### Scenario: Conflicto informado por la API
- **WHEN** la API responde 409 al registrar
- **THEN** la tarjeta muestra el motivo y el turno conserva su estado anterior
