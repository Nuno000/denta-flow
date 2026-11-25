# Spec Delta — autenticacion-personal

## Purpose

Define cómo ingresa el personal del consultorio (administración, recepción y profesionales) al panel interno con email y contraseña, cómo se mantiene y se cierra esa sesión, y qué protecciones tiene el ingreso.

## ADDED Requirements

### Requirement: Ingreso con email y contraseña
El sistema SHALL permitir ingresar al personal con su email y su contraseña. Ante credenciales válidas de un usuario activo MUST devolver un token de acceso con su vencimiento y los datos del usuario (id, email, rol y profesional vinculado si lo tiene), nunca el hash de la credencial. El email MUST compararse sin distinguir mayúsculas ni espacios al inicio o al final.

#### Scenario: Ingreso correcto
- **WHEN** un usuario activo envía su email y su contraseña correctos a `POST /api/auth/login`
- **THEN** recibe 200 con un token, su fecha y hora de vencimiento en UTC y sus datos (id, email, rol y profesional vinculado), sin ningún dato de la credencial

#### Scenario: Email con mayúsculas o espacios
- **WHEN** un usuario registrado como `recepcion@example.com` ingresa como `  Recepcion@Example.com `
- **THEN** el ingreso es correcto

#### Scenario: Datos de ingreso incompletos
- **WHEN** se envía el login sin email, con un email mal formado o sin contraseña
- **THEN** el sistema responde 400 indicando el dato inválido y no emite token

### Requirement: Rechazo genérico de credenciales inválidas
Ante un email inexistente, una contraseña incorrecta o un usuario desactivado, el sistema MUST responder 401 con el mismo mensaje genérico, sin revelar cuál de las tres condiciones ocurrió ni si el email está registrado.

#### Scenario: Contraseña incorrecta
- **WHEN** un usuario activo envía una contraseña incorrecta
- **THEN** recibe 401 con el mensaje "Email o contraseña incorrectos" y ningún token

#### Scenario: Email inexistente
- **WHEN** se intenta ingresar con un email que no corresponde a ningún usuario
- **THEN** recibe 401 con el mismo mensaje que ante una contraseña incorrecta

#### Scenario: Usuario desactivado no ingresa
- **WHEN** un usuario desactivado envía su email y su contraseña correctos
- **THEN** recibe 401 con el mismo mensaje genérico y ningún token

### Requirement: Límite de intentos fallidos
El sistema SHALL limitar los intentos de ingreso fallidos por email: tras 5 fallos dentro de 15 minutos MUST rechazar nuevos intentos para ese email con 429 e indicar cuándo reintentar, aun si la contraseña es correcta, hasta que pase la ventana. Un ingreso correcto MUST reiniciar el contador.

#### Scenario: Quinto fallo y bloqueo
- **WHEN** se registran 5 intentos fallidos para el mismo email en menos de 15 minutos y llega un sexto intento
- **THEN** el sistema responde 429 con el header `Retry-After` y no evalúa la contraseña

#### Scenario: La ventana vence
- **WHEN** pasaron 15 minutos desde el primer fallo de la ventana bloqueada
- **THEN** el usuario puede volver a intentar el ingreso

#### Scenario: Ingreso correcto reinicia el contador
- **WHEN** un usuario falla 3 veces y en el cuarto intento ingresa correctamente
- **THEN** el contador de fallos de ese email vuelve a cero

### Requirement: Token de acceso con vencimiento
El token SHALL estar firmado con la clave secreta de la aplicación y vencer 8 horas después de emitido. El sistema MUST rechazar con 401 un token vencido, adulterado, firmado con otra clave o con formato inválido.

#### Scenario: Token vigente
- **WHEN** un usuario envía su token vigente en el header `Authorization: Bearer` a un endpoint permitido para su rol
- **THEN** el endpoint responde normalmente

#### Scenario: Token vencido
- **WHEN** se usa un token emitido hace más de 8 horas
- **THEN** el sistema responde 401

#### Scenario: Token adulterado
- **WHEN** se usa un token al que se le modificó el contenido o que fue firmado con otra clave
- **THEN** el sistema responde 401

### Requirement: El estado del usuario se verifica en cada request
En cada request autenticado el sistema SHALL tomar el estado activo y el rol vigentes del usuario, no los del momento en que se emitió el token. Un usuario desactivado MUST dejar de operar desde el request siguiente, aunque su token no haya vencido.

#### Scenario: Usuario desactivado con token vigente
- **WHEN** el administrador desactiva a un usuario y ese usuario hace un request con su token todavía vigente
- **THEN** el sistema responde 401

#### Scenario: Cambio de rol con token vigente
- **WHEN** el administrador cambia el rol de un usuario de `recepcion` a `profesional` y el usuario hace un request con su token anterior
- **THEN** el sistema aplica los permisos del rol `profesional`

### Requirement: Consulta del usuario autenticado
El sistema SHALL exponer `GET /api/auth/yo`, que devuelve los datos del usuario autenticado (id, email, rol y profesional vinculado) y responde 401 sin un token válido.

#### Scenario: Usuario autenticado consulta sus datos
- **WHEN** un usuario autenticado consulta `GET /api/auth/yo`
- **THEN** recibe 200 con su id, email, rol y profesional vinculado

#### Scenario: Consulta sin token
- **WHEN** se consulta `GET /api/auth/yo` sin token
- **THEN** el sistema responde 401

### Requirement: Credenciales almacenadas con hash
El sistema SHALL guardar solo un hash adaptativo con sal de cada contraseña, nunca la contraseña en claro. Las contraseñas MUST tener entre 8 y 72 caracteres. Ni las contraseñas ni los hashes MUST aparecer en respuestas de la API ni en los logs.

#### Scenario: Contraseña guardada con hash
- **WHEN** se crea un usuario con una contraseña válida
- **THEN** en la base queda un hash distinto de la contraseña y dos usuarios con la misma contraseña tienen hashes distintos

#### Scenario: Contraseña demasiado corta
- **WHEN** se intenta crear un usuario con una contraseña de 7 caracteres
- **THEN** el sistema la rechaza con 400 indicando la longitud mínima

### Requirement: Clave de firma obligatoria
La aplicación SHALL tomar la clave de firma de tokens de la variable de entorno `AUTH_SECRET` y MUST negarse a arrancar si falta o tiene menos de 32 bytes, con un mensaje que explique cómo generarla. La clave nunca MUST estar versionada en el repositorio.

#### Scenario: Arranque sin clave
- **WHEN** se inicia el backend fuera del perfil de pruebas sin `AUTH_SECRET` o con una clave de menos de 32 bytes
- **THEN** el arranque falla con un mensaje que indica que falta una clave válida y cómo generarla

### Requirement: Administrador inicial
Al arrancar, si no existe ningún administrador activo y están definidas las variables `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`, el sistema SHALL crear un usuario administrador activo con esos datos. Si ya existe un administrador activo MUST no hacer nada; si faltan las variables MUST avisar en el log sin mostrar su valor.

#### Scenario: Primer arranque con variables definidas
- **WHEN** el backend arranca con la tabla de usuarios vacía y las dos variables definidas
- **THEN** existe un usuario administrador activo con ese email, que puede ingresar con esa contraseña

#### Scenario: Ya hay un administrador
- **WHEN** el backend arranca y ya existe un administrador activo
- **THEN** no se crea ni se modifica ningún usuario

#### Scenario: Faltan las variables
- **WHEN** el backend arranca sin administradores activos y sin `APP_ADMIN_EMAIL` o `APP_ADMIN_PASSWORD`
- **THEN** arranca igual, registra en el log una advertencia sin datos sensibles y no crea ningún usuario

### Requirement: Pantalla de ingreso
El frontend SHALL ofrecer una pantalla de ingreso con email y contraseña, usable desde el celular y accesible (WCAG 2.1 AA): campos con etiqueta, errores anunciados a lectores de pantalla y foco en el primer campo inválido. Tras un ingreso correcto MUST llevar al usuario a la página protegida que pidió o, si no pidió ninguna, al panel interno.

#### Scenario: Ingreso desde la pantalla
- **WHEN** el usuario completa email y contraseña correctos y envía el formulario
- **THEN** entra al panel interno y ve su email y su rol

#### Scenario: Credenciales incorrectas en la pantalla
- **WHEN** el usuario envía una contraseña incorrecta
- **THEN** la pantalla muestra "Email o contraseña incorrectos", lo anuncia a lectores de pantalla y conserva el email cargado

#### Scenario: Validación antes de enviar
- **WHEN** el usuario envía el formulario con el email vacío o mal formado
- **THEN** la pantalla muestra el error junto al campo, mueve el foco a ese campo y no llama a la API

#### Scenario: Demasiados intentos en la pantalla
- **WHEN** la API responde 429 al ingreso
- **THEN** la pantalla informa que hubo demasiados intentos y que se puede reintentar más tarde

### Requirement: Cierre y vencimiento de la sesión en el frontend
El frontend SHALL permitir cerrar la sesión, lo que MUST descartar el token y volver a la pantalla de ingreso. Cuando la API responde 401 a un request autenticado, el frontend MUST descartar el token y llevar al usuario a la pantalla de ingreso.

#### Scenario: Cerrar sesión
- **WHEN** el usuario elige "Cerrar sesión"
- **THEN** el token se descarta, vuelve a la pantalla de ingreso y al ir a una ruta del panel se le pide ingresar de nuevo

#### Scenario: Sesión vencida o usuario desactivado
- **WHEN** la API responde 401 a un request del panel interno
- **THEN** el frontend descarta el token y muestra la pantalla de ingreso

### Requirement: Acceso al ingreso desde la portada
La portada pública SHALL ofrecer un enlace "Ingreso del personal" que lleva a la pantalla de ingreso. Si ya hay una sesión iniciada, el enlace MUST decir "Ir al panel" y llevar al panel interno.

#### Scenario: Portada sin sesión
- **WHEN** alguien sin sesión abre la portada
- **THEN** ve el enlace "Ingreso del personal" que lleva a la pantalla de ingreso

#### Scenario: Portada con sesión
- **WHEN** un usuario con sesión iniciada abre la portada
- **THEN** ve el enlace "Ir al panel" que lleva al panel interno
