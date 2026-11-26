## ADDED Requirements

### Requirement: Selección de turnos a recordar
Cada ejecución SHALL seleccionar los turnos en estado `pendiente` cuyo inicio está después del instante actual y no más allá del instante actual más la antelación configurada (`recordatorio-horas-antes`), de pacientes no anonimizados y sin un recordatorio ya enviado (RN-NO-01, Flujo 2). Los instantes se comparan en UTC.

#### Scenario: Turno dentro de la ventana
- **WHEN** con antelación de 24 horas y reloj en `2026-12-01T12:00:00Z` hay un turno `pendiente` que empieza el `2026-12-02T11:00:00Z`
- **THEN** la ejecución le envía el recordatorio

#### Scenario: Turno fuera de la ventana
- **WHEN** con antelación de 24 horas y reloj en `2026-12-01T12:00:00Z` hay un turno `pendiente` que empieza el `2026-12-02T12:00:01Z`
- **THEN** la ejecución no le envía nada

#### Scenario: Borde exacto de la ventana
- **WHEN** con antelación de 24 horas y reloj en `2026-12-01T12:00:00Z` hay un turno `pendiente` que empieza el `2026-12-02T12:00:00Z`
- **THEN** la ejecución le envía el recordatorio

#### Scenario: Turno ya empezado
- **WHEN** un turno `pendiente` empezó antes del instante actual
- **THEN** la ejecución no le envía nada

#### Scenario: Turno confirmado o cancelado
- **WHEN** un turno dentro de la ventana está `confirmado`, `cancelado`, `asistio`, `ausente` o `ausente_con_aviso`
- **THEN** la ejecución no le envía nada

#### Scenario: Paciente anonimizado
- **WHEN** un turno `pendiente` dentro de la ventana es de un paciente anonimizado
- **THEN** la ejecución no le envía nada

#### Scenario: Cambio de la antelación
- **WHEN** el administrador cambia `recordatorio-horas-antes` de 24 a 48
- **THEN** la ejecución siguiente usa 48 horas, sin reiniciar el sistema

### Requirement: Un solo recordatorio por turno
El sistema MUST NOT enviar dos veces el recordatorio de un mismo turno, aunque se ejecute varias veces o dos ejecuciones corran al mismo tiempo. Cada recordatorio enviado SHALL quedar registrado como notificación del turno, con tipo `recordatorio`, canal email, estado e instante de envío.

#### Scenario: Dos ejecuciones seguidas
- **WHEN** se ejecuta el envío dos veces seguidas con el mismo turno en la ventana
- **THEN** se envía un solo email y hay una sola notificación `recordatorio` de ese turno en estado `enviada`

#### Scenario: Dos ejecuciones simultáneas
- **WHEN** dos ejecuciones arrancan al mismo tiempo con el mismo turno en la ventana, contra MySQL 8
- **THEN** se envía un solo email para ese turno

#### Scenario: Registro del envío
- **WHEN** se envía el recordatorio con el reloj en `2026-12-01T12:00:00Z`
- **THEN** la notificación queda en estado `enviada` con instante de envío `2026-12-01T12:00:00Z`

### Requirement: Contenido del recordatorio
El email recordatorio SHALL indicar la fecha y la hora del turno en la zona `America/Argentina/Buenos_Aires`, la prestación y el profesional, e incluir el enlace de confirmación de un toque y el de cancelación del turno (C-14). El sistema MUST NOT incluir el DNI del paciente en el email.

#### Scenario: Hora local en el email
- **WHEN** se recuerda un turno que empieza el `2026-12-02T13:00:00Z`
- **THEN** el email muestra el 2 de diciembre de 2026 a las 10:00

### Requirement: Fallos de envío registrados y reintentados
Si el envío falla, el sistema SHALL registrar la notificación en estado `fallida` con el número de intentos, sin interrumpir los demás envíos de la ejecución, y SHALL reintentarla en las ejecuciones siguientes mientras el turno siga en la ventana y `pendiente`, hasta 3 intentos en total. Una notificación que quedó `en_curso` por más de 15 minutos (por ejemplo, por una caída del proceso) MUST tratarse como fallida.

#### Scenario: Falla de un envío
- **WHEN** en una ejecución con dos turnos el proveedor de email falla para el primero
- **THEN** el primero queda `fallida` con 1 intento y el segundo se envía igual

#### Scenario: Reintento exitoso
- **WHEN** una notificación `fallida` con 1 intento se reintenta en la ejecución siguiente y el envío funciona
- **THEN** queda `enviada` con 2 intentos

#### Scenario: Tope de intentos
- **WHEN** una notificación ya tiene 3 intentos fallidos
- **THEN** las ejecuciones siguientes no la vuelven a intentar

#### Scenario: Envío interrumpido
- **WHEN** una notificación quedó `en_curso` desde hace 16 minutos
- **THEN** la ejecución siguiente la reintenta y cuenta el intento interrumpido

### Requirement: Disparo externo protegido por credencial de tarea
El sistema SHALL exponer `POST /api/tareas/recordatorios`, que ejecuta el envío y responde 200 con la cantidad de turnos seleccionados, enviados, fallidos y omitidos, sin datos personales. El endpoint MUST exigir el header `X-Tarea-Token` igual a `APP_TAREAS_TOKEN`, comparado en tiempo constante, y MUST responder 401 en formato de problema (RFC 9457) sin ejecutar nada si falta, no coincide o la variable no está configurada. Un JWT del personal MUST NOT habilitar este endpoint.

#### Scenario: Llamada con la credencial correcta
- **WHEN** el cron externo llama con el `X-Tarea-Token` correcto
- **THEN** recibe 200 con el resumen de la ejecución

#### Scenario: Sin credencial
- **WHEN** se llama sin `X-Tarea-Token`
- **THEN** recibe 401 y no se envía ningún email

#### Scenario: Credencial incorrecta
- **WHEN** se llama con un `X-Tarea-Token` distinto
- **THEN** recibe 401 y no se envía ningún email

#### Scenario: Token de administrador
- **WHEN** un administrador llama con su JWT y sin `X-Tarea-Token`
- **THEN** recibe 401 y no se envía ningún email

#### Scenario: Variable sin configurar
- **WHEN** el backend arranca sin `APP_TAREAS_TOKEN` y alguien llama al endpoint con cualquier header
- **THEN** recibe 401 y el backend avisa al arrancar que el disparo externo está deshabilitado, sin mostrar ningún secreto

### Requirement: Disparo local programado
El sistema SHALL ejecutar el envío cada 15 minutos dentro del proceso solo cuando `app.recordatorios.programador-habilitado` es verdadero, que vale verdadero en el perfil `dev` y falso en `test` y en despliegue.

#### Scenario: Perfil de pruebas
- **WHEN** el backend arranca en el perfil `test`
- **THEN** no hay ninguna ejecución programada de recordatorios

#### Scenario: Perfil de desarrollo
- **WHEN** el backend arranca en el perfil `dev`
- **THEN** el envío de recordatorios queda programado cada 15 minutos
