# Visión y Objetivos

## Propósito del sistema

DentaFlow es un sistema web de **turnos y agenda online para consultorios odontológicos chicos de Argentina** (1 a 5 profesionales, una sola sede). Permite que el paciente reserve, confirme, cancele y reprograme su turno desde un enlace, y que la recepción, los profesionales y el administrador gestionen la agenda sin superposiciones.

Contexto: estos consultorios coordinan hoy los turnos por teléfono, WhatsApp o papel, lo que provoca turnos superpuestos (mismo profesional o mismo sillón), ausencias sin aviso, huecos liberados que nadie reocupa y mucho tiempo de recepción dedicado a recordar y confirmar. Los sistemas del mercado resuelven la agenda básica, pero ninguno comprueba públicamente la combinación de **sillón como recurso + prevención real de solapamientos**, y varios anuncian recordatorios "automáticos" que su propia ayuda describe como envío manual (ver `docs/discovery/informe-discovery.md`, secciones C.2 y C.3).

El proyecto es el trabajo final de la materia Desarrollo de Software (UTN), realizado por un equipo de 4 personas.

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios |
|---|---|---|
| Paciente | Reservar un turno online según la prestación, sin llamar ni escribir | Confirmar, cancelar o reprogramar desde el recordatorio; usarlo cómodamente desde el celular; no crear cuenta |
| Recepción | Gestionar la agenda y cargar turnos telefónicos/presenciales sin dobles reservas | Registrar asistencia; cargar sobreturnos explícitos; ver el estado de confirmación |
| Profesional | Ver su agenda del día | Marcar asistencias y ausencias; medir su ausentismo |
| Administrador | Configurar profesionales, sillones/boxes, prestaciones, horarios y bloqueos | Gestionar roles; auditar cambios |
| Consultorio (negocio) | Reducir ausentismo y huecos perdidos | Que un turno liberado se ofrezca automáticamente a la lista de espera (diferenciador) |

## Alcance del MVP

El alcance es **solo turnos y agenda**:

1. Profesionales, sillones/boxes y prestaciones con duración propia.
2. Horarios de atención por profesional, con bloqueos (vacaciones, feriados, ausencias).
3. Agenda diaria y semanal por profesional y por sillón, con **prevención real de solapamientos en ambos recursos** (no una simple alerta).
4. Reserva online 24/7 por link público, sin registro obligatorio del paciente (identificación por DNI y teléfono).
5. Cancelación y reprogramación por el paciente, con anticipación mínima configurable.
6. Recordatorio automático por email y confirmación de un toque.
7. Carga manual de turnos por la recepción, con sobreturno explícito y registrado.
8. Estados de asistencia: asistió, ausente y ausente con aviso.
9. Roles de administrador, recepción y profesional, con registro de cambios (quién y cuándo).
10. Aviso de privacidad conforme a la Ley 25.326.

### Diferenciadores (después del núcleo del MVP)

- Lista de espera automática para turnos liberados.
- Panel de ausentismo (por profesional, franja horaria y paciente).
- Mensajes de WhatsApp prellenados (wa.me).
- Reintento de confirmación.
- Manual público breve.

## Fuera de alcance (etapas posteriores)

- Ficha clínica, anamnesis y evolución (exigiría cumplir la Ley 26.529).
- Odontograma y periodontograma.
- Imágenes y radiografías.
- Presupuestos y planes de tratamiento.
- Caja, cobros y señas con Mercado Pago.
- Obras sociales, prepagas y liquidaciones.
- Facturación electrónica ARCA.
- API oficial de WhatsApp Business y chatbot con IA.
- Multisede.
- Campañas de marketing y recuperación.
- App móvil nativa (se usa web adaptable al celular).
- Integración con Google Calendar.

## Métricas de éxito

Orientativas (no definidas en las fuentes; confirmar con el equipo):

- Cero turnos superpuestos por profesional o por sillón en la agenda.
- Porcentaje de turnos confirmados tras el recordatorio.
- Tasa de ausentismo por profesional y franja horaria (cuando exista el panel).
- Porcentaje de turnos reservados online frente a los cargados por recepción (valida el supuesto de adopción de la reserva online).

## Restricciones del proyecto

- Trabajo final universitario, equipo de 4 personas; el plazo de entrega es una **pregunta abierta** (ver `10_preguntas_abiertas.md`).
- Consultorio de una sola sede.
- Mobile-first para el paciente.
- Datos personales de pacientes: Ley 25.326 desde el MVP.
