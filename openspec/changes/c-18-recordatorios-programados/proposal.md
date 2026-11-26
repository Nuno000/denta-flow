# Proposal

## Why

RN-NO-01 y el Flujo 2 piden un recordatorio automático por email con confirmación de un toque, que es lo que baja el ausentismo (objetivo del MVP). En los planes gratuitos de hosting (Render) el proceso se suspende por inactividad, así que un `@Scheduled` en proceso no alcanza: hace falta una operación idempotente que un cron externo pueda disparar sin duplicar emails. C-18 **no está en el corte del 26/11** (DD-10); se prepara ahora para aplicarlo apenas estén C-09 y C-14, y para que C-19 sepa qué cron configurar.

## What Changes

- **Parámetro nuevo `recordatorio-horas-antes`** en el catálogo de C-07 (entero de 1 a 168, por defecto 24 provisorio), con migración `V018_1__...` que agrega la fila. Resuelve PA-07 para el MVP: **un** recordatorio, con antelación configurable.
- **Operación `EnviarRecordatorios.ejecutar()`**: selecciona turnos `pendiente` cuyo inicio cae entre ahora y ahora + antelación, de pacientes no anonimizados (C-15), sin recordatorio ya enviado, y envía el email recordatorio con los enlaces de confirmar y cancelar de C-14.
- **Idempotencia y concurrencia**: cada envío "reserva" primero su fila en `notificacion` (`turno_id`, tipo `recordatorio`) con una clave única; dos ejecuciones simultáneas no pueden mandar el mismo recordatorio. Un fallo de envío queda registrado y se reintenta en la ejecución siguiente hasta 3 intentos.
- **Disparo externo protegido**: `POST /api/tareas/recordatorios`, autenticado con el header `X-Tarea-Token` comparado en tiempo constante contra `APP_TAREAS_TOKEN` (mínimo 32 caracteres). Sin la variable configurada, el endpoint rechaza todo (falla cerrado). Responde un resumen sin datos personales.
- **Disparo local**: `@Scheduled` cada 15 minutos, solo si `app.recordatorios.programador-habilitado=true` (activado en el perfil `dev`, apagado en `test` y en despliegue).
- La configuración del cron externo gratuito (cron-job.org o GitHub Actions `schedule`) se documenta acá y se conecta en C-19.

## Capabilities

### New Capabilities
- `recordatorios-turnos`: selección por ventana, envío único por turno, registro y reintento de fallos, y disparo protegido (externo y local) del recordatorio de turnos pendientes.

### Modified Capabilities
- `parametros-consultorio`: el catálogo cerrado suma `recordatorio-horas-antes` (con su valor provisorio por defecto y su lectura tipada).
- `autorizacion-por-rol`: el prefijo `/api/tareas/**` queda fuera del JWT y se autentica con la credencial de tarea.

## Impact

- **Backend**: `dominio/notificaciones/` (regla de selección y de reintento, Java puro), `aplicacion/notificaciones/EnviarRecordatorios`, `infraestructura/scheduler/` (programador local), `presentacion/tareas/`, configuración de seguridad (cadena de filtros para `/api/tareas/**`), `ClaveParametro` de C-07, migración `V018_1`.
- **Depende de C-09** (turnos y estados) **y de C-14** (tabla y modelo `Notificacion`, puerto de envío de email, plantillas y enlaces con token). Bloque A de `tasks.md` (parámetro, credencial de tarea, programador) se puede hacer ya; bloque B espera C-09 y C-14.
- **Contrato con C-14 (Jessica)**: `notificacion` con `UNIQUE (turno_id, tipo)`, `estado` (`en_curso|enviada|fallida|omitida`), `intentos` y `actualizada_en`; si C-14 no lo incluye, lo agrega `V018_1`.
- **Variables de entorno nuevas**: `APP_TAREAS_TOKEN` (`.env.example` y `08_arquitectura_propuesta.md`).
- **Contrato con C-19**: un cron externo llama cada 15 minutos a `POST /api/tareas/recordatorios` con el header; la primera llamada también despierta la API de Render.
- **Fuera de alcance**: varios recordatorios por turno, reintento de confirmación a las X horas (C-22, PA-08), WhatsApp.
