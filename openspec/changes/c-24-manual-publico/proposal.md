# Proposal — C-24 `manual-publico`

## Why

DentaFlow atiende a múltiples actores con necesidades operativas diversas: pacientes que gestionan sus turnos de forma autónoma sin necesidad de registro previo (C-16, C-17), personal de recepción que coordina la agenda diaria y semanal, carga turnos y sobreturnos y envía recordatorios por WhatsApp (C-11, C-12, C-13, C-21), odontólogos que consultan su agenda del día y registran asistencia (C-11, C-13), y administradores que configuran recursos, horarios, usuarios, auditoría y parámetros (C-04, C-05, C-06, C-07, C-08).

Para que cualquier usuario aprenda a utilizar cada función del sistema sin requerir capacitación presencial ni asistencia técnica, la historia **US-034** define un manual público breve que describa visualmente los flujos con capturas o pasos guiados (`knowledge-base/06_funcionalidades.md` §US-034, `CHANGES.md` §[C-24]). Dado que es una funcionalidad de difusión y autoayuda, debe ubicarse en una ruta pública (`/manual`) accesible sin autenticación (`knowledge-base/03_actores_y_roles.md` §Rutas públicas), manteniendo un formato ágil, con datos ficticios para salvaguardar la privacidad (Ley 25.326) y garantizando compatibilidad móvil y accesibilidad (WCAG 2.1 AA).

## What Changes

- **Nueva ruta pública `/manual`**:
  - Implementada íntegramente en el frontend como una página estática accesible sin token ni sesión (`frontend/src/routes/rutas.tsx`).
  - No requiere nuevos endpoints en la API, no altera el modelo de base de datos ni requiere migraciones Flyway.
- **Estructura organizada por actor y caso de uso**:
  - **Paciente**:
    - Reserva de turnos online paso a paso sin registro (selección de prestación, profesional optativo, selección de franja horaria, ingreso de datos de contacto y consentimiento informado).
    - Confirmación de un toque desde el correo de recordatorio.
    - Cancelación y reprogramación mediante enlace con token seguro dentro de la ventana de anticipación mínima permitida.
    - Información sobre el aviso de privacidad y derechos bajo la Ley 25.326.
  - **Recepción**:
    - Uso de la agenda interna en vistas diaria y semanal con filtros interactivos por profesional y sillón.
    - Carga manual de turnos presenciales o telefónicos con validación estricta de solapamientos.
    - Carga de sobreturnos explícitos debidamente identificados y auditados.
    - Registro de estados de asistencia (`asistio`, `ausente`, `ausente_con_aviso`).
    - Envío manual de mensajes prellenados de WhatsApp (`wa.me`) para recordatorio o confirmación.
  - **Profesional**:
    - Visualización de "mi agenda del día" (aislada estrictamente a los turnos asignados).
    - Marcación del estado de asistencia de sus pacientes.
  - **Administrador**:
    - Gestión de recursos del consultorio (alta, edición y desactivación de profesionales, sillones y prestaciones con duración).
    - Definición de horarios semanales y bloqueos por profesional, sillón o consultorio completo.
    - Gestión de usuarios y asignación de roles RBAC (`administrador`, `recepcion`, `profesional`).
    - Consulta del historial inmutable de auditoría de cada turno.
    - Parámetros operativos del consultorio (anticipación mínima en horas, textos del aviso de privacidad y plantilla de WhatsApp).
- **Puntos de entrada y navegación accesible**:
  - Enlace directo desde la portada pública (`/`, `InicioPage.tsx`) accesible para pacientes y visitantes generales.
  - Enlace visible en la barra de navegación del panel interno (`BarraDelPanel.tsx`) para todos los roles autenticados (`administrador`, `recepcion`, `profesional`).
  - Tabla de contenidos interna con enlaces ancla (`#paciente`, `#recepcion`, `#profesional`, `#administrador`) para salto rápido de sección.
- **Capturas de pantalla y privacidad (Ley 25.326)**:
  - Capturas de pantalla e ilustraciones basadas en datos de demostración rigurosamente ficticios (ej. "Juan Pérez", DNI 20.000.001, teléfonos ficticios, sin datos médicos ni personales reales).
  - Cada captura va acompañada de pasos ordenados en texto legible por lectores de pantalla (soporte accesible que no depende exclusivamente de la imagen).
- **Diseño Mobile-First y WCAG 2.1 AA**:
  - Interfaz responsiva probada y optimizada en ancho de 390 px (viewport móvil estándar) sin desbordamiento horizontal.
  - Tipografía clara, contrastes superiores a 4.5:1, etiquetas alternativas (`alt`) descriptivas y navegación íntegramente operable mediante teclado.

Fuera de alcance: gestor dinámico de contenidos (CMS), videos embebidos de alta densidad, edición en tiempo real del manual desde el panel y endpoints backend adicionales.

Referencias: `CHANGES.md` §C-24. Historia: US-034. Reglas: RN-PR-03, RN-AU-02. Actores y Rutas públicas: `knowledge-base/03_actores_y_roles.md`.

## Capabilities

### New Capabilities
- `manual-publico`: pantalla pública accesible `/manual` con guía paso a paso y capturas ilustrativas organizadas por actor (paciente, recepción, profesional, administrador), respetando la Ley 25.326, enfoque mobile-first (390 px) y pautas de accesibilidad WCAG 2.1 AA.

### Modified Capabilities
- `autorizacion-por-rol`: el menú de navegación del panel interno (`BarraDelPanel`) incorpora el enlace a la guía "Manual" accesible para los tres roles internos (`administrador`, `recepcion`, `profesional`).

## Impact

- **Frontend**:
  - `frontend/src/features/manual/`: creación de `ManualPage.tsx` y subcomponentes modulares de sección (`ManualPaciente.tsx`, `ManualRecepcion.tsx`, `ManualProfesional.tsx`, `ManualAdministrador.tsx`).
  - `frontend/src/routes/rutas.tsx`: declaración de la ruta pública `/manual`.
  - `frontend/src/routes/InicioPage.tsx`: incorporación del enlace hacia `/manual`.
  - `frontend/src/features/auth/BarraDelPanel.tsx`: incorporación de la sección "Manual" en la navegación del panel.
  - Activos estáticos en `frontend/public/manual/` con capturas optimizadas y datos anonimizados.
- **Backend**:
  - Ningún cambio en backend, API, modelos, seguridad ni Flyway. El contenido se entrega íntegramente de manera estática desde el frontend.
- **Dependencias**:
  - Depende formalmente de `C-19` (`CHANGES.md`), dado que las capturas definitivas de pantalla de la aplicación completa se toman una vez estabilizado y desplegado el MVP.
