# Arquitectura Propuesta

> Stack decidido el 2026-11-23 (ver `09_decisiones_y_supuestos.md`, DD-08; resuelve PA-02): **Java 21 + Spring Boot 3 + Maven**, **PostgreSQL 16 + Flyway**, API REST con OpenAPI (springdoc-openapi), frontend **React + Vite + TypeScript** (SPA única para reserva pública y panel interno), email por SMTP (Spring Mail), hosting gratuito en Render (API), Vercel (frontend) y Neon (Postgres), monorepo con CI en GitHub Actions. El esquema de autenticación (sesión por cookie o JWT) se decide en C-04.

## Patrones aplicados

| Patrón / principio | Dónde se usa | Por qué |
|---|---|---|
| Separación en capas (presentación / aplicación / dominio / infraestructura) | Todo el sistema | Aislar reglas de negocio (solapamientos, estados) del framework elegido |
| Motor de disponibilidad único | Reserva online, carga manual, reprogramación, lista de espera | Evita reglas duplicadas; un solo lugar garantiza RN-TU-01/02 |
| Control de concurrencia / transacción en reserva | Creación y reprogramación de turnos | Impedir solapamientos con reservas simultáneas |
| Restricción de exclusión en PostgreSQL (`btree_gist`, `EXCLUDE USING gist` sobre `tstzrange`) | Tabla de turnos, una por profesional y otra por sillón, solo turnos no cancelados (y no sobreturno) | La garantía de RN-TU-01/02 no depende solo del código de aplicación; se suma a la validación en la aplicación |
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
│       │       └── db/migration/         migraciones Flyway (V001__...)
│       └── test/java/ar/dentaflow/       JUnit 5, Mockito, Testcontainers
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
├── docker-compose.yml                    desarrollo local: Postgres (+ Mailpit opcional)
├── .github/workflows/                    CI con GitHub Actions (backend, frontend, E2E)
├── knowledge-base/                       documentación del proyecto
├── docs/                                 documentos fuente (discovery)
└── openspec/                             changes y specs (OpenSpec)
```

## Seguridad

- Autenticación del personal: usuario + credencial con hash; implementada con Spring Security. El esquema (cookie de sesión o JWT) se decide en C-04: el frontend (Vercel) y la API (Render) están en dominios distintos, lo que complica las cookies de sesión (SameSite/CORS).
- Autorización: RBAC con tres roles (ver `03_actores_y_roles.md`), verificado en el backend, no solo en la UI.
- Acceso del paciente: enlaces con token no adivinable por turno (supuesto SU-03); sin cuenta.
- Validación de input: en el backend para todo dato público (DNI, teléfono, email, fechas).
- Protección de datos personales (Ley 25.326): minimización, aviso de privacidad, consentimiento registrado, acceso restringido por rol, transporte cifrado (HTTPS). Política de retención de datos: pendiente (PA-12).
- Anti-abuso en la reserva pública (rate limiting / captcha): a evaluar.
- Secrets management: variables de entorno, nunca en el repositorio.

## Variables de entorno

Nombres según convenciones de Spring Boot (binding relajado) y Vite:

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Conexión JDBC a PostgreSQL | `jdbc:postgresql://localhost:5432/dentaflow` | Sí |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `dentaflow` | Sí |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base | (generada) | Sí |
| `SPRING_MAIL_HOST` | Servidor SMTP | `smtp-relay.brevo.com` | No |
| `SPRING_MAIL_PORT` | Puerto SMTP | `587` | No |
| `SPRING_MAIL_USERNAME` | Usuario SMTP | (del proveedor) | Sí |
| `SPRING_MAIL_PASSWORD` | Contraseña / clave SMTP | (del proveedor) | Sí |
| `APP_MAIL_FROM` | Remitente de recordatorios | `turnos@consultorio.example` | No |
| `APP_BASE_URL` | URL pública base del frontend para armar enlaces de confirmación/cancelación | `https://turnos.example` | No |
| `AUTH_SECRET` | Clave de firma de sesiones/tokens (uso según el esquema de C-04) | (generada) | Sí |
| `TZ` | Zona horaria del consultorio | `America/Argentina/Buenos_Aires` | No |
| `VITE_API_BASE_URL` | URL base de la API que consume el frontend (pública, se incluye en el bundle) | `https://api.turnos.example` | No |

La zona horaria es crítica: todos los horarios de agenda se interpretan en la zona del consultorio (supuesto SU-09).
