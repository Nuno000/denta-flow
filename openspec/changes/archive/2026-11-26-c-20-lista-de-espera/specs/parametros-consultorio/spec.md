# Spec Delta — `parametros-consultorio`

## MODIFIED Requirements

### Requirement: Catálogo cerrado de parámetros
El sistema SHALL reconocer exactamente estos parámetros, identificados por su clave: `anticipacion-minima-horas` (entero, en horas), `aviso-privacidad-texto` (texto), `aviso-privacidad-leyenda` (texto), `contacto-consultorio` (texto), `whatsapp-plantilla` (texto) y `lista-espera-plazo-oferta-horas` (entero, en horas, entre 1 y 72). Una clave fuera del catálogo MUST responderse con 404, sin crear ningún parámetro nuevo.

#### Scenario: Listado con las tres claves
- **WHEN** un administrador consulta `GET /api/parametros`
- **THEN** recibe 200 con exactamente los seis parámetros del catálogo, cada uno con su clave, una descripción en español, su tipo, su valor actual, la marca `provisorio`, sus límites y la fecha y el usuario de la última modificación (vacíos si nunca se modificó)

#### Scenario: Consulta de un parámetro
- **WHEN** un administrador consulta `GET /api/parametros/anticipacion-minima-horas`
- **THEN** recibe 200 con ese parámetro solo

#### Scenario: Clave desconocida
- **WHEN** un administrador consulta o edita `/api/parametros/recordatorio-horas`, que no está en el catálogo
- **THEN** recibe 404 en formato de problema (RFC 9457) y la tabla de parámetros no cambia

## ADDED Requirements

### Requirement: Plazo de oferta de lista de espera en horas enteras
El parámetro `lista-espera-plazo-oferta-horas` SHALL aceptar únicamente números enteros entre `1` y `72` horas, ambos incluidos, con valor provisorio inicial `4` horas cargado en la migración `V020_1`. Cualquier valor fuera de rango o no entero MUST rechazarse con `400`.

#### Scenario: Valor válido para plazo de oferta
- **WHEN** un administrador guarda `PUT /api/parametros/lista-espera-plazo-oferta-horas` con valor `6`
- **THEN** recibe `200` con valor `6`, `provisorio` pasa a `false` y las nuevas ofertas de lista de espera vencen a las 6 horas

#### Scenario: Valor fuera de rango
- **WHEN** un administrador envía `0` o `73` para `lista-espera-plazo-oferta-horas`
- **THEN** recibe `400` y el valor guardado no cambia
