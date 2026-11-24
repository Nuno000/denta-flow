# Modelo de Datos

> Modelo **conceptual propuesto** derivado de las reglas de negocio de discovery. No hay modelo previo en las fuentes; nombres orientativos (supuesto SU-04). Motor y tipos según DD-09 (MySQL 8, compatible con H2 `MODE=MySQL`).

## Dominios

- **Configuración del consultorio**: profesionales, sillones/boxes, prestaciones, horarios, bloqueos, parámetros.
- **Agenda**: turnos, estados, sobreturnos, lista de espera.
- **Pacientes**: datos mínimos de contacto (sin ficha clínica en el MVP).
- **Seguridad y auditoría**: usuarios, roles, registro de cambios, consentimiento de privacidad.
- **Notificaciones**: recordatorios y confirmaciones.

## ERD (descripción textual)

```
Profesional 1---N HorarioAtencion
Profesional 1---N Bloqueo            (Bloqueo puede ser de profesional, de sillón o general)
Sillon      1---N Bloqueo
Profesional N---M Prestacion         (qué prestaciones realiza cada profesional)
Prestacion  1---N Turno
Profesional 1---N Turno
Sillon      1---N Turno
Paciente    1---N Turno
Turno       1---N TurnoHistorial     (auditoría: quién, cuándo, qué cambió)
Turno       1---N Notificacion
Paciente    1---N ListaEspera N---1 Prestacion (y opcionalmente Profesional)
Usuario     N---1 Rol ; Usuario 0..1---1 Profesional
```

## Entidades

### Profesional
- id, nombre, apellido, matrícula (opcional), email, activo.
- Relaciones: N:M con Prestacion; 1:N con HorarioAtencion, Bloqueo, Turno; 0..1 con Usuario.

### Sillon (box)
- id, nombre/identificador (p. ej. "Sillón 1"), activo.
- Relaciones: 1:N con Turno y Bloqueo.

### Prestacion
- id, nombre, **duracion_minutos**, activa.
- La duración del turno la define la prestación (RN-TU-03).

### HorarioAtencion
- id, profesional_id, día de la semana, hora_inicio, hora_fin.
- Constraint: sin rangos superpuestos para un mismo profesional y día.

### Bloqueo
- id, ámbito (profesional | sillón | consultorio), profesional_id/sillon_id (según ámbito), inicio, fin, motivo (vacaciones, feriado, ausencia).

### Paciente
- id, nombre, apellido, **dni**, **telefono**, email (necesario para recordatorios por email), fecha de consentimiento del aviso de privacidad.
- Identificación sin cuenta: DNI + teléfono. Unicidad por DNI es un supuesto (SU-03).

### Turno
- id, paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin (= inicio + duración de la prestación), estado, origen (online | recepción), es_sobreturno (bool), creado_por, creado_en, token de acceso/confirmación del paciente.
- Estados: `pendiente`, `confirmado`, `cancelado`, `asistio`, `ausente`, `ausente_con_aviso` (RN-TU-05).
- Constraints:
  - Sin solapamiento de rangos [inicio, fin) por `profesional_id` entre turnos activos (no cancelados), salvo `es_sobreturno = true`.
  - Idem por `sillon_id`.
  - La garantía es **a nivel de transacción**, no solo en la UI (DD-09): la creación o el cambio de un turno bloquea con `SELECT ... FOR UPDATE` la fila del profesional y después la del sillón (siempre en ese orden), verifica el solapamiento leyendo el último estado confirmado y recién entonces escribe. Toda escritura de turnos pasa por ese servicio (C-09).
- Estado inicial: el turno online nace `pendiente` y pasa a `confirmado` con el enlace del email (PA-10); el de recepción, según el flujo de carga manual.
- Asignación en la reserva online: el paciente elige un profesional o "cualquiera / primero disponible" (PA-05); el sistema asigna después el primer sillón libre en ese rango (PA-06). El paciente nunca elige sillón.
- Índices relevantes: (profesional_id, estado, inicio, fin), (sillon_id, estado, inicio, fin), (paciente_id), (estado, inicio).
- Tipos (DD-09): PK `BIGINT AUTO_INCREMENT`; inicio y fin `DATETIME(6)` en UTC; estado `VARCHAR` + `CHECK`.

### TurnoHistorial (auditoría)
- id, turno_id, usuario_id o actor (paciente/sistema), fecha-hora, acción, valores anterior/nuevo.
- Cumple RN-AU-01 (todo cambio registra quién y cuándo).

### Notificacion
- id, turno_id, tipo (recordatorio | confirmación | reintento), canal (email), enviada_en, estado de envío.

### ListaEspera (diferenciador)
- id, paciente_id, prestacion_id, profesional_id (opcional), rango deseado, estado (activa | ofrecida | atendida | vencida).

### Usuario / Rol
- Usuario: id, email, credencial (hash), rol, profesional_id (opcional), activo.
- Rol: `administrador`, `recepcion`, `profesional`.

## Seed data inicial

- Un usuario administrador inicial.
- Roles: administrador, recepción, profesional.
- Parámetros de configuración: anticipación mínima de cancelación/reprogramación (valor por defecto **pendiente**, PA-04), textos del aviso de privacidad.
- Datos de ejemplo para desarrollo/demo: 2-3 profesionales, 2-3 sillones, 4-5 prestaciones con duraciones distintas, horarios de atención.
