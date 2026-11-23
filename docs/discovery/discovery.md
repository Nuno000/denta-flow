# Discovery — denta-flow

**Fecha:** 2026-11-23
**Materia:** Desarrollo de Software
**Equipo:** Cirrincione, Bruno (51938) · Rodríguez Quiñonez, Jessica Magaly (48913) · Sosa Avila, Florencia Natasha (50716) · Sosa, Renzo (50173)
**Fuentes investigadas:** 17 sistemas odontológicos de turnos y agenda, con su sitio oficial, su centro de ayuda o documentación y sus videos oficiales. El detalle, el nivel de evidencia de cada funcionalidad y las referencias están en [informe-discovery.md](informe-discovery.md).

Este documento resume los 11 puntos del checklist de discovery. Es el insumo de la etapa siguiente de la fundación, la base de conocimiento (`kb`).

## 1. Problema que resuelve

Los consultorios odontológicos chicos de Argentina (1 a 5 profesionales, una sede) coordinan los turnos por teléfono, WhatsApp o papel. Eso provoca:

- turnos superpuestos para un mismo profesional o un mismo sillón;
- ausencias sin aviso;
- huecos liberados que nadie vuelve a ocupar;
- mucho tiempo de recepción dedicado a recordar y confirmar turnos.

Los sistemas del mercado ya resuelven la agenda básica. Sin embargo, ninguno comprueba públicamente la combinación de sillón como recurso y prevención de solapamientos, y varios anuncian recordatorios "automáticos" que su propia ayuda describe como envío manual.

## 2. Usuarios y roles

- **Paciente:** reserva, confirma, cancela y reprograma su turno desde un enlace.
- **Recepción:** gestiona la agenda, carga turnos telefónicos o presenciales y registra la asistencia.
- **Profesional:** consulta su agenda y marca asistencias y ausencias.
- **Administrador:** configura profesionales, sillones o boxes, prestaciones, horarios y bloqueos.

## 3. Casos de uso

1. Como **paciente**, quiero reservar un turno online según la prestación para no tener que llamar ni escribir.
2. Como **paciente**, quiero confirmar, cancelar o reprogramar mi turno desde el recordatorio para liberar el horario si no puedo ir.
3. Como **recepción**, quiero cargar un turno telefónico sin riesgo de superponer profesional o sillón para evitar dobles reservas.
4. Como **profesional**, quiero ver mi agenda del día y marcar ausentes para saber a quién atiendo y medir el ausentismo.
5. Como **consultorio**, quiero que un turno liberado se ofrezca automáticamente a la lista de espera para no perder ese horario.

## 4. Competidores y soluciones existentes

Se relevaron 17 sistemas. El ranking completo está en la sección B del informe. Estos son los cinco prioritarios para analizar mediante demo:

| Competidor | Puntaje (B) | Pricing publicado | Qué lo destaca (evidencia) | Qué validar en la demo |
|---|---|---|---|---|
| Oclusar | 3,42 | Gratis al empezar; precio en pesos sin montos | Prevención de doble turno, ARCA y firma documentadas en sus guías | Si la prevención incluye el sillón; flujo de seña |
| DentalSoft | 3,39 | $30.000 a $60.000 por mes y por profesional | Muchas pantallas de producto: seña con Mercado Pago, obras sociales, recuperación | Solapamientos y bloqueos (hoy solo como texto); costo real de WhatsApp |
| AgendaPro | 3,14 | ARS 9.900 a 129.900 por mes | Sillón y duración por práctica en pantalla; el solapamiento solo genera una alerta aceptable | Comportamiento ante solapamientos; costo con adicionales |
| Atiendo | 3,12 | 30 días gratis; montos no legibles | Lista de espera, roles y sala de espera documentados en su manual | Aviso de turno libre a la lista de espera |
| Dentalink | 2,16 | Cotización a pedido | Centro de ayuda extenso, en buena parte revisado solo por títulos | Box/Sillón, Google Calendar y módulo para Argentina |

Si alguna demo no se consigue, el reemplazo es **Bilog**. Las referencias de experiencia de usuario son AgendaPro, Atiendo y Oclusar.

## 5. Funcionalidades necesarias (MVP)

El alcance del MVP es **solo turnos y agenda**:

1. Profesionales, sillones o boxes y prestaciones con duración propia.
2. Horarios de atención por profesional, con bloqueos.
3. Agenda diaria y semanal por profesional y por sillón, con **prevención real de solapamientos en ambos recursos**.
4. Reserva online 24/7 por link público, sin registro obligatorio del paciente.
5. Cancelación y reprogramación por el paciente, con anticipación mínima configurable.
6. Recordatorio automático por email y confirmación de un toque.
7. Carga manual de turnos por la recepción, con sobreturno explícito y registrado.
8. Estados de asistencia: asistió, ausente y ausente con aviso.
9. Roles de administrador, recepción y profesional, con registro de cambios.
10. Aviso de privacidad conforme a la Ley 25.326.

## 6. Funcionalidades opcionales

**Diferenciadores** (después del núcleo del MVP):

- Lista de espera automática para turnos liberados.
- Panel de ausentismo.
- Mensajes de WhatsApp prellenados (wa.me).
- Reintento de confirmación.
- Manual público breve.

**Etapas posteriores:**

- Ficha clínica y anamnesis.
- Odontograma y periodontograma.
- Imágenes y radiografías.
- Presupuestos y planes de tratamiento.
- Caja, cobros y señas con Mercado Pago.
- Obras sociales, prepagas y liquidaciones.
- Facturación electrónica ARCA.
- API oficial de WhatsApp Business y chatbot con IA.
- Multisede.
- Campañas de marketing y recuperación.
- App móvil nativa.

La justificación de cada una está en la sección D.3 del informe.

## 7. Reglas de negocio

- Un profesional no puede tener dos turnos superpuestos.
- Un sillón o box no puede tener dos turnos superpuestos.
- La duración del turno la define la prestación.
- No se pueden reservar turnos en horarios bloqueados ni fuera del horario de atención.
- La reserva online solo ofrece huecos donde entra completa la duración de la prestación.
- Un turno pasa por estos estados: pendiente, confirmado, cancelado, asistió, ausente y ausente con aviso.
- El paciente puede cancelar o reprogramar solo hasta una anticipación mínima, que configura cada consultorio.
- El sobreturno solo lo puede cargar la recepción, de forma explícita y con registro de quién lo hizo. La reserva online nunca permite solapamientos.
- Cada cambio en un turno registra quién lo hizo y cuándo.

## 8. Integraciones

- **MVP:** email transaccional (recordatorios y confirmaciones) y enlaces de WhatsApp prellenados (wa.me), sin costo por mensaje.
- **Etapas posteriores:** API oficial de WhatsApp Business (con costo por mensaje de Meta), Mercado Pago, facturación electrónica ARCA y Google Calendar.

## 9. Restricciones

- Es un trabajo final de la materia Desarrollo de Software, hecho por un equipo de 4 personas.
- El segmento es un consultorio chico con una sola sede; la multisede queda fuera del MVP.
- Para el paciente, el sistema debe funcionar bien en el celular.
- El sistema guarda datos personales de pacientes, por lo que debe cumplir la Ley 25.326 desde el MVP. Como el MVP no incluye ficha clínica, la Ley 26.529 recién aplica en etapas posteriores.

## 10. Riesgos

- **Supuesto sin probar:** que los pacientes prefieran reservar online en lugar de seguir escribiendo por WhatsApp.
- **Supuesto sin probar:** que la prevención de solapamientos por sillón diferencie realmente al producto. Las demos podrían mostrar que algún competidor ya la tiene aunque no la publique.
- **Riesgo de costo:** automatizar WhatsApp con la API oficial implica un costo por mensaje (por ejemplo, USD 0,026 según DentalSoft).
- **Riesgo de alcance:** la presión por sumar ficha clínica u odontograma, que son estándar del mercado, puede desviar el MVP de su foco en turnos.
- **Riesgo operativo:** si el consultorio no mantiene al día sus bloqueos y horarios, la reserva online ofrece huecos que en realidad no están libres.
- **Riesgo de evidencia:** el informe se basa en fuentes públicas y no en demos. La sección "Verificación de fuentes" todavía tiene que completarla el equipo.

## 11. Preguntas abiertas

- ¿Cuál es el plazo de entrega del trabajo final?
- ¿El stack tecnológico es libre o lo fija la cátedra? (se define en la etapa `kb`)
- En el MVP, ¿los recordatorios de WhatsApp son solo por enlace prellenado o se automatizan pagando la API oficial?
- ¿Qué anticipación mínima de cancelación se usa por defecto?
- ¿La reserva online ofrece la elección de profesional o asigna el primero disponible?

## Supuestos aceptados

- El paciente no necesita crear una cuenta: se identifica con DNI y teléfono.
- La anticipación mínima para cancelar la configura cada consultorio.
- Los sobreturnos los carga solo la recepción; la reserva online nunca permite solapamientos.
