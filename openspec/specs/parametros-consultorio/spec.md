# parametros-consultorio Specification

## Purpose

Define los parámetros de funcionamiento del consultorio que el administrador puede ajustar sin tocar código (anticipación mínima de cancelación/reprogramación y textos del aviso de privacidad), cómo se validan, con qué valores provisorios arranca el sistema y cómo los leen los demás procesos (US-004, RN-CA-01, RN-CA-02, RN-PR-01).

## Requirements

### Requirement: Catálogo cerrado de parámetros
El sistema SHALL reconocer exactamente estos parámetros, identificados por su clave: `anticipacion-minima-horas` (entero, en horas), `aviso-privacidad-texto` (texto) y `aviso-privacidad-leyenda` (texto). Una clave fuera del catálogo MUST responderse con 404, sin crear ningún parámetro nuevo.

#### Scenario: Listado con las tres claves
- **WHEN** un administrador consulta `GET /api/parametros`
- **THEN** recibe 200 con exactamente los tres parámetros del catálogo, cada uno con su clave, una descripción en español, su tipo, su valor actual, la marca `provisorio`, sus límites y la fecha y el usuario de la última modificación (vacíos si nunca se modificó)

#### Scenario: Consulta de un parámetro
- **WHEN** un administrador consulta `GET /api/parametros/anticipacion-minima-horas`
- **THEN** recibe 200 con ese parámetro solo

#### Scenario: Clave desconocida
- **WHEN** un administrador consulta o edita `/api/parametros/recordatorio-horas`, que no está en el catálogo
- **THEN** recibe 404 en formato de problema (RFC 9457) y la tabla de parámetros no cambia

### Requirement: Anticipación mínima en horas enteras dentro de rango
La anticipación mínima de cancelación/reprogramación (RN-CA-01, RN-CA-02) SHALL ser un número entero de horas entre 1 y 168 (una semana), ambos incluidos. El sistema MUST rechazar con 400 cualquier otro valor y MUST dejar el valor anterior sin cambios.

#### Scenario: Valor válido
- **WHEN** un administrador envía `PUT /api/parametros/anticipacion-minima-horas` con valor `48`
- **THEN** recibe 200 con el valor `48` y las lecturas siguientes devuelven 48 horas

#### Scenario: Límites del rango
- **WHEN** un administrador guarda el valor `1` y después el valor `168`
- **THEN** ambos se aceptan

#### Scenario: Fuera de rango
- **WHEN** un administrador envía el valor `0`, `-5` o `169`
- **THEN** recibe 400 con un detalle en español que indica el rango permitido (de 1 a 168 horas) y el valor guardado sigue siendo el anterior

#### Scenario: Valor no entero
- **WHEN** un administrador envía el valor `veinte`, `24.5`, un texto vacío o no envía el valor
- **THEN** recibe 400 y el valor guardado sigue siendo el anterior

### Requirement: Textos del aviso de privacidad obligatorios y acotados
Los textos del aviso de privacidad (RN-PR-01) SHALL ser obligatorios: el sistema MUST guardarlos sin espacios al inicio ni al final y MUST rechazar con 400 un texto vacío o solo de espacios, o uno más largo que su máximo (4000 caracteres el texto del aviso y 200 la leyenda de aceptación), sin cambiar el valor anterior.

#### Scenario: Texto válido
- **WHEN** un administrador guarda en `aviso-privacidad-texto` un texto de 1500 caracteres con espacios al inicio y al final
- **THEN** recibe 200 y el texto queda guardado sin esos espacios

#### Scenario: Texto vacío
- **WHEN** un administrador guarda en `aviso-privacidad-leyenda` un valor vacío o solo de espacios
- **THEN** recibe 400 indicando que el texto es obligatorio y el valor guardado no cambia

#### Scenario: Texto demasiado largo
- **WHEN** un administrador guarda en `aviso-privacidad-texto` un texto de 4001 caracteres, o en `aviso-privacidad-leyenda` uno de 201
- **THEN** recibe 400 indicando el máximo permitido y el valor guardado no cambia

#### Scenario: Caracteres del español
- **WHEN** un administrador guarda un texto con tildes, eñes y signos de apertura (`¿Aceptás el tratamiento de tus datos según la Ley 25.326?`)
- **THEN** la lectura siguiente devuelve exactamente el mismo texto, tanto en MySQL 8 como en H2 `MODE=MySQL`

### Requirement: Valores por defecto provisorios
Al aplicar las migraciones, todo entorno (desarrollo, pruebas y despliegue) SHALL tener los tres parámetros con un valor por defecto marcado como provisorio: la anticipación mínima en 24 horas, hasta que se resuelva PA-04, y textos de aviso de privacidad de borrador, pendientes de revisión legal. Los parámetros MUST existir sin depender de ningún dato de demostración.

#### Scenario: Base recién migrada
- **WHEN** se arranca el backend contra una base vacía, en cualquier perfil
- **THEN** existen los tres parámetros, todos con `provisorio` verdadero, la anticipación mínima vale 24 horas y ninguno registra usuario ni fecha de modificación

#### Scenario: Catálogo y base coinciden
- **WHEN** se comparan las claves del catálogo con las filas de la tabla de parámetros
- **THEN** hay exactamente una fila por clave del catálogo y ninguna fila de más

### Requirement: Confirmación al guardar
Guardar un valor válido SHALL marcar el parámetro como no provisorio y MUST registrar qué usuario lo guardó y en qué instante, en UTC. Guardar el mismo valor que ya tenía cuenta como confirmación del valor provisorio.

#### Scenario: Confirmar el valor provisorio
- **WHEN** un administrador guarda en `anticipacion-minima-horas` el mismo valor `24` que tenía por defecto
- **THEN** el parámetro sigue valiendo 24, pasa a `provisorio` falso y registra a ese administrador como autor de la modificación

#### Scenario: Instante de la modificación en UTC
- **WHEN** un administrador guarda un parámetro el 2026-12-01 a las 21:30 hora de Buenos Aires (UTC−3)
- **THEN** la respuesta y las lecturas siguientes informan la modificación como `2026-12-02T00:30:00Z`, sin corrimiento, en cualquier perfil

#### Scenario: Un valor rechazado no confirma
- **WHEN** un administrador envía un valor inválido a un parámetro provisorio
- **THEN** recibe 400 y el parámetro sigue provisorio, con el mismo valor, autor y fecha que antes

### Requirement: Edición y consulta solo por el administrador
Consultar y editar parámetros por la API SHALL estar permitido solo al rol `administrador` (US-004, RN-AU-02), verificado en el backend. El sistema MUST responder 403 a los roles `recepcion` y `profesional` y 401 sin token, sin ejecutar la operación.

#### Scenario: Recepción intenta editar
- **WHEN** un usuario con rol `recepcion` envía `PUT /api/parametros/anticipacion-minima-horas` con un valor válido
- **THEN** recibe 403 en formato de problema y el valor guardado no cambia

#### Scenario: Profesional intenta consultar
- **WHEN** un usuario con rol `profesional` consulta `GET /api/parametros`
- **THEN** recibe 403 y no recibe ningún parámetro

#### Scenario: Sin token
- **WHEN** alguien sin token consulta o edita `/api/parametros`
- **THEN** recibe 401 con el header `WWW-Authenticate: Bearer` y no se ejecuta la operación

#### Scenario: Administrador desactivado
- **WHEN** un administrador que fue desactivado usa un token emitido antes de la desactivación para editar un parámetro
- **THEN** recibe 401 y el valor no cambia

### Requirement: Lectura tipada para otros procesos
El sistema SHALL ofrecer a los demás procesos internos (cancelación/reprogramación de C-17 y aviso de privacidad de C-15) la anticipación mínima como una duración exacta de N horas y el aviso de privacidad como texto y leyenda, siempre con el último valor guardado, sin reiniciar el sistema.

#### Scenario: Anticipación como duración
- **WHEN** la anticipación mínima guardada es 36
- **THEN** la lectura interna devuelve una duración de exactamente 36 horas, independiente de la zona horaria del servidor

#### Scenario: Cambio visible de inmediato
- **WHEN** un administrador cambia la anticipación mínima de 24 a 48
- **THEN** la siguiente lectura interna, sin reiniciar el backend, devuelve 48 horas

#### Scenario: Aviso de privacidad
- **WHEN** un proceso interno pide el aviso de privacidad
- **THEN** recibe el texto y la leyenda de aceptación vigentes, aunque sigan siendo provisorios

### Requirement: Ediciones simultáneas sin valores mezclados
Cada edición SHALL guardar el valor, la marca provisoria, el autor y el instante de forma atómica. Ante dos ediciones simultáneas del mismo parámetro, el valor final MUST ser completo y uno de los dos enviados (gana la última), nunca una mezcla ni un valor inválido.

#### Scenario: Dos administradores guardan a la vez
- **WHEN** dos administradores guardan al mismo tiempo los valores `48` y `72` en `anticipacion-minima-horas`
- **THEN** ambos reciben 200 y el valor final es `48` o `72`, con el autor y el instante de esa misma edición

### Requirement: Integridad de los parámetros en la base
La base SHALL impedir, también ante escrituras que no pasen por la API, dos filas con la misma clave, un parámetro sin valor o sin la marca provisoria, y un autor de modificación que no sea un usuario existente.

#### Scenario: Clave repetida
- **WHEN** se intenta insertar directamente en la base una segunda fila con la clave `anticipacion-minima-horas`
- **THEN** la base rechaza la escritura, tanto en MySQL 8 como en H2 `MODE=MySQL`

#### Scenario: Autor inexistente
- **WHEN** se intenta guardar directamente en la base un parámetro cuyo autor de modificación es un id de usuario que no existe
- **THEN** la base rechaza la escritura

#### Scenario: Valor nulo
- **WHEN** se intenta guardar directamente en la base un parámetro sin valor
- **THEN** la base rechaza la escritura
