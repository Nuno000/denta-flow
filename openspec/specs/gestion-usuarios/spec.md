# gestion-usuarios Specification

## Purpose
Define cómo el administrador controla quién accede al sistema interno: alta de usuarios, asignación de un rol, vínculo con el profesional que representa y baja lógica sin borrado (US-003, RN-AU-02).

## Requirements

### Requirement: Alta de usuario por el administrador
El administrador SHALL poder crear un usuario con email, contraseña y un rol (`administrador`, `recepcion` o `profesional`). El usuario MUST quedar activo y el email MUST guardarse normalizado (sin espacios al inicio o al final y en minúsculas). La respuesta MUST incluir los datos del usuario sin la credencial.

#### Scenario: Alta de un usuario de recepción
- **WHEN** el administrador crea el usuario `recepcion@example.com` con una contraseña válida y rol `recepcion`
- **THEN** recibe 201 con el id, el email, el rol y el estado activo del usuario, sin la contraseña ni su hash

#### Scenario: Rol inexistente
- **WHEN** el administrador intenta crear un usuario con el rol `superusuario`
- **THEN** el sistema responde 400 indicando los roles válidos y no crea nada

#### Scenario: Email mal formado
- **WHEN** el administrador intenta crear un usuario con el email `recepcion-sin-arroba`
- **THEN** el sistema responde 400 indicando que el email no es válido

### Requirement: Email único
El email SHALL identificar a un único usuario, sin distinguir mayúsculas. El sistema MUST rechazar con 409 un alta o modificación que repita el email de otro usuario, activo o inactivo, y la base MUST impedirlo también ante escrituras concurrentes.

#### Scenario: Email repetido
- **WHEN** el administrador intenta crear un usuario con `Recepcion@Example.com` y ya existe `recepcion@example.com`
- **THEN** el sistema responde 409 indicando que el email ya está en uso

#### Scenario: La base rechaza el duplicado
- **WHEN** se intenta guardar directamente en la base un segundo usuario con un email ya registrado
- **THEN** la base rechaza la escritura por una restricción de unicidad, tanto en MySQL 8 como en H2 `MODE=MySQL`

### Requirement: Un solo rol por usuario
Cada usuario SHALL tener exactamente un rol mientras PA-09 (usuarios con varios roles) siga abierta. El sistema MUST rechazar un usuario sin rol o con más de uno.

#### Scenario: Usuario sin rol
- **WHEN** el administrador intenta crear un usuario sin indicar el rol
- **THEN** el sistema responde 400 indicando que el rol es obligatorio

### Requirement: Catálogo de roles
Los roles válidos SHALL estar registrados en la base como un catálogo con un código (`administrador`, `recepcion`, `profesional`) y un nombre visible (Administrador, Recepción, Profesional). El rol de cada usuario MUST ser uno del catálogo. El administrador MUST poder consultar el catálogo en `GET /api/roles`.

#### Scenario: Consulta del catálogo
- **WHEN** el administrador consulta `GET /api/roles`
- **THEN** recibe los tres roles, cada uno con su código y su nombre visible

#### Scenario: La base rechaza un rol fuera del catálogo
- **WHEN** se intenta guardar directamente en la base un usuario con el rol `superusuario`
- **THEN** la base rechaza la escritura por una restricción de integridad, tanto en MySQL 8 como en H2 `MODE=MySQL`

### Requirement: Vínculo con el profesional
Un usuario con rol `profesional` SHALL estar vinculado a exactamente un profesional existente y activo, y cada profesional MUST tener como máximo un usuario. Los usuarios con rol `administrador` o `recepcion` MUST no tener profesional vinculado.

#### Scenario: Usuario profesional vinculado
- **WHEN** el administrador crea un usuario con rol `profesional` vinculado a un profesional activo sin usuario
- **THEN** el usuario queda creado con ese profesional vinculado

#### Scenario: Usuario profesional sin profesional
- **WHEN** el administrador intenta crear un usuario con rol `profesional` sin indicar el profesional
- **THEN** el sistema responde 400 indicando que el rol profesional requiere un profesional vinculado

#### Scenario: Profesional inexistente o inactivo
- **WHEN** el administrador intenta vincular un usuario a un profesional que no existe o está inactivo
- **THEN** el sistema responde 400 y no crea el usuario

#### Scenario: Profesional ya vinculado
- **WHEN** el administrador intenta vincular un segundo usuario a un profesional que ya tiene usuario
- **THEN** el sistema responde 409 y no crea el usuario

#### Scenario: Recepción con profesional
- **WHEN** el administrador intenta crear un usuario con rol `recepcion` indicando un profesional
- **THEN** el sistema responde 400 indicando que solo el rol profesional se vincula a un profesional

#### Scenario: La base rechaza un vínculo incoherente
- **WHEN** se intenta guardar directamente en la base un usuario con rol `profesional` sin profesional, o con rol `recepcion` y un profesional
- **THEN** la base rechaza la escritura por una restricción de integridad, tanto en MySQL 8 como en H2 `MODE=MySQL`

### Requirement: Profesionales disponibles para vincular
El administrador SHALL poder consultar los profesionales activos que todavía no tienen usuario (id, nombre y apellido), para elegir a cuál vincular un usuario con rol `profesional`.

#### Scenario: Solo activos y sin usuario
- **WHEN** hay tres profesionales: uno activo sin usuario, uno activo con usuario y uno inactivo, y el administrador consulta los profesionales disponibles
- **THEN** recibe solo el profesional activo sin usuario

### Requirement: Listado de usuarios
El administrador SHALL poder listar todos los usuarios, activos e inactivos, con id, email, rol, profesional vinculado y estado, ordenados por id y sin datos de la credencial.

#### Scenario: Listado completo
- **WHEN** el administrador consulta `GET /api/usuarios` con un usuario activo y uno desactivado
- **THEN** recibe ambos, cada uno con su estado, y ninguno incluye la contraseña ni el hash

### Requirement: Cambio de rol
El administrador SHALL poder cambiar el rol de otro usuario, con las mismas reglas de vínculo con el profesional que en el alta. El cambio MUST aplicarse desde el request siguiente de ese usuario.

#### Scenario: De recepción a profesional
- **WHEN** el administrador cambia el rol de un usuario de `recepcion` a `profesional` vinculándolo a un profesional activo sin usuario
- **THEN** el usuario queda con rol `profesional` y ese profesional vinculado

#### Scenario: De profesional a recepción
- **WHEN** el administrador cambia el rol de un usuario de `profesional` a `recepcion`
- **THEN** el usuario queda con rol `recepcion` y sin profesional vinculado

#### Scenario: Usuario inexistente
- **WHEN** el administrador intenta cambiar el rol de un id que no existe
- **THEN** el sistema responde 404

### Requirement: Desactivar y reactivar sin borrar
El administrador SHALL poder desactivar y reactivar usuarios. Un usuario desactivado MUST no poder ingresar ni operar, y MUST conservarse con sus datos: el sistema no ofrece borrado de usuarios.

#### Scenario: Desactivar un usuario
- **WHEN** el administrador desactiva a un usuario de recepción
- **THEN** el usuario sigue en el listado como inactivo y no puede ingresar

#### Scenario: Reactivar un usuario
- **WHEN** el administrador reactiva a un usuario desactivado
- **THEN** el usuario vuelve a poder ingresar con su contraseña anterior

#### Scenario: Sin borrado
- **WHEN** se busca una operación para eliminar un usuario en la API
- **THEN** no existe: la única baja es la desactivación

### Requirement: Blanqueo de contraseña
El administrador SHALL poder definir una contraseña nueva para otro usuario, con las mismas reglas de longitud que en el alta. Desde ese momento el usuario MUST ingresar con la contraseña nueva y la anterior MUST dejar de funcionar.

#### Scenario: Blanqueo exitoso
- **WHEN** el administrador blanquea la contraseña de un usuario de recepción con una contraseña nueva válida
- **THEN** recibe 204, el usuario ingresa con la contraseña nueva y la anterior da 401

#### Scenario: Contraseña nueva inválida
- **WHEN** el administrador blanquea una contraseña con una nueva de 7 caracteres
- **THEN** el sistema responde 400 indicando la longitud mínima y la contraseña anterior sigue vigente

### Requirement: Siempre queda un administrador
El administrador MUST no poder desactivarse, cambiarse el rol ni blanquearse la contraseña a sí mismo, para que el sistema no quede sin ningún administrador activo ni se cambie la propia credencial sin la anterior.

#### Scenario: Auto-desactivación
- **WHEN** el administrador intenta desactivar su propio usuario
- **THEN** el sistema responde 409 indicando que no puede desactivarse a sí mismo, y el usuario sigue activo

#### Scenario: Auto-cambio de rol
- **WHEN** el administrador intenta cambiar su propio rol a `recepcion`
- **THEN** el sistema responde 409 y el rol no cambia

#### Scenario: Auto-blanqueo de contraseña
- **WHEN** el administrador intenta blanquear su propia contraseña
- **THEN** el sistema responde 409 y la contraseña no cambia

### Requirement: Pantalla de gestión de usuarios
El frontend SHALL ofrecer al administrador una pantalla de usuarios con el listado (email, rol con su nombre visible, profesional vinculado y estado), legible en el celular y en escritorio y accesible (WCAG 2.1 AA). El acceso desde el panel MUST mostrarse solo a administradores.

#### Scenario: Listado en la pantalla
- **WHEN** el administrador abre la pantalla de usuarios
- **THEN** ve todos los usuarios, activos e inactivos, con su email, rol, profesional vinculado y estado

#### Scenario: Acceso solo para administradores
- **WHEN** un usuario con rol `recepcion` entra al panel
- **THEN** no ve el acceso a la pantalla de usuarios

### Requirement: Alta de usuario desde la pantalla
La pantalla SHALL permitir crear un usuario con email, contraseña y rol elegido del catálogo. Cuando el rol elegido es `profesional`, MUST pedir el profesional entre los disponibles y, para los otros roles, MUST no mostrar ese campo. Los errores de validación y los conflictos MUST mostrarse junto al formulario sin perder lo cargado.

#### Scenario: Alta desde la pantalla
- **WHEN** el administrador completa email, contraseña y rol `recepcion` y confirma
- **THEN** el usuario aparece en el listado como activo

#### Scenario: Rol profesional pide profesional
- **WHEN** el administrador elige el rol `profesional` en el formulario
- **THEN** aparece el campo obligatorio de profesional con los profesionales disponibles, y no se puede confirmar sin elegir uno

#### Scenario: Email repetido en la pantalla
- **WHEN** la API responde 409 porque el email ya está en uso
- **THEN** el formulario muestra que el email ya está en uso y conserva los datos cargados

### Requirement: Modificación y baja desde la pantalla
Desde el listado, el administrador SHALL poder cambiar el rol (y el profesional vinculado), desactivar o reactivar y blanquear la contraseña de cada usuario. La desactivación MUST pedir confirmación. Sobre su propio usuario estas acciones MUST no estar disponibles.

#### Scenario: Cambio de rol desde la pantalla
- **WHEN** el administrador cambia el rol de un usuario de `recepcion` a `profesional` y elige un profesional disponible
- **THEN** el listado muestra el rol y el profesional nuevos

#### Scenario: Desactivar con confirmación
- **WHEN** el administrador elige desactivar a un usuario y confirma
- **THEN** el listado lo muestra inactivo y ofrece reactivarlo

#### Scenario: Blanqueo desde la pantalla
- **WHEN** el administrador ingresa una contraseña nueva válida para otro usuario y confirma
- **THEN** la pantalla informa que la contraseña se cambió, sin mostrarla

#### Scenario: Acciones sobre el propio usuario
- **WHEN** el administrador ve su propia fila en el listado
- **THEN** las acciones de cambiar rol, desactivar y blanquear contraseña no están disponibles
