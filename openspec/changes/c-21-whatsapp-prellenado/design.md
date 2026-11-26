# Design — C-21 `whatsapp-prellenado`

## Context

Ver `proposal.md` (Why). Estado actual relevante:

- **Agenda (C-11, en `main`):** `GET /api/agenda/turnos` devuelve una proyección mínima del paciente (nombre y apellido), sin teléfono, email, DNI ni token (C-11 design D1, Ley 25.326). `TarjetaDeTurno` muestra cada turno y ya tiene acciones por rol: asistencia (C-13) e historial (C-13, solo administrador).
- **Paciente (C-09/C-15):** el teléfono se guarda sin separadores con el formato `^\+?\d{8,15}$`. Un paciente anonimizado (C-15) queda con el teléfono `00000000`.
- **Enlaces (C-14):** `EnlacesDeTurno.confirmar(turno)` arma `<APP_BASE_URL>/turno/<token>/confirmar`, la pantalla de C-17.
- **Parámetros (C-07/C-16):** catálogo cerrado `ClaveParametro`. Agregar un parámetro es agregar una constante y una fila en una migración. Solo el administrador lee y edita `/api/parametros`.
- **Guardas:** `@AdministradorORecepcion` existe (C-04).
- **Política visual:** `off` (`design-system/README.md`), sin gate de mockups.

## Goals / Non-Goals

**Goals**
- Recepción abre WhatsApp con el mensaje listo en dos toques, sin costo y sin integración externa (RN-NO-04).
- No ampliar los datos personales de la lista de la agenda.
- Que el administrador pueda cambiar el texto sin tocar código.

**Non-Goals**
- Envío automático, masivo o programado. Integración con la API de WhatsApp Business.
- Registrar en el historial que se abrió el enlace: abrirlo no cambia el turno (RN-AU-01 no aplica).
- Plantillas distintas para recordatorio y confirmación. Hay una sola, y `{enlace}` sirve para las dos cosas.
- Pantalla de edición de parámetros.

## Decisions

### D1. El enlace se arma en el backend, turno por turno

`GET /api/turnos/{id}/whatsapp`, con `@AdministradorORecepcion` y `Cache-Control: no-store`. Respuesta:

```json
{ "mensaje": "Hola Ana, te recordamos tu turno…", "enlace": "https://wa.me/5492615550000?text=Hola%20Ana…" }
```

Si el paciente no tiene un teléfono utilizable, `enlace` viene en `null` y `mensaje` igual viene armado.

- **Por qué no en el frontend:** el frontend necesitaría el teléfono y el token de todos los turnos en la lista de la agenda, lo que revierte la decisión de privacidad de C-11. Además necesitaría leer un parámetro que hoy solo ve el administrador. Con el endpoint, el dato sale solo cuando recepción lo pide para un turno.
- **Errores:**
  - 404 si el turno no existe.
  - 409 si el turno no está `pendiente` ni `confirmado` (no tiene sentido recordar un turno cancelado o ya atendido).
  - 403 para el rol profesional.
  - 401 sin sesión.

### D2. Normalización del teléfono argentino (`TelefonoWhatsapp`, dominio puro)

1. Se quitan los espacios, guiones, paréntesis y puntos. Un `+` o un `00` inicial marcan el número como internacional.
2. **Internacional:**
   - Debe empezar con `54`; si no, se rechaza (el consultorio es argentino).
   - Si sigue un `9`, el resto debe tener 10 dígitos: `549` + 10 dígitos se acepta tal cual.
   - Sin el `9`, el resto se trata como número nacional (paso 3) y se le agrega `549`.
3. **Nacional:**
   - Se quita el `0` inicial.
   - Con 10 dígitos (área + abonado) da `549` + 10 dígitos.
   - Con 12 dígitos, se busca el `15` de celular inmediatamente después del código de área y se quita: área de 2 dígitos si empieza con `11`; si no, de 3 dígitos y, si ahí no hay `15`, de 4 dígitos. Si no aparece, se rechaza.
4. Cualquier otra longitud se rechaza. También se rechaza un número de un solo dígito repetido (cubre el `00000000` de un paciente anonimizado).

Resultado: `Optional<String>` con solo dígitos (`5492615550000`). No intenta validar contra la numeración oficial de ENACOM.

### D3. Plantilla con marcadores (`PlantillaWhatsapp`, dominio puro)

- **Marcadores:**
  - `{nombre}`: nombre del paciente;
  - `{fecha}`: `dd/MM/yyyy` en la zona del consultorio;
  - `{hora}`: `HH:mm` en la zona del consultorio;
  - `{prestacion}`;
  - `{profesional}`: nombre y apellido;
  - `{enlace}`: enlace de confirmación (C-14).
- Un marcador desconocido o mal escrito queda como texto literal: no rompe el envío.
- **Valor provisorio por defecto:** "Hola {nombre}, te recordamos tu turno de {prestacion} con {profesional} el {fecha} a las {hora} (hora de Argentina). Para confirmarlo, cancelarlo o reprogramarlo entrá a {enlace}".

### D4. Enlace `wa.me` (`EnlaceWhatsapp`, dominio puro)

- Formato: `https://wa.me/<dígitos>?text=<mensaje codificado>`.
- El texto se codifica en UTF-8 como componente de URL, con los espacios como `%20` y no como `+`. WhatsApp no siempre decodifica `+` como espacio, y un `+` literal del mensaje queda como `%2B`.

### D5. Parámetro `whatsapp-plantilla`

- Constante `WHATSAPP_PLANTILLA` en `ClaveParametro`: tipo `TEXTO`, máximo 1000 caracteres, validada con `ValidacionesDeTexto.obligatorio`.
- Migración `V021_1__parametro_plantilla_whatsapp.sql`: inserta la fila con el valor de D3 y `provisorio = TRUE`.
- `ParametrosDelConsultorio.plantillaWhatsapp()` la lee aunque siga siendo provisoria.
- La edición sigue siendo solo del administrador (`PUT /api/parametros/whatsapp-plantilla`).

### D6. UI en la agenda

- **Botón:** `TarjetaDeTurno` muestra el botón "WhatsApp" (con `CLASES_ACCION`) solo si el rol es administrador o recepción y el turno está `pendiente` o `confirmado`.
- **Diálogo `WhatsappDialogo`:** sigue el patrón de `HistorialDeTurnoDialogo`. Al abrirse pide `useMensajeWhatsapp(turnoId)` y muestra:
  - la vista previa del mensaje;
  - el enlace "Abrir WhatsApp" (`<a target="_blank" rel="noopener noreferrer">`), o un aviso `role="alert"` si `enlace` es `null`.
- **Por qué un enlace real y no `window.open`:** `window.open` después de un pedido asíncrono suele terminar bloqueado como ventana emergente. Además, el enlace permite revisar el texto antes de abrir WhatsApp.

### D7. Privacidad y logs

- El backend no loguea el teléfono, el mensaje, el enlace ni el token: solo el id del turno.
- La respuesta va con `no-store`.
- El frontend no guarda el enlace fuera de la caché de TanStack Query, que se descarta al cerrar el diálogo (`gcTime: 0`).

## Risks / Trade-offs

- **[Riesgo] Teléfonos cargados con un formato raro quedan sin enlace.** → Mitigación: el aviso lo explica y recepción puede corregir el teléfono en la próxima carga manual (C-12) o copiar el mensaje.
- **[Riesgo] El token del enlace pasa por el navegador de recepción.** → Aceptado. Es el mismo enlace que el paciente recibe por email y recepción ya opera sobre el turno desde el panel. No se guarda ni se loguea.
- **[Trade-off] La heurística del `15` puede fallar con algún código de área de 4 dígitos que termine en `15`.** → Aceptado: es poco frecuente y termina en el aviso, no en un mensaje a un número equivocado, porque la longitud se sigue validando.
