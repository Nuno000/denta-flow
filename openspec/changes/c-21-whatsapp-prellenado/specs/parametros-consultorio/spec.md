## MODIFIED Requirements

### Requirement: Catálogo cerrado de parámetros
El sistema SHALL reconocer exactamente estos parámetros, identificados por su clave: `anticipacion-minima-horas` (entero, en horas), `aviso-privacidad-texto` (texto), `aviso-privacidad-leyenda` (texto), `contacto-consultorio` (texto) y `whatsapp-plantilla` (texto). Una clave fuera del catálogo MUST responderse con 404, sin crear ningún parámetro nuevo.

#### Scenario: Listado con las tres claves
- **WHEN** un administrador consulta `GET /api/parametros`
- **THEN** recibe 200 con exactamente los cinco parámetros del catálogo, cada uno con su clave, una descripción en español, su tipo, su valor actual, la marca `provisorio`, sus límites y la fecha y el usuario de la última modificación (vacíos si nunca se modificó)

#### Scenario: Consulta de un parámetro
- **WHEN** un administrador consulta `GET /api/parametros/anticipacion-minima-horas`
- **THEN** recibe 200 con ese parámetro solo

#### Scenario: Clave desconocida
- **WHEN** un administrador consulta o edita `/api/parametros/recordatorio-horas`, que no está en el catálogo
- **THEN** recibe 404 en formato de problema (RFC 9457) y la tabla de parámetros no cambia

### Requirement: Valores por defecto provisorios
Al aplicar las migraciones, todo entorno (desarrollo, pruebas y despliegue) SHALL tener los cinco parámetros con un valor por defecto marcado como provisorio: la anticipación mínima en 24 horas, hasta que se resuelva PA-04, textos de aviso de privacidad de borrador, pendientes de revisión legal, un texto de contacto genérico que el consultorio reemplaza y una plantilla de WhatsApp de recordatorio con todos los marcadores. Los parámetros MUST existir sin depender de ningún dato de demostración.

#### Scenario: Base recién migrada
- **WHEN** se arranca el backend contra una base vacía, en cualquier perfil
- **THEN** existen los cinco parámetros, todos con `provisorio` verdadero, la anticipación mínima vale 24 horas y ninguno registra usuario ni fecha de modificación

#### Scenario: Catálogo y base coinciden
- **WHEN** se comparan las claves del catálogo con las filas de la tabla de parámetros
- **THEN** hay exactamente una fila por clave del catálogo y ninguna fila de más

## ADDED Requirements

### Requirement: Plantilla de WhatsApp obligatoria y acotada
El parámetro `whatsapp-plantilla` SHALL ser un texto de 1 a 1000 caracteres sin espacios sobrantes al principio ni al final. Un valor vacío o más largo MUST rechazarse con 400 sin cambiar el valor guardado.

#### Scenario: Plantilla vacía
- **WHEN** un administrador guarda `whatsapp-plantilla` con un texto vacío o solo espacios
- **THEN** recibe 400 con un detalle en español y el valor anterior se conserva

#### Scenario: Plantilla demasiado larga
- **WHEN** un administrador guarda `whatsapp-plantilla` con 1001 caracteres
- **THEN** recibe 400 y el valor anterior se conserva

#### Scenario: Plantilla válida
- **WHEN** un administrador guarda `whatsapp-plantilla` con "Hola {nombre}, tu turno es el {fecha} a las {hora}"
- **THEN** recibe 200, el parámetro deja de ser provisorio y el siguiente mensaje de WhatsApp usa el texto nuevo
