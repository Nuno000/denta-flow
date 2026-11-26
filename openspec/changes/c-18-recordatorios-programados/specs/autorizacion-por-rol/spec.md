## MODIFIED Requirements

### Requirement: Rutas públicas
Las únicas rutas SHALL accesibles sin autenticación de usuario son: `GET /actuator/health`, `POST /api/auth/login`, la documentación OpenAPI (`/v3/api-docs/**`, `/swagger-ui/**`) y el prefijo `/api/publico/**`, reservado para la reserva online del paciente (C-10, C-16). El prefijo `/api/tareas/**` tampoco usa el token del personal: MUST autenticarse con la credencial de tarea (`X-Tarea-Token`, C-18) y rechazar con 401 cualquier llamada sin ella.

#### Scenario: Estado de salud sin token
- **WHEN** se consulta `GET /actuator/health` sin token
- **THEN** responde 200, sin datos personales

#### Scenario: Login sin token
- **WHEN** se llama a `POST /api/auth/login` sin token
- **THEN** el sistema procesa el ingreso

#### Scenario: Tarea sin credencial de tarea
- **WHEN** se llama a `POST /api/tareas/recordatorios` sin `X-Tarea-Token`, aunque se envíe un JWT válido del personal
- **THEN** el sistema responde 401 y no ejecuta la tarea
