# Relevamiento de mercado: sistemas odontológicos de gestión de turnos y agenda

**Proyecto:** denta-flow — sistema de turnos odontológicos
**Materia:** Desarrollo de Software
**Integrantes:**

| Apellido y nombre | Legajo |
|---|---|
| Cirrincione, Bruno | 51938 |
| Rodríguez Quiñonez, Jessica Magaly | 48913 |
| Sosa Avila, Florencia Natasha | 50716 |
| Sosa, Renzo | 50173 |

**Fecha:** 23 de noviembre de 2026
**Estado del documento:** borrador para revisión del equipo (no generar PDF hasta completar la sección "Verificación de fuentes")

---

## Resumen ejecutivo

**Qué se investigó.** Se relevaron **17 sistemas** que gestionan turnos y agenda en consultorios odontológicos:

- 8 argentinos (en Odonto.ar y OdontoGRAMA el origen se infiere del dominio y los precios);
- 5 latinoamericanos, 3 de ellos con oferta para Argentina;
- 4 internacionales, entre ellos Doctoralia Pro, que tiene oferta en pesos.

Para cada sistema se consultaron el sitio oficial y, cuando existían, el centro de ayuda, la documentación y el canal oficial de videos; todas las consultas se hicieron el 2026-11-23. Cada funcionalidad se clasificó en uno de tres niveles:

- **comprobada**, cuando se ve en una pantalla del producto, en el centro de ayuda o la documentación, o en un video oficial;
- **afirmación comercial**, cuando solo aparece como texto de venta;
- **no evidenciada**, cuando no aparece en ninguna fuente consultada.

No se hicieron demos.

**Principales hallazgos**

1. **Lo básico es estándar.** 14 de 17 sistemas ofrecen reserva online (11 de forma comprobada) y 15 de 17 mencionan odontograma (11 comprobados). Eso no diferencia a un producto nuevo.
2. **La automatización anunciada no siempre existe.** En Bilog, Dentalink, Oclusar y Clinic Cloud, el centro de ayuda o la documentación muestran que el recordatorio por WhatsApp se envía **a mano**, aunque la web hable de automatización. Solo 4 sistemas comprueban recordatorios automáticos por WhatsApp (DentalSoft en su plan Pro, AgendaPro, Mi Agenda Dental y Odontosist).
3. **Ningún sistema comprueba a la vez el sillón como recurso y la prevención de solapamientos.**
   - Oclusar y Odontosist documentan que impiden el doble turno, pero en ninguno de los dos está comprobado el sillón como recurso (en Oclusar es una afirmación comercial; en Odontosist, no está evidenciado).
   - AgendaPro, Mi Agenda Dental y Agenda Profesional muestran el sillón o box en pantalla. Sin embargo, su prevención de solapamientos es solo una afirmación comercial, no está evidenciada o, en el caso de AgendaPro, es una alerta que se puede aceptar.
4. **Pocas listas de espera comprobadas.** Solo Atiendo, Agenda Profesional y Open Dental la comprueban; Doctoralia Pro la afirma (plan VIP) y en los demás no está evidenciada.
5. **El cumplimiento legal argentino solo se declara como texto.** Oclusar (Leyes 26.529 y 25.326) y Agenda Profesional (Ley 25.326, en su política de privacidad) son los únicos que lo mencionan.
6. **Los precios suelen ser opacos.** Oclusar, Bilog, Atiendo, Agenda Profesional, OdontoGRAMA, Dentalink y Reservo no muestran montos.

**Ranking (sección B, calculado a partir de los niveles de evidencia):** Oclusar 3,42; DentalSoft 3,39; AgendaPro 3,14; Atiendo 3,12.

**MVP recomendado.** Un sistema **solo de turnos y agenda** para consultorios chicos (1 a 5 profesionales, una sede) que:

- combine profesional, sillón o box y prestación con duración propia, con **prevención real de solapamientos en ambos recursos**;
- permita reservar, confirmar, cancelar y reprogramar por enlace;
- envíe recordatorios automáticos.

Los diferenciadores son la **lista de espera automática** y la **medición del ausentismo**. La ficha clínica, el odontograma, los cobros, las obras sociales y la facturación ARCA quedan para etapas posteriores.

---

## Metodología y alcance

- **Ámbito geográfico, por prioridad:** Argentina; Latinoamérica; soluciones internacionales útiles como referencia.
- **Fuentes:** sitio oficial, páginas de funcionalidades y de precios, y, cuando existían, el centro de ayuda, la documentación o el manual, y el canal oficial de videos. Todas se consultaron el **2026-11-23**. Las reseñas de terceros no se usaron.
- **Niveles de evidencia** (se aplican a cada funcionalidad en las tablas de la sección A):
  - **Comprobado (pantalla):** una página oficial muestra una pantalla o maqueta del producto donde la función es visible.
  - **Comprobado (centro de ayuda)** / **Comprobado (documentación)**: un artículo de ayuda, una guía o un manual oficial explica cómo usar la función.
  - **Comprobado (video oficial):** un video del canal oficial muestra o enseña la función.
  - **Afirmación comercial:** la función solo aparece como texto de venta.
  - **No evidenciado:** no aparece en ninguna fuente consultada. **No significa que el sistema no la tenga.**
- **Criterios aplicados:**
  - Si el centro de ayuda contradice la web (por ejemplo, la web dice "automático" y la ayuda describe un envío manual), **prevalece el centro de ayuda**.
  - Un artículo de ayuda del que solo se vio el **título** en un índice, sin abrirlo, **no** cuenta como comprobación. Esos casos se indican como "artículo titulado, no revisado".
  - Las cifras de adopción y las certificaciones sin alcance publicado son siempre afirmaciones comerciales.
- **Limitaciones:**
  - Las pantallas se identificaron a partir del texto de las páginas: etiquetas dentro de maquetas, textos alternativos de imágenes y demos interactivas. No hubo inspección visual de cada imagen.
  - Los videos de Open Dental los reprodujo el equipo: "A Quick Look at Web Sched Recall", "Appointment views", "Chart Module Main Window", "Perio Chart", "Treatment Plan Basics", "Patient Payments" y "Adding Insurance". Todos mostraron la funcionalidad, salvo "Appointment views", que no muestra varios profesionales. El canal de YouTube de Dentalink no se revisó.
  - El manual online de Open Dental respondió con acceso denegado (HTTP 403).
  - Los montos de Atiendo no se pueden leer en su sitio.
  - Las capturas de OdontoGRAMA no tienen texto alternativo, por lo que no pudieron clasificarse como pantallas.
  - Varios artículos de Dentalink (Box/Sillón, Google Calendar, facturación electrónica, firma digital), de AgendaPro (auditoría de cambios en citas) y de Clinic Cloud (bloqueos, presupuestos, pagos online, mutuas, firma) se vieron solo por el título. Abrirlos podría subir el nivel de evidencia de esos sistemas.

---

## A. Tabla comparativa de sistemas

Los sistemas están ordenados por **relevancia para Argentina**: primero los argentinos y los regionales con oferta local explícita (pesos, ARCA, Mercado Pago, obras sociales), luego otros latinoamericanos y al final los internacionales. Cada fila indica sus fuentes con URL y la fecha de consulta.

### A.0 Tabla resumen

| Producto / Proveedor | País · Segmento | Reserva online | Recordatorios | Fuente y fecha |
|---|---|---|---|---|
| DentalSoft / No evidenciado (razón social) | Argentina · Consultorio y clínica | Comprobado (pantalla): página de reserva en 4 pasos con DNI | Comprobado (pantalla): WhatsApp y email; automáticos solo en el plan Pro | [Turnos online](https://www.dentalsoft.com.ar/funcionalidades/turnos-online) · [Recordatorios](https://www.dentalsoft.com.ar/funcionalidades/recordatorios-whatsapp) · consultada el 2026-11-23 |
| Oclusar / No evidenciado (razón social) | Argentina · Consultorio | Comprobado (documentación): el paciente solicita y el consultorio aprueba | Comprobado (documentación): WhatsApp manual (un clic), no automático | [Guía agenda y recordatorios](https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp) · consultada el 2026-11-23 |
| Bilog / No evidenciado (razón social) | Argentina · Consultorio, clínica y obras sociales | Comprobado (centro de ayuda): link o QR | Comprobado (centro de ayuda): WhatsApp, SMS, email o llamada; envío manual | [Reserva online](https://docs.bilog.com.ar/schedule/online-appointment) · [Recordatorios](https://docs.bilog.com.ar/schedule/reminders) · consultada el 2026-11-23 |
| Dentalink / Engenis SpA | Chile (con módulo para Argentina) · Independiente, clínica y multicentro | Afirmación comercial: "agendamiento online" | Comprobado (centro de ayuda): email automático; WhatsApp manual por cita | [Planes AR](https://www.softwaredentalink.com/ar/planes) · [Ayuda: notificar por WhatsApp](https://intercom.help/softwaredentalink/es/articles/9493137-como-notificar-cita-por-whatsapp) · consultada el 2026-11-23 |
| AgendaPro / AgendaPro Inc. | Chile, alcance LATAM · Dentistas y clínicas | Comprobado (pantalla): reserva por chat con IA; sitio de reservas como afirmación comercial | Comprobado (pantalla): "Recordatorio enviado · WhatsApp" | [Software odontológico AR](https://agendapro.com/ar/dental/software-odontologico) · consultada el 2026-11-23 |
| Atiendo / No evidenciado (contacto en bitpampa.com) | Argentina · Consultorios de salud chicos y medianos | Comprobado (documentación): reserva sin cuenta, por especialidad, día y horario | Comprobado (documentación): email automático; WhatsApp "llega pronto" | [Manual de uso](https://atiendoapp.com/web/ayuda/) · consultada el 2026-11-23 |
| DentApp / No evidenciado (Codilium) | Argentina · Consultorio y clínica | No evidenciado | Afirmación comercial: email (Profesional) y WhatsApp (Clínica Plus) | [dentapp.ar](https://www.dentapp.ar/) · consultada el 2026-11-23 |
| Odonto.ar / No evidenciado | No evidenciado (precios en ARS) · Independiente y clínica de hasta 10 personas | Comprobado (pantalla): demo interactiva de reserva | No evidenciado | [Demo](https://odonto.ar/demo) · [odonto.ar](https://odonto.ar/) · consultada el 2026-11-23 |
| Doctoralia Pro / Doctoralia Internet SL | España (oferta para Argentina) · Especialistas de salud | Comprobado (documentación): guía de configuración de la reserva | Afirmación comercial: email, push o SMS según el plan | [Guía de reserva](https://pro.doctoralia.com/ar/guias/configuracion-de-la-reserva-de-cita) · [Precios](https://pro.doctoralia.com/ar/precio) · consultada el 2026-11-23 |
| Agenda Profesional / No evidenciado | Argentina · Consultorios de salud | Comprobado (pantalla): maqueta de página pública con "Reservar turno" | Afirmación comercial: avisos por email y en el celular | [agenda-profesional.com](https://www.agenda-profesional.com/) · consultada el 2026-11-23 |
| Reservo / No evidenciado (razón social) | Chile (oferta para Argentina) · Odontólogos y clínicas | Afirmación comercial: módulo de reserva en el sitio web | Afirmación comercial: correo un día antes | [Precios](https://reservo.cl/precios/) · [Centro dental](https://reservo.cl/salud/centro-dental/) · consultada el 2026-11-23 |
| OdontoGRAMA / No evidenciado (© Julio Santolo) | No evidenciado (dominio .com.ar) · Consultorio | No evidenciado | No evidenciado | [odontograma.com.ar](https://odontograma.com.ar/) · consultada el 2026-11-23 |
| Clinic Cloud / Doctoralia España SL | España · Clínicas y grupos | Afirmación comercial: vía portal Doctoralia | Comprobado (centro de ayuda): WhatsApp, SMS y email; envío manual | [Ayuda: recordatorio por WhatsApp](https://clinic-cloud.com/faq/configuracion-centro/recodatorio-de-citas-via-whatsapp) · [Tarifas](https://clinic-cloud.com/tarifas) · consultada el 2026-11-23 |
| Mi Agenda Dental / No evidenciado | México · Consultorio y clínica multisede | No evidenciado | Comprobado (pantalla): WhatsApp, recordatorio a 24 h y confirmación a 6 h | [miagendadental.com](https://miagendadental.com/) · consultada el 2026-11-23 |
| Odontosist / Odontosist (RUC peruano) | Perú · Clínicas dentales | Comprobado (documentación): portal público de reservas | Comprobado (documentación): WhatsApp y email automáticos, 1 h y 24 h antes | [Guía de uso](https://odontosist.com/guia-usuario.html) · consultada el 2026-11-23 |
| MedFarmaSoft Dental / MedFarmaSoft | España · Dentista solo y clínica | Comprobado (pantalla): maqueta "Reserva tu cita" | Afirmación comercial: WhatsApp y email | [Dental](https://medfarmasoft.com/dental/) · [Recordatorios](https://medfarmasoft.es/recordatorios-whatsapp/) · consultada el 2026-11-23 |
| Open Dental / Open Dental Software | EE. UU. · Prácticas dentales | Comprobado (video oficial): reserva online con Web Sched Recall | Comprobado (documentación): eReminders por texto o email | [Video: A Quick Look at Web Sched Recall](https://youtu.be/yZvlozVRdN8) · [Web Sched](https://www.opendental.com/site/websched.html) · [eReminders](https://www.opendental.com/site/ereminders.html) · consultada el 2026-11-23 |

### A.1 Identificación, modalidad y fuentes oficiales

| # | Producto | Empresa / país | Segmento | Modalidad | Sitio oficial | Centro de ayuda / documentación | Videos oficiales | Consulta |
|---|---|---|---|---|---|---|---|---|
| 1 | DentalSoft | No evidenciado / Argentina | Consultorios y clínicas | SaaS en la nube | [dentalsoft.com.ar](https://dentalsoft.com.ar/) · [funcionalidades](https://www.dentalsoft.com.ar/funcionalidades) | No encontrado (solo preguntas frecuentes en la web) | No encontrado | 2026-11-23 |
| 2 | Oclusar | No evidenciado / Argentina | Consultorios; aparte, casas dentales y laboratorios | Web y móvil, sin instalación | [oclusar.com.ar](https://www.oclusar.com.ar/) | [Guías](https://www.oclusar.com.ar/guias) (artículos sobre funciones; no es un manual) | No encontrado | 2026-11-23 |
| 3 | Bilog | No evidenciado / Argentina | Consultorios, clínicas y obras sociales | Web y app iOS y Android | [bilog.com.ar](https://bilog.com.ar/) | [docs.bilog.com.ar](https://docs.bilog.com.ar/) | No encontrado | 2026-11-23 |
| 4 | Dentalink | Engenis SpA / Chile | Independiente, clínicas y multicentro | SaaS (AWS) | [softwaredentalink.com/ar](https://www.softwaredentalink.com/ar) | [ayuda.softwaredentalink.com](https://ayuda.softwaredentalink.com/es) | [YouTube](https://www.youtube.com/c/softwaredentalink) (enlazado; no revisado) | 2026-11-23 |
| 5 | AgendaPro | AgendaPro Inc. / Chile | Dentistas y clínicas (multirrubro) | Nube, con app iOS y Android | [agendapro.com/ar/dental](https://agendapro.com/ar/dental/software-odontologico) | [ayuda.agendapro.com](https://ayuda.agendapro.com/es/); [Academia](https://academia.agendapro.com) (no revisada) | Canal no encontrado (solo testimonios embebidos) | 2026-11-23 |
| 6 | Atiendo | No evidenciado / Argentina | Consultorios de salud, incluidos los odontológicos | Web en el navegador | [atiendoapp.com](https://atiendoapp.com/) | [Manual de uso](https://atiendoapp.com/web/ayuda/) | No encontrado | 2026-11-23 |
| 7 | DentApp | No evidenciado (Codilium) / Argentina | Consultorios y clínicas | Aplicación 100 % web | [dentapp.ar](https://www.dentapp.ar/) | No encontrado | No encontrado | 2026-11-23 |
| 8 | Odonto.ar | No evidenciado / No evidenciado | Independiente y clínicas de hasta 10 personas | Web | [odonto.ar](https://odonto.ar/) | No encontrado ([demo interactiva](https://odonto.ar/demo)) | No encontrado | 2026-11-23 |
| 9 | Doctoralia Pro | Doctoralia Internet SL / España | Especialistas de salud | Web, app y directorio | [pro.doctoralia.com/ar](https://pro.doctoralia.com/ar) | [Guías](https://pro.doctoralia.com/ar/guias) · [Preguntas frecuentes](https://pro.doctoralia.com/ar/preguntas-frecuentes) | No encontrado | 2026-11-23 |
| 10 | Agenda Profesional | No evidenciado / Argentina | Consultorios de salud | Web | [agenda-profesional.com](https://www.agenda-profesional.com/) | No encontrado (solo [privacidad](https://www.agenda-profesional.com/privacidad) y [condiciones](https://www.agenda-profesional.com/condiciones)) | No encontrado | 2026-11-23 |
| 11 | Reservo | No evidenciado / Chile | Odontólogos y clínicas | No evidenciado | [reservo.cl/precios](https://reservo.cl/precios/) · [reservo.com.ar](https://reservo.com.ar/) | No encontrado (solo teléfonos de soporte y capacitación) | No encontrado | 2026-11-23 |
| 12 | OdontoGRAMA | No evidenciado / No evidenciado | Consultorio | Nube | [odontograma.com.ar](https://odontograma.com.ar/) | No encontrado | No encontrado | 2026-11-23 |
| 13 | Clinic Cloud | Doctoralia España SL / España | Clínicas y franquicias | Nube | [clinic-cloud.com](https://clinic-cloud.com/) · [tarifas](https://clinic-cloud.com/tarifas) | [Centro de ayuda](https://clinic-cloud.com/faq) | No encontrado | 2026-11-23 |
| 14 | Mi Agenda Dental | No evidenciado / México | Consultorios y clínicas multisede | Web | [miagendadental.com](https://miagendadental.com/) | No encontrado (el [blog](https://miagendadental.com/blog/) trata normativa) | No encontrado | 2026-11-23 |
| 15 | Odontosist | Odontosist / Perú | Clínicas dentales | En el navegador | [odontosist.com](https://odontosist.com/) | [Guía de uso](https://odontosist.com/guia-usuario.html) | No encontrado | 2026-11-23 |
| 16 | MedFarmaSoft Dental | MedFarmaSoft / España | Dentista solo y clínicas | Nube y apps nativas | [medfarmasoft.com/dental](https://medfarmasoft.com/dental/) | No encontrado | Tutorial y demo embebidos en la web; canal no identificado | 2026-11-23 |
| 17 | Open Dental | Open Dental Software / EE. UU. | Prácticas dentales | Instalación local, con nube opcional | [opendental.com](https://www.opendental.com/) | [Documentación](https://www.opendental.com/site/documentation.html) (manual completo solo para clientes) | [YouTube](https://www.youtube.com/channel/UC2f1mkxYSud-V72gxpgmRrQ) | 2026-11-23 |

### A.2 Gestión de agenda

| # | Producto | Multiprofesional | Sillón o box | Duración por prestación | Bloqueos | Sobreturnos | Prevención de solapamientos | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|---|---|---|
| 1 | DentalSoft | Comprobado (pantalla) | No evidenciado | Afirmación comercial | Afirmación comercial | Afirmación comercial | Afirmación comercial | [Agenda](https://www.dentalsoft.com.ar/funcionalidades/agenda-odontologica) · [Presupuestos](https://www.dentalsoft.com.ar/funcionalidades/presupuestos) |
| 2 | Oclusar | Afirmación comercial | Afirmación comercial ("asignás los sillones") | No evidenciado | Comprobado (documentación) | Comprobado (documentación) | Comprobado (documentación): la base de datos impide el doble turno | [Guía agenda](https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp) · [Web](https://www.oclusar.com.ar/) |
| 3 | Bilog | Comprobado (centro de ayuda) | Afirmación comercial ("consultorios") | Afirmación comercial (la ayuda solo muestra intervalos por horario) | Comprobado (centro de ayuda): "Anular horario" | Comprobado (centro de ayuda) | Afirmación comercial | [Turnos](https://docs.bilog.com.ar/schedule/appointments) · [Reserva online](https://docs.bilog.com.ar/schedule/online-appointment) |
| 4 | Dentalink | Afirmación comercial | No evidenciado (artículo titulado "Box/Sillón", no revisado) | Comprobado (centro de ayuda): por motivo de atención | No evidenciado | No evidenciado | No evidenciado | [Configurar agenda](https://intercom.help/softwaredentalink/es/articles/9493114-termina-de-configurar-tu-agenda) · [Colección Agenda](https://ayuda.softwaredentalink.com/es/collections/9633575-agenda) |
| 5 | AgendaPro | Comprobado (pantalla) | Comprobado (pantalla): "Sillón 1" | Comprobado (pantalla): 45 y 90 min | Comprobado (centro de ayuda) | Comprobado (centro de ayuda) | **No evidenciado:** al superponer solo aparece una alerta que se puede aceptar | [Dental AR](https://agendapro.com/ar/dental/software-odontologico) · [Bloqueos](https://ayuda.agendapro.com/es/articles/5837664-como-programar-y-gestionar-bloqueos-de-horario-de-mis-profesionales) · [Sobrecupo](https://ayuda.agendapro.com/es/articles/6500344-como-agendar-un-sobrecupo) |
| 6 | Atiendo | Comprobado (documentación) | No evidenciado | Comprobado (documentación): por profesional, no por prestación | Comprobado (documentación) | Comprobado (documentación) | No evidenciado (el sobreturno admite un horario ocupado) | [Manual](https://atiendoapp.com/web/ayuda/) |
| 7 | DentApp | Afirmación comercial | No evidenciado | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [dentapp.ar](https://www.dentapp.ar/) |
| 8 | Odonto.ar | Afirmación comercial | No evidenciado | Comprobado (pantalla): 30 o 60 min según el motivo | No evidenciado | No evidenciado | No evidenciado | [Web](https://odonto.ar/) · [Demo](https://odonto.ar/demo) |
| 9 | Doctoralia Pro | Comprobado (documentación) | No evidenciado | Comprobado (documentación): tiempo por servicio | Afirmación comercial | No evidenciado | No evidenciado | [Guía de reserva](https://pro.doctoralia.com/ar/guias/configuracion-de-la-reserva-de-cita) · [Agenda](https://pro.doctoralia.com/ar/agenda-doctoralia-para-especialistas) |
| 10 | Agenda Profesional | No evidenciado | Comprobado (pantalla): pestaña "Box" | No evidenciado | Afirmación comercial | No evidenciado | **No evidenciado** (la maqueta muestra dos turnos a las 10:00) | [Web](https://www.agenda-profesional.com/) |
| 11 | Reservo | Afirmación comercial | No evidenciado | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Precios](https://reservo.cl/precios/) |
| 12 | OdontoGRAMA | Afirmación comercial | No evidenciado | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | Comprobado (centro de ayuda) | No evidenciado | No evidenciado | No evidenciado (artículo titulado, no revisado) | No evidenciado | No evidenciado | [Usuarios](https://clinic-cloud.com/faq/configuracion-centro/configuracion-de-usuarios) · [Agenda](https://clinic-cloud.com/faq/agenda-y-comunicaciones) |
| 14 | Mi Agenda Dental | Comprobado (pantalla) | Comprobado (pantalla): "multi-unidad" | No evidenciado | No evidenciado | No evidenciado | Afirmación comercial ("candados" contra la doble cita) | [Web](https://miagendadental.com/) |
| 15 | Odontosist | Comprobado (documentación) | No evidenciado | No evidenciado | Comprobado (documentación): solo descanso semanal | No evidenciado | Comprobado (documentación): bloquea los horarios ocupados del dentista | [Guía](https://odontosist.com/guia-usuario.html) |
| 16 | MedFarmaSoft Dental | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial | No evidenciado | Afirmación comercial | [Dental](https://medfarmasoft.com/dental/) |
| 17 | Open Dental | No evidenciado (video revisado: Appointment views) | No evidenciado | No evidenciado | Comprobado (documentación): parcial, bloqueo desde la lista ASAP | No evidenciado | No evidenciado | [Lista ASAP](https://www.opendental.com/site/0_asaplist.html) · [Video: Appointment views](https://www.youtube.com/watch?v=WQUMsj0ihb0) |

### A.3 Turnos digitales y automatización

La reserva online y los recordatorios figuran en la tabla A.0.

| # | Producto | Confirmación por el paciente | Cancelación / reprogramación por el paciente | Lista de espera | Recuperación de ausentes, controles y campañas | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|---|
| 1 | DentalSoft | Comprobado (pantalla): botones Confirmar y Cancelar | Cancelación: Comprobado (pantalla); reprogramación: No evidenciado | No evidenciado | Comprobado (pantalla): recuperación a 6 meses por WhatsApp | [Recordatorios](https://www.dentalsoft.com.ar/funcionalidades/recordatorios-whatsapp) · [Recuperación](https://www.dentalsoft.com.ar/funcionalidades/recuperacion-de-pacientes) |
| 2 | Oclusar | Afirmación comercial | No evidenciado | No evidenciado | No evidenciado | [Web](https://www.oclusar.com.ar/) |
| 3 | Bilog | Comprobado (centro de ayuda): responde "confirmo" o "cancelo" | Cancelación: Comprobado (centro de ayuda); reprogramación: No evidenciado | No evidenciado | No evidenciado | [Recordatorios](https://docs.bilog.com.ar/schedule/reminders) · [Reserva online](https://docs.bilog.com.ar/schedule/online-appointment) |
| 4 | Dentalink | Comprobado (centro de ayuda): link para confirmar o anular | Cancelación: Comprobado (centro de ayuda); reprogramación: No evidenciado | No evidenciado | Afirmación comercial ("tareas automáticas de control", plan Titanium) | [Notificar por WhatsApp](https://intercom.help/softwaredentalink/es/articles/9493137-como-notificar-cita-por-whatsapp) · [Web AR](https://www.softwaredentalink.com/ar) |
| 5 | AgendaPro | Afirmación comercial ("el asistente IA insiste") | Afirmación comercial | No evidenciado | Comprobado (pantalla): segmento automático y envío de campaña | [Dental AR](https://agendapro.com/ar/dental/software-odontologico) · [Planes](https://agendapro.com/ar/planes) |
| 6 | Atiendo | Comprobado (documentación): link del email o código del turno | Cancelación: Comprobado (documentación); la reprogramación la hace el consultorio | Comprobado (documentación): aviso por email y WhatsApp | No evidenciado | [Manual](https://atiendoapp.com/web/ayuda/) |
| 7 | DentApp | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [dentapp.ar](https://www.dentapp.ar/) |
| 8 | Odonto.ar | No evidenciado | Afirmación comercial | No evidenciado | No evidenciado | [Web](https://odonto.ar/) |
| 9 | Doctoralia Pro | Afirmación comercial | Afirmación comercial | Afirmación comercial (plan VIP) | Afirmación comercial (campañas por SMS y email) | [Precios](https://pro.doctoralia.com/ar/precio) · [Agenda](https://pro.doctoralia.com/ar/agenda-doctoralia-para-especialistas) |
| 10 | Agenda Profesional | Afirmación comercial | Afirmación comercial | Comprobado (pantalla): en el menú de la maqueta | No evidenciado | [Web](https://www.agenda-profesional.com/) · [Condiciones](https://www.agenda-profesional.com/condiciones) |
| 11 | Reservo | No evidenciado | No evidenciado | No evidenciado | Afirmación comercial (mailing) | [Precios](https://reservo.cl/precios/) |
| 12 | OdontoGRAMA | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial (campañas de marketing) | [Web](https://clinic-cloud.com/) · [Tarifas](https://clinic-cloud.com/tarifas) |
| 14 | Mi Agenda Dental | Afirmación comercial | No evidenciado | No evidenciado | Comprobado (pantalla): "no asistió" y "limpieza a 6 meses" | [Web](https://miagendadental.com/) |
| 15 | Odontosist | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Guía](https://odontosist.com/guia-usuario.html) |
| 16 | MedFarmaSoft Dental | Afirmación comercial (responde por WhatsApp) | Afirmación comercial (solo cancelación) | No evidenciado | No evidenciado | [Dental](https://medfarmasoft.com/dental/) |
| 17 | Open Dental | Comprobado (documentación): eConfirmations | No evidenciado | Comprobado (documentación): lista ASAP | Comprobado (documentación): recall | [eReminders](https://www.opendental.com/site/ereminders.html) · [Lista ASAP](https://www.opendental.com/site/0_asaplist.html) · [Web Sched](https://www.opendental.com/site/websched.html) |

### A.4 Gestión odontológica clínica

| # | Producto | Historia clínica y anamnesis | Odontograma | Periodontograma | Imágenes y radiografías | Presupuestos y plan de tratamiento | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|---|---|
| 1 | DentalSoft | Comprobado (pantalla) | Comprobado (pantalla) | No evidenciado | Comprobado (pantalla): estudios y escaneos 3D | Comprobado (pantalla) | [HC y odontograma](https://www.dentalsoft.com.ar/funcionalidades/historia-clinica-odontograma) · [Presupuestos](https://www.dentalsoft.com.ar/funcionalidades/presupuestos) |
| 2 | Oclusar | Comprobado (documentación) | Comprobado (documentación) | Comprobado (documentación) | Afirmación comercial | Comprobado (documentación) | [Guía HC](https://www.oclusar.com.ar/guias/historia-clinica-digital-ley-26529) · [Guía odontograma](https://www.oclusar.com.ar/guias/odontograma-digital) · [Guía presupuestos](https://www.oclusar.com.ar/guias/presupuestos-que-el-paciente-acepta) |
| 3 | Bilog | Comprobado (centro de ayuda) | Comprobado (pantalla) | No evidenciado | Afirmación comercial | Comprobado (centro de ayuda) | [HC](https://docs.bilog.com.ar/patients/medical-history) · [Presupuestos](https://docs.bilog.com.ar/patients/budgets) · [Web](https://bilog.com.ar/) |
| 4 | Dentalink | Comprobado (centro de ayuda) | Comprobado (centro de ayuda) | Afirmación comercial | Afirmación comercial | Afirmación comercial | [Odontograma](https://intercom.help/softwaredentalink/es/articles/9493218-como-usar-el-odontograma) · [Módulos](https://ayuda.softwaredentalink.com/es/collections/9620066-conoce-todos-los-modulos-de-dentalink) · [Planes](https://www.softwaredentalink.com/ar/planes) |
| 5 | AgendaPro | Comprobado (pantalla) | Comprobado (pantalla) | No evidenciado | Afirmación comercial | Comprobado (pantalla) | [Dental AR](https://agendapro.com/ar/dental/software-odontologico) |
| 6 | Atiendo | Comprobado (documentación) | Comprobado (documentación) | No evidenciado | Comprobado (documentación) | Comprobado (documentación): plan de tratamiento, sin presupuesto | [Manual](https://atiendoapp.com/web/ayuda/) |
| 7 | DentApp | Afirmación comercial | Afirmación comercial | No evidenciado | Afirmación comercial | Afirmación comercial | [dentapp.ar](https://www.dentapp.ar/) |
| 8 | Odonto.ar | Afirmación comercial | Comprobado (pantalla) | No evidenciado | Afirmación comercial | Afirmación comercial | [Web](https://odonto.ar/) |
| 9 | Doctoralia Pro | Afirmación comercial ("registros médicos electrónicos") | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Precios](https://pro.doctoralia.com/ar/precio) |
| 10 | Agenda Profesional | Comprobado (pantalla): en el menú de la maqueta | Afirmación comercial | Afirmación comercial | Afirmación comercial | Afirmación comercial | [Web](https://www.agenda-profesional.com/) · [Privacidad](https://www.agenda-profesional.com/privacidad) |
| 11 | Reservo | Afirmación comercial | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial | [Dental](https://reservo.cl/homepage/dental/) · [Centro dental](https://reservo.cl/salud/centro-dental/) |
| 12 | OdontoGRAMA | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial | No evidenciado | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | Afirmación comercial | Afirmación comercial (plan Max) | Afirmación comercial | Afirmación comercial | Afirmación comercial | [Tarifas](https://clinic-cloud.com/tarifas) |
| 14 | Mi Agenda Dental | Comprobado (pantalla) | Comprobado (pantalla) | Afirmación comercial | No evidenciado | Comprobado (pantalla) | [Web](https://miagendadental.com/) |
| 15 | Odontosist | Comprobado (documentación) | Comprobado (documentación) | No evidenciado | Comprobado (documentación) | Comprobado (documentación): tratamientos por sesión, sin presupuesto formal | [Guía](https://odontosist.com/guia-usuario.html) |
| 16 | MedFarmaSoft Dental | Afirmación comercial | Comprobado (pantalla) | Afirmación comercial | Afirmación comercial | Comprobado (pantalla): plan con importes | [Odontograma](https://medfarmasoft.es/odontograma/) · [Dental](https://medfarmasoft.com/dental/) |
| 17 | Open Dental | Historia clínica: Comprobado (video oficial); anamnesis: Comprobado (video oficial) | Comprobado (pantalla): odontograma 3D | Comprobado (video oficial): periodontograma | Afirmación comercial | Plan de tratamiento: Comprobado (video oficial); importes: Comprobado (video oficial) | [Web](https://www.opendental.com/) · [Video: Chart Module Main Window](https://www.youtube.com/watch?v=H1PpDOY8zOg) · [Video: Perio Chart](https://www.youtube.com/watch?v=ApO1MkPkq48) · [Video: Treatment Plan Basics](https://www.youtube.com/watch?v=apYXaA3_JVU) |

### A.5 Administración, cobros y facturación

| # | Producto | Caja y cobros | Seña / Mercado Pago | Facturación electrónica | Obras sociales y prepagas | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|---|
| 1 | DentalSoft | Comprobado (pantalla) | Comprobado (pantalla): link de seña (plan Pro) | No evidenciado | Comprobado (pantalla): cobertura por plan y coseguro | [Caja](https://www.dentalsoft.com.ar/funcionalidades/caja-cobros) · [Mercado Pago](https://www.dentalsoft.com.ar/funcionalidades/cobros-mercadopago) · [Obras sociales](https://www.dentalsoft.com.ar/funcionalidades/obras-sociales) |
| 2 | Oclusar | Comprobado (documentación) | Comprobado (pantalla): seña por Mercado Pago | Comprobado (documentación): facturas B y C con CAE de ARCA | Comprobado (documentación): estado por prestación | [Guía facturación](https://www.oclusar.com.ar/guias/facturacion-electronica-para-odontologos) · [Guía obras sociales](https://www.oclusar.com.ar/guias/obras-sociales-sin-volverse-loco) · [Web](https://www.oclusar.com.ar/) |
| 3 | Bilog | Comprobado (centro de ayuda): caja diaria | No evidenciado | Comprobado (pantalla): factura B vía Facturante (ARCA no nombrada) | Comprobado (centro de ayuda): liquidación por obra social | [Caja](https://docs.bilog.com.ar/administration/box-daily) · [Factura](https://bilog.com.ar/electronic-invoice/) · [Liquidaciones](https://docs.bilog.com.ar/liquidations/liquidations-coverage-medical) |
| 4 | Dentalink | Afirmación comercial | No evidenciado | No evidenciado (colección "Facturación electrónica" sin mención de ARCA) | Afirmación comercial (artículos del módulo argentino vistos solo por título) | [Planes](https://www.softwaredentalink.com/ar/planes) · [Módulo para Argentina](https://ayuda.softwaredentalink.com/es/collections/19196578-modulo-exclusivo-para-argentina) |
| 5 | AgendaPro | Comprobado (pantalla): "Caja de hoy" y cuotas | Comprobado (pantalla): "Agenda · Señas" con Mercado Pago | No evidenciado ("próximamente") | No evidenciado | [Dental AR](https://agendapro.com/ar/dental/software-odontologico) · [Planes](https://agendapro.com/ar/planes) |
| 6 | Atiendo | No evidenciado | Comprobado (documentación) | No evidenciado (declara que no la emite) | Comprobado (documentación): coberturas y planilla mensual | [Manual](https://atiendoapp.com/web/ayuda/) |
| 7 | DentApp | Afirmación comercial | Afirmación comercial (cobros con Mercado Pago) | No evidenciado | Afirmación comercial (solo integración a medida) | [dentapp.ar](https://www.dentapp.ar/) |
| 8 | Odonto.ar | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial | [Web](https://odonto.ar/) |
| 9 | Doctoralia Pro | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Precios](https://pro.doctoralia.com/ar/precio) |
| 10 | Agenda Profesional | Comprobado (pantalla) | Comprobado (pantalla): "Señas"; flujo con Mercado Pago en las condiciones | No evidenciado | Comprobado (pantalla): etiqueta "Obra social" | [Web](https://www.agenda-profesional.com/) · [Condiciones](https://www.agenda-profesional.com/condiciones) |
| 11 | Reservo | Afirmación comercial | Afirmación comercial | Afirmación comercial ("factura AFIP/ARCA" en el título) | No evidenciado | [reservo.com.ar](https://reservo.com.ar/) · [Centro dental](https://reservo.cl/salud/centro-dental/) |
| 12 | OdontoGRAMA | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | Afirmación comercial | Afirmación comercial (pago online por TPV) | Afirmación comercial (Verifactu y TicketBAI, de España) | Afirmación comercial (mutuas españolas) | [Tarifas](https://clinic-cloud.com/tarifas) · [Web](https://clinic-cloud.com/) |
| 14 | Mi Agenda Dental | Comprobado (pantalla) | No evidenciado | Comprobado (pantalla): CFDI de México; automática "próximamente" | No evidenciado | [Web](https://miagendadental.com/) |
| 15 | Odontosist | Comprobado (documentación) | No evidenciado | No evidenciado | No evidenciado | [Guía](https://odontosist.com/guia-usuario.html) |
| 16 | MedFarmaSoft Dental | Afirmación comercial | No evidenciado | No evidenciado | No evidenciado | [Dental](https://medfarmasoft.com/dental/) |
| 17 | Open Dental | Comprobado (video oficial): registro de pagos del paciente | Afirmación comercial (Payment Portal) | No evidenciado | Comprobado (video oficial): alta de seguros de EE. UU. | [Video: Patient Payments](https://www.youtube.com/watch?v=x3CKA_n1qEs) · [Video: Adding Insurance](https://www.youtube.com/watch?v=7wRVh0A2uP0) |

### A.6 Integraciones

| # | Producto | WhatsApp | Google Calendar | API pública | Firma electrónica o digital | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|---|
| 1 | DentalSoft | Comprobado (pantalla): Meta cobra USD 0,026 por mensaje | Afirmación comercial | No evidenciado | Comprobado (pantalla): consentimiento "Firmado" | [Recordatorios](https://www.dentalsoft.com.ar/funcionalidades/recordatorios-whatsapp) · [Documentación clínica](https://www.dentalsoft.com.ar/funcionalidades/documentacion-clinica) |
| 2 | Oclusar | Comprobado (documentación): envío manual desde el número propio | Afirmación comercial | No evidenciado | Comprobado (documentación): Ley 25.506 | [Guía agenda](https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp) · [Guía consentimiento](https://www.oclusar.com.ar/guias/consentimiento-informado-odontologia) |
| 3 | Bilog | Comprobado (centro de ayuda): envío manual | No evidenciado | No evidenciado (su llms.txt indica que no hay API pública) | Afirmación comercial: Ley 25.506 | [Recordatorios](https://docs.bilog.com.ar/schedule/reminders) · [llms.txt](https://bilog.com.ar/llms.txt) |
| 4 | Dentalink | Comprobado (centro de ayuda): envío manual por cita | No evidenciado (artículo titulado, no revisado) | Afirmación comercial (adicional pago) | Afirmación comercial (adicional) | [Notificar por WhatsApp](https://intercom.help/softwaredentalink/es/articles/9493137-como-notificar-cita-por-whatsapp) · [Planes](https://www.softwaredentalink.com/ar/planes) |
| 5 | AgendaPro | Comprobado (pantalla) | Afirmación comercial | Afirmación comercial (USD 200 por mes) | Comprobado (pantalla): consentimiento "Firmado" | [Dental AR](https://agendapro.com/ar/dental/software-odontologico) · [Planes](https://agendapro.com/ar/planes) |
| 6 | Atiendo | Comprobado (documentación): solo aviso a la lista de espera; recordatorio "próximamente" | Comprobado (documentación) | No evidenciado | No evidenciado | [Manual](https://atiendoapp.com/web/ayuda/) |
| 7 | DentApp | Afirmación comercial | Afirmación comercial | Afirmación comercial (plan Empresarial) | No evidenciado | [dentapp.ar](https://www.dentapp.ar/) |
| 8 | Odonto.ar | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Web](https://odonto.ar/) |
| 9 | Doctoralia Pro | Afirmación comercial | No evidenciado (la guía y las preguntas frecuentes se contradicen) | No evidenciado | No evidenciado | [Agenda](https://pro.doctoralia.com/ar/agenda-doctoralia-para-especialistas) · [Sincronización](https://pro.doctoralia.com/ar/preguntas-frecuentes/sincronizacion) |
| 10 | Agenda Profesional | No evidenciado | No evidenciado (declara que no sincroniza calendarios) | No evidenciado | Afirmación comercial | [Privacidad](https://www.agenda-profesional.com/privacidad) |
| 11 | Reservo | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Precios](https://reservo.cl/precios/) |
| 12 | OdontoGRAMA | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | Comprobado (centro de ayuda): envío manual | No evidenciado | No evidenciado | Afirmación comercial | [Recordatorio por WhatsApp](https://clinic-cloud.com/faq/configuracion-centro/recodatorio-de-citas-via-whatsapp) · [Tarifas](https://clinic-cloud.com/tarifas) |
| 14 | Mi Agenda Dental | Comprobado (pantalla) | Afirmación comercial | No evidenciado | No evidenciado (se sube el PDF firmado) | [Web](https://miagendadental.com/) |
| 15 | Odontosist | Comprobado (documentación) | Comprobado (documentación) | No evidenciado | No evidenciado | [Guía](https://odontosist.com/guia-usuario.html) |
| 16 | MedFarmaSoft Dental | Afirmación comercial | Afirmación comercial | No evidenciado | Afirmación comercial | [Dental](https://medfarmasoft.com/dental/) · [Recordatorios](https://medfarmasoft.es/recordatorios-whatsapp/) |
| 17 | Open Dental | No evidenciado | No evidenciado | No evidenciado (enlace a la especificación no revisado) | No evidenciado | [Documentación](https://www.opendental.com/site/documentation.html) |

### A.7 Operación, seguridad y cumplimiento

| # | Producto | Roles y permisos | Auditoría y trazabilidad | Exportación de datos | Seguridad y cumplimiento declarado | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|---|
| 1 | DentalSoft | Afirmación comercial | Afirmación comercial | Afirmación comercial (PDF y Excel) | Afirmación comercial (sin normativa citada) | [Funcionalidades](https://www.dentalsoft.com.ar/funcionalidades) · [Caja](https://www.dentalsoft.com.ar/funcionalidades/caja-cobros) |
| 2 | Oclusar | Afirmación comercial | Comprobado (documentación): solo evoluciones | Comprobado (documentación) | Afirmación comercial: Leyes 26.529 y 25.326, copias diarias, SHA-256 | [Guía HC](https://www.oclusar.com.ar/guias/historia-clinica-digital-ley-26529) · [Web](https://www.oclusar.com.ar/) |
| 3 | Bilog | Afirmación comercial | Comprobado (centro de ayuda): historia clínica | No evidenciado | No evidenciado | [HC](https://docs.bilog.com.ar/patients/medical-history) · [llms.txt](https://bilog.com.ar/llms.txt) |
| 4 | Dentalink | Afirmación comercial | Comprobado (centro de ayuda): quién agendó y cuándo | Afirmación comercial | Afirmación comercial: ISO 27001, HIPAA, respaldos diarios | [Configurar agenda](https://intercom.help/softwaredentalink/es/articles/9493114-termina-de-configurar-tu-agenda) · [Planes](https://www.softwaredentalink.com/ar/planes) |
| 5 | AgendaPro | Afirmación comercial | No evidenciado (artículos titulados, no revisados) | No evidenciado | Afirmación comercial ("ficha clínica cifrada") | [Dental AR](https://agendapro.com/ar/dental/software-odontologico) · [Colección agenda](https://ayuda.agendapro.com/es/collections/969562-agenda-reservas-y-bloqueos) |
| 6 | Atiendo | Comprobado (documentación): la secretaria no ve la historia clínica | Afirmación comercial | Comprobado (documentación): historia clínica en PDF | Afirmación comercial: copias diarias y cifrado | [Manual](https://atiendoapp.com/web/ayuda/) · [Web](https://atiendoapp.com/) |
| 7 | DentApp | Afirmación comercial | No evidenciado | Afirmación comercial (CSV o JSON al terminar el contrato) | Afirmación comercial: backups y registro en la AAIP (logo) | [Términos](https://www.dentapp.ar/terms) |
| 8 | Odonto.ar | Afirmación comercial | Afirmación comercial | No evidenciado | No evidenciado | [Web](https://odonto.ar/) |
| 9 | Doctoralia Pro | No evidenciado | No evidenciado | No evidenciado | Afirmación comercial: RGPD y AWS | [Seguridad de datos](https://pro.doctoralia.com/ar/preguntas-frecuentes/seguridad-datos) |
| 10 | Agenda Profesional | Afirmación comercial | No evidenciado | No evidenciado | Afirmación comercial: Ley 25.326 y cifrado (política de privacidad) | [Privacidad](https://www.agenda-profesional.com/privacidad) |
| 11 | Reservo | No evidenciado | No evidenciado | Afirmación comercial (resumen de comisiones) | No evidenciado | [Centro dental](https://reservo.cl/salud/centro-dental/) |
| 12 | OdontoGRAMA | No evidenciado | No evidenciado | No evidenciado | Afirmación comercial ("olvídese de los backups") | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | Comprobado (centro de ayuda): cinco roles; doble factor | Afirmación comercial | No evidenciado | Afirmación comercial: RGPD e ISO/IEC 27001 | [Usuarios](https://clinic-cloud.com/faq/configuracion-centro/configuracion-de-usuarios) · [Tarifas](https://clinic-cloud.com/tarifas) |
| 14 | Mi Agenda Dental | Afirmación comercial | Comprobado (pantalla): pestaña "Auditoría" | Comprobado (pantalla): CSV | Afirmación comercial: LFPDPPP (México) | [Web](https://miagendadental.com/) |
| 15 | Odontosist | Comprobado (documentación) | Comprobado (documentación): parcial, solo pagos | No evidenciado | Afirmación comercial: HTTPS | [Guía](https://odontosist.com/guia-usuario.html) |
| 16 | MedFarmaSoft Dental | Afirmación comercial | Afirmación comercial | No evidenciado | Afirmación comercial: RGPD y Ley 41/2002 | [Dental](https://medfarmasoft.com/dental/) |
| 17 | Open Dental | No evidenciado | No evidenciado | No evidenciado | No evidenciado | [Documentación](https://www.opendental.com/site/documentation.html) |

### A.8 Modelo comercial, fortalezas, limitaciones y adopción

Los precios provienen de las páginas oficiales de precios. Las cifras de adopción son siempre **afirmaciones comerciales**.

| # | Producto | Modelo comercial publicado | Fortalezas / limitaciones / diferencial | Adopción (afirmación comercial) | Fuentes (consulta 2026-11-23) |
|---|---|---|---|---|---|
| 1 | DentalSoft | Gratis 3 meses (hasta 50 turnos por mes); Gestión $30.000 y Pro $60.000 por mes y por profesional; WhatsApp a USD 0,026 por mensaje (Meta) | **+** La mayor cantidad de funciones comprobadas en pantalla para el circuito argentino (señas, obras sociales, recuperación). **−** Agenda (solapamientos, bloqueos) solo como texto; automatización solo en Pro; facturación ARCA y centro de ayuda no evidenciados | 11 reseñas de Google (5,0) | [Planes](https://dentalsoft.com.ar/) |
| 2 | Oclusar | Empieza gratis, sin tarjeta; "precio en pesos" sin montos | **+** Única con prevención de solapamientos, ARCA y firma documentadas. **−** Recordatorios manuales; lista de espera no evidenciada; sillones solo como texto | No evidenciado | [Web](https://www.oclusar.com.ar/) |
| 3 | Bilog | Sin precios; configuración sin costo; sin permanencia | **+** Centro de ayuda completo (sobreturnos, bloqueos, caja, liquidaciones). **−** Recordatorios manuales pese a lo que dice la web; sin precios | +1.500 clínicas; +150.000 turnos por mes | [Web](https://bilog.com.ar/) |
| 4 | Dentalink | Cotización a pedido; pago mensual, semestral o anual; adicionales pagos | **+** Centro de ayuda y canal de YouTube extensos. **−** WhatsApp manual; la mayor parte de lo publicado es afirmación comercial o solo un título de artículo | +15.000 clínicas; +20 países | [Planes](https://www.softwaredentalink.com/ar/planes) |
| 5 | AgendaPro | ARS con IVA: $9.900 a $129.900 por mes; ficha clínica +$10.000; WhatsApp desde $7.900; API USD 200 | **+** La agenda más completa en pantalla (sillón, duración por práctica); campañas. **−** El solapamiento solo genera una alerta aceptable; ficha clínica como adicional; obras sociales no evidenciadas | +135.000 profesionales; +30.000 clínicas | [Planes](https://agendapro.com/ar/planes) |
| 6 | Atiendo | Prueba de 30 días; cobro por profesional en ARS; montos no legibles | **+** Manual público; lista de espera y roles documentados. **−** no emite factura electrónica (declarado por el proveedor); caja no evidenciada; WhatsApp "próximamente" | No evidenciado | [Web](https://atiendoapp.com/) |
| 7 | DentApp | 30 días gratis; $24.900, $54.900 y $74.900 por mes | **+** Precio de entrada bajo. **−** reserva por el paciente no evidenciada; todo como afirmación comercial | No evidenciado | [Web](https://www.dentapp.ar/) |
| 8 | Odonto.ar | 30 días gratis; USD 29, 49 y 99 por mes (aprox. ARS 47.995 a 163.845) | **+** Demo interactiva pública. **−** recordatorios e integraciones no evidenciados | No evidenciado | [Precios](https://odonto.ar/) |
| 9 | Doctoralia Pro | Versión gratuita; ARS 25.000, 35.000 y 55.000 por mes con facturación anual | **+** Captación por el directorio. **−** odontograma y administración no evidenciados | 110.000 profesionales en el directorio | [Precios](https://pro.doctoralia.com/ar/precio) |
| 10 | Agenda Profesional | No evidenciado ("no hay alta pública") | **+** Box, lista de espera y señas visibles en pantalla. **−** Sin precios; documentación no encontrada; prevención de solapamientos no evidenciada | No evidenciado | [Web](https://www.agenda-profesional.com/) |
| 11 | Reservo | Plan único según el tamaño, sin montos | **−** Casi todo es afirmación comercial | No evidenciado | [Precios](https://reservo.cl/precios/) |
| 12 | OdontoGRAMA | Cuenta gratis para probar; sin precios | **−** turnos digitales no evidenciados; casi sin evidencia | No evidenciado | [Web](https://odontograma.com.ar/) |
| 13 | Clinic Cloud | EUR más IVA: 29, 49 y 79 por mes (promoción de 50 % por 3 meses) | **+** Centro de ayuda. **−** WhatsApp manual; oferta para Argentina no evidenciada | +12.000 doctores | [Tarifas](https://clinic-cloud.com/tarifas) |
| 14 | Mi Agenda Dental | 30 días gratis; de $99 a $2.990 por mes (moneda no indicada; México) | **+** Pantallas de unidad (sillón), confirmaciones y auditoría. **−** reserva online no evidenciada; producto incipiente | "En uso real en 2 clínicas" | [Web](https://miagendadental.com/) |
| 15 | Odontosist | 14 días gratis; S/ 55, 99 y 350 por mes | **+** Guía pública; prevención de solapamientos documentada. **−** sillones y lista de espera no evidenciados | Busca "10 clínicas fundadoras" | [Web](https://odontosist.com/) |
| 16 | MedFarmaSoft Dental | Plan gratis permanente; EUR 20 y 42 por mes | **+** Clínica en pantalla (odontograma a presupuesto). **−** Agenda solo como texto | No evidenciado | [Dental](https://medfarmasoft.com/dental/) |
| 17 | Open Dental | USD 89 por mes y por sede fuera de EE. UU.; adicionales pagos | **+** Documentación y videos; lista ASAP. **−** Instalación local; en inglés; WhatsApp no evidenciado | No evidenciado | [Precios](https://www.opendental.com/site/fees.html) |

---

## B. Matriz de puntuación

### Nota metodológica

- **Cómo se calcula cada nota (0 a 5).** Cada criterio agrupa funcionalidades de la sección A. Cada funcionalidad aporta crédito según su nivel de evidencia:

  | Nivel de evidencia | Crédito |
  |---|---|
  | Comprobado (pantalla, centro de ayuda, documentación o video oficial) | 1 |
  | Comprobado parcial (solo una parte de la función, por ejemplo cancelación sin reprogramación o recordatorio manual) | 0,5 |
  | Afirmación comercial | 0,5 |
  | No evidenciado | 0 |

  La nota del criterio es 5 × (crédito obtenido ÷ cantidad de funcionalidades del criterio).
- **Funcionalidades por criterio:**
  - **Turnos y automatización (25 %):** reserva online, recordatorios, confirmación, cancelación o reprogramación, lista de espera, multiprofesional, sillón o box, duración por prestación, bloqueos, sobreturnos, prevención de solapamientos, y recuperación o campañas (12).
  - **Clínica (20 %):** historia clínica, odontograma, periodontograma, imágenes, y presupuestos o plan (5).
  - **Integraciones locales y WhatsApp (15 %):** WhatsApp, seña o Mercado Pago, facturación electrónica, Google Calendar, API pública y firma (6).
  - **Administración (15 %):** caja, seña o Mercado Pago, facturación electrónica y obras sociales (4).
  - **Experiencia del paciente (10 %):** reserva online, confirmación, cancelación o reprogramación, y lista de espera (4).
  - **Seguridad, exportación y trazabilidad (10 %):** roles, auditoría, exportación y seguridad o cumplimiento declarado (4).
  - **Precio y facilidad de adopción (5 %):** precio publicado, prueba o plan gratuito, y precio en pesos argentinos (3).
- **Solapamiento entre criterios.** Algunas funcionalidades cuentan en dos criterios porque pertenecen a ambos: Mercado Pago y ARCA son a la vez integración local y administración, y la reserva y la confirmación son a la vez turnos y experiencia del paciente.
- **Datos "No evidenciados".** Valen 0 (criterio conservador). Por eso la matriz mide **lo que cada proveedor demuestra públicamente**, no la calidad real del producto. Un sistema con poca documentación pública, como Dentalink (con muchos artículos vistos solo por el título), obtiene una nota menor que la que podría lograr en una demo.
- **Localización.** La facturación de otros países (CFDI, Verifactu) cuenta como parcial en los criterios locales.
- **Total** = Σ (nota × peso). Las notas se muestran con un decimal y el total con dos.

### Matriz

| Pos. | Producto | Turnos y autom. (25 %) | Clínica (20 %) | Integr. locales y WhatsApp (15 %) | Administración (15 %) | Exp. paciente (10 %) | Seguridad (10 %) | Precio y adopción (5 %) | **Total (0–5)** |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Oclusar | 2,5 | 4,5 | 3,3 | 5,0 | 1,9 | 3,8 | 1,7 | **3,42** |
| 2 | DentalSoft | 3,1 | 4,0 | 2,9 | 3,8 | 3,1 | 2,5 | 5,0 | **3,39** |
| 3 | AgendaPro | 3,8 | 3,5 | 3,3 | 2,5 | 2,5 | 1,2 | 5,0 | **3,14** |
| 4 | Atiendo | 3,3 | 3,5 | 2,1 | 2,5 | 4,4 | 3,8 | 1,7 | **3,12** |
| 5 | Bilog | 3,1 | 3,5 | 1,2 | 3,1 | 3,1 | 1,9 | 0,0 | **2,64** |
| 6 | Open Dental | 2,3 | 4,5 | 0,4 | 3,1 | 3,8 | 0,0 | 3,3 | **2,55** |
| 7 | Agenda Profesional | 2,1 | 3,0 | 1,2 | 3,8 | 3,8 | 1,2 | 0,0 | **2,37** |
| 8 | Mi Agenda Dental | 2,1 | 3,5 | 1,7 | 1,9 | 0,6 | 3,8 | 3,3 | **2,36** |
| 9 | Dentalink | 2,1 | 3,5 | 1,2 | 1,2 | 2,5 | 3,1 | 0,0 | **2,16** |
| 10 | Odontosist | 1,9 | 3,5 | 1,7 | 1,2 | 1,2 | 2,5 | 3,3 | **2,15** |
| 11 | MedFarmaSoft Dental | 1,7 | 3,5 | 1,2 | 0,6 | 2,5 | 1,9 | 3,3 | **2,00** |
| 12 | Clinic Cloud | 1,2 | 2,5 | 1,7 | 2,5 | 1,2 | 2,5 | 3,3 | **1,98** |
| 13 | Odonto.ar | 1,2 | 2,5 | 0,0 | 1,2 | 1,9 | 1,2 | 5,0 | **1,56** |
| 14 | DentApp | 0,4 | 2,0 | 1,7 | 1,9 | 0,0 | 1,9 | 5,0 | **1,47** |
| 15 | Doctoralia Pro | 2,5 | 0,5 | 0,4 | 0,0 | 3,1 | 0,6 | 5,0 | **1,41** |
| 16 | Reservo | 0,8 | 1,5 | 0,8 | 1,9 | 0,6 | 0,6 | 0,0 | **1,04** |
| 17 | OdontoGRAMA | 0,2 | 1,0 | 0,0 | 1,2 | 0,0 | 0,6 | 1,7 | **0,59** |

**Lectura crítica.**

- **Puntajes bajos en general.** Ningún sistema supera 3,5 porque buena parte de lo que se publica es texto comercial sin pantalla ni documentación.
- **Primeros puestos.** Oclusar lidera porque documenta en guías lo que afirma: ARCA, obras sociales, firma y prevención de solapamientos. DentalSoft queda segundo con muchas pantallas de producto, pero su agenda se apoya en texto.
- **En el criterio de turnos**, que es el que importa para denta-flow, el mejor es AgendaPro (3,8). Aun así, el centro de ayuda de AgendaPro muestra que un turno superpuesto **solo genera una alerta**.
- **Dentalink**, que se declara líder regional, queda noveno por falta de evidencia pública, no necesariamente de funciones. Por eso se incluye entre las demos prioritarias.

---

## C. Análisis competitivo

### C.1 Funcionalidades que ya son estándar de mercado

- **Agenda multiprofesional:** comprobada en 8 de 17 sistemas y mencionada en 15.
- **Reserva online:** 14 de 17 la mencionan y 11 la comprueban.
- **Historia clínica y odontograma:** 15 de 17 mencionan odontograma y 11 lo comprueban.
- **Recordatorios por algún canal:** 15 de 17 los mencionan, aunque el envío no siempre es automático (ver C.3).
- **Prueba gratuita sin tarjeta:** DentalSoft, Oclusar, Atiendo, DentApp, Odonto.ar, Mi Agenda Dental y Odontosist.

### C.2 Funcionalidades diferenciadoras reales (pocas y comprobadas)

- **Prevención de solapamientos comprobada:** solo **Oclusar** y **Odontosist** (ambos por documentación).
  - DentalSoft, Bilog, Mi Agenda Dental y MedFarmaSoft la afirman sin evidencia.
  - AgendaPro, según su centro de ayuda, solo muestra una alerta que se puede aceptar.
  - En Agenda Profesional no está evidenciada.
- **Sillón o box en pantalla:** AgendaPro ("Sillón 1"), Mi Agenda Dental ("multi-unidad") y Agenda Profesional (pestaña "Box"). **Ninguno de los tres comprueba además la prevención de solapamientos.**
- **Lista de espera:** comprobada solo en Atiendo (documentación), Agenda Profesional (pantalla) y Open Dental (documentación).
- **Recordatorio automático por WhatsApp:** comprobado solo en DentalSoft (plan Pro), AgendaPro, Mi Agenda Dental y Odontosist.
- **Seña con Mercado Pago:** comprobada en DentalSoft, Oclusar, AgendaPro, Atiendo y Agenda Profesional.
- **Facturación ARCA:** documentada solo en Oclusar; Bilog muestra una factura emitida vía Facturante.

### C.3 Vacíos de evidencia frecuentes en el mercado argentino

Son funciones que los proveedores no comprueban públicamente. Que no estén evidenciadas no significa que los sistemas no las tengan; lo que muestran es dónde un producto nuevo puede diferenciarse demostrándolas.

1. **El "automático" que es manual.** En Bilog, Dentalink, Oclusar y Clinic Cloud, la ayuda o la documentación oficial describen el envío de WhatsApp como manual. DentalSoft lo automatiza solo en su plan más caro y con costo por mensaje (USD 0,026, Meta).
2. **Protección de recursos físicos no comprobada.** Nadie comprueba la combinación profesional + sillón + duración por prestación con prevención de solapamientos.
3. **Gestión del ausentismo poco comprobada.** La recuperación o el recall solo se comprueba en DentalSoft, Mi Agenda Dental, AgendaPro (campañas) y Open Dental. Ninguno publica métricas de ausentismo.
4. **Cumplimiento declarado, no demostrado.** Solo Oclusar y Agenda Profesional mencionan la Ley 25.326, y solo como texto.
5. **Precios opacos.** 5 de los 8 argentinos (Oclusar, Bilog, Atiendo, Agenda Profesional y OdontoGRAMA) no muestran montos legibles; tampoco lo hacen Dentalink ni Reservo.
6. **Poca documentación pública.** DentalSoft, DentApp, Odonto.ar, Agenda Profesional, Reservo, OdontoGRAMA, Mi Agenda Dental y MedFarmaSoft: no se encontró centro de ayuda ni manual público.

### C.4 Oportunidades de innovación para denta-flow

- **Agenda "por recursos" con prevención real.** Ningún turno puede superponer el mismo profesional ni el mismo sillón; los sobreturnos solo los carga la recepción, de forma explícita. Resuelve el vacío 2, que ningún competidor comprueba.
- **Ciclo de confirmación cerrado y automático.** Recordatorio → confirmación de un toque → si el paciente cancela, el hueco se ofrece automáticamente a la lista de espera. Hoy esas piezas están dispersas en productos distintos.
- **Ausentismo como métrica central:** tasa por profesional, franja horaria y paciente (vacío 3).
- **Documentación pública desde el día uno.** Un manual o centro de ayuda breve que demuestre cada función (vacío 6), con precio publicado en pesos y una política de datos clara (Ley 25.326).
- **WhatsApp sin costo oculto en el MVP:** enlace prellenado (wa.me) más email automático. La API oficial queda para cuando el modelo de costos esté validado.

---

## D. Recomendación final

### D.1 Cinco competidores prioritarios para analizar mediante demo

| # | Producto | Qué validar en la demo |
|---|---|---|
| 1 | **Oclusar** | Si la prevención de doble turno incluye el sillón; cómo se ve la gestión de sillones; flujo de seña y aprobación del turno |
| 2 | **DentalSoft** | La detección de solapamientos y los bloqueos (hoy solo como texto); confirmación automática en el plan Pro; costo real de WhatsApp |
| 3 | **AgendaPro** | Agenda por sillón y práctica; comportamiento ante solapamientos (alerta frente a bloqueo); costo total con adicionales |
| 4 | **Atiendo** | Lista de espera y aviso de turno libre; sala de espera; separación de roles |
| 5 | **Dentalink** | Lo que su centro de ayuda solo mostró por título: Box/Sillón, Google Calendar, facturación y obras sociales para Argentina; si el WhatsApp sigue siendo manual |

Si alguna demo no se consigue, el reemplazo es **Bilog** (centro de ayuda completo, sobreturnos y liquidaciones).

### D.2 Tres productos de referencia para la experiencia de usuario

1. **AgendaPro:** agenda por práctica y sillón con duraciones visibles, y experiencia multicanal del paciente.
2. **Atiendo:** lista de espera, sala de espera (llegó, en atención, faltó) y un manual público claro.
3. **Oclusar:** lenguaje simple para el odontólogo y guías que explican cada función.

### D.3 MVP sugerido

**Segmento:** consultorio chico (1 a 5 profesionales, una sede) en Argentina.
**Alcance:** solo turnos y agenda.

#### Imprescindible

1. Alta y configuración de **profesionales**, **sillones o boxes** y **prestaciones con duración propia**.
2. Horarios de atención por profesional, con **bloqueos** (vacaciones, feriados o ausencias).
3. Agenda diaria y semanal por profesional y por sillón, con **prevención real de solapamientos en ambos recursos** (no una simple alerta).
4. **Reserva online 24/7** por link público, sin registro obligatorio (DNI y teléfono), mostrando solo los huecos reales según la duración de la prestación.
5. **Cancelación y reprogramación por el paciente** desde su enlace, con anticipación mínima configurable.
6. **Recordatorio automático** por email y **confirmación de un toque**; la agenda refleja el estado (pendiente, confirmado o cancelado).
7. Carga manual de turnos por la recepción, con sobreturno explícito y registrado.
8. Estados de asistencia (asistió, ausente, ausente con aviso).
9. Roles: administrador, recepción y profesional; registro de cambios (quién y cuándo).
10. Aviso de privacidad y tratamiento de datos personales conforme a la Ley 25.326.

#### Diferenciador

1. **Lista de espera automática:** cuando se libera un turno, se ofrece a los pacientes en espera compatibles con la prestación y el profesional.
2. **Panel de ausentismo:** tasa por profesional, franja horaria y paciente, más pacientes con ausencias reiteradas.
3. **Mensajes de WhatsApp prellenados** (wa.me) para recordar o confirmar sin costo por mensaje, como complemento del email.
4. **Reintento de confirmación:** si el paciente no confirma en X horas, segundo aviso y alerta a la recepción.
5. **Manual público breve** que demuestre cada función, frente a la escasa documentación pública encontrada en el mercado.

#### Etapas posteriores

| Funcionalidad | Justificación basada en la investigación |
|---|---|
| Ficha clínica, anamnesis y evolución | Es estándar (C.1) y no diferencia; exige cumplir la Ley 26.529 (integridad, inalterabilidad, firma), como documenta Oclusar |
| Odontograma y periodontograma | 15 de 17 lo mencionan y 11 lo comprueban; depende de la ficha clínica |
| Imágenes y radiografías | Almacenamiento costoso (DentalSoft lo limita por plan) y depende de la ficha clínica |
| Presupuestos y planes de tratamiento | Dependen del odontograma y de la ficha clínica |
| Caja, cobros y señas con Mercado Pago | Comprobado en 5 sistemas; agrega integración de pagos y conciliación. Conviene validar primero el flujo de turnos |
| Obras sociales, prepagas y liquidaciones | Alta complejidad; solo DentalSoft, Oclusar, Bilog, Atiendo y Agenda Profesional lo comprueban |
| Facturación electrónica ARCA | Solo Oclusar la documenta; requiere certificados y homologación |
| WhatsApp Business API oficial y chatbot con IA | Costo por mensaje (Meta); validar primero la demanda con enlaces prellenados |
| Multisede | Fuera del segmento elegido |
| Campañas de marketing y recuperación | Dependen del historial y del consentimiento para comunicaciones comerciales |
| App móvil nativa | Una web adaptable al celular cubre el uso inicial |

---

## Verificación de fuentes

*Plantilla a completar por el equipo antes de generar el PDF. Una fila por cada fuente comprobada (mínimo 5).*

| Fuente (URL) | Qué afirma el informe | Qué encontramos | Qué corregimos |
|---|---|---|---|
| | | | |
| | | | |
| | | | |
| | | | |
| | | | |

---

## Referencias

Todas las fuentes fueron consultadas el **2026-11-23**.

**DentalSoft**
1. Página oficial y planes. https://dentalsoft.com.ar/
2. Funcionalidades. https://www.dentalsoft.com.ar/funcionalidades
3. Agenda odontológica. https://www.dentalsoft.com.ar/funcionalidades/agenda-odontologica
4. Turnos online. https://www.dentalsoft.com.ar/funcionalidades/turnos-online
5. Recordatorios por WhatsApp. https://www.dentalsoft.com.ar/funcionalidades/recordatorios-whatsapp
6. Recuperación de pacientes. https://www.dentalsoft.com.ar/funcionalidades/recuperacion-de-pacientes
7. Historia clínica y odontograma. https://www.dentalsoft.com.ar/funcionalidades/historia-clinica-odontograma
8. Presupuestos. https://www.dentalsoft.com.ar/funcionalidades/presupuestos
9. Caja y cobros. https://www.dentalsoft.com.ar/funcionalidades/caja-cobros
10. Cobros con Mercado Pago. https://www.dentalsoft.com.ar/funcionalidades/cobros-mercadopago
11. Obras sociales. https://www.dentalsoft.com.ar/funcionalidades/obras-sociales
12. Documentación clínica. https://www.dentalsoft.com.ar/funcionalidades/documentacion-clinica

**Oclusar**
13. Página oficial. https://www.oclusar.com.ar/
14. Guías. https://www.oclusar.com.ar/guias
15. Guía: agenda y recordatorios por WhatsApp. https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp
16. Guía: facturación electrónica. https://www.oclusar.com.ar/guias/facturacion-electronica-para-odontologos
17. Guía: odontograma digital. https://www.oclusar.com.ar/guias/odontograma-digital
18. Guía: presupuestos. https://www.oclusar.com.ar/guias/presupuestos-que-el-paciente-acepta
19. Guía: obras sociales. https://www.oclusar.com.ar/guias/obras-sociales-sin-volverse-loco
20. Guía: consentimiento informado. https://www.oclusar.com.ar/guias/consentimiento-informado-odontologia
21. Guía: historia clínica digital y Ley 26.529. https://www.oclusar.com.ar/guias/historia-clinica-digital-ley-26529

**Bilog**
22. Página oficial. https://bilog.com.ar/
23. llms.txt. https://bilog.com.ar/llms.txt
24. Factura electrónica. https://bilog.com.ar/electronic-invoice/
25. Centro de documentación. https://docs.bilog.com.ar/
26. Reserva online. https://docs.bilog.com.ar/schedule/online-appointment
27. Recordatorios. https://docs.bilog.com.ar/schedule/reminders
28. Turnos. https://docs.bilog.com.ar/schedule/appointments
29. Historia clínica. https://docs.bilog.com.ar/patients/medical-history
30. Presupuestos. https://docs.bilog.com.ar/patients/budgets
31. Pagos. https://docs.bilog.com.ar/patients/payments
32. Caja diaria. https://docs.bilog.com.ar/administration/box-daily
33. Liquidaciones de obras sociales. https://docs.bilog.com.ar/liquidations/liquidations-coverage-medical

**Dentalink**
34. Página para Argentina. https://www.softwaredentalink.com/ar
35. Planes para Argentina. https://www.softwaredentalink.com/ar/planes
36. Planes (sitio general). https://web-es.softwaredentalink.com/en/planes
37. Centro de ayuda. https://ayuda.softwaredentalink.com/es
38. Colección "Conoce todos los módulos". https://ayuda.softwaredentalink.com/es/collections/9620066-conoce-todos-los-modulos-de-dentalink
39. Colección Agenda. https://ayuda.softwaredentalink.com/es/collections/9633575-agenda
40. Módulo exclusivo para Argentina. https://ayuda.softwaredentalink.com/es/collections/19196578-modulo-exclusivo-para-argentina
41. Colección Facturación electrónica. https://ayuda.softwaredentalink.com/es/collections/9620069-facturacion-electronica
42. Cómo notificar cita por WhatsApp. https://intercom.help/softwaredentalink/es/articles/9493137-como-notificar-cita-por-whatsapp
43. Termina de configurar tu agenda. https://intercom.help/softwaredentalink/es/articles/9493114-termina-de-configurar-tu-agenda
44. Cómo usar el odontograma. https://intercom.help/softwaredentalink/es/articles/9493218-como-usar-el-odontograma
45. Canal oficial de YouTube. https://www.youtube.com/c/softwaredentalink

**AgendaPro**
46. Software odontológico para Argentina. https://agendapro.com/ar/dental/software-odontologico
47. Planes para Argentina. https://agendapro.com/ar/planes
48. Reservas online. https://agendapro.com/ar/reservas-online
49. Centro de ayuda. https://ayuda.agendapro.com/es/
50. Colección Agenda, reservas y bloqueos. https://ayuda.agendapro.com/es/collections/969562-agenda-reservas-y-bloqueos
51. Cómo agendar un sobrecupo. https://ayuda.agendapro.com/es/articles/6500344-como-agendar-un-sobrecupo
52. Bloqueos de horario. https://ayuda.agendapro.com/es/articles/5837664-como-programar-y-gestionar-bloqueos-de-horario-de-mis-profesionales

**Atiendo**
53. Página oficial y precios. https://atiendoapp.com/
54. Manual de uso. https://atiendoapp.com/web/ayuda/

**DentApp**
55. Página oficial y planes. https://www.dentapp.ar/
56. Términos. https://www.dentapp.ar/terms

**Odonto.ar**
57. Página oficial y precios. https://odonto.ar/
58. Demo interactiva. https://odonto.ar/demo

**Doctoralia Pro**
59. Doctoralia Pro Argentina. https://pro.doctoralia.com/ar
60. Precios. https://pro.doctoralia.com/ar/precio
61. Agenda para especialistas. https://pro.doctoralia.com/ar/agenda-doctoralia-para-especialistas
62. Guías. https://pro.doctoralia.com/ar/guias
63. Guía de configuración de la reserva. https://pro.doctoralia.com/ar/guias/configuracion-de-la-reserva-de-cita
64. Preguntas frecuentes. https://pro.doctoralia.com/ar/preguntas-frecuentes
65. Preguntas frecuentes: seguridad de datos. https://pro.doctoralia.com/ar/preguntas-frecuentes/seguridad-datos
66. Preguntas frecuentes: sincronización. https://pro.doctoralia.com/ar/preguntas-frecuentes/sincronizacion

**Agenda Profesional**
67. Página oficial. https://www.agenda-profesional.com/
68. Privacidad. https://www.agenda-profesional.com/privacidad
69. Condiciones. https://www.agenda-profesional.com/condiciones

**Reservo**
70. Página dental. https://reservo.cl/homepage/dental/
71. Precios. https://reservo.cl/precios/
72. Centro dental. https://reservo.cl/salud/centro-dental/
73. Página para Argentina. https://reservo.com.ar/

**OdontoGRAMA**
74. Página oficial. https://odontograma.com.ar/

**Clinic Cloud**
75. Página oficial. https://clinic-cloud.com/
76. Tarifas. https://clinic-cloud.com/tarifas
77. Centro de ayuda. https://clinic-cloud.com/faq
78. Agenda y comunicaciones. https://clinic-cloud.com/faq/agenda-y-comunicaciones
79. Recordatorio de citas por WhatsApp. https://clinic-cloud.com/faq/configuracion-centro/recodatorio-de-citas-via-whatsapp
80. Configuración de usuarios. https://clinic-cloud.com/faq/configuracion-centro/configuracion-de-usuarios

**Mi Agenda Dental**
81. Página oficial. https://miagendadental.com/
82. Blog. https://miagendadental.com/blog/

**Odontosist**
83. Página oficial. https://odontosist.com/
84. Guía de uso. https://odontosist.com/guia-usuario.html

**MedFarmaSoft**
85. Programa para clínica dental. https://medfarmasoft.com/dental/
86. Odontograma. https://medfarmasoft.es/odontograma/
87. Recordatorios por WhatsApp. https://medfarmasoft.es/recordatorios-whatsapp/
88. Ausencias. https://medfarmasoft.es/ausencias/

**Open Dental**
89. Página oficial. https://www.opendental.com/
90. Order / pricing. https://www.opendental.com/site/order.html
91. Fees. https://www.opendental.com/site/fees.html
92. Documentación. https://www.opendental.com/site/documentation.html
93. Web Sched. https://www.opendental.com/site/websched.html
94. eReminders. https://www.opendental.com/site/ereminders.html
95. Lista ASAP. https://www.opendental.com/site/0_asaplist.html
96. Canal oficial de YouTube. https://www.youtube.com/channel/UC2f1mkxYSud-V72gxpgmRrQ
97. Videos: Appointments Module. https://www.youtube.com/playlist?list=PLyjZ9PCVynCmlEORfQVD5o1a1lC78tlMz
98. Videos: Chart Module. https://www.youtube.com/playlist?list=PLyjZ9PCVynCkrtlgrAR0MSazd7Ez9-Z0R
99. Videos: Treatment Plan Module. https://www.youtube.com/playlist?list=PLyjZ9PCVynClmOo-iRmtFC7cOuq7Je5Io
100. Videos: Account Module. https://www.youtube.com/playlist?list=PLyjZ9PCVynCnYKR2Y5ngkYf-rLqjw9j5F
101. Videos: Insurance. https://www.youtube.com/playlist?list=PLyjZ9PCVynCmo7F4AApt-eOkyqV5iuDlO
102. Video: A Quick Look at Web Sched Recall. https://youtu.be/yZvlozVRdN8
103. Video: Appointment views. https://www.youtube.com/watch?v=WQUMsj0ihb0
104. Video: Chart Module Main Window. https://www.youtube.com/watch?v=H1PpDOY8zOg
105. Video: Perio Chart. https://www.youtube.com/watch?v=ApO1MkPkq48
106. Video: Treatment Plan Basics. https://www.youtube.com/watch?v=apYXaA3_JVU
107. Video: Patient Payments. https://www.youtube.com/watch?v=x3CKA_n1qEs
108. Video: Adding Insurance. https://www.youtube.com/watch?v=7wRVh0A2uP0
