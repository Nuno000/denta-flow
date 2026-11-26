# Proposal

## Why

El Flujo 1 termina con un email de confirmación que trae los enlaces para confirmar, cancelar y reprogramar el turno, y RN-NO-01 / RN-NO-02 dependen de que ese canal exista: sin email no hay confirmación de un toque (US-011) ni cancelación por el paciente (US-012). C-14 está en el corte de entrega del 26/11 (DD-10), depende solo de C-02 y C-09 (ya en `main`) y bloquea C-16, C-17 y C-18, así que el contrato tiene que quedar fijado antes de que esos changes lo consuman.

## What Changes

- **Modelo `Notificacion`** (`dominio/notificaciones/`, Java puro): turno, tipo (`confirmacion` | `recordatorio`), canal `email`, estado (`en_curso` | `enviada` | `fallida` | `omitida`), intentos, motivo de fallo categorizado, instantes de creación, actualización y envío. Un registro por turno y tipo; los reintentos se cuentan en `intentos` en vez de crear filas de tipo `reintento` (ver design D2, desvío explícito de `04_modelo_de_datos.md` §Notificacion).
- **Migración `V014_1__crear_notificacion.sql`** (reemplaza la "Migración 005" de la ficha, según `docs/plan-trabajo-equipo.md`), con `UNIQUE (turno_id, tipo)` y las columnas que C-18 ya espera (design D1 de C-18).
- **Puerto `EnviadorDeEmail`** en el dominio y dos adaptadores en `infraestructura/email/`: SMTP con Spring Mail (`SPRING_MAIL_*`, `APP_MAIL_FROM`) y uno **simulado en memoria** para tests y desarrollo local. Proveedor elegido: **Brevo** (SMTP relay gratuito); Gmail con contraseña de aplicación queda como alternativa con las mismas variables (DD-12).
- **Plantilla de confirmación de reserva** (texto plano + HTML escapado): fecha y hora en `America/Argentina/Buenos_Aires`, prestación, profesional y los tres enlaces. Sin DNI, teléfono ni token fuera de los enlaces.
- **Enlaces del paciente** (SU-03): `EnlacesDeTurno` arma `APP_BASE_URL/turno/{token}/confirmar|cancelar|reprogramar` reutilizando el `token_acceso` de C-09 (sin generar ni guardar otro token). `ServicioDeEnlaces.resolver(token)` devuelve `VIGENTE`, `VENCIDO`, `CANCELADO` o `INEXISTENTE` para que C-17 muestre el mensaje correcto (Flujo 2, casos de error).
- **Envío con reintento y registro**: `ServicioDeNotificaciones.enviarConfirmacion(turnoId)`, que C-16 invoca después de confirmar su transacción. Se ejecuta fuera del hilo de la request, con hasta 3 intentos ante fallas transitorias; cada intento y el resultado final quedan en `notificacion`. Paciente anonimizado (C-15) → `omitida`, sin enviar.
- **Configuración**: `app.base-url`, `app.mail.from` y `app.mail.modo` (`smtp` | `simulado`) por variables de entorno, validadas al arrancar; `spring-boot-starter-mail` en `pom.xml` (commit aparte, archivo compartido).

## Capabilities

### New Capabilities
- `notificaciones-email`: registro de notificaciones por turno y tipo, envío por un puerto desacoplado del proveedor, plantilla de confirmación de reserva, reintento ante fallas, estado auditable del envío y protección de datos personales en el canal.
- `enlaces-paciente`: armado de los enlaces de confirmar, cancelar y reprogramar a partir del token del turno y `APP_BASE_URL`, y resolución de un token a su situación (vigente, vencido, cancelado o inexistente).

### Modified Capabilities
- `configuracion-entorno`: se agregan las variables de email (`SPRING_MAIL_*`, `APP_MAIL_FROM`, `APP_MAIL_MODO`) y la validación al arrancar de `APP_BASE_URL` y del modo de envío.

## Impact

- **Backend**: `dominio/notificaciones/` (modelo, puertos, regla de reintento y de vigencia del enlace), `aplicacion/notificaciones/` (servicios de notificaciones y de enlaces), `infraestructura/email/` (adaptadores SMTP y simulado, plantilla, propiedades), `infraestructura/persistencia/notificaciones/` (JPA + `META-INF/orm/notificaciones.xml`), migración `V014_1`, `application.yml`, `pom.xml`, `.env.example`.
- **Reutiliza de C-09**: `Turno.tokenAcceso()`, `TurnoRepositorio.buscarPorToken`, `TurnoCreado`. No toca `ServicioDeTurnos` ni `GeneradorDeTokens`.
- **Reutiliza de C-15**: `Paciente.puedeRecibirEmails()`.
- **Contrato con C-16**: llamar a `ServicioDeNotificaciones.enviarConfirmacion(turnoId)` después del commit de la reserva; la respuesta HTTP no espera al SMTP.
- **Contrato con C-17**: rutas del frontend `/turno/{token}/confirmar|cancelar|reprogramar` y `ServicioDeEnlaces.resolver(token)`; los endpoints públicos y las pantallas son de C-17.
- **Contrato con C-18**: tabla `notificacion` con `UNIQUE (turno_id, tipo)`, `intentos`, `actualizada_en`, estados `en_curso|enviada|fallida|omitida`; puerto `EnviadorDeEmail`; `EnlacesDeTurno.confirmar(turno)` / `.cancelar(turno)`. Con esto `V018_1` no necesita ALTER sobre `notificacion`.
- **Contrato con C-19**: el adaptador simulado guarda los últimos emails en memoria para que los E2E lean los enlaces sin buzón (el endpoint de desarrollo, si se quiere, lo agrega C-19).
- **Fuera de alcance**: endpoints públicos de confirmar/cancelar/reprogramar y sus pantallas (C-17), recordatorios programados y reintentos diferidos entre reinicios (C-18), segundo aviso y alerta a recepción (RN-NO-03, diferenciador), WhatsApp (RN-NO-04), vista de notificaciones fallidas para recepción (C-11/C-12).
