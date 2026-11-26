# Design — C-24 `manual-publico`

## Context

DentaFlow dispone de un conjunto amplio de funcionalidades orientadas a la autogestión de pacientes y a la operación interna del consultorio:
- **Reserva y gestión pública (C-16, C-17)**: el paciente agenda sin usuario ni contraseña, confirma su turno con un toque desde su email y cancela o reprograma mediante enlaces con tokens firmados.
- **Operación interna (C-11, C-12, C-13, C-21)**: recepción y profesionales gestionan la agenda en vistas diaria y semanal, cargan turnos y sobreturnos, registran asistencia y despachan mensajes por WhatsApp (`wa.me`).
- **Administración y configuración (C-04, C-05, C-06, C-07, C-08)**: configuración de personal, sillones, prestaciones, horarios, bloqueos, usuarios RBAC, historial de auditoría y parámetros de anticipación y privacidad.

La historia **US-034** y `CHANGES.md` establecen la necesidad de un manual público breve que muestre de forma directa cómo operar cada parte del sistema. Esta guía debe ser abierta a la comunidad sin barreras de autenticación (`knowledge-base/03_actores_y_roles.md`), ágil para consultar desde celulares (390 px), accesible para usuarios con discapacidades (WCAG 2.1 AA) y respetuosa de la legislación de protección de datos personales (Ley 25.326), asegurando que todas las capturas o ejemplos utilicen identidades y datos estrictamente ficticios.

## Goals / Non-Goals

**Goals**
- Ofrecer una página pública en la ruta `/manual` con explicaciones claras, ordenadas y visuales de cada módulo del sistema.
- Estructurar el contenido por perfil de actor: Paciente, Recepción, Profesional y Administrador.
- Incorporar accesos directos al manual tanto para pacientes desde la portada pública (`/`) como para el personal interno desde la barra superior del panel (`BarraDelPanel`).
- Proveer capturas ilustrativas y pasos numerados redactados con datos 100% ficticios conforme a la Ley 25.326.
- Cumplir las pautas de accesibilidad WCAG 2.1 nivel AA: navegación completa por teclado, etiquetas descriptivas para imágenes, ratios de contraste superiores a 4.5:1 y semántica estructurada (`<main>`, `<nav>`, `<article>`, `<section>`, `<h1>` a `<h3>`).
- Asegurar una experiencia de lectura fluida en dispositivos móviles, validada en pantallas angostas de 390 px sin scroll horizontal.

**Non-Goals**
- No se implementa un gestor dinámico de contenidos (CMS) ni almacenamiento en base de datos para los textos del manual; el contenido reside como componentes estáticos en el frontend.
- No se exponen nuevos endpoints en el backend ni se agregan tablas o migraciones Flyway.
- No se incorporan videos pesados ni visores externos de PDF que incrementen los tiempos de carga en conexiones móviles.
- No se reemplaza la documentación técnica de arquitectura ni los diagramas de ingeniería (los cuales residen en `knowledge-base/` y `docs/`).

## Decisions

### D1. Ruta pública estática en React Router

La pantalla se monta en la ruta `/manual` dentro del árbol de rutas de la SPA (`frontend/src/routes/rutas.tsx`), sin envolverse en `RutaProtegida`.
- Permite acceso libre a pacientes, personal del consultorio y evaluadores sin necesidad de credenciales ni sesión activa.
- Como toda la lógica y contenido son estáticos, el rendimiento es instantáneo y no genera tráfico hacia la API.
- Al no requerir autenticación, cumple con la matriz de rutas públicas de `03_actores_y_roles.md`.

### D2. Arquitectura de componentes por actor

Para mantener el código mantenible y desacoplado, la funcionalidad se organiza en `frontend/src/features/manual/`:
- `ManualPage.tsx`: componente contenedor que incluye el título principal, una introducción breve sobre DentaFlow, una tabla de contenidos / navegación rápida por anclas y el renderizado secuencial de las secciones.
- `ManualPaciente.tsx` (`#paciente`):
  - Paso 1: Reserva online (elección de prestación, profesional optativo, selección de fecha y hora, carga de datos y aceptación de privacidad).
  - Paso 2: Confirmación de turno desde el email de recordatorio con un solo clic.
  - Paso 3: Cancelación o reprogramación mediante enlace seguro respetando la ventana de anticipación mínima.
- `ManualRecepcion.tsx` (`#recepcion`):
  - Navegación de agenda: vistas diaria y semanal, filtrado por profesional y sillón.
  - Carga manual de turnos y validación de disponibilidad anti-solapamiento.
  - Carga de sobreturnos con justificación y registro auditado.
  - Marcación de estados de asistencia (`asistio`, `ausente`, `ausente_con_aviso`).
  - Envío de recordatorio vía WhatsApp web/móvil mediante enlace `wa.me` prellenado.
- `ManualProfesional.tsx` (`#profesional`):
  - Acceso a la vista "Mi agenda del día" con los turnos del profesional autenticado.
  - Registro de asistencia de sus propios pacientes.
- `ManualAdministrador.tsx` (`#administrador`):
  - Catálogo de profesionales, sillones y prestaciones con sus duraciones en minutos.
  - Horarios de atención y registro de bloqueos temporales o extraordinarios.
  - Gestión de usuarios y asignación de roles.
  - Consulta de auditoría de turnos e historial de modificaciones.
  - Configuración de parámetros generales (anticipación mínima, aviso de privacidad, plantilla de mensajes).

### D3. Puntos de acceso y navegación contextual

1. **Desde la portada pública (`InicioPage.tsx`)**:
   - Se añade un enlace accesible "Manual de uso" en la sección de navegación/acciones públicas, permitiendo a los pacientes consultar las instrucciones antes o después de reservar.
2. **Desde la barra del panel (`BarraDelPanel.tsx`)**:
   - Se incorpora `{ ruta: '/manual', nombre: 'Manual' }` al listado de secciones del panel.
   - Es visible para todos los roles autenticados (`administrador`, `recepcion`, `profesional`), permitiendo al personal recurrir al manual en cualquier momento de su jornada.
3. **Navegación interna en la página del manual**:
   - Menú superior de navegación interna (`<nav aria-label="Índice del manual">`) con enlaces ancla hacia `#paciente`, `#recepcion`, `#profesional` y `#administrador`.
   - Cada sección incluye un enlace de retorno "Volver al índice" (`#inicio`) para facilitar la navegación rápida en teléfonos móviles.

### D4. Tratamiento de capturas y privacidad (Ley 25.326)

- Las imágenes se ubican en `frontend/public/manual/` con nombres descriptivos (ej. `reserva-paso-1.webp`, `agenda-semanal.webp`).
- **Garantía de privacidad estricta**:
  - En ningún caso se utilizan capturas con pacientes reales, historias clínicas, datos de contacto auténticos ni credenciales operativas.
  - Toda la información visual proviene del seed de desarrollo o datos simulados normalizados:
    - Pacientes de fantasía (ej.: "Ana Gómez", "Juan Pérez", "María Rodríguez").
    - DNI ficticios de prueba (ej.: 20.000.001, 30.000.002) y teléfonos ficticios (ej.: 011 15 5555-0001).
    - Profesionales y sillones ficticios ("Dra. Valeria Blanco", "Dr. Martín Silva", "Sillón 1").
- **Doble soporte textual**:
  - Las imágenes son complementarias: cada flujo se explica exhaustivamente con pasos enumerados y listas descriptivas en HTML. Si una imagen no carga o el usuario utiliza un lector de pantalla, el flujo completo puede comprenderse sin pérdida de información.
  - Las imágenes cuentan con atributo `alt` detallado y descriptivo.

### D5. Mobile-first (390 px) y WCAG 2.1 AA

- **Dimensionamiento**:
  - Contenedor con `max-w-4xl mx-auto px-4 py-8` y `w-full`.
  - Imágenes fluidas con clases `max-w-full h-auto rounded-lg border border-slate-200 shadow-sm`.
  - Ausencia de tablas rígidas de ancho fijo que provoquen scroll lateral en anchos de 390 px.
- **Accesibilidad**:
  - Estructura jerárquica con un único `<h1>`, `<h2>` por cada actor/sección principal y `<h3>` por cada funcionalidad o flujo.
  - Enfoque de foco visible (`focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:ring-offset-2`) en todos los botones y enlaces interactivos.
  - Contraste de colores verificado: texto en `slate-800`/`slate-900` sobre fondos claros (`white`, `slate-50`), enlaces en `sky-800` (ratio superior a 4.5:1).

## Risks / Trade-offs

- **Desactualización visual ante cambios de interfaz**: Si la interfaz de usuario evoluciona, las capturas estáticas podrían diferir sutilmente. **Mitigación**: la redacción de los pasos es conceptual y explicativa, enfocada en los objetivos de negocio y en la nomenclatura de los botones principales, de modo que el manual mantenga su vigencia aun ante retoques cosméticos menores.
- **Tamaño del bundle de assets**: Imágenes sin optimizar pueden ralentizar la carga en redes móviles lentas. **Mitigación**: compresión en formato WebP con resolución adaptada y atributos `loading="lazy"`.
- **Dependencia de C-19**: La toma de capturas definitivas requiere que los flujos de C-16, C-17 y C-19 estén completamente estables. **Mitigación**: se deja la arquitectura, especificaciones, componentes y tests listos; las imágenes definitivas se generan e incorporan una vez cerrado el MVP.
