## ADDED Requirements

### Requirement: Entorno público accesible
El MVP SHALL quedar accesible por HTTPS en una URL pública del frontend (Vercel) que consume la API pública (Render) conectada a MySQL 8 (Aiven), con las migraciones aplicadas y el administrador inicial creado desde variables de entorno.

#### Scenario: Ingreso al panel desplegado
- **WHEN** el administrador abre la URL pública del frontend e ingresa con las credenciales configuradas en el entorno
- **THEN** ve el panel según su rol

#### Scenario: Salud de la API desplegada
- **WHEN** se consulta `GET /actuator/health` en la URL de la API
- **THEN** responde 200 con estado `UP` y sin datos personales

### Requirement: Rutas de la SPA en el hosting
El hosting del frontend SHALL servir `index.html` para cualquier ruta de la aplicación que no sea un archivo estático, para que los enlaces del email y las recargas del navegador funcionen.

#### Scenario: Enlace de confirmación abierto directo
- **WHEN** un paciente abre desde el email una URL del frontend con ruta profunda (por ejemplo, la de confirmar su turno)
- **THEN** la aplicación carga y muestra la pantalla correspondiente, no un 404 del hosting

### Requirement: Sin secretos en el repositorio ni en el build del frontend
Las credenciales de la base, la clave de firma de tokens, las credenciales SMTP, la contraseña del administrador inicial y la credencial de tareas SHALL configurarse solo como variables de entorno de la plataforma. El repositorio y el bundle del frontend MUST NOT contenerlas.

#### Scenario: Bundle del frontend
- **WHEN** se busca en el build del frontend cualquiera de los valores secretos configurados
- **THEN** no aparece ninguno; solo está la URL pública de la API

### Requirement: Origen del frontend desplegado
La API desplegada SHALL aceptar requests de navegador solo desde la URL pública del frontend configurada en `APP_BASE_URL`.

#### Scenario: Llamada desde el frontend desplegado
- **WHEN** el frontend desplegado llama a la API con el token del personal
- **THEN** la API responde con los headers CORS que autorizan ese origen

#### Scenario: Llamada desde otro origen
- **WHEN** una página de otro origen intenta llamar a la API desde el navegador
- **THEN** el navegador no recibe autorización CORS

### Requirement: Arranque en frío tolerado
Como la API del plan gratuito se suspende sin tráfico, el frontend SHALL mostrar un estado de carga comprensible mientras la API despierta, sin perder lo que el usuario cargó, y la demo MUST incluir un paso previo para despertarla.

#### Scenario: Primera llamada con la API dormida
- **WHEN** el paciente abre la reserva con la API suspendida
- **THEN** ve un indicador de carga en español hasta que la API responde, o un mensaje de reintento si la espera supera el límite del cliente

### Requirement: Demo local equivalente como plan B
El repositorio SHALL incluir una guía y un script que levanten la demo completa en una máquina del equipo con MySQL 8 nativo, sin Docker, con los mismos flujos del corte y datos de demostración ficticios.

#### Scenario: Ensayo del plan B
- **WHEN** un integrante sigue la guía en una máquina con Java 21, Node y MySQL 8 instalados
- **THEN** en menos de 10 minutos puede reservar un turno, confirmarlo por enlace e ingresar al panel

### Requirement: Flujos del corte verificados de punta a punta
Los flujos del corte (reserva online con aceptación del aviso, confirmación y cancelación por enlace, reprogramación, ingreso del personal y edición de un parámetro) SHALL tener pruebas E2E automatizadas que pasen contra el entorno local y que puedan ejecutarse contra el desplegado.

#### Scenario: Suite E2E local
- **WHEN** se corren las pruebas E2E contra el entorno local con datos de demostración
- **THEN** todas pasan

#### Scenario: Suite E2E contra el despliegue
- **WHEN** se corren las pruebas E2E con la URL base del entorno desplegado
- **THEN** las pruebas de solo lectura y la reserva de punta a punta pasan

### Requirement: Reservas simultáneas sobre el mismo hueco
Ante dos reservas simultáneas por la API pública sobre el mismo profesional y horario, el sistema SHALL aceptar exactamente una y rechazar la otra con conflicto, verificado contra MySQL 8 real.

#### Scenario: Dos pacientes eligen el mismo hueco a la vez
- **WHEN** dos requests de reserva para el mismo profesional, prestación e inicio llegan al mismo tiempo
- **THEN** una crea el turno y la otra recibe 409, y hay un solo turno activo en ese rango
