# Spec Delta — auditoria-turnos

## ADDED Requirements

### Requirement: Consulta HTTP del historial por turno solo para administrador
El sistema SHALL exponer `GET /api/turnos/{id}/historial` protegido para el rol `administrador` (RN-AU-02). MUST responder 200 con la lista cronológica de cambios (`id`, `ocurridoEn` en UTC, `actorTipo`, `actorEmail` si es usuario, `accion`, `valorAnterior` y `valorNuevo`), 404 si el turno no existe, 403 para `recepcion` o `profesional` y 401 sin sesión. MUST NOT exponer endpoints de modificación ni borrado, ni datos personales del paciente o token (RN-PR-03).

#### Scenario: Administrador consulta el historial de un turno con cambios
- **WHEN** un administrador solicita `GET /api/turnos/{id}/historial` de un turno con registros de `creacion` y `cambio_estado`
- **THEN** recibe 200 con los cambios en orden cronológico, incluyendo el email del usuario que actuó, los instantes en UTC y sin DNI, teléfono, email del paciente ni token

#### Scenario: Recepción o profesional intenta consultar el historial
- **WHEN** un usuario con rol `recepcion` o `profesional` solicita `GET /api/turnos/{id}/historial`
- **THEN** el sistema responde 403 en formato de problema y no devuelve registros

#### Scenario: Turno inexistente o sin sesión
- **WHEN** un administrador consulta el historial de un turno que no existe, o se consulta sin token
- **THEN** recibe 404 si el turno no existe y 401 si no hay sesión

### Requirement: Visualización de solo lectura del historial en la agenda
En `/panel/agenda`, cada tarjeta de turno (incluidos los turnos `cancelado`) SHALL mostrar el botón "Ver historial" únicamente al rol `administrador` (US-025). Al abrirlo, MUST mostrar en un diálogo accesible cada cambio con fecha y hora en `America/Argentina/Buenos_Aires`, quién lo hizo (email del personal, "Paciente" o "Sistema"), la acción en castellano y qué cambió, sin botones de editar ni borrar.

#### Scenario: Administrador abre el historial desde la agenda
- **WHEN** un administrador toca "Ver historial" en una tarjeta de turno
- **THEN** se consulta `GET /api/turnos/{id}/historial` y el diálogo muestra los cambios ordenados con hora del consultorio, actor, acción y valores anterior y nuevo

#### Scenario: Recepción y profesional no ven el acceso al historial
- **WHEN** un usuario de `recepcion` o `profesional` abre la agenda
- **THEN** las tarjetas no muestran el botón "Ver historial"

#### Scenario: Turno sin registros o error al cargar
- **WHEN** el historial vuelve vacío o la API falla al consultarlo
- **THEN** el diálogo informa que no hay cambios registrados o muestra una alerta con botón "Reintentar", sin ofrecer edición ni borrado
