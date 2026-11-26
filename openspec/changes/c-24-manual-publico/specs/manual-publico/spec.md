## Purpose

Ofrecer un manual público breve y visual en la ruta `/manual` que permita a cualquier usuario (pacientes, personal administrativo, profesionales y administradores) aprender a operar cada funcionalidad del sistema de forma autónoma, sin barreras de autenticación, con datos estrictamente ficticios (Ley 25.326) y garantizando compatibilidad mobile-first (390 px) y accesibilidad WCAG 2.1 AA (US-034, CHANGES.md §C-24).

## ADDED Requirements

### Requirement: Ruta pública del manual
El sistema SHALL exponer una página de manual en la ruta `/manual` del frontend, accesible de manera libre sin requerir inicio de sesión, credenciales ni tokens de autorización (RN-AU-02).

#### Scenario: Acceso libre sin autenticación
- **WHEN** un usuario no autenticado navega a `/manual`
- **THEN** el sistema presenta el manual de uso completo sin redirigir al login ni exigir credenciales

#### Scenario: Acceso desde dispositivo móvil
- **WHEN** un usuario accede a `/manual` desde un navegador móvil con resolución de 390 px de ancho
- **THEN** el contenido se visualiza adaptado fluidamente sin provocar desplazamiento horizontal en la ventana

### Requirement: Estructura por actor del sistema
El manual SHALL organizar sus contenidos en secciones modulares claramente diferenciadas para los cuatro actores del sistema: paciente, recepción, profesional y administrador. Cada sección debe describir sus flujos principales mediante pasos ordenados en texto y capturas ilustrativas.

#### Scenario: Sección del paciente
- **WHEN** se visualiza la sección orientada al paciente
- **THEN** se detallan el flujo de reserva online paso a paso, la confirmación de un toque desde el correo de recordatorio, la cancelación o reprogramación con token dentro de la anticipación mínima y el aviso de consentimiento de datos (Ley 25.326)

#### Scenario: Sección de recepción
- **WHEN** se visualiza la sección orientada a recepción
- **THEN** se detallan la visualización de la agenda (vistas diaria y semanal con filtros), la carga manual de turnos sin solapamientos, la carga de sobreturnos explícitos, el registro de asistencia (`asistio`, `ausente`, `ausente_con_aviso`) y el recordatorio de turnos mediante enlace prellenado de WhatsApp (`wa.me`)

#### Scenario: Sección del profesional
- **WHEN** se visualiza la sección orientada al profesional
- **THEN** se detallan la consulta de la agenda diaria personal del profesional y la marcación de asistencia de sus pacientes atendidos

#### Scenario: Sección del administrador
- **WHEN** se visualiza la sección orientada al administrador
- **THEN** se detallan la administración de recursos (profesionales, sillones y prestaciones con duración), configuración de horarios y bloqueos, gestión de usuarios con roles RBAC, consulta del historial inmutable de auditoría y configuración de parámetros operativos

### Requirement: Enlaces de acceso al manual
La aplicación SHALL incluir accesos directos visibles hacia la ruta `/manual` tanto en la portada pública para pacientes como en la barra de navegación del panel interno para el personal autenticado.

#### Scenario: Enlace desde la portada pública
- **WHEN** un visitante accede a la página de inicio (`/`)
- **THEN** visualiza un enlace accesible hacia el manual de uso que navega a `/manual`

#### Scenario: Enlace desde el panel interno
- **WHEN** un usuario con cualquier rol interno (`administrador`, `recepcion`, `profesional`) navega por el panel
- **THEN** la barra de navegación superior muestra la opción "Manual" con enlace directo a `/manual`

### Requirement: Privacidad y datos ficticios en capturas y ejemplos
Todo el contenido gráfico y textual del manual MUST utilizar datos estrictamente ficticios y anonimizados, sin revelar nombres reales, contactos verdaderos, historias clínicas ni datos sensibles de pacientes o personal (Ley 25.326 y RN-PR-03).

#### Scenario: Ejemplos de pacientes y turnos ficticios
- **WHEN** se examinan las capturas y descripciones de flujos en el manual
- **THEN** todos los pacientes exhiben nombres y documentos simulados (ej. Juan Pérez, DNI 20.000.001) y no existen referencias a datos clínicos ni personales auténticos

### Requirement: Accesibilidad y navegación responsive
La página del manual MUST cumplir las pautas de accesibilidad WCAG 2.1 nivel AA: jerarquía semántica de encabezados (`<h1>` a `<h3>`), contraste mínimo de 4.5:1, etiquetas `alt` descriptivas en todas las capturas, índice de navegación rápida con anclas de salto accesibles y operatividad íntegra mediante teclado.

#### Scenario: Navegación completa por teclado
- **WHEN** un usuario navega el manual exclusivamente mediante las teclas Tab, Shift+Tab y Enter
- **THEN** el foco visible recorre ordenadamente los enlaces del índice, los botones y los enlaces de retorno, permitiendo saltar a las secciones deseadas

#### Scenario: Soporte para lectores de pantalla
- **WHEN** un software lector de pantalla procesa la página del manual
- **THEN** los encabezados proporcionan una estructura semántica clara y las imágenes presentan textos alternativos explicativos de las pantallas mostradas
