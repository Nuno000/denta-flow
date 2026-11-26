## ADDED Requirements

### Requirement: Configuración del email por variables de entorno
El backend SHALL tomar el servidor de correo (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`), el remitente (`APP_MAIL_FROM`) y el modo de envío (`APP_MAIL_MODO`: `smtp` o `simulado`) de variables de entorno. `.env.example` MUST listarlas con valores no sensibles y el repositorio MUST NOT contener credenciales SMTP.

#### Scenario: Desarrollo local sin servidor de correo
- **GIVEN** `APP_MAIL_MODO` no está definida
- **WHEN** arranca el backend en desarrollo o en pruebas
- **THEN** usa el modo `simulado` y no intenta conectarse a ningún servidor de correo

#### Scenario: Ejemplo de entorno completo
- **WHEN** se revisa `.env.example`
- **THEN** contiene `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `APP_MAIL_FROM`, `APP_MAIL_MODO` y `APP_BASE_URL`, sin valores reales de credenciales

### Requirement: Configuración de email inválida detiene el arranque
Con `APP_MAIL_MODO=smtp`, el backend MUST NOT arrancar si falta el servidor de correo o el remitente, o si el remitente no es una dirección válida. En cualquier modo MUST NOT arrancar si `APP_BASE_URL` no es una URL absoluta `http` o `https`.

#### Scenario: Modo SMTP sin servidor
- **GIVEN** `APP_MAIL_MODO=smtp` y `SPRING_MAIL_HOST` vacío
- **WHEN** arranca el backend
- **THEN** el arranque falla con un mensaje que nombra la variable faltante y no muestra ninguna contraseña

#### Scenario: URL pública relativa
- **GIVEN** `APP_BASE_URL=turnos.example`
- **WHEN** arranca el backend
- **THEN** el arranque falla indicando que `APP_BASE_URL` debe ser una URL absoluta
