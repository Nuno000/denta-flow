## MODIFIED Requirements

### Requirement: Catálogo cerrado de parámetros
El sistema SHALL reconocer exactamente estos parámetros, identificados por su clave: `anticipacion-minima-horas` (entero, en horas), `aviso-privacidad-texto` (texto), `aviso-privacidad-leyenda` (texto) y `recordatorio-horas-antes` (entero, en horas, de 1 a 168). Una clave fuera del catálogo MUST responderse con 404, sin crear ningún parámetro nuevo.

#### Scenario: Listado con las tres claves
- **WHEN** un administrador consulta `GET /api/parametros`
- **THEN** recibe 200 con exactamente los parámetros del catálogo (las tres claves originales más `recordatorio-horas-antes`), cada uno con su clave, una descripción en español, su tipo, su valor actual, la marca `provisorio`, sus límites y la fecha y el usuario de la última modificación (vacíos si nunca se modificó)

#### Scenario: Consulta de un parámetro
- **WHEN** un administrador consulta `GET /api/parametros/anticipacion-minima-horas`
- **THEN** recibe 200 con ese parámetro solo

#### Scenario: Clave desconocida
- **WHEN** un administrador consulta o edita `/api/parametros/recordatorios-por-turno`, que no está en el catálogo
- **THEN** recibe 404 en formato de problema (RFC 9457) y la tabla de parámetros no cambia

#### Scenario: Antelación del recordatorio fuera de rango
- **WHEN** un administrador envía `PUT /api/parametros/recordatorio-horas-antes` con valor `0` o `169`
- **THEN** recibe 400 y el valor anterior no cambia

### Requirement: Valores por defecto provisorios
Al aplicar las migraciones, todo entorno (desarrollo, pruebas y despliegue) SHALL tener los cuatro parámetros con un valor por defecto marcado como provisorio: la anticipación mínima en 24 horas, textos de aviso de privacidad de borrador, pendientes de revisión legal, y la antelación del recordatorio en 24 horas. Los parámetros MUST existir sin depender de ningún dato de demostración.

#### Scenario: Base recién migrada
- **WHEN** se arranca el backend contra una base vacía, en cualquier perfil
- **THEN** existen los cuatro parámetros, todos con `provisorio` verdadero, la anticipación mínima y la antelación del recordatorio valen 24 horas y ninguno registra usuario ni fecha de modificación

#### Scenario: Catálogo y base coinciden
- **WHEN** se comparan las claves del catálogo con las filas de la tabla de parámetros
- **THEN** hay exactamente una fila por clave del catálogo y ninguna fila de más

### Requirement: Lectura tipada para otros procesos
El sistema SHALL ofrecer a los demás procesos internos (cancelación/reprogramación de C-17, aviso de privacidad de C-15 y recordatorios de C-18) la anticipación mínima y la antelación del recordatorio como duraciones exactas de N horas y el aviso de privacidad como texto y leyenda, siempre con el último valor guardado, sin reiniciar el sistema.

#### Scenario: Anticipación como duración
- **WHEN** la anticipación mínima guardada es 36
- **THEN** la lectura interna devuelve una duración de exactamente 36 horas, independiente de la zona horaria del servidor

#### Scenario: Cambio visible de inmediato
- **WHEN** un administrador cambia la anticipación mínima de 24 a 48
- **THEN** la siguiente lectura interna, sin reiniciar el backend, devuelve 48 horas

#### Scenario: Aviso de privacidad
- **WHEN** un proceso interno pide el aviso de privacidad
- **THEN** recibe el texto y la leyenda de aceptación vigentes, aunque sigan siendo provisorios

#### Scenario: Antelación del recordatorio como duración
- **WHEN** la antelación del recordatorio guardada es 48
- **THEN** la lectura interna devuelve una duración de exactamente 48 horas
