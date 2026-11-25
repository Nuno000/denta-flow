# Arquitectura Propuesta

> Stack decidido el 2026-11-23 y actualizado el 2026-11-24 (ver `09_decisiones_y_supuestos.md`, DD-08 y DD-09): **Java 21 + Spring Boot 3 + Maven**, **MySQL 8 + Flyway** (tests sobre H2 `MODE=MySQL` y MySQL real en el perfil `it-mysql`; sin Docker), API REST con OpenAPI (springdoc-openapi), frontend **React + Vite + TypeScript** (SPA única para reserva pública y panel interno), email por SMTP (Spring Mail), hosting gratuito en Render (API) y Vercel (frontend); el proveedor de MySQL se elige en C-19 (PA-14). Monorepo con CI en GitHub Actions. El esquema de autenticación (sesión por cookie o JWT) se decide en C-04.

## Patrones aplicados

| Patrón / principio | Dónde se usa | Por qué |
|---|---|---|
| Separación en capas (presentación / aplicación / dominio / infraestructura) | Todo el sistema | Aislar reglas de negocio (solapamientos, estados) del framework elegido |
| Motor de disponibilidad único | Reserva online, carga manual, reprogramación, lista de espera | Evita reglas duplicadas; un solo lugar garantiza RN-TU-01/02 |
| Control de concurrencia / transacción en reserva | Creación y reprogramación de turnos | Impedir solapamientos con reservas simultáneas |
| Bloqueo pesimista por recurso (DD-09) | Servicio transaccional de creación y cambio de turnos (C-09): `SELECT ... FOR UPDATE` sobre la fila del profesional y después la del sillón (orden fijo; varios del mismo tipo por id ascendente), verificación de solapamiento leyendo el último estado confirmado (`READ COMMITTED` o `FOR SHARE`), y recién entonces la escritura | Garantiza RN-TU-01/02 ante reservas simultáneas en MySQL 8; el orden fijo evita interbloqueos. Los sobreturnos toman los bloqueos pero no se verifican (RN-TU-07) |
| Intentos sucesivos por candidato | Reserva con "cualquier profesional" (PA-05) y sillón asignado por el sistema (PA-06) | El motor ordena los candidatos (profesional, sillón) y la creación prueba uno por transacción; ninguna transacción retiene bloqueos de recursos que no usa |
| Auditoría por evento | Cambios de turno | RN-AU-01 |
| Tareas programadas (operación idempotente) | Recordatorios, reintentos, ofertas de lista de espera | Automatización sin intervención de recepción; en hosting gratuito el proceso puede estar suspendido, por lo que se propone un endpoint protegido disparado por un cron externo (decisión en C-18) y `@Scheduled` solo en local |
| Máquina de estados | Turno | Transiciones válidas explícitas entre estados |

## Estructura de directorios

Monorepo (DD-08):

```
denta-flow/
├── backend/                              API Spring Boot (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/ar/dentaflow/
│       │   │   ├── dominio/              reglas de negocio puras
│       │   │   │   ├── turnos/
│       │   │   │   ├── pacientes/
│       │   │   │   ├── recursos/         profesionales, sillones, prestaciones
│       │   │   │   ├── disponibilidad/   motor de disponibilidad, horarios, bloqueos
│       │   │   │   ├── notificaciones/
│       │   │   │   ├── usuarios/         usuarios del personal y roles
│       │   │   │   ├── listaespera/
│       │   │   │   └── auditoria/
│       │   │   ├── aplicacion/           casos de uso / servicios transaccionales
│       │   │   │   ├── configuracion/    parámetros del consultorio
│       │   │   │   └── reportes/         panel de ausentismo
│       │   │   ├── infraestructura/      JPA y adaptadores externos
│       │   │   │   ├── email/            Spring Mail (SMTP)
│       │   │   │   ├── scheduler/        recordatorios y tareas programadas
│       │   │   │   └── seguridad/        Spring Security
│       │   │   └── presentacion/         controladores REST, DTOs, manejo de errores
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/         migraciones Flyway (V0NN_<n>__descripcion.sql, NN = número del change)
│       └── test/java/ar/dentaflow/       JUnit 5, Mockito, H2 (perfil test) / MySQL (perfil it-mysql)
├── frontend/                             SPA React + Vite + TypeScript
│   ├── package.json
│   ├── vite.config.ts
│   ├── playwright.config.ts
│   └── src/
│       ├── features/
│       │   ├── reserva/                  reserva pública mobile-first
│       │   ├── agenda/                   panel interno
│       │   └── auth/
│       ├── components/
│       ├── lib/                          cliente API, TanStack Query, esquemas Zod
│       └── routes/
├── .github/workflows/                    CI con GitHub Actions (backend, it-mysql con el MySQL del runner, frontend, E2E)
├── knowledge-base/                       documentación del proyecto
├── docs/                                 documentos fuente (discovery)
└── openspec/                             changes y specs (OpenSpec)
```

## Seguridad

- Autenticación del personal (DD-11, C-04): email + contraseña con hash BCrypt; Spring Security como resource server con **JWT Bearer** (HS256, firmado con `AUTH_SECRET`, 8 h). Sin cookies ni CSRF, porque el frontend (Vercel) y la API (Render) están en sitios distintos. En cada request se relee el usuario: desactivarlo o cambiarle el rol tiene efecto inmediato. Límite de 5 ingresos fallidos en 15 minutos por email (429).
- Autorización: RBAC con tres roles (ver `03_actores_y_roles.md`), verificado en el backend, no solo en la UI. Se deniega por defecto: las únicas rutas públicas son `GET /actuator/health`, `POST /api/auth/login`, la documentación OpenAPI (`/v3/api-docs/**`, `/swagger-ui/**`) y `/api/publico/**`, reservado para la reserva online del paciente. Cada endpoint protegido declara su guarda (`@SoloAdministrador`, `@AdministradorORecepcion`, `@PersonalInterno`).
- Errores de la API: formato `ProblemDetail` (RFC 9457, `application/problem+json`) con título y detalle en español, sin trazas.
- CORS: solo el origen del frontend (`APP_BASE_URL`), con el header `Authorization` y sin credenciales de navegador.
- Acceso del paciente: enlaces con token no adivinable por turno (supuesto SU-03); sin cuenta.
- Validación de input: en el backend para todo dato público (DNI, teléfono, email, fechas).
- Protección de datos personales (Ley 25.326): minimización, aviso de privacidad, consentimiento registrado, acceso restringido por rol, transporte cifrado (HTTPS). Política de retención de datos: pendiente (PA-12).
- Anti-abuso en la reserva pública (rate limiting / captcha): a evaluar.
- Secrets management: variables de entorno, nunca en el repositorio.

## Variables de entorno

Nombres según convenciones de Spring Boot (binding relajado) y Vite:

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Conexión JDBC a MySQL 8 | `jdbc:mysql://localhost:3306/dentaflow` | Sí |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `dentaflow` | Sí |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base | (generada) | Sí |
| `SPRING_MAIL_HOST` | Servidor SMTP | `smtp-relay.brevo.com` | No |
| `SPRING_MAIL_PORT` | Puerto SMTP | `587` | No |
| `SPRING_MAIL_USERNAME` | Usuario SMTP | (del proveedor) | Sí |
| `SPRING_MAIL_PASSWORD` | Contraseña / clave SMTP | (del proveedor) | Sí |
| `APP_MAIL_FROM` | Remitente de recordatorios | `turnos@consultorio.example` | No |
| `APP_BASE_URL` | URL pública base del frontend para armar enlaces de confirmación/cancelación | `https://turnos.example` | No |
| `AUTH_SECRET` | Clave de firma de los tokens JWT (DD-11). Obligatoria, de al menos 32 bytes: el backend no arranca sin ella. Generar con `openssl rand -base64 48` | (generada) | Sí |
| `APP_ADMIN_EMAIL` | Email del administrador inicial, que se crea al arrancar si no hay ningún administrador activo (C-04) | `admin@consultorio.example` | Sí |
| `APP_ADMIN_PASSWORD` | Contraseña del administrador inicial (8 a 72 caracteres) | (elegida por el consultorio) | Sí |
| `TZ` | Zona horaria del consultorio | `America/Argentina/Buenos_Aires` | No |
| `VITE_API_BASE_URL` | URL base de la API que consume el frontend (pública, se incluye en el bundle) | `https://api.turnos.example` | No |

La zona horaria es crítica: todos los horarios de agenda se interpretan en la zona del consultorio (supuesto SU-09). En la base, los tiempos se guardan en UTC (`DATETIME(6)`, `hibernate.jdbc.time_zone=UTC`) y se mapean a `Instant`.

## Migraciones y pruebas (DD-09)

- **Migraciones**: una sola ubicación, `backend/src/main/resources/db/migration/`, con SQL válido en MySQL 8 y en H2 `MODE=MySQL` (PK `BIGINT AUTO_INCREMENT`, `DATETIME(6)`, estados `VARCHAR` + `CHECK`, sin `ENUM`). Versión por change: `V0NN_<n>__descripcion.sql` (C-09 → `V009_1__...`), para que changes paralelos no choquen. En el perfil de desarrollo, `spring.flyway.out-of-order=true`. Nunca se edita una migración ya mergeada.
- **Perfil `test`**: H2 en memoria (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`) para unitarios, slices e integración.
- **Perfil `it-mysql`**: tests `@Tag("mysql")` de concurrencia y bloqueo contra MySQL 8 real (local o el preinstalado en los runners de GitHub Actions). Un test de concurrencia solo cuenta como verificado en este perfil.
- Sin Docker, `docker-compose` ni Testcontainers.
