# carga-manual-turnos Specification

## Purpose

Permite que la recepción y el administrador carguen turnos telefónicos o presenciales, y sobreturnos explícitos y auditados, sin saltear las reglas de solapamiento, horario y bloqueos, desde el panel interno (US-021, US-022, Flujo 4).

## Requirements

### Requirement: Carga manual de un turno
El sistema SHALL permitir cargar un turno con los datos del paciente (DNI, nombre, apellido, teléfono y email), la prestación, el profesional, el sillón y el inicio con su desplazamiento horario. El turno MUST crearse `pendiente`, de origen `recepcion`, no sobreturno, con el usuario que lo carga como creador, y la respuesta MUST ser 201 con el turno creado.

#### Scenario: Turno telefónico en un horario libre
- **GIVEN** el profesional 1 atiende los martes de 09:00 a 13:00, realiza `Limpieza` (30 minutos) y el sillón 1 está libre
- **WHEN** el usuario 7 de recepción carga un turno de `Limpieza` para el DNI `30123456` con el profesional 1 en el sillón 1 con inicio `2026-12-01T09:00-03:00`
- **THEN** el sistema responde 201 con un turno `pendiente`, origen `recepcion`, `esSobreturno` falso, inicio `2026-12-01T09:00-03:00` y fin `2026-12-01T09:30-03:00`, y el turno guarda como creador al usuario 7

#### Scenario: Carga por el administrador
- **WHEN** un administrador carga un turno en un horario libre
- **THEN** el sistema responde 201 y el turno queda con el administrador como creador

### Requirement: Respuesta de la carga sin datos sensibles
La respuesta de una carga exitosa SHALL incluir id, inicio, fin, estado, `esSobreturno` y los ids de paciente, prestación, profesional y sillón, con los instantes expresados con el desplazamiento de `America/Argentina/Buenos_Aires`. MUST NOT incluir el token de acceso del turno ni el DNI, teléfono o email del paciente.

#### Scenario: Respuesta sin token ni datos de contacto
- **WHEN** recepción carga un turno con éxito
- **THEN** el cuerpo de la respuesta no contiene el token de acceso ni el DNI, el teléfono o el email del paciente

### Requirement: Inicio interpretado en la zona del consultorio
El inicio recibido SHALL interpretarse como instante absoluto según su desplazamiento y guardarse en UTC. Un inicio sin desplazamiento, con segundos o anterior a la hora actual MUST rechazarse con 400 sin crear el turno.

#### Scenario: Inicio con desplazamiento de Buenos Aires
- **WHEN** recepción carga un turno con inicio `2026-12-01T09:00-03:00`
- **THEN** el turno se guarda con inicio `2026-12-01T12:00:00Z`

#### Scenario: Inicio en el pasado
- **GIVEN** la hora actual es `2026-12-01T13:00:00Z`
- **WHEN** recepción carga un turno con inicio `2026-12-01T09:00-03:00`
- **THEN** el sistema responde 400 y no se crea ningún turno

#### Scenario: Inicio sin desplazamiento
- **WHEN** recepción carga un turno con inicio `2026-12-01T09:00`
- **THEN** el sistema responde 400 y no se crea ningún turno

### Requirement: Alta o actualización del paciente por DNI
Al cargar un turno o un sobreturno, el sistema SHALL registrar al paciente por su DNI normalizado: si no existe lo crea y si existe reemplaza su nombre, apellido, teléfono y email por los recibidos (política de recepción, SU-03). El email MUST ser obligatorio (DD-12). El paciente MUST quedar registrado aunque el turno se rechace después por un conflicto.

#### Scenario: Paciente nuevo
- **GIVEN** no existe ningún paciente con DNI `30999888`
- **WHEN** recepción carga un turno para el DNI `30.999.888` con todos sus datos
- **THEN** se crea un único paciente con DNI `30999888` y el turno queda asociado a él

#### Scenario: Paciente existente con teléfono nuevo
- **GIVEN** el paciente 5 con DNI `30123456` tiene teléfono `+541155550000`
- **WHEN** recepción carga un turno para el DNI `30123456` con teléfono `+541166660000`
- **THEN** el turno queda asociado al paciente 5, que pasa a tener teléfono `+541166660000`, y sigue habiendo un solo paciente con ese DNI

#### Scenario: Falta el email
- **WHEN** recepción carga un turno sin email del paciente
- **THEN** el sistema responde 400 indicando el campo, y no se crea ni modifica ningún paciente ni turno

#### Scenario: Conflicto después de registrar al paciente
- **GIVEN** no existe el DNI `30999888` y el profesional 1 está ocupado de 09:00 a 09:30
- **WHEN** recepción carga para el DNI `30999888` un turno del profesional 1 a las 09:00
- **THEN** el sistema responde 409, no se crea ningún turno y el paciente con DNI `30999888` queda registrado

### Requirement: Conflicto de profesional o sillón rechazado
Una carga manual que se superponga con otro turno activo y no sobreturno del mismo profesional o del mismo sillón MUST rechazarse con 409, indicando cuál de los dos está ocupado, sin crear el turno ni registro de historial (RN-TU-01, RN-TU-02, RN-GL-02). No es una advertencia: no existe forma de confirmarla igual por este endpoint.

#### Scenario: Profesional ocupado
- **GIVEN** el profesional 1 tiene un turno `confirmado` de 09:00 a 09:30 en el sillón 1
- **WHEN** recepción carga un turno del profesional 1 en el sillón 2 de 09:15 a 09:45
- **THEN** el sistema responde 409 con un detalle que indica que el profesional está ocupado, y no se crea ningún turno ni registro de historial

#### Scenario: Sillón ocupado con profesional libre
- **GIVEN** el sillón 1 tiene un turno del profesional 1 de 09:00 a 09:30
- **WHEN** recepción carga un turno del profesional 2 en el sillón 1 a las 09:00
- **THEN** el sistema responde 409 con un detalle que indica que el sillón está ocupado

#### Scenario: Turno cancelado no bloquea
- **GIVEN** el único turno del profesional 1 de 09:00 a 09:30 está `cancelado`
- **WHEN** recepción carga un turno del profesional 1 a las 09:00
- **THEN** el sistema responde 201

### Requirement: Horario de atención y bloqueos respetados
Una carga manual fuera del horario de atención del profesional, o dentro de un bloqueo del profesional, del sillón o del consultorio, MUST rechazarse con 409 sin crear el turno (RN-TU-04).

#### Scenario: Fuera del horario
- **GIVEN** el profesional 1 atiende los martes de 09:00 a 13:00
- **WHEN** recepción carga un turno de 30 minutos del profesional 1 el martes 2026-12-01 a las 12:45
- **THEN** el sistema responde 409 y no se crea ningún turno

#### Scenario: Bloqueo del sillón
- **GIVEN** el sillón 1 tiene un bloqueo de 10:00 a 12:00 y el profesional 1 está en horario
- **WHEN** recepción carga un turno del profesional 1 en el sillón 1 a las 10:00
- **THEN** el sistema responde 409

### Requirement: Alternativas sugeridas ante un conflicto
Cuando una carga manual se rechaza por superposición, horario o bloqueo, la respuesta 409 SHALL incluir una lista `alternativas` con hasta 3 horarios disponibles del mismo profesional para la misma prestación, cada uno con inicio, fin y sillón. MUST elegirse los más cercanos al inicio pedido, en el pasado nunca, dentro del día del consultorio pedido y los 6 siguientes. Sin horarios disponibles, la lista MUST venir vacía.

#### Scenario: Alternativas cercanas
- **GIVEN** el profesional 1 atiende de 09:00 a 13:00, la grilla es de 15 minutos y solo tiene ocupado de 09:00 a 10:00 en todos los sillones
- **WHEN** recepción carga un turno de 30 minutos del profesional 1 a las 09:30
- **THEN** el sistema responde 409 y `alternativas` trae 3 horarios: 10:00, 10:15 y 10:30, con el desplazamiento `-03:00`, cada uno con su sillón

#### Scenario: Se prefiere el sillón pedido
- **GIVEN** a las 10:00 están libres los sillones 1 y 2 para el profesional 1
- **WHEN** una carga en el sillón 2 se rechaza y 10:00 es una alternativa
- **THEN** esa alternativa trae el sillón 2

#### Scenario: Sin horarios en la ventana
- **GIVEN** el profesional 1 no tiene ningún horario libre en el día pedido ni en los 6 siguientes
- **WHEN** una carga del profesional 1 se rechaza por conflicto
- **THEN** el sistema responde 409 con `alternativas` vacía

#### Scenario: Ventana por día del consultorio
- **WHEN** se rechaza una carga con inicio `2026-12-01T23:30-03:00` (que en UTC ya es 2 de diciembre)
- **THEN** las alternativas se buscan desde el día del consultorio 2026-12-01 hasta el 2026-12-07 inclusive

#### Scenario: Error de validación sin alternativas
- **WHEN** una carga se rechaza con 400 o 404
- **THEN** la respuesta no incluye `alternativas`

### Requirement: Sobreturno explícito por una acción separada
El sistema SHALL permitir cargar un sobreturno solo por una operación distinta de la carga común, con los mismos datos. El sobreturno MUST crearse `pendiente`, de origen `recepcion`, con `esSobreturno` verdadero, aunque se superponga con otros turnos del profesional o del sillón (RN-SO-01, RN-TU-07), y MUST NOT impedir turnos posteriores en ese horario.

#### Scenario: Sobreturno superpuesto
- **GIVEN** el profesional 1 tiene un turno de 09:00 a 09:30 en el sillón 1
- **WHEN** el usuario 7 de recepción carga un sobreturno del profesional 1 en el sillón 1 a las 09:00
- **THEN** el sistema responde 201 con `esSobreturno` verdadero

#### Scenario: La carga común nunca crea sobreturnos
- **WHEN** recepción usa la carga común sobre un horario ocupado, aunque el cuerpo traiga un campo `esSobreturno` verdadero
- **THEN** el sistema responde 409 y no se crea ningún sobreturno

### Requirement: Sobreturno auditado con quién y cuándo
Cada sobreturno SHALL quedar registrado en el historial del turno, en la misma transacción, con acción `creacion`, el usuario que lo cargó, el instante y `"es_sobreturno":"true"` (RN-SO-02, RN-AU-01).

#### Scenario: Historial del sobreturno
- **WHEN** el usuario 7 de recepción carga un sobreturno a las `2026-11-30T15:00:00Z`
- **THEN** el historial del turno tiene un registro `creacion` con el usuario 7, ese instante y `"es_sobreturno":"true"`, sin DNI, teléfono, email ni token

### Requirement: El sobreturno respeta horario y bloqueos
El sobreturno SHALL eximir solo de la superposición con otros turnos. Un sobreturno fuera del horario de atención o dentro de un bloqueo MUST rechazarse con 409 sin crear el turno (RN-TU-04).

#### Scenario: Sobreturno fuera de horario
- **GIVEN** el profesional 1 atiende los martes de 09:00 a 13:00
- **WHEN** recepción carga un sobreturno del profesional 1 el martes a las 13:00
- **THEN** el sistema responde 409 y no se crea ningún turno

### Requirement: Solo recepción y administrador cargan turnos
Las operaciones de carga común, sobreturno y búsqueda de paciente SHALL estar disponibles solo para los roles `administrador` y `recepcion`, verificado en el backend. El rol `profesional` MUST recibir 403 y sin sesión la respuesta MUST ser 401, sin crear ni modificar pacientes ni turnos.

#### Scenario: Profesional intenta cargar un sobreturno
- **WHEN** un usuario con rol `profesional` carga un sobreturno
- **THEN** el sistema responde 403 y no se crea ningún turno ni paciente

#### Scenario: Profesional intenta cargar un turno común
- **WHEN** un usuario con rol `profesional` carga un turno
- **THEN** el sistema responde 403

#### Scenario: Sin sesión
- **WHEN** alguien sin sesión carga un turno, un sobreturno o busca un paciente
- **THEN** el sistema responde 401

### Requirement: Sin sobreturno en la reserva online
Ninguna ruta pública SHALL permitir crear un sobreturno (RN-DI-02, DD-03): la operación de sobreturno MUST requerir sesión del personal.

#### Scenario: Ruta pública
- **WHEN** se revisan las rutas bajo `/api/publico/`
- **THEN** ninguna crea sobreturnos, y la operación de sobreturno sin sesión responde 401

### Requirement: Búsqueda de paciente por DNI
El sistema SHALL devolver los datos de contacto del paciente cuyo DNI normalizado coincide con el recibido en el cuerpo del pedido (nunca en la URL), o 404 si no existe. Un DNI mal formado MUST rechazarse con 400. El DNI buscado MUST NOT escribirse en los logs.

#### Scenario: Paciente encontrado
- **GIVEN** existe el paciente 5 con DNI `30123456`
- **WHEN** recepción busca el DNI `30.123.456`
- **THEN** el sistema responde 200 con id 5, DNI `30123456`, nombre, apellido, teléfono y email

#### Scenario: Paciente inexistente
- **WHEN** recepción busca un DNI que no está registrado
- **THEN** el sistema responde 404

#### Scenario: DNI mal formado
- **WHEN** recepción busca el DNI `30A23456`
- **THEN** el sistema responde 400

### Requirement: Cargas simultáneas sin doble ocupación
Ante cargas manuales simultáneas que compiten por el mismo profesional o sillón en rangos superpuestos, el sistema SHALL crear como máximo una y responder 409 a las demás, verificado contra MySQL 8 real (perfil `it-mysql`).

#### Scenario: Dos recepcionistas, mismo horario
- **WHEN** dos usuarios de recepción cargan a la vez un turno del profesional 1 a las 09:00, en los sillones 1 y 2
- **THEN** uno recibe 201, el otro 409, y queda un solo turno activo del profesional 1 en ese rango

### Requirement: Pantalla de carga de turno en el panel
El panel SHALL ofrecer en `/panel/agenda/nuevo-turno`, a administrador y recepción, una pantalla para buscar al paciente por DNI, completar o corregir sus datos, elegir prestación, profesional (solo los que la realizan), sillón, fecha y hora del consultorio, y cargar el turno. La agenda MUST mostrar el acceso "Nuevo turno" solo a esos roles.

#### Scenario: Paciente encontrado precarga el formulario
- **WHEN** recepción busca un DNI registrado en la pantalla de carga
- **THEN** el formulario muestra nombre, apellido, teléfono y email del paciente, editables

#### Scenario: Paciente no encontrado
- **WHEN** recepción busca un DNI no registrado
- **THEN** la pantalla avisa que es un paciente nuevo y deja los campos vacíos para completarlos

#### Scenario: Profesional sin acceso
- **WHEN** un usuario con rol `profesional` mira la agenda o abre `/panel/agenda/nuevo-turno`
- **THEN** no ve el acceso "Nuevo turno" y la ruta muestra el aviso de que no tiene permiso

#### Scenario: Hora elegida en la zona del consultorio
- **WHEN** recepción elige el 2026-12-01 a las 09:00 desde una computadora con otra zona horaria
- **THEN** el pedido envía el inicio `2026-12-01T09:00:00-03:00`

#### Scenario: Turno cargado
- **WHEN** la carga termina bien
- **THEN** la pantalla anuncia "Turno cargado" a lectores de pantalla y la agenda se refresca al volver

### Requirement: Conflicto y alternativas en la pantalla
Ante un 409, la pantalla SHALL mostrar el motivo y las alternativas sugeridas como opciones seleccionables en hora del consultorio; elegir una MUST actualizar fecha, hora y sillón del formulario sin enviar la carga. Sin alternativas, MUST indicarlo.

#### Scenario: Elegir una alternativa
- **GIVEN** la carga respondió 409 con la alternativa de las 10:00 en el sillón 2
- **WHEN** recepción elige esa alternativa
- **THEN** el formulario queda con la hora 10:00 y el sillón 2, y no se envió ningún pedido nuevo

#### Scenario: Conflicto sin alternativas
- **WHEN** la carga respondió 409 con `alternativas` vacía
- **THEN** la pantalla muestra el motivo y avisa que no hay horarios cercanos libres para ese profesional

### Requirement: Sobreturno explícito en la pantalla
La pantalla SHALL ofrecer el sobreturno como una acción separada de la carga común, que pide una confirmación explícita antes de enviar y deja claro que se superpondrá con otros turnos. El resultado MUST anunciarse como "Sobreturno cargado".

#### Scenario: Confirmar el sobreturno
- **WHEN** recepción elige "Cargar sobreturno" y confirma en el diálogo
- **THEN** se envía la operación de sobreturno y la pantalla anuncia "Sobreturno cargado"

#### Scenario: Cancelar el diálogo
- **WHEN** recepción elige "Cargar sobreturno" y cancela el diálogo
- **THEN** no se envía ningún pedido

### Requirement: Formulario accesible y validado
El formulario SHALL validar en el cliente los campos obligatorios y el formato de DNI, teléfono y email antes de enviar, mostrar los errores por campo que devuelva el backend, y cumplir WCAG 2.1 AA: etiquetas asociadas, errores anunciados y uso completo con teclado.

#### Scenario: Campo obligatorio vacío
- **WHEN** recepción intenta cargar sin teléfono
- **THEN** la pantalla marca el campo con su error, lo anuncia y no envía el pedido

#### Scenario: Error de campo del backend
- **WHEN** el backend responde 400 con un error en `paciente.email`
- **THEN** la pantalla muestra ese error junto al campo email
