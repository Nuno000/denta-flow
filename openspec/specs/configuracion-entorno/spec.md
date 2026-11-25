# configuracion-entorno Specification

## Purpose

Define cómo arranca DentaFlow en cada entorno (desarrollo, pruebas y pruebas contra MySQL real), de dónde toma su configuración, cómo aplica las migraciones y cómo informa que está sano.

## Requirements

### Requirement: Configuración por variables de entorno
El backend SHALL tomar la conexión a la base y la URL pública de la aplicación de variables de entorno. El repositorio MUST NOT contener secretos y MUST incluir un `.env.example` con todas las variables y valores de ejemplo no sensibles.

#### Scenario: Conexión tomada del entorno
- **WHEN** el backend arranca en el perfil `dev` con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` definidas
- **THEN** se conecta a la base indicada por esas variables

#### Scenario: Ejemplo de variables documentado
- **WHEN** se revisa la raíz del repositorio
- **THEN** existe `.env.example` con todas las variables de `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno, sin valores reales de contraseñas ni claves

### Requirement: Perfil de pruebas autocontenido
Las pruebas automatizadas del backend SHALL correr sin ningún servicio externo, usando una base en memoria compatible con MySQL, y MUST NOT requerir Docker ni contenedores.

#### Scenario: Build en una máquina sin MySQL
- **WHEN** se ejecuta el build de pruebas del backend en una máquina sin MySQL ni Docker
- **THEN** la aplicación arranca en el perfil `test` y todas las pruebas no marcadas como `mysql` pasan

### Requirement: Pruebas contra MySQL real separadas
Las pruebas marcadas como `mysql` SHALL ejecutarse solo cuando se activa explícitamente el perfil `it-mysql`, contra un MySQL 8 real.

#### Scenario: Build por defecto
- **WHEN** se ejecuta el build de pruebas sin activar `it-mysql`
- **THEN** las pruebas marcadas como `mysql` no se ejecutan

#### Scenario: Build con it-mysql
- **WHEN** se ejecuta el build con `it-mysql` activado y un MySQL 8 accesible
- **THEN** las pruebas marcadas como `mysql` se ejecutan y verifican la conexión a esa base

### Requirement: Migraciones aplicadas al arrancar
Al arrancar, el backend SHALL aplicar las migraciones de esquema pendientes y SHALL validar que las entidades coinciden con el esquema; MUST NOT crear ni modificar tablas por fuera de las migraciones.

#### Scenario: Esquema validado al inicio
- **WHEN** el backend arranca contra una base con las migraciones aplicadas
- **THEN** arranca sin errores y no genera DDL propio

#### Scenario: Esquema desalineado
- **WHEN** una entidad no coincide con el esquema migrado
- **THEN** el arranque falla con un error de validación del esquema

### Requirement: Estado de salud
El backend SHALL exponer un endpoint público de estado de salud que responda sin autenticación y sin datos personales.

#### Scenario: Aplicación sana
- **WHEN** se consulta `GET /actuator/health` con la aplicación levantada
- **THEN** responde `200` con estado `UP` y sin detalles de configuración ni datos personales

### Requirement: Frontend configurado por entorno
El frontend SHALL tomar la URL base de la API de `VITE_API_BASE_URL` y todas sus llamadas HTTP MUST pasar por un único cliente API.

#### Scenario: URL de la API configurable
- **WHEN** el frontend se construye con `VITE_API_BASE_URL=https://api.ejemplo`
- **THEN** el cliente API arma sus solicitudes sobre `https://api.ejemplo`
