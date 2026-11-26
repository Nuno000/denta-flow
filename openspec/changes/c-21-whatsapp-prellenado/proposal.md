# Proposal — C-21 `whatsapp-prellenado`

## Why

Hoy recepción solo puede recordarle o pedirle la confirmación del turno a un paciente con el email automático (C-14). Muchos pacientes no leen el correo, y WhatsApp es el canal habitual en Argentina. La API oficial de WhatsApp Business cobra por mensaje y quedó fuera del MVP (DD-05, PA-03). Un enlace `wa.me` con el mensaje ya escrito cubre el caso sin costo: recepción lo abre y lo envía desde su propio WhatsApp (US-032, RN-NO-04).

## What Changes

- En la agenda del panel (C-11), cada turno **pendiente** o **confirmado** muestra el botón **"WhatsApp"** al administrador y a recepción. El profesional no lo ve.
- El botón abre un diálogo con:
  - la vista previa del mensaje que se va a enviar;
  - el enlace **"Abrir WhatsApp"** (`https://wa.me/<teléfono>?text=<mensaje>`), que se abre en una pestaña nueva.
- Si el teléfono del paciente no se puede convertir a un número argentino válido para WhatsApp (por ejemplo, un paciente anonimizado), el diálogo muestra un aviso en lugar del enlace. Recepción igual puede copiar el mensaje.
- Nuevo endpoint `GET /api/turnos/{id}/whatsapp` (solo administrador y recepción). Devuelve el mensaje armado y el enlace, o el enlace vacío si no hay teléfono válido.
  - El enlace se arma en el backend, turno por turno.
  - La lista de la agenda sigue sin traer el teléfono ni el token del paciente (Ley 25.326, decisión de C-11).
- El teléfono se normaliza al formato internacional argentino de WhatsApp: `549` + código de área sin `0` + número sin `15`.
- Nuevo parámetro del consultorio **`whatsapp-plantilla`**:
  - es un texto de 1 a 1000 caracteres con los marcadores `{nombre}`, `{fecha}`, `{hora}`, `{prestacion}`, `{profesional}` y `{enlace}`;
  - `{enlace}` se reemplaza por el enlace de confirmación del turno (C-14/C-17);
  - arranca con un valor provisorio que el administrador puede editar.
- Sin envío automático, sin API de WhatsApp Business y sin guardar el mensaje enviado: el envío lo hace una persona (RN-NO-04).

Fuera de alcance: envío automático o masivo, plantillas distintas por tipo de mensaje, registro de mensajes enviados y pantalla de edición de parámetros (el administrador sigue usando `PUT /api/parametros/{clave}`).

Referencias: `CHANGES.md` §C-21. Historia: US-032. Reglas: RN-NO-04 y RN-PR-03. Decisión: DD-05 (PA-03 resuelta).

## Capabilities

### New Capabilities
- `mensajes-whatsapp`: el mensaje de WhatsApp prellenado de un turno (plantilla, normalización del teléfono, enlace `wa.me`), quién puede pedirlo y cómo lo presenta el panel.

### Modified Capabilities
- `parametros-consultorio`: el catálogo suma `whatsapp-plantilla`, con su valor provisorio por defecto y sus reglas de validación.

## Impact

- **Backend**:
  - Normalización del teléfono, plantilla y enlace en `dominio/notificaciones/` (Java puro).
  - Caso de uso en `aplicacion/notificaciones/`.
  - Controller y DTO en `presentacion/turnos/`.
  - Nueva constante en `ClaveParametro` y lectura en `ParametrosDelConsultorio`.
  - Migración `V021_1__parametro_plantilla_whatsapp.sql`: un `INSERT` válido en MySQL 8 y en H2 `MODE=MySQL`.
- **Frontend**:
  - `features/agenda/`: botón en `TarjetaDeTurno`, nuevo `WhatsappDialogo` y hook en `api.ts`.
  - Sin dependencias nuevas.
- **API**: `GET /api/turnos/{id}/whatsapp` (administrador y recepción), documentado en OpenAPI.
- **Privacidad**: el teléfono solo sale del backend dentro del enlace, a pedido explícito sobre un turno y para roles que ya ven el teléfono en la carga manual (C-12). La respuesta no se cachea (`no-store`) y no se loguea el teléfono, el mensaje ni el token.
