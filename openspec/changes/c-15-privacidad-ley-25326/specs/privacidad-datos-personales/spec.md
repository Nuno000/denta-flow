## ADDED Requirements

### Requirement: Aviso de privacidad público
El sistema SHALL exponer sin autenticación `GET /api/publico/aviso-privacidad`, que devuelve el texto del aviso y la leyenda de aceptación vigentes en los parámetros del consultorio (`aviso-privacidad-texto`, `aviso-privacidad-leyenda`), sin datos personales ni la marca `provisorio`, el autor o la fecha de modificación.

#### Scenario: Consulta sin token
- **WHEN** un visitante sin token llama a `GET /api/publico/aviso-privacidad`
- **THEN** recibe 200 con exactamente los campos `texto` y `leyenda` con los valores guardados

#### Scenario: Cambio del texto visible de inmediato
- **WHEN** el administrador guarda un texto nuevo del aviso y luego un visitante consulta el aviso
- **THEN** recibe el texto nuevo sin reiniciar el sistema

### Requirement: Aceptación obligatoria del aviso al reservar
Toda reserva online SHALL exigir la aceptación explícita del aviso de privacidad (US-040, RN-PR-01). El sistema MUST rechazar con 400 una reserva sin aceptación o con aceptación falsa, sin crear paciente ni turno, y MUST verificarlo en el backend aunque la UI ya lo impida.

#### Scenario: Reserva sin aceptar
- **WHEN** se envía una reserva con `aceptaAvisoPrivacidad` en `false` o ausente
- **THEN** el sistema responde 400 con un detalle en español que indica que hay que aceptar el aviso, y no se crea paciente ni turno

#### Scenario: Reserva aceptando
- **WHEN** se envía una reserva válida con `aceptaAvisoPrivacidad` en `true`
- **THEN** la reserva sigue su curso normal

### Requirement: Instante del consentimiento registrado
Al aceptar el aviso en una reserva, el sistema SHALL guardar en el paciente el instante de la aceptación, tomado del reloj del sistema y almacenado en UTC, en la misma transacción que la reserva. Si el paciente ya existía (mismo DNI), MUST guardar el instante de la última aceptación. Si la reserva falla, MUST NOT quedar registrado el consentimiento.

#### Scenario: Paciente nuevo
- **WHEN** un paciente nuevo reserva aceptando el aviso con el reloj en `2026-12-01T13:15:00Z`
- **THEN** su consentimiento queda registrado en `2026-12-01T13:15:00Z`

#### Scenario: Paciente que vuelve a reservar
- **WHEN** un paciente con consentimiento del `2026-12-01T13:15:00Z` reserva de nuevo el `2026-12-05T10:00:00Z`
- **THEN** su consentimiento queda registrado en `2026-12-05T10:00:00Z`

#### Scenario: Reserva que falla por solapamiento
- **WHEN** una reserva que acepta el aviso es rechazada porque el hueco ya fue tomado
- **THEN** el instante de consentimiento del paciente no cambia

### Requirement: Datos mínimos del paciente
El paciente SHALL guardar solo DNI, nombre, apellido, teléfono, email, el instante de consentimiento y los datos de anonimización (RN-PR-03, SU-07). El sistema MUST NOT tener campos clínicos ni datos personales adicionales del paciente.

#### Scenario: Campos del paciente
- **WHEN** se revisan las columnas de la tabla `paciente` y los campos de la entidad del dominio
- **THEN** coinciden con la lista permitida y no hay ninguno más

### Requirement: Anonimización del paciente
El administrador SHALL poder anonimizar un paciente con `POST /api/pacientes/{id}/anonimizar`. La anonimización MUST reemplazar nombre, apellido, DNI, teléfono y email por valores sustitutos que no identifican a la persona y que no reciben emails (dominio `.invalid`), MUST registrar el usuario y el instante (UTC) y MUST conservar los turnos del paciente y su historial de auditoría, que se retienen 5 años (PA-12, DD-12).

#### Scenario: Anonimización exitosa
- **WHEN** un administrador anonimiza a un paciente sin turnos futuros activos
- **THEN** el sistema responde 200, el paciente ya no tiene su nombre, DNI, teléfono ni email originales, queda registrado quién y cuándo lo anonimizó y sus turnos pasados siguen existiendo

#### Scenario: El DNI original queda libre
- **WHEN** después de anonimizar a un paciente con DNI `30111222` alguien reserva con ese mismo DNI
- **THEN** se crea un paciente nuevo, sin vínculo con el anonimizado

#### Scenario: Paciente con turnos futuros activos
- **WHEN** un administrador intenta anonimizar a un paciente con un turno futuro `pendiente` o `confirmado`
- **THEN** el sistema responde 409 indicando que primero hay que cancelar esos turnos, y no cambia nada

#### Scenario: Anonimizar dos veces
- **WHEN** un administrador anonimiza a un paciente ya anonimizado
- **THEN** el sistema responde 200 sin cambiar los datos ni el registro de la primera anonimización

#### Scenario: Paciente inexistente
- **WHEN** un administrador anonimiza un id de paciente que no existe
- **THEN** el sistema responde 404

### Requirement: Anonimización solo por el administrador
La anonimización SHALL estar permitida solo al rol `administrador`, verificado en el backend. El sistema MUST responder 403 a `recepcion` y `profesional`, y 401 sin token, sin ejecutar la operación.

#### Scenario: Recepción intenta anonimizar
- **WHEN** un usuario con rol `recepcion` llama a `POST /api/pacientes/{id}/anonimizar`
- **THEN** el sistema responde 403 y el paciente no cambia

#### Scenario: Sin token
- **WHEN** se llama a `POST /api/pacientes/{id}/anonimizar` sin token
- **THEN** el sistema responde 401 y el paciente no cambia

### Requirement: Sin emails a pacientes anonimizados
El sistema MUST NOT enviar recordatorios ni otros emails a un paciente anonimizado.

#### Scenario: Turno pasado de paciente anonimizado
- **WHEN** un proceso de notificaciones evalúa un turno de un paciente anonimizado
- **THEN** no envía ningún email a ese paciente

### Requirement: Sin datos personales en logs ni en errores
El sistema MUST NOT escribir DNI, teléfono, email, nombre ni apellido de pacientes en los logs, y las respuestas de error MUST NOT repetir esos datos del input.

#### Scenario: Reserva rechazada
- **WHEN** se rechaza una reserva con DNI `30111222` y email `ana@example.com` por falta de aceptación del aviso
- **THEN** ni los logs ni el cuerpo de la respuesta contienen `30111222` ni `ana@example.com`

#### Scenario: Anonimización
- **WHEN** se anonimiza a un paciente
- **THEN** los logs registran el id del paciente y del usuario, pero no sus datos personales

### Requirement: Transporte cifrado en el despliegue
En el perfil de despliegue, el backend SHALL reconocer el esquema HTTPS informado por el proxy de la plataforma y MUST enviar el header `Strict-Transport-Security` en las respuestas servidas por HTTPS.

#### Scenario: Request HTTPS a través del proxy
- **WHEN** llega un request con `X-Forwarded-Proto: https`
- **THEN** la respuesta incluye `Strict-Transport-Security`
