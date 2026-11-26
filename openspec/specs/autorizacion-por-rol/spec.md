# autorizacion-por-rol Specification

## Purpose
Define qué partes de la API y del panel son públicas, que todo lo demás exige un usuario autenticado con el rol adecuado verificado en el backend, y cómo responde el sistema cuando falta la autenticación o el permiso (RN-AU-02).

## Requirements

### Requirement: Denegar por defecto
Todo endpoint de la API SHALL exigir un usuario autenticado, salvo la lista explícita de rutas públicas. Un endpoint nuevo MUST nacer protegido sin necesidad de configurarlo.

#### Scenario: Endpoint protegido sin token
- **WHEN** se llama a un endpoint no público sin header `Authorization`
- **THEN** el sistema responde 401 y no ejecuta la operación

#### Scenario: Ruta inexistente sin token
- **WHEN** se llama sin token a una ruta de la API que no existe
- **THEN** el sistema responde 401, sin revelar qué rutas existen

### Requirement: Rutas públicas
Las únicas rutas SHALL accesibles sin autenticación son: `GET /actuator/health`, `POST /api/auth/login`, la documentación OpenAPI (`/v3/api-docs/**`, `/swagger-ui/**`) y el prefijo `/api/publico/**`, reservado para la reserva online del paciente (C-10, C-16).

#### Scenario: Estado de salud sin token
- **WHEN** se consulta `GET /actuator/health` sin token
- **THEN** responde 200, sin datos personales

#### Scenario: Login sin token
- **WHEN** se llama a `POST /api/auth/login` sin token
- **THEN** el sistema procesa el ingreso

### Requirement: Rol verificado en el backend
Cada endpoint protegido SHALL declarar qué roles lo pueden usar según la matriz de permisos de `03_actores_y_roles.md`, y el backend MUST rechazar con 403 a un usuario autenticado cuyo rol no esté permitido, aunque la UI no le muestre la opción.

#### Scenario: Recepción intenta gestionar usuarios
- **WHEN** un usuario con rol `recepcion` llama a `GET /api/usuarios`
- **THEN** el sistema responde 403 y no devuelve datos

#### Scenario: Profesional intenta gestionar usuarios
- **WHEN** un usuario con rol `profesional` llama a `POST /api/usuarios`
- **THEN** el sistema responde 403 y no crea ningún usuario

#### Scenario: Administrador gestiona usuarios
- **WHEN** un usuario con rol `administrador` llama a `GET /api/usuarios`
- **THEN** el sistema responde 200 con la lista de usuarios

### Requirement: Errores de acceso en JSON
Las respuestas 401 y 403 SHALL tener cuerpo JSON en formato de problema (RFC 9457) con un título y un detalle en español, sin trazas ni datos internos. La 401 MUST incluir el header `WWW-Authenticate: Bearer`.

#### Scenario: Cuerpo de una 401
- **WHEN** se llama a un endpoint protegido sin token
- **THEN** la respuesta es 401 con `Content-Type: application/problem+json`, el header `WWW-Authenticate: Bearer` y un detalle que indica que hace falta ingresar

#### Scenario: Cuerpo de una 403
- **WHEN** un usuario autenticado llama a un endpoint que su rol no permite
- **THEN** la respuesta es 403 con `Content-Type: application/problem+json` y un detalle que indica que no tiene permiso

### Requirement: Origen del frontend permitido
La API SHALL aceptar requests de navegador con el header `Authorization` solo desde los orígenes configurados del frontend (`APP_BASE_URL`), y MUST no habilitar cookies ni credenciales de navegador entre sitios.

#### Scenario: Origen del frontend
- **WHEN** el navegador hace una consulta previa (preflight) desde el origen configurado del frontend pidiendo el header `Authorization`
- **THEN** la API la autoriza

#### Scenario: Origen desconocido
- **WHEN** el navegador hace una consulta previa desde un origen que no está configurado
- **THEN** la API no la autoriza

### Requirement: Rutas del panel protegidas por rol en el frontend
El frontend SHALL redirigir a la pantalla de ingreso a quien entre sin sesión a una ruta del panel interno, recordando la ruta pedida, y MUST mostrar un aviso de acceso denegado a quien tenga sesión pero no el rol que la ruta requiere. Esta protección es de usabilidad: la autorización real es la del backend.

#### Scenario: Ruta del panel sin sesión
- **WHEN** alguien sin sesión abre una ruta del panel interno
- **THEN** ve la pantalla de ingreso y, tras ingresar, vuelve a la ruta que había pedido

#### Scenario: Ruta de otro rol
- **WHEN** un usuario con rol `recepcion` abre la dirección de la pantalla de usuarios, reservada al rol `administrador`
- **THEN** ve un aviso de que no tiene permiso para esa sección, sin el contenido de la ruta ni ningún dato de usuarios

### Requirement: Menú del panel según el rol
La barra superior del panel interno SHALL mostrar un menú con las secciones a las que el rol del usuario puede entrar, marcando la sección actual también para lectores de pantalla. El enlace a "Usuarios" MUST mostrarse solo al rol `administrador`. Los enlaces a "Agenda" y a "Recursos" MUST mostrarse a los tres roles internos (`administrador`, `recepcion` y `profesional`), y "Recursos" MUST quedar marcado como actual en cualquiera de sus pestañas.

#### Scenario: Administrador en la pantalla de usuarios
- **WHEN** un administrador está en la pantalla de usuarios
- **THEN** el menú muestra "Panel", "Agenda", "Recursos" y "Usuarios", con "Usuarios" marcado como página actual

#### Scenario: Recepción en el panel
- **WHEN** un usuario con rol `recepcion` está en el panel
- **THEN** el menú muestra solo "Panel", "Agenda" y "Recursos", con "Panel" marcado como página actual

#### Scenario: Profesional en una pestaña de recursos
- **WHEN** un usuario con rol `profesional` está en la pestaña Sillones de la sección Recursos
- **THEN** el menú muestra "Panel", "Agenda" y "Recursos", con "Recursos" marcado como página actual

#### Scenario: Profesional en la agenda
- **WHEN** un usuario con rol `profesional` está en la agenda con filtros en la URL
- **THEN** el menú muestra "Panel", "Agenda" y "Recursos", con "Agenda" marcado como página actual
