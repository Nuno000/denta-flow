# Funcionalidades

Organizadas por épica e historia de usuario (US-NNN). Las historias derivan de los casos de uso y del MVP de `docs/discovery/`. Los criterios de aceptación son propuestos (supuesto SU-08) y deben validarse. Marca: **[MVP]** = imprescindible; **[DIF]** = diferenciador.

## Épica 1: Configuración del consultorio

### US-001 — Gestionar profesionales, sillones y prestaciones [MVP]
**Como** administrador
**Quiero** dar de alta y editar profesionales, sillones/boxes y prestaciones con duración propia
**Para** que la agenda refleje los recursos reales

**Criterios de aceptación**:
- [ ] Puedo crear, editar y desactivar profesionales, sillones y prestaciones.
- [ ] Cada prestación tiene una duración en minutos.
- [ ] Puedo indicar qué prestaciones realiza cada profesional.

**Reglas relacionadas**: RN-TU-03

### US-002 — Definir horarios y bloqueos [MVP]
**Como** administrador
**Quiero** definir el horario de atención por profesional y bloquear franjas (vacaciones, feriados, ausencias)
**Para** que nadie pueda reservar en horarios no disponibles

**Criterios de aceptación**:
- [ ] Puedo cargar horarios semanales por profesional.
- [ ] Puedo crear bloqueos por profesional, sillón o consultorio.
- [ ] Los huecos bloqueados no aparecen en la reserva online ni se aceptan en carga manual.

**Reglas relacionadas**: RN-TU-04, RN-GL-01

### US-003 — Gestionar usuarios y roles [MVP]
**Como** administrador
**Quiero** crear usuarios con rol de administrador, recepción o profesional
**Para** controlar quién accede a qué

**Criterios de aceptación**:
- [ ] Puedo crear/desactivar usuarios y asignarles rol.
- [ ] Los permisos respetan la matriz de `03_actores_y_roles.md`.

**Reglas relacionadas**: RN-AU-02

### US-004 — Configurar anticipación mínima [MVP]
**Como** administrador
**Quiero** configurar la anticipación mínima para cancelar/reprogramar
**Para** adaptar la política al consultorio

**Criterios de aceptación**:
- [ ] Existe un parámetro editable; el valor por defecto está pendiente de definición (PA-04).

**Reglas relacionadas**: RN-CA-01, RN-CA-02

## Épica 2: Reserva online del paciente

### US-010 — Reservar un turno online [MVP]
**Como** paciente
**Quiero** reservar un turno según la prestación desde un link, sin registrarme
**Para** no tener que llamar ni escribir

**Criterios de aceptación**:
- [ ] Elijo prestación y veo solo huecos donde entra la duración completa.
- [ ] Ingreso DNI, teléfono y email, y acepto el aviso de privacidad.
- [ ] Recibo confirmación por email.
- [ ] La interfaz es usable en celular.
- [ ] Si dos pacientes reservan el mismo hueco a la vez, solo uno lo obtiene.

**Reglas relacionadas**: RN-DI-01, RN-DI-02, RN-DI-03, RN-TU-01, RN-TU-02, RN-PR-01
**Nota**: la elección de profesional está abierta (PA-05).

### US-011 — Confirmar el turno con un toque [MVP]
**Como** paciente
**Quiero** confirmar mi turno desde el recordatorio
**Para** avisar que asistiré

**Criterios de aceptación**:
- [ ] El email trae un enlace que pasa el turno de `pendiente` a `confirmado`.
- [ ] La agenda refleja el cambio.

**Reglas relacionadas**: RN-NO-01, RN-NO-02

### US-012 — Cancelar o reprogramar mi turno [MVP]
**Como** paciente
**Quiero** cancelar o reprogramar desde mi enlace
**Para** liberar el horario si no puedo asistir

**Criterios de aceptación**:
- [ ] Solo permitido hasta la anticipación mínima.
- [ ] Reprogramar ofrece solo huecos válidos.
- [ ] Fuera de la ventana, se indica contactar al consultorio.
- [ ] El cambio queda auditado.

**Reglas relacionadas**: RN-CA-01, RN-CA-02, RN-CA-03, RN-AU-01

## Épica 3: Agenda y operación diaria

### US-020 — Ver agenda diaria y semanal [MVP]
**Como** recepción
**Quiero** ver la agenda por profesional y por sillón
**Para** organizar el día

**Criterios de aceptación**:
- [ ] Vistas diaria y semanal, filtrables por profesional y por sillón.
- [ ] Se visualiza el estado de cada turno.

### US-021 — Cargar turno manual sin superposiciones [MVP]
**Como** recepción
**Quiero** cargar turnos telefónicos o presenciales
**Para** evitar dobles reservas

**Criterios de aceptación**:
- [ ] El sistema rechaza turnos que superponen profesional o sillón (no es solo una alerta).
- [ ] Respeta horarios y bloqueos.

**Reglas relacionadas**: RN-TU-01, RN-TU-02, RN-TU-04, RN-GL-02

### US-022 — Cargar sobreturno explícito [MVP]
**Como** recepción
**Quiero** cargar un sobreturno de forma explícita
**Para** atender casos excepcionales

**Criterios de aceptación**:
- [ ] Requiere una acción explícita y queda marcado como sobreturno.
- [ ] Registra quién y cuándo.
- [ ] No existe esta opción en la reserva online.

**Reglas relacionadas**: RN-SO-01, RN-SO-02, RN-DI-02

### US-023 — Ver mi agenda del día [MVP]
**Como** profesional
**Quiero** ver mi agenda del día
**Para** saber a quién atiendo

**Criterios de aceptación**:
- [ ] Solo veo mis turnos.

### US-024 — Registrar asistencia [MVP]
**Como** profesional o recepción
**Quiero** marcar asistió, ausente o ausente con aviso
**Para** llevar el control de asistencia

**Criterios de aceptación**:
- [ ] Puedo cambiar el estado de un turno a asistió/ausente/ausente con aviso.
- [ ] El cambio queda auditado.

**Reglas relacionadas**: RN-TU-05, RN-AU-01

### US-025 — Consultar el registro de cambios [MVP]
**Como** administrador
**Quiero** ver quién modificó cada turno y cuándo
**Para** auditar la operación

**Reglas relacionadas**: RN-AU-01

## Épica 4: Diferenciadores

### US-030 — Lista de espera automática [DIF]
**Como** consultorio
**Quiero** que un turno liberado se ofrezca a pacientes en espera compatibles
**Para** no perder el horario

**Criterios de aceptación**:
- [ ] Al cancelarse un turno, se notifica a los pacientes en espera de esa prestación/profesional.
- [ ] El primero en aceptar obtiene el turno, sin solapamientos.

**Reglas relacionadas**: RN-CA-04

### US-031 — Panel de ausentismo [DIF]
**Como** administrador o profesional
**Quiero** ver la tasa de ausentismo por profesional, franja horaria y paciente
**Para** medir y actuar sobre las ausencias

### US-032 — Mensajes de WhatsApp prellenados [DIF]
**Como** recepción
**Quiero** abrir un mensaje de WhatsApp (wa.me) prellenado para recordar o confirmar
**Para** complementar el email sin costo por mensaje

**Reglas relacionadas**: RN-NO-04

### US-033 — Reintento de confirmación [DIF]
**Como** recepción
**Quiero** que se reenvíe el aviso y se me alerte si el paciente no confirma
**Para** actuar a tiempo

**Reglas relacionadas**: RN-NO-03

### US-034 — Manual público breve [DIF]
**Como** usuario del sistema
**Quiero** un manual público que muestre cada función
**Para** aprender a usarlo

## Épica 5: Privacidad

### US-040 — Aviso de privacidad [MVP]
**Como** paciente
**Quiero** conocer cómo se usan mis datos antes de reservar
**Para** dar mi consentimiento informado

**Criterios de aceptación**:
- [ ] El aviso es visible y requiere aceptación en la reserva.
- [ ] Se registra la fecha del consentimiento.

**Reglas relacionadas**: RN-PR-01, RN-PR-03
