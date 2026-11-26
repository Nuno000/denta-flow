# gestion-turnos-paciente Specification

## Purpose

Permitir al paciente gestionar su turno de forma autónoma desde los enlaces recibidos por correo electrónico (C-14), utilizando su token no adivinable sin requerir usuario ni contraseña: consultar el resumen y estado de su turno, confirmar asistencia con un toque (US-011), cancelar el turno dentro de la anticipación mínima (US-012, RN-CA-01/02), o reprogramar a otro hueco disponible validando solapamientos de forma transaccional (US-012, RN-CA-01, RN-TU-01/02), asegurando el registro de auditoría con actor paciente (RN-AU-01) y la preservación de la privacidad sin exponer datos sensibles (RN-PR-03).

## ADDED Requirements

### Requirement: Consulta pública de estado y resumen de turno mediante token
El sistema SHALL exponer `GET /api/publico/turnos/{token}` para consultar el estado del turno sin autenticación. Resuelve el token a su situación (`VIGENTE`, `CANCELADO`, `VENCIDO`, `INEXISTENTE`). Para un turno no inexistente, devuelve `inicio`, `fin`, `prestacion`, `profesional`, `estado` y el booleano `puedeModificar` según la anticipación mínima configurada. MUST NOT devolver datos personales del paciente ni el sillón asignado. Ante token inválido o inexistente, MUST responder con 404.

#### Scenario: Consulta de turno vigente dentro de ventana
- **GIVEN** un turno `pendiente` con inicio en 48 horas y la anticipación mínima configurada en 24 horas
- **WHEN** el paciente consulta `GET /api/publico/turnos/{token}`
- **THEN** recibe 200 con situación `VIGENTE`, estado `pendiente`, datos de prestación y profesional, y `puedeModificar: true`

#### Scenario: Consulta de turno vigente fuera de ventana de anticipación
- **GIVEN** un turno con inicio en 12 horas y la anticipación mínima configurada en 24 horas
- **WHEN** el paciente consulta `GET /api/publico/turnos/{token}`
- **THEN** recibe 200 con situación `VIGENTE`, estado actual y `puedeModificar: false`

#### Scenario: Consulta de turno cancelado o vencido
- **GIVEN** un turno en estado `cancelado` o cuya fecha de inicio ya pasó
- **WHEN** el paciente consulta su token
- **THEN** recibe 200 con situación `CANCELADO` o `VENCIDO` respectivamente, y `puedeModificar: false`

#### Scenario: Token mal formado o inexistente
- **WHEN** se consulta un token inválido o que no pertenece a ningún turno
- **THEN** recibe 404 con mensaje explicativo genérico sin revelar detalles internos

### Requirement: Confirmación de un toque por el paciente
El sistema SHALL exponer `POST /api/publico/turnos/{token}/confirmar` para confirmar asistencia. Si el turno está en estado `pendiente` y su situación es `VIGENTE`, cambia el estado a `confirmado` y audita con `Actor.paciente()`. La operación MUST ser idempotente: si el turno ya está `confirmado`, responde 200 sin duplicar auditoría. Si no está vigente, MUST rechazar la confirmación con error de conflicto.

#### Scenario: Confirmación exitosa de turno pendiente
- **GIVEN** un turno en estado `pendiente` con situación `VIGENTE`
- **WHEN** el paciente envía `POST /api/publico/turnos/{token}/confirmar`
- **THEN** recibe 200, el estado pasa a `confirmado`, y el historial registra la acción `CONFIRMACION` con actor `paciente`

#### Scenario: Confirmación idempotente de turno ya confirmado
- **GIVEN** un turno que ya está en estado `confirmado`
- **WHEN** el paciente envía `POST /api/publico/turnos/{token}/confirmar`
- **THEN** recibe 200 con el turno confirmado y no se genera ninguna entrada adicional en el historial

#### Scenario: Intento de confirmar turno cancelado o vencido
- **GIVEN** un turno en estado `cancelado` o con inicio pasado
- **WHEN** se envía confirmación
- **THEN** se rechaza con error de conflicto (409) informando que el turno no puede ser confirmado

### Requirement: Cancelación de turno por el paciente dentro de la anticipación mínima
El sistema SHALL exponer `POST /api/publico/turnos/{token}/cancelar` para cancelar el turno. Si el turno está en estado `pendiente` o `confirmado` y la anticipación al inicio es mayor o igual a `anticipacion-minima-horas`, pasa a `cancelado`, libera el profesional y el sillón para nuevas reservas, y registra la auditoría con `Actor.paciente()` y acción `CANCELACION`.

#### Scenario: Cancelación dentro de término
- **GIVEN** la anticipación mínima es 24 horas y el turno inicia en 30 horas
- **WHEN** el paciente envía `POST /api/publico/turnos/{token}/cancelar`
- **THEN** recibe 200 con estado `cancelado`, el historial registra `CANCELACION` con actor `paciente`, y el profesional y sillón quedan disponibles

#### Scenario: Cancelación con anticipación mínima en cero
- **GIVEN** la anticipación mínima configurada es 0 horas y el turno inicia en 15 minutos en el futuro
- **WHEN** el paciente envía la cancelación
- **THEN** el turno se cancela exitosamente

### Requirement: Rechazo de cancelación fuera de la ventana de anticipación mínima
Si el paciente intenta cancelar un turno cuando faltan menos horas que `anticipacion-minima-horas`, el sistema MUST rechazar la solicitud con código 409 e indicar contactar al consultorio (RN-CA-03). El turno MUST permanecer en su estado original y no se debe modificar el historial ni liberar recursos.

#### Scenario: Cancelación tardía fuera de la ventana
- **GIVEN** la anticipación mínima es 24 horas y el turno inicia en 6 horas
- **WHEN** el paciente intenta cancelar mediante su token
- **THEN** recibe 409 con el mensaje de contactar al consultorio por estar fuera de la anticipación permitida, y el turno sigue en su estado original

### Requirement: Consulta de huecos disponibles para reprogramar excluyendo el turno propio
El sistema SHALL exponer `GET /api/publico/turnos/{token}/huecos?desde=&hasta=` para consultar horarios de reprogramación. La consulta valida que el turno esté vigente y dentro de la anticipación mínima. El cálculo MUST invocar a `MotorDeDisponibilidad.buscarHuecos` con `turnoAExcluir = turno.getId()`, permitiendo que el horario actual del paciente aparezca disponible para reprogramar.

#### Scenario: Consulta de huecos incluye el hueco propio si no hay otros bloqueos
- **GIVEN** un paciente tiene un turno asignado el día 2026-12-05 a las 10:00
- **WHEN** consulta huecos para reprogramar pasando su token para esa misma fecha
- **THEN** el hueco de las 10:00 se incluye en la lista de alternativas disponibles

#### Scenario: Consulta de huecos fuera de la ventana de anticipación
- **GIVEN** faltan menos horas que la anticipación mínima para el inicio del turno
- **WHEN** se intenta consultar huecos para reprogramar
- **THEN** se rechaza con error indicando que no está permitido reprogramar fuera del plazo

### Requirement: Reprogramación atómica y transaccional del turno
El sistema SHALL exponer `POST /api/publico/turnos/{token}/reprogramar` con el nuevo inicio. Verifica anticipación mínima, resuelve candidatos con `MotorDeDisponibilidad` pasando `turnoAExcluir`, y ejecuta `ServicioDeTurnos.reprogramar` con bloqueo pesimista en orden para evitar solapamientos (RN-TU-01/02). Audita con `REPROGRAMACION` y `Actor.paciente()`. Si el horario ya fue tomado, MUST responder con 409.

#### Scenario: Reprogramación exitosa
- **GIVEN** un turno vigente dentro de la anticipación mínima y un nuevo horario libre
- **WHEN** el paciente envía `POST /api/publico/turnos/{token}/reprogramar` con el nuevo inicio
- **THEN** recibe 200, el turno actualiza su horario y fin, y el historial registra `REPROGRAMACION` con actor `paciente`

#### Scenario: Reprogramación concurrente con horario disputado
- **GIVEN** dos solicitudes simultáneas intentan tomar el mismo hueco (una reserva nueva y una reprogramación)
- **WHEN** ambas transacciones compiten en la base de datos
- **THEN** exactamente una triunfa con 200/201 y la otra es rechazada con 409

### Requirement: Pantallas web públicas accesibles y mobile-first
El frontend SHALL proveer pantallas públicas en las rutas `/turno/:token/confirmar`, `/turno/:token/cancelar` y `/turno/:token/reprogramar`. Cada pantalla MUST ser responsive desde 390 px de ancho, cumplir WCAG 2.1 AA con foco visible y navegación por teclado, mostrar el resumen del turno sin datos personales, e informar mensajes explicativos claros ante errores o cancelaciones fuera de término.

#### Scenario: Pantalla pública en dispositivo móvil
- **WHEN** el paciente accede a `/turno/:token/confirmar` en un viewport de 390x844 px
- **THEN** la interfaz se adapta fluidamente sin desbordes horizontales y presenta el botón de confirmación con tamaño táctil adecuado

#### Scenario: Navegación accesible por teclado
- **WHEN** un usuario navega por `/turno/:token/cancelar` utilizando exclusivamente la tecla Tab
- **THEN** los elementos interactivos reciben foco visible en orden lógico y los encabezados orientan la lectura asistida
