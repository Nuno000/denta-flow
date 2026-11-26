# Design — C-18 `recordatorios-programados`

## Context

- **Gobernanza ALTA**: el diseño se revisa y aprueba antes de escribir código.
- **No está en el corte del 26/11** (DD-10). Se prepara para aplicarlo después de C-09 (turnos) y C-14 (notificaciones y email), y para fijar el contrato del cron con C-19.
- Render (plan gratuito) suspende la API tras un rato sin tráfico: un `@Scheduled` en proceso no corre mientras está dormida. Un cron externo que llama a un endpoint la despierta y dispara el envío.
- PA-07 no estaba en la lista de Florencia: se propone resolverla así para el MVP: **un recordatorio, 24 h antes, configurable** (parámetro). Hace falta que el equipo lo confirme en la revisión.

## Goals / Non-Goals

**Goals**: envío único por turno aun con ejecuciones concurrentes; fallos registrados y reintentados; disparo externo seguro; nada de datos personales en logs ni en la respuesta.

**Non-Goals**: varios recordatorios por turno, reintento de confirmación (C-22), alerta a recepción, WhatsApp, cola de mensajes.

## Decisions

### D1. Qué espera C-18 de C-09 y C-14 (acordar con Renzo y Jessica)
- C-09: `turno(estado, inicio)` con el índice `(estado, inicio)` ya previsto; puerto de lectura de turnos con profesional, prestación y paciente.
- C-15: `paciente.anonimizado_en`.
- C-14: tabla `notificacion` con `turno_id`, `tipo`, `canal`, `estado`, `intentos`, `enviada_en`, `actualizada_en`, y `UNIQUE (turno_id, tipo)`; puerto `EnviadorDeEmail` y plantilla `recordatorio`; servicio de enlaces `EnlacesDeTurno.confirmar(turno)` y `.cancelar(turno)`.
- Si C-14 no trae `intentos`, `actualizada_en`, el estado `en_curso` o la clave única, los agrega `V018_1` (ALTER).

### D2. Parámetro `recordatorio-horas-antes`
- Nueva constante en `ClaveParametro` (C-07) con tipo entero 1..168; lectura tipada `Duration antelacionRecordatorio()`.
- `V018_1__recordatorio_parametro_y_notificacion.sql`: `INSERT INTO parametro (clave, valor, provisorio) VALUES ('recordatorio-horas-antes', '24', TRUE);` más los ALTER de D1 si hicieran falta.
- El test "Catálogo y base coinciden" de C-07 pasa a esperar 4 filas.

### D3. Selección (consulta de lectura, sin bloqueos)
```sql
SELECT t.id FROM turno t
JOIN paciente p ON p.id = t.paciente_id
LEFT JOIN notificacion n ON n.turno_id = t.id AND n.tipo = 'recordatorio'
WHERE t.estado = 'pendiente'
  AND t.inicio > :ahora AND t.inicio <= :ahora + antelación
  AND p.anonimizado_en IS NULL
  AND (n.id IS NULL
       OR (n.estado = 'fallida' AND n.intentos < 3)
       OR (n.estado = 'en_curso' AND n.actualizada_en < :ahora - 15 min AND n.intentos < 3))
```
Se escribe en JPQL con parámetros `Instant` (la suma se hace en Java, no en SQL, para no depender del motor). Usa el índice `(estado, inicio)`.

### D4. Reclamo atómico por turno (idempotencia)
Por cada turno seleccionado, en una transacción corta y propia (`REQUIRES_NEW`):
- Sin notificación previa: `INSERT` con `estado='en_curso'`, `intentos=1`. Si la clave única `(turno_id, tipo)` lo rechaza, otra ejecución lo tomó → se omite.
- Con notificación `fallida` o `en_curso` vencida: `UPDATE notificacion SET estado='en_curso', intentos=intentos+1, actualizada_en=:ahora WHERE id=:id AND estado=:estadoLeido AND intentos=:intentosLeidos`. Si afecta 0 filas, otra ejecución la tomó → se omite.
Después, **fuera de esa transacción**, se envía el email (no se mantiene una transacción abierta durante la llamada SMTP) y en otra transacción corta se marca `enviada` (con `enviada_en`) o `fallida`. Antes de enviar se relee el turno: si ya no está `pendiente` (lo confirmaron o cancelaron entretanto) se marca `omitida` y no se envía.

Garantía: como mucho un email por turno salvo una caída entre el envío SMTP y la marca `enviada` (ver Riesgos).

### D5. Dominio puro `dominio.notificaciones`
- `PoliticaDeRecordatorio`: `ventana(Instant ahora, Duration antelacion)`, `puedeReintentar(estado, intentos, actualizadaEn, ahora)`, constantes `MAX_INTENTOS = 3`, `VENCIMIENTO_EN_CURSO = 15 min`.
- `ResumenDeEjecucion(seleccionados, enviados, fallidos, omitidos)`.

### D6. Aplicación `aplicacion.notificaciones.EnviarRecordatorios`
- `ResumenDeEjecucion ejecutar()`: toma `ahora` del `Clock`, lee la antelación, selecciona, reclama, envía, marca. Captura las excepciones de envío por turno (un fallo no corta la ejecución).
- Log: `INFO recordatorios: seleccionados=N enviados=N fallidos=N omitidos=N` y, por fallo, `WARN recordatorio turno {id} fallido intento {n}` (solo ids, sin email ni mensaje del proveedor que pueda contenerlo).

### D7. Disparo externo `/api/tareas/**`
- `SecurityFilterChain` propia con `@Order` antes de la del JWT, `securityMatcher("/api/tareas/**")`, sin sesión, sin CSRF, con un filtro `CredencialDeTareaFilter` que compara `X-Tarea-Token` con `APP_TAREAS_TOKEN` usando `MessageDigest.isEqual` sobre los bytes UTF-8. Si coincide, autentica con la autoridad `TAREA`; si no, 401 RFC 9457 (mismo `AuthenticationEntryPoint` de C-04, con `WWW-Authenticate` propio sin `Bearer`).
- La cadena **no** tiene el resource server JWT: un Bearer válido no autentica ahí.
- `APP_TAREAS_TOKEN` ausente o de menos de 32 caracteres → el filtro rechaza todo y se loguea un `WARN` al arrancar (sin el valor). No se impide el arranque: la API debe poder funcionar sin recordatorios.
- `presentacion.tareas.TareasController`: `POST /api/tareas/recordatorios` → 200 `ResumenDeEjecucion`.
- Ejecuciones simultáneas son seguras por D4; no hace falta un lock global.

### D8. Programador local
- `infraestructura.scheduler.ProgramadorDeRecordatorios` con `@Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")`, registrado con `@ConditionalOnProperty("app.recordatorios.programador-habilitado")`. `@EnableScheduling` en una configuración propia también condicionada.
- `application-dev.yml`: `true`; `application.yml`: `false`.

### D9. Cron externo (se conecta en C-19)
- Opción recomendada: **cron-job.org** (gratis, cada 15 min, permite headers). Alternativa: GitHub Actions `schedule` con el token como secreto del repo (horarios imprecisos, mínimo 5 min).
- La primera llamada tras la suspensión de Render puede tardar ~1 min y el cron cortar por timeout; no importa: la API se despierta y la llamada siguiente procesa. Como la operación es idempotente, una ejecución interrumpida no duplica envíos.

## Risks / Trade-offs

- **Caída entre el SMTP y la marca `enviada`** → la notificación queda `en_curso`, se considera vencida a los 15 min y se reintenta: puede salir un segundo email. Aceptado (es raro y el contenido es el mismo); la alternativa (transacción abierta durante el SMTP) bloquea filas y no evita el caso.
- **Turno reservado dentro de la ventana** (por ejemplo, 3 h antes) → recibe el email de confirmación de C-16 y además el recordatorio en la ejecución siguiente. Aceptado para el MVP; se puede filtrar luego por `creado_en`.
- **Cron externo con el token** → si se filtra, solo permite disparar una operación idempotente; se rota cambiando la variable.
- **PA-07 propuesta, no confirmada** → si el equipo quiere dos recordatorios, cambia el modelo de `tipo` (por ejemplo `recordatorio-24h`, `recordatorio-2h`).
- **Depende de dos changes no mergeados** → el bloque B puede moverse si C-14 cambia el modelo de `notificacion`.

## Migration Plan

1. Bloque A (ya): parámetro, credencial de tarea, programador con una operación vacía.
2. Merge de C-09, C-14 y C-15 a `main`, rebase de `feat/C-18-recordatorios-programados`.
3. Bloque B: selección, reclamo, envío, endpoint real.
4. C-19 configura `APP_TAREAS_TOKEN` en Render y el cron externo.
5. Rollback: revertir el PR y borrar el cron; la fila del parámetro puede quedar (inofensiva) o borrarse con una migración nueva.

## UI Mockups

- Design system revision: no aplica
- Decision: no (no UI) — 2026-11-25. El parámetro nuevo aparece en la pantalla de parámetros existente de C-07 sin cambios de diseño.
