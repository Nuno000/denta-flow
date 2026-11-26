# gestion-recursos Specification

## Purpose

Permite que el administrador mantenga los profesionales, sillones y prestaciones reales del consultorio (alta, edición, activación, desactivación y prestaciones por profesional) y que todo el personal interno los consulte, tanto por la API como desde el panel (US-001, Flujo 7).

## Requirements

### Requirement: Consulta de recursos por el personal interno
Los usuarios con rol `administrador`, `recepcion` o `profesional` SHALL poder listar profesionales, sillones y prestaciones, activos e inactivos, con su estado, y consultar cada uno por su identificador. El profesional MUST incluir los identificadores de las prestaciones que realiza.

#### Scenario: Recepción lista los profesionales
- **WHEN** un usuario con rol `recepcion` lista los profesionales y hay uno activo y uno inactivo
- **THEN** recibe 200 con ambos, cada uno con su estado y las prestaciones que realiza

#### Scenario: Profesional consulta un sillón por id
- **WHEN** un usuario con rol `profesional` consulta un sillón inactivo por su identificador
- **THEN** recibe 200 con el sillón y su estado inactivo

#### Scenario: Recurso inexistente
- **WHEN** se consulta una prestación con un identificador que no existe
- **THEN** el sistema responde 404 en formato de problema

#### Scenario: Consulta sin sesión
- **WHEN** se lista cualquiera de los recursos sin token
- **THEN** el sistema responde 401 y no devuelve datos

### Requirement: Modificación solo por el administrador
Dar de alta, editar, desactivar, reactivar y asignar prestaciones SHALL estar permitido solo al rol `administrador`. El backend MUST rechazar con 403 a recepción y a profesional sin modificar nada, aunque la pantalla no les muestre la acción.

#### Scenario: Recepción intenta crear un sillón
- **WHEN** un usuario con rol `recepcion` envía el alta de un sillón válido
- **THEN** el sistema responde 403 y el sillón no se crea

#### Scenario: Profesional intenta cambiar una duración
- **WHEN** un usuario con rol `profesional` envía la edición de una prestación
- **THEN** el sistema responde 403 y la duración no cambia

### Requirement: Alta de recursos
El administrador SHALL poder dar de alta profesionales (nombre, apellido, email y matrícula opcional), sillones (nombre) y prestaciones (nombre y duración en minutos), con las validaciones de `recursos-consultorio`. Todo recurso nuevo MUST nacer activo y el alta MUST responder 201 con el recurso creado.

#### Scenario: Alta de una prestación
- **WHEN** el administrador da de alta la prestación "Limpieza" con 40 minutos
- **THEN** recibe 201 con la prestación activa, su identificador y 40 minutos

#### Scenario: Alta con datos inválidos
- **WHEN** el administrador da de alta una prestación sin duración, o un profesional con el apellido en blanco
- **THEN** el sistema responde 400 en formato de problema, indicando el dato inválido, y no registra nada

#### Scenario: Profesional nuevo sin prestaciones
- **WHEN** el administrador da de alta un profesional sin matrícula
- **THEN** el profesional queda activo, sin matrícula y sin prestaciones

### Requirement: Edición de recursos
El administrador SHALL poder editar los datos de un profesional (nombre, apellido, matrícula y email), el nombre de un sillón y el nombre y la duración de una prestación, con las mismas validaciones del alta. Una edición rechazada MUST dejar el recurso como estaba. Editar MUST NOT cambiar el estado activo ni las prestaciones del profesional.

#### Scenario: Cambio de duración
- **WHEN** el administrador cambia la duración de "Limpieza" de 40 a 45 minutos
- **THEN** la prestación queda con 45 minutos y sigue activa

#### Scenario: Edición inválida
- **WHEN** el administrador intenta dejar la duración de una prestación en 0
- **THEN** el sistema responde 400 y la prestación conserva su duración anterior

#### Scenario: Quitar la matrícula
- **WHEN** el administrador edita un profesional dejando la matrícula vacía
- **THEN** el profesional queda sin matrícula y conserva sus prestaciones

#### Scenario: Editar un recurso inexistente
- **WHEN** el administrador edita un sillón con un identificador que no existe
- **THEN** el sistema responde 404

### Requirement: Duplicados rechazados como conflicto
El sistema MUST rechazar con 409 el alta o la edición que repita el email o la matrícula de otro profesional, o el nombre de otro sillón o de otra prestación. La comparación de nombres MUST ignorar mayúsculas y espacios al inicio y al final, y el resultado MUST ser el mismo en MySQL 8 y en H2 `MODE=MySQL`.

#### Scenario: Nombre de sillón repetido con otras mayúsculas
- **WHEN** existe "Sillón 1" y el administrador da de alta "sillón 1 "
- **THEN** el sistema responde 409 y no crea el sillón

#### Scenario: Email de otro profesional
- **WHEN** el administrador edita un profesional poniéndole el email de otro
- **THEN** el sistema responde 409 y ninguno de los dos cambia

#### Scenario: Conservar el propio nombre
- **WHEN** el administrador edita una prestación cambiando solo la duración y deja su mismo nombre
- **THEN** la edición se acepta

#### Scenario: Duplicado que llega a la base
- **WHEN** dos altas con el mismo nombre de sillón pasan la verificación previa y la segunda choca con la restricción única de la base
- **THEN** la segunda se responde con 409, no con 500

### Requirement: Desactivación y reactivación
El administrador SHALL poder desactivar y reactivar cada recurso. Ambas operaciones MUST ser idempotentes y MUST conservar los datos y las relaciones del recurso. MUST NOT existir ningún endpoint que borre recursos.

#### Scenario: Desactivar una prestación que realizan profesionales
- **WHEN** el administrador desactiva "Blanqueamiento", que realizan dos profesionales
- **THEN** la prestación queda inactiva y los dos profesionales la siguen teniendo asociada

#### Scenario: Reactivar dos veces
- **WHEN** el administrador reactiva un sillón que ya está activo
- **THEN** responde 200 y el sillón sigue activo

#### Scenario: Intento de borrado
- **WHEN** se envía un `DELETE` a un profesional, sillón o prestación
- **THEN** el sistema no lo borra y responde con un error (405 o 404 en formato de problema)

### Requirement: Asignación de prestaciones a un profesional
El administrador SHALL poder reemplazar de una vez el conjunto de prestaciones que realiza un profesional. Toda prestación agregada MUST existir y estar activa. Una prestación inactiva que el profesional ya tenía MUST poder conservarse. Si algún identificador es inválido, MUST rechazarse todo el pedido sin cambiar nada.

#### Scenario: Asignar prestaciones
- **WHEN** el administrador asigna "Consulta" y "Extracción" a un profesional que tenía "Limpieza"
- **THEN** el profesional realiza exactamente "Consulta" y "Extracción"

#### Scenario: Agregar una prestación inactiva
- **WHEN** el pedido agrega "Blanqueamiento", que está inactiva y el profesional no tenía
- **THEN** el sistema responde 400 y el profesional conserva sus prestaciones anteriores

#### Scenario: Conservar una inactiva ya asociada
- **WHEN** el profesional tiene "Blanqueamiento", que fue desactivada, y el pedido la incluye junto con "Consulta"
- **THEN** se acepta y el profesional realiza ambas

#### Scenario: Prestación inexistente
- **WHEN** el pedido incluye un identificador de prestación que no existe
- **THEN** el sistema responde 400 indicando cuál, y no cambia nada

#### Scenario: Conjunto vacío
- **WHEN** el administrador envía un conjunto vacío
- **THEN** el profesional queda sin prestaciones y deja de aparecer como candidato para cualquier prestación

#### Scenario: Identificadores repetidos
- **WHEN** el pedido incluye dos veces el mismo identificador
- **THEN** la prestación queda asociada una sola vez

### Requirement: Sección Recursos del panel
El panel SHALL ofrecer la sección "Recursos" con pestañas Profesionales, Sillones y Prestaciones, cada una con el listado de los recursos y su estado. La pantalla MUST ser usable en un celular de 390 px de ancho y cumplir WCAG 2.1 AA (tablas con encabezados, pestañas navegables con teclado, mensajes de estado anunciados).

#### Scenario: Listado de prestaciones
- **WHEN** un usuario interno abre la pestaña Prestaciones
- **THEN** ve cada prestación con su nombre, duración en minutos y estado

#### Scenario: Profesional con sus prestaciones
- **WHEN** un usuario interno abre la pestaña Profesionales
- **THEN** ve cada profesional con su nombre completo, email, matrícula, estado y los nombres de las prestaciones que realiza

#### Scenario: Error de carga
- **WHEN** la API no responde al abrir una pestaña
- **THEN** la pantalla muestra un aviso de que no se pudieron cargar los datos, sin romperse

### Requirement: Acciones de edición en el panel solo para el administrador
El administrador SHALL ver, en cada pestaña, las acciones de alta, edición, desactivación y reactivación (y, en Profesionales, la de asignar prestaciones). Recepción y profesional MUST ver solo la lectura, sin esas acciones.

#### Scenario: Recepción en la pestaña Sillones
- **WHEN** un usuario con rol `recepcion` abre la pestaña Sillones
- **THEN** ve el listado, pero no los botones de alta, edición ni desactivación

#### Scenario: Administrador en la pestaña Sillones
- **WHEN** el administrador abre la pestaña Sillones
- **THEN** ve el botón "Nuevo sillón" y las acciones de edición y de activar o desactivar en cada fila

### Requirement: Formularios de recursos con validación y respuesta de la API
Los formularios de alta y edición SHALL validar antes de enviar con las mismas reglas del backend (campos obligatorios, largos máximos, email y duración entera mayor que cero). MUST mostrar cada error junto a su campo y los errores de la API (400 y 409) dentro del formulario. Un éxito MUST cerrar el formulario, refrescar el listado y anunciar el resultado.

#### Scenario: Duración inválida antes de enviar
- **WHEN** el administrador escribe 0 en la duración de una prestación nueva y confirma
- **THEN** el formulario muestra "La duración debe ser mayor que cero" junto al campo y no llama a la API

#### Scenario: Nombre repetido informado por la API
- **WHEN** el administrador da de alta un sillón con un nombre que ya existe
- **THEN** el formulario queda abierto y muestra el mensaje de conflicto de la API

#### Scenario: Alta exitosa
- **WHEN** el administrador da de alta una prestación válida
- **THEN** el formulario se cierra, la prestación aparece en el listado y se anuncia "Prestación creada"

### Requirement: Desactivación con confirmación en el panel
Desactivar un recurso desde el panel SHALL pedir confirmación en un diálogo que diga que el recurso se conserva y deja de ofrecerse para turnos nuevos. Reactivar MUST hacerse sin confirmación.

#### Scenario: Confirmar la desactivación de un profesional
- **WHEN** el administrador pide desactivar un profesional y confirma en el diálogo
- **THEN** el profesional figura como inactivo en el listado y se anuncia el cambio

#### Scenario: Cancelar la desactivación
- **WHEN** el administrador cierra el diálogo sin confirmar
- **THEN** no se llama a la API y el recurso sigue activo

### Requirement: Asignación de prestaciones desde el panel
El administrador SHALL poder elegir, en un diálogo con casillas, las prestaciones activas que realiza un profesional. Las inactivas que ya tenía MUST aparecer marcadas e identificadas como inactivas, y MUST poder quitarse.

#### Scenario: Elegir prestaciones
- **WHEN** el administrador marca "Consulta" y "Extracción" en el diálogo de un profesional y guarda
- **THEN** el listado muestra al profesional con esas dos prestaciones

#### Scenario: Prestación inactiva ya asignada
- **WHEN** el diálogo se abre para un profesional que tiene "Blanqueamiento", que fue desactivada
- **THEN** "Blanqueamiento" aparece marcada con la indicación "inactiva" y puede quitarse; las demás inactivas no se ofrecen
