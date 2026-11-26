# Spec Delta — autorizacion-por-rol

## MODIFIED Requirements

### Requirement: Menú del panel según el rol
La barra superior del panel interno SHALL mostrar un menú con las secciones a las que el rol del usuario puede entrar, marcando la sección actual también para lectores de pantalla. El enlace a "Usuarios" MUST mostrarse solo al rol `administrador`. El enlace a "Recursos" MUST mostrarse a los tres roles internos (`administrador`, `recepcion` y `profesional`) y MUST quedar marcado como actual en cualquiera de sus pestañas.

#### Scenario: Administrador en la pantalla de usuarios
- **WHEN** un administrador está en la pantalla de usuarios
- **THEN** el menú muestra "Panel", "Recursos" y "Usuarios", con "Usuarios" marcado como página actual

#### Scenario: Recepción en el panel
- **WHEN** un usuario con rol `recepcion` está en el panel
- **THEN** el menú muestra solo "Panel" y "Recursos", con "Panel" marcado como página actual

#### Scenario: Profesional en una pestaña de recursos
- **WHEN** un usuario con rol `profesional` está en la pestaña Sillones de la sección Recursos
- **THEN** el menú muestra "Panel" y "Recursos", con "Recursos" marcado como página actual
