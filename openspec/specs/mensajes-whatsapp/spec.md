# mensajes-whatsapp Specification

## Purpose

Permitir que recepción y el administrador abran desde la agenda un mensaje de WhatsApp ya escrito para recordarle o pedirle la confirmación de su turno a un paciente, mediante un enlace `wa.me` sin costo ni envío automático (US-032, RN-NO-04, DD-05).

## Requirements

### Requirement: Mensaje de WhatsApp de un turno a pedido
El sistema SHALL exponer `GET /api/turnos/{id}/whatsapp` para el administrador y recepción, que devuelve el `mensaje` armado con la plantilla `whatsapp-plantilla` y el `enlace` `wa.me` del turno. La respuesta MUST enviarse con `Cache-Control: no-store` y el mensaje no se envía ni se registra: abrir el enlace no cambia el turno.

#### Scenario: Turno pendiente con teléfono válido
- **GIVEN** un turno `pendiente` cuyo paciente tiene el teléfono `0261 15 555-0000`
- **WHEN** recepción consulta `GET /api/turnos/{id}/whatsapp`
- **THEN** recibe 200 con el mensaje completo y un enlace que empieza con `https://wa.me/5492615550000?text=`, y el historial del turno no cambia

#### Scenario: Turno no existente
- **WHEN** recepción consulta el mensaje de un id de turno que no existe
- **THEN** recibe 404 en formato de problema

#### Scenario: Turno cancelado o ya atendido
- **GIVEN** un turno en estado `cancelado`, `asistio`, `ausente` o `ausente_con_aviso`
- **WHEN** recepción pide su mensaje de WhatsApp
- **THEN** recibe 409 con un detalle en español indicando que solo se envían turnos pendientes o confirmados

### Requirement: Solo administrador y recepción
El endpoint del mensaje de WhatsApp MUST verificar el rol en el backend: el administrador y recepción acceden, el profesional recibe 403 y un pedido sin sesión recibe 401.

#### Scenario: Profesional
- **WHEN** un usuario con rol profesional consulta `GET /api/turnos/{id}/whatsapp`, aunque el turno sea propio
- **THEN** recibe 403

#### Scenario: Sin sesión
- **WHEN** se consulta el endpoint sin token
- **THEN** recibe 401

### Requirement: Mensaje desde la plantilla con marcadores
El mensaje SHALL armarse reemplazando en `whatsapp-plantilla` los marcadores `{nombre}` (nombre del paciente), `{fecha}` (`dd/MM/yyyy`), `{hora}` (`HH:mm`), `{prestacion}`, `{profesional}` (nombre y apellido) y `{enlace}` (enlace de confirmación del turno). Fecha y hora MUST expresarse en `America/Argentina/Buenos_Aires`. Un marcador desconocido MUST quedar como texto literal.

#### Scenario: Fecha y hora en la zona del consultorio
- **GIVEN** un turno que empieza en `2026-12-05T02:30:00Z` y la plantilla "{fecha} {hora}"
- **WHEN** se arma el mensaje
- **THEN** el mensaje es "04/12/2026 23:30"

#### Scenario: Marcador desconocido
- **GIVEN** la plantilla "Hola {nombre}, {saludo}"
- **WHEN** se arma el mensaje para la paciente Ana
- **THEN** el mensaje es "Hola Ana, {saludo}"

#### Scenario: Enlace de confirmación
- **GIVEN** la plantilla "Confirmá en {enlace}"
- **WHEN** se arma el mensaje de un turno con token `abc`
- **THEN** el mensaje es "Confirmá en <APP_BASE_URL>/turno/abc/confirmar"

### Requirement: Teléfono normalizado al formato argentino de WhatsApp
El teléfono del paciente SHALL convertirse a dígitos con el formato `549` + código de área sin `0` + número sin `15` (13 dígitos). Se aceptan el formato nacional con o sin `0` y con o sin `15`, y el internacional `+54` con o sin `9`. Un teléfono que no encaje, de otro país o de un paciente anonimizado MUST dar `enlace` nulo con el mensaje igualmente armado.

#### Scenario: Celular de Buenos Aires con 0 y 15
- **WHEN** el teléfono es `011 15 5555-0000`
- **THEN** el enlace usa `5491155550000`

#### Scenario: Celular de Mendoza sin 0 ni 15
- **WHEN** el teléfono es `2615550000`
- **THEN** el enlace usa `5492615550000`

#### Scenario: Internacional sin 9
- **WHEN** el teléfono es `+542615550000`
- **THEN** el enlace usa `5492615550000`

#### Scenario: Internacional ya normalizado
- **WHEN** el teléfono es `+5492954555000`
- **THEN** el enlace usa `5492954555000`

#### Scenario: Teléfono que no es argentino
- **WHEN** el teléfono es `+59899123456`
- **THEN** la respuesta trae `enlace` nulo y el mensaje armado

#### Scenario: Paciente anonimizado
- **GIVEN** un paciente anonimizado, cuyo teléfono quedó en `00000000`
- **WHEN** se pide el mensaje de su turno
- **THEN** la respuesta trae `enlace` nulo

### Requirement: Mensaje codificado en el enlace
El texto SHALL ir en el parámetro `text` del enlace, codificado en UTF-8 como componente de URL, con los espacios como `%20`. Los caracteres reservados (`+`, `&`, `?`, `#`) y los acentos MUST quedar codificados.

#### Scenario: Acentos y signos
- **WHEN** el mensaje es "¿Venís? Turno #2 & más"
- **THEN** el enlace termina en `?text=%C2%BFVen%C3%ADs%3F%20Turno%20%232%20%26%20m%C3%A1s`

### Requirement: Sin datos personales en logs
El backend MUST NOT registrar en logs el teléfono, el mensaje, el enlace ni el token del turno al armar el mensaje de WhatsApp.

#### Scenario: Log del pedido
- **WHEN** recepción pide el mensaje de un turno
- **THEN** la salida de logs no contiene el teléfono, el nombre del paciente ni el token

### Requirement: Botón y diálogo de WhatsApp en la agenda
La agenda del panel SHALL mostrar un botón "WhatsApp" en cada turno `pendiente` o `confirmado` solo al administrador y a recepción. El botón abre un diálogo accesible con la vista previa del mensaje y el enlace "Abrir WhatsApp" en una pestaña nueva, o un aviso si no hay teléfono válido. MUST ser usable con teclado y a 390 px.

#### Scenario: Recepción abre el diálogo
- **GIVEN** recepción ve un turno `confirmado` en la agenda
- **WHEN** toca "WhatsApp"
- **THEN** ve un diálogo con el mensaje y un enlace "Abrir WhatsApp" con `target="_blank"` y `rel="noopener noreferrer"` hacia el enlace recibido

#### Scenario: Sin teléfono válido
- **WHEN** la respuesta trae `enlace` nulo
- **THEN** el diálogo muestra la vista previa y un aviso con `role="alert"` que explica que el paciente no tiene un teléfono válido para WhatsApp, sin el enlace

#### Scenario: Profesional o turno no vigente
- **WHEN** la agenda la ve un profesional, o el turno está cancelado o ya atendido
- **THEN** el turno no muestra el botón "WhatsApp"

#### Scenario: Cierre con teclado
- **WHEN** el diálogo está abierto y se presiona Escape o el botón "Cerrar"
- **THEN** el diálogo se cierra y el foco vuelve al botón "WhatsApp"
