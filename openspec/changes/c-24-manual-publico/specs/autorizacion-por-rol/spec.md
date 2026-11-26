## MODIFIED Requirements

### Requirement: Menú del panel según el rol
La barra superior del panel interno SHALL mostrar un menú con las secciones permitidas por rol, marcando la sección actual para lectores de pantalla. "Usuarios" MUST mostrarse solo al rol `administrador`. "Agenda", "Recursos" y "Manual" (que dirige a `/manual`) MUST mostrarse a los tres roles internos (`administrador`, `recepcion` y `profesional`). "Recursos" MUST marcarse como actual en cualquiera de sus pestañas.

#### Scenario: Administrador en la pantalla de usuarios
- **WHEN** un administrador está en la pantalla de usuarios
- **THEN** el menú muestra "Panel", "Agenda", "Recursos", "Usuarios" y "Manual", con "Usuarios" marcado como página actual

#### Scenario: Recepción en el panel
- **WHEN** un usuario con rol `recepcion` está en el panel
- **THEN** el menú muestra "Panel", "Agenda", "Recursos" y "Manual", con "Panel" marcado como página actual

#### Scenario: Profesional en una pestaña de recursos
- **WHEN** un usuario con rol `profesional` está en la pestaña Sillones de la sección Recursos
- **THEN** el menú muestra "Panel", "Agenda", "Recursos" y "Manual", con "Recursos" marcado como página actual

#### Scenario: Profesional en la agenda
- **WHEN** un usuario con rol `profesional` está en la agenda con filtros en la URL
- **THEN** el menú muestra "Panel", "Agenda", "Recursos" y "Manual", con "Agenda" marcado como página actual
