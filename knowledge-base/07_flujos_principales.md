# Flujos Principales

Flujos propuestos extremo a extremo a partir de los casos de uso de discovery. Dependen de decisiones aún abiertas (marcadas).

## Flujo 1: Reserva online del paciente

**Disparador**: el paciente abre el link público de reserva.
**Actor**: Paciente.

**Pasos**:
1. El paciente elige la prestación (y el profesional, si PA-05 lo permite).
2. El sistema calcula los huecos: horario de atención, sin bloqueos, sin turnos de profesional/sillón, con espacio para la duración completa.
3. El paciente elige un hueco e ingresa DNI, teléfono y email; acepta el aviso de privacidad.
4. El sistema, en una única transacción, vuelve a verificar la disponibilidad, asigna profesional y sillón y crea el turno en estado `pendiente` (u otro estado inicial, ver PA-10).
5. El sistema registra el historial y envía el email de confirmación con enlaces de confirmar/cancelar/reprogramar.

```
Paciente -> Reserva pública -> Backend (disponibilidad) -> BD (turno + auditoría)
                                            |-> Email al paciente
```

**Casos de error**:
- El hueco fue tomado entre la selección y la confirmación -> se informa y se ofrecen otros huecos (RN-TU-01/02).
- Datos inválidos o aviso de privacidad no aceptado -> no se crea el turno.
- No hay huecos para la prestación -> mensaje con contacto del consultorio.

## Flujo 2: Recordatorio y confirmación

**Disparador**: tarea programada previa al turno (antelación no definida, PA-08).
**Actor**: Sistema, Paciente.

**Pasos**:
1. El sistema selecciona turnos próximos en estado `pendiente` y envía un email recordatorio.
2. El paciente confirma con un toque -> el turno pasa a `confirmado`.
3. O bien cancela -> el turno pasa a `cancelado` y se libera el recurso (ver Flujo 3).
4. (Diferenciador) Si no responde en X horas, se reenvía el aviso y se alerta a la recepción.

**Casos de error**:
- Falla de envío de email -> reintento y registro del estado en Notificacion; la recepción puede recurrir a un enlace wa.me.
- Enlace vencido o turno ya cancelado -> mensaje explicativo.

## Flujo 3: Cancelación o reprogramación por el paciente

**Disparador**: el paciente usa el enlace de su turno.
**Actor**: Paciente.

**Pasos**:
1. El sistema verifica la anticipación mínima configurada (RN-CA-01).
2. Cancelación: el turno pasa a `cancelado`, queda auditado y se libera profesional y sillón.
3. Reprogramación: se muestran solo huecos válidos; el cambio se aplica en una transacción que valida solapamientos.
4. (Diferenciador) El hueco liberado se ofrece a la lista de espera (Flujo 6).

**Casos de error**:
- Fuera de la ventana de anticipación -> se indica contactar al consultorio (RN-CA-03).
- El nuevo hueco fue tomado -> se ofrecen alternativas.

## Flujo 4: Carga manual por recepción (incluye sobreturno)

**Disparador**: llamada o visita del paciente.
**Actor**: Recepción.

**Pasos**:
1. Recepción busca/crea al paciente (DNI, teléfono, email si lo tiene).
2. Elige prestación, profesional, sillón y horario.
3. El sistema valida horario, bloqueos y solapamientos (profesional y sillón) y rechaza conflictos.
4. Si el caso lo requiere, recepción activa explícitamente "sobreturno": el sistema permite el solapamiento y lo marca, registrando quién y cuándo.
5. Se crea el turno y se audita.

**Casos de error**:
- Conflicto de profesional o sillón -> se rechaza y se sugiere la alternativa más cercana.
- Intento de sobreturno por rol sin permiso -> denegado.

## Flujo 5: Atención y registro de asistencia

**Disparador**: llega el día/hora del turno.
**Actor**: Profesional, Recepción.

**Pasos**:
1. El profesional abre su agenda del día.
2. Marca cada turno como `asistió`, `ausente` o `ausente con aviso`.
3. El cambio se audita; los datos alimentan el panel de ausentismo (diferenciador).

## Flujo 6: Lista de espera automática (diferenciador)

**Disparador**: un turno se cancela o libera.
**Actor**: Sistema, Paciente en espera.

**Pasos**:
1. El sistema busca pacientes en lista de espera compatibles (prestación y profesional/rango).
2. Notifica la oferta al/los pacientes (canal por definir; email en MVP).
3. El primero que acepta obtiene el turno mediante la verificación transaccional de disponibilidad.
4. Si nadie acepta en un plazo (no definido), el hueco queda libre en la reserva online.

## Flujo 7: Configuración del consultorio

**Actor**: Administrador.

**Pasos**: alta de profesionales, sillones, prestaciones (con duración) -> horarios por profesional -> bloqueos -> usuarios y roles -> anticipación mínima.

**Casos de error**: bloqueo que afecta turnos ya existentes -> el sistema avisa y requiere decidir cómo resolverlos (PA-11).
