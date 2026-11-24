# Actores y Roles

## Actores del sistema

| Actor | Descripción | Cómo interactúa |
|---|---|---|
| Paciente | Persona que necesita atención odontológica. No tiene cuenta; se identifica con DNI y teléfono | Reserva, confirma, cancela y reprograma desde enlaces públicos (web mobile-first, email) |
| Recepción | Personal administrativo del consultorio | Panel interno: gestiona la agenda, carga turnos telefónicos/presenciales, sobreturnos, registra asistencia |
| Profesional | Odontólogo/a del consultorio | Panel interno: ve su agenda del día, marca asistió/ausente/ausente con aviso |
| Administrador | Responsable de configuración del consultorio | Panel interno: configura profesionales, sillones/boxes, prestaciones, horarios, bloqueos y usuarios |
| Sistema (automático) | Tareas programadas | Envía recordatorios por email, reintentos de confirmación, ofrece turnos liberados a lista de espera |

## RBAC — Matriz de permisos

Matriz propuesta a partir de los casos de uso (supuesto SU-05; el detalle fino debe validarse). C = crear, R = leer, U = actualizar, D = baja/anular, — = sin acceso.

| Recurso | Paciente (sin cuenta) | Recepción | Profesional | Administrador |
|---|---|---|---|---|
| Disponibilidad pública | R | R | R | R |
| Turno propio (por enlace) | C, R, cancelar/reprogramar (hasta anticipación mínima), confirmar | — | — | — |
| Turnos (todos) | — | C, R, U (estado, reprogramar), cancelar | R (propios); U estado de asistencia (propios) | C, R, U, cancelar |
| Sobreturno | — | C (explícito, con registro) | — | C (supuesto) |
| Profesionales | — | R | R | C, R, U, D |
| Sillones/boxes | — | R | R | C, R, U, D |
| Prestaciones y duraciones | R (en reserva) | R | R | C, R, U, D |
| Horarios y bloqueos | — | R | R (propios) | C, R, U, D |
| Lista de espera | C (anotarse, diferenciador) | R, U | — | R, U |
| Panel de ausentismo | — | R (supuesto) | R (propio) | R |
| Usuarios y roles | — | — | — | C, R, U, D |
| Registro de auditoría | — | — | — | R |
| Configuración (anticipación mínima, textos) | — | — | — | C, R, U |

Notas:

- Cada cambio en un turno genera un registro de auditoría con usuario/actor y fecha-hora (RN-AU-01).
- El paciente se identifica por DNI + teléfono; el mecanismo exacto de acceso a "su" turno (token en el enlace del email) es una decisión de diseño pendiente (ver SU-03).
- Si un usuario puede tener más de un rol (p. ej. profesional que también es admin) está sin definir (PA-09).

## Rutas públicas

Accesibles sin autenticación (nombres conceptuales; las rutas reales dependen del stack):

- Reserva online (selección de prestación, profesional si aplica, fecha/hora, datos del paciente).
- Confirmación de turno (enlace de un toque desde el email).
- Cancelación / reprogramación de un turno (enlace con token).
- Aviso de privacidad (Ley 25.326).
- Manual público breve (diferenciador).

Todo lo demás (agenda, configuración, usuarios, auditoría) requiere autenticación y rol.
