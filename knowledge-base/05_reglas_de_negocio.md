# Reglas de Negocio

Cada regla tiene un código único `RN-{DOMINIO}-{NN}`. Las reglas RN-TU, RN-DI, RN-CA, RN-AU, RN-RE derivan directamente de la sección 7 de `docs/discovery/discovery.md`; los códigos son de esta KB.

## Dominio: Turnos y recursos (RN-TU)

- **RN-TU-01**: Un profesional no puede tener dos turnos superpuestos.
- **RN-TU-02**: Un sillón o box no puede tener dos turnos superpuestos.
- **RN-TU-03**: La duración del turno la define la prestación (fin = inicio + duración).
- **RN-TU-04**: No se pueden reservar turnos en horarios bloqueados ni fuera del horario de atención del profesional.
- **RN-TU-05**: Un turno pasa por los estados: `pendiente`, `confirmado`, `cancelado`, `asistió`, `ausente`, `ausente con aviso`.
- **RN-TU-06**: Los turnos cancelados liberan el recurso (profesional y sillón) para nuevas reservas.
- **RN-TU-07**: El control de solapamiento aplica a ambos recursos a la vez; un turno requiere profesional **y** sillón libres.

## Dominio: Disponibilidad y reserva online (RN-DI)

- **RN-DI-01**: La reserva online solo ofrece huecos donde entra completa la duración de la prestación.
- **RN-DI-02**: La reserva online **nunca** permite solapamientos (no existe sobreturno online).
- **RN-DI-03**: El paciente reserva sin registro obligatorio; se identifica con DNI y teléfono.
- **RN-DI-04**: Si se elige profesional o se asigna el primero disponible en la reserva online es una **pregunta abierta** (PA-05); no codificar hasta resolver.

## Dominio: Cancelación y reprogramación (RN-CA)

- **RN-CA-01**: El paciente puede cancelar o reprogramar solo hasta una anticipación mínima antes del turno.
- **RN-CA-02**: La anticipación mínima la configura cada consultorio. El valor por defecto es una **pregunta abierta** (PA-04).
- **RN-CA-03**: Dentro de la ventana de anticipación mínima, la cancelación/reprogramación queda a cargo de la recepción (supuesto SU-06).
- **RN-CA-04** (diferenciador): Al liberarse un turno por cancelación, se ofrece automáticamente a la lista de espera compatible (prestación y profesional).

## Dominio: Sobreturnos (RN-SO)

- **RN-SO-01**: El sobreturno solo lo puede cargar la recepción, de forma explícita.
- **RN-SO-02**: Todo sobreturno registra quién lo cargó y cuándo.

## Dominio: Notificaciones (RN-NO)

- **RN-NO-01**: El sistema envía un recordatorio automático por email con confirmación de un toque.
- **RN-NO-02**: La agenda refleja el estado resultante (pendiente, confirmado, cancelado).
- **RN-NO-03** (diferenciador): Si el paciente no confirma en X horas, se envía un segundo aviso y se alerta a la recepción. X no definido (PA-08).
- **RN-NO-04** (diferenciador): Los mensajes de WhatsApp en el MVP son enlaces prellenados (wa.me), sin costo por mensaje. Que se automatice con la API oficial es **pregunta abierta** (PA-03).

## Dominio: Auditoría y acceso (RN-AU)

- **RN-AU-01**: Cada cambio en un turno registra quién lo hizo y cuándo.
- **RN-AU-02**: Existen tres roles internos: administrador, recepción y profesional (ver `03_actores_y_roles.md`).

## Dominio: Privacidad (RN-PR)

- **RN-PR-01**: El sistema debe cumplir la Ley 25.326 de protección de datos personales desde el MVP, incluido un aviso de privacidad visible en la reserva.
- **RN-PR-02**: Como el MVP no incluye ficha clínica, la Ley 26.529 aplica recién en etapas posteriores.
- **RN-PR-03**: Los datos recolectados se limitan a lo necesario para la gestión de turnos (principio de minimización; supuesto SU-07).

## Dominio: Excepciones globales

- **RN-GL-01**: Si el consultorio no mantiene al día sus bloqueos y horarios, la reserva online ofrecerá huecos no reales; se debe facilitar la carga/edición de bloqueos (riesgo operativo, ver `10_preguntas_abiertas.md`).
- **RN-GL-02**: Ningún flujo (online, recepción, edición, reprogramación) puede saltear RN-TU-01 y RN-TU-02 salvo el sobreturno explícito de RN-SO-01.
