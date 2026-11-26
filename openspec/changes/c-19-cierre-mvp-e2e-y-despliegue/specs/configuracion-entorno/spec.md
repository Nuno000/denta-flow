## ADDED Requirements

### Requirement: Perfil de despliegue
El backend SHALL tener un perfil `prod` para el entorno público que toma toda la configuración de variables de entorno, no carga datos de demostración salvo que `APP_SEED_DEMO` sea `true`, no ejecuta tareas programadas en proceso y no muestra SQL ni detalles internos en logs o errores.

#### Scenario: Arranque en prod sin seed de demo
- **WHEN** el backend arranca con el perfil `prod` y sin `APP_SEED_DEMO`
- **THEN** aplica las migraciones, crea el administrador inicial si falta y no crea profesionales, sillones ni prestaciones de demostración

#### Scenario: Demo con datos ficticios
- **WHEN** el backend arranca con el perfil `prod` y `APP_SEED_DEMO=true`
- **THEN** carga los datos de demostración ficticios una sola vez, aunque se reinicie

#### Scenario: Variable obligatoria faltante
- **WHEN** el backend arranca con el perfil `prod` sin `SPRING_DATASOURCE_URL` o sin `AUTH_SECRET`
- **THEN** el arranque falla con un mensaje que nombra la variable faltante, sin mostrar valores

### Requirement: Conexión cifrada a la base en el despliegue
En el perfil `prod`, la conexión a MySQL SHALL usar TLS y MUST fallar si el servidor no ofrece una conexión cifrada.

#### Scenario: URL de conexión de producción
- **WHEN** se configura `SPRING_DATASOURCE_URL` para Aiven según la guía de despliegue
- **THEN** la URL incluye `sslMode=REQUIRED` (o más estricto) y la conexión se establece cifrada
