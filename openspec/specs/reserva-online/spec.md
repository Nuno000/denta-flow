# reserva-online Specification

## Purpose

Permitir que un paciente reserve un turno desde un link público, sin registrarse y desde el celular, recibiendo solo horarios reales y sin que dos reservas puedan quedarse con el mismo horario, profesional o sillón (US-010, Flujo 1).

## Requirements

### Requirement: Catálogo público para reservar
El sistema SHALL exponer sin autenticación las prestaciones activas (id, nombre, duración en minutos) y, para una prestación, los profesionales activos que la realizan (id, nombre y apellido). MUST NOT exponer sillones, emails, matrículas ni recursos inactivos.

#### Scenario: Prestaciones activas
- **GIVEN** las prestaciones "Limpieza" (activa, 30 min) y "Ortodoncia" (inactiva)
- **WHEN** un visitante sin token consulta las prestaciones
- **THEN** recibe 200 solo con "Limpieza" y su duración

#### Scenario: Profesionales de una prestación
- **GIVEN** "Ana Pérez" realiza "Limpieza" y "Luis Gómez" no
- **WHEN** un visitante consulta los profesionales de "Limpieza"
- **THEN** recibe solo a "Ana Pérez", con id, nombre y apellido, sin email ni matrícula

#### Scenario: Prestación inexistente o inactiva
- **WHEN** se consultan los profesionales de una prestación inexistente o inactiva
- **THEN** el sistema responde 404 con un detalle en español

### Requirement: Contacto del consultorio público
El sistema SHALL exponer sin autenticación el texto de contacto del consultorio guardado en los parámetros, para mostrarlo cuando no hay huecos o el horario elegido ya no está disponible.

#### Scenario: Consulta del contacto
- **WHEN** un visitante sin token consulta el contacto del consultorio
- **THEN** recibe 200 con exactamente el campo `contacto` con el valor guardado

### Requirement: Datos de la reserva validados en el backend
Una reserva SHALL indicar prestación, inicio, profesional opcional (vacío = cualquiera) y DNI, nombre, apellido, teléfono y email del paciente. El backend MUST validarlos aunque la pantalla ya lo haga y MUST rechazar con 400, sin crear paciente ni turno, un dato faltante o con formato inválido. MUST ignorar el sillón, el origen o el sobreturno que se envíen.

#### Scenario: Email inválido
- **WHEN** se envía una reserva con email `ana@` y los demás datos válidos
- **THEN** el sistema responde 400 indicando el campo email, sin repetir el valor enviado, y no crea paciente ni turno

#### Scenario: Intento de elegir sillón o sobreturno
- **WHEN** el cuerpo de la reserva incluye `sillonId` o `esSobreturno: true`
- **THEN** esos campos se ignoran: el sillón lo asigna el sistema y el turno nunca es sobreturno

### Requirement: Aviso de privacidad aceptado al reservar
La reserva SHALL exigir `aceptaAvisoPrivacidad` en `true` (US-040, RN-PR-01) y registrar en el paciente el instante de aceptación tomado del reloj del sistema en UTC. Sin aceptación MUST responder 400 sin crear ni modificar paciente ni turno.

#### Scenario: Aviso no aceptado
- **WHEN** se envía una reserva válida con `aceptaAvisoPrivacidad` en `false` o ausente
- **THEN** el sistema responde 400 con "Tenés que aceptar el aviso de privacidad para reservar" y no crea paciente ni turno

### Requirement: Reserva exitosa
Con datos válidos y el horario disponible, el sistema SHALL registrar o reutilizar al paciente por DNI, crear un turno `pendiente` de origen `online` con profesional (el elegido o el primero disponible) y sillón asignados por el sistema, registrar su historial y pedir el email de confirmación con los enlaces. MUST responder 201 con inicio y fin en la zona del consultorio, prestación, profesional y estado, sin token, sillón ni datos del paciente.

#### Scenario: Reserva con "cualquiera"
- **GIVEN** el hueco del 2026-12-02 a las 10:00 (hora del consultorio) para "Limpieza" con "Ana Pérez" libre
- **WHEN** un paciente nuevo reserva ese horario sin elegir profesional, con datos válidos y el aviso aceptado
- **THEN** recibe 201 con inicio `2026-12-02T10:00:00-03:00`, profesional "Ana Pérez" y estado `pendiente`, y el turno queda en la base con un sillón asignado

#### Scenario: Email de confirmación
- **WHEN** se crea una reserva
- **THEN** se envía un email de confirmación al paciente con los enlaces de confirmar, cancelar y reprogramar (C-14), y una falla del email no deshace la reserva

#### Scenario: Paciente que ya existe
- **GIVEN** un paciente con DNI `30111222` y email `viejo@example.com`
- **WHEN** reserva con ese DNI y el email `nuevo@example.com`
- **THEN** la reserva se crea para ese paciente, su email registrado no cambia, y la respuesta no revela a qué dirección se envió el email

### Requirement: Horario tomado entre la selección y la confirmación
Si el horario elegido dejó de estar disponible (otra reserva, un bloqueo o un cambio de agenda), el sistema SHALL responder 409 con un detalle en español sin crear el turno. La pantalla MUST informarlo, volver a mostrar los huecos del mismo día y conservar los datos del paciente ya cargados.

#### Scenario: Hueco tomado
- **GIVEN** un paciente eligió el hueco de las 10:00 y otro lo reservó antes
- **WHEN** el primero confirma su reserva
- **THEN** recibe 409 "El horario elegido ya no está disponible; elegí otro" y la pantalla le muestra los huecos actuales de ese día con sus datos todavía cargados

### Requirement: Sin solapamientos ante reservas simultáneas
Dos reservas simultáneas del mismo horario SHALL resolverse con exactamente una aceptada: si solo hay un profesional y un sillón libres, una recibe 201 y la otra 409, y queda un único turno activo. MUST verificarse contra MySQL 8 real.

#### Scenario: Mismo hueco a la vez
- **GIVEN** un único profesional y un único sillón libres a las 10:00
- **WHEN** dos pacientes envían a la vez la reserva de ese horario
- **THEN** una recibe 201, la otra 409, y la base tiene un solo turno activo a esa hora

### Requirement: Sin huecos para la prestación
Si en el rango consultado no hay huecos, la pantalla SHALL decirlo y mostrar el contacto del consultorio.

#### Scenario: Agenda llena
- **GIVEN** no hay huecos para "Limpieza" en los próximos 14 días
- **WHEN** el paciente elige "Limpieza"
- **THEN** ve "No hay horarios disponibles" junto al contacto del consultorio, sin ningún horario para elegir

### Requirement: Límite de reservas por origen
El sistema SHALL aceptar como máximo 5 pedidos de reserva por dirección IP cada 10 minutos. Pasado el límite MUST responder 429 con `Retry-After` y un detalle en español, sin crear paciente ni turno.

#### Scenario: Sexto pedido seguido
- **WHEN** la misma dirección IP envía 6 reservas dentro de 10 minutos
- **THEN** la sexta recibe 429 con `Retry-After` y no crea nada

#### Scenario: Otra dirección no se ve afectada
- **GIVEN** una dirección IP ya llegó al límite
- **WHEN** otra dirección envía una reserva válida
- **THEN** se procesa con normalidad

### Requirement: Sin datos personales en logs ni errores de la reserva
Los logs y las respuestas de error de la reserva MUST NOT contener DNI, nombre, teléfono ni email del paciente.

#### Scenario: Reserva rechazada
- **WHEN** una reserva con DNI `30111222` es rechazada por horario tomado
- **THEN** ni el log ni el cuerpo del 409 contienen `30111222`, el email ni el teléfono

### Requirement: Pantalla de reserva mobile-first y accesible
La pantalla pública de reserva SHALL usarse completa en un celular de 390 px de ancho y solo con teclado, cumpliendo WCAG 2.1 AA: cada campo con etiqueta, errores asociados al campo y anunciados, foco visible y movido al primer error, contraste suficiente y horarios en la hora del consultorio.

#### Scenario: Error de validación anunciado
- **WHEN** el paciente envía el formulario con el DNI vacío
- **THEN** ve el error junto al campo, el campo queda asociado al mensaje (`aria-describedby`) y el foco pasa a ese campo

#### Scenario: Envío sin doble reserva por doble toque
- **WHEN** el paciente toca dos veces "Reservar turno"
- **THEN** se envía un solo pedido y el botón queda deshabilitado hasta la respuesta

#### Scenario: Confirmación visible
- **WHEN** la reserva se crea
- **THEN** la pantalla muestra fecha y hora, prestación, profesional y que el turno queda pendiente hasta confirmarlo desde el email
