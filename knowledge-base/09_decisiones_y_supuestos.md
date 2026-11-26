# Decisiones y Supuestos

## Decisiones documentadas

Provienen de `docs/discovery/discovery.md` e `informe-discovery.md`.

### DD-01 — MVP limitado a turnos y agenda
**Decisión**: el MVP cubre solo turnos y agenda; ficha clínica, odontograma, cobros, obras sociales, ARCA y similares quedan para etapas posteriores.
**Contexto**: son estándar de mercado pero no diferencian; la presión de alcance es un riesgo.
**Alternativas consideradas**: sistema integral de gestión odontológica.
**Justificación**: foco en el problema de coordinación de turnos y viabilidad para un equipo de 4.
**Trade-offs aceptados**: no se compite en funciones clínicas.

### DD-02 — Prevención real de solapamientos en profesional y sillón
**Decisión**: el sistema bloquea (no solo alerta) superposiciones de profesional y de sillón/box.
**Contexto**: ningún competidor comprueba públicamente esa combinación; AgendaPro solo muestra una alerta aceptable.
**Alternativas consideradas**: alerta con posibilidad de aceptar.
**Justificación**: es el diferencial central propuesto.
**Trade-offs aceptados**: mayor complejidad en validación y concurrencia. Queda el riesgo de que el diferencial no sea tal (demos pendientes).

### DD-03 — Sobreturno solo por recepción, explícito y registrado
**Decisión**: la reserva online nunca permite solapamientos; el sobreturno lo carga solo la recepción.
**Justificación**: conserva la regla sin perder flexibilidad operativa.
**Trade-offs aceptados**: requiere un flujo y permiso específicos.

### DD-04 — Paciente sin cuenta
**Decisión**: reserva sin registro obligatorio; identificación por DNI y teléfono.
**Justificación**: reducir fricción en la reserva.
**Trade-offs aceptados**: acceso al turno por enlace con token; menos historial del paciente.

### DD-05 — Canal de recordatorios en el MVP: email + wa.me
**Decisión**: email automático y enlaces de WhatsApp prellenados; la API oficial de WhatsApp queda para después.
**Contexto**: la API tiene costo por mensaje y varios competidores "automatizan" de forma manual.
**Trade-offs aceptados**: el WhatsApp del MVP no es automático. PA-03 resuelta el 2026-11-24: en el MVP, WhatsApp es solo por enlace `wa.me` prellenado; la API oficial queda fuera.

### DD-06 — Mobile-first web, sin app nativa
**Decisión**: web adaptable al celular para el paciente.
**Justificación**: cubre el uso inicial con menor costo.

### DD-07 — Cumplimiento de Ley 25.326 desde el MVP
**Decisión**: aviso de privacidad y tratamiento acorde desde el inicio; Ley 26.529 recién con ficha clínica.

### DD-08 — Stack tecnológico (aprobado 2026-11-23)
> **Reemplazada parcialmente por DD-09 (2026-11-24)** en base de datos, tests de backend, hosting de la base y desarrollo local. Siguen vigentes backend, frontend, email, hosting de API y frontend, repositorio y CI. El texto se conserva como registro histórico.

**Decisión**: la cátedra permite elegir el stack libremente (resuelve PA-02). Se adopta:
- **Backend**: Java 21 LTS + Spring Boot 3 (Spring Web, Spring Security, Spring Data JPA, Bean Validation, Spring Mail), build con Maven. API REST documentada con OpenAPI (springdoc-openapi).
- **Base de datos**: PostgreSQL 16, migraciones con Flyway. El anti-solapamiento (RN-TU-01/02) se garantiza también a nivel de base con una restricción de exclusión (`btree_gist`, `EXCLUDE USING gist` sobre un rango `tstzrange`) por profesional y por sillón, para turnos no cancelados, además de la validación en la aplicación.
- **Tests backend**: JUnit 5, Mockito, Spring Boot Test y Testcontainers (Postgres real en Docker); Strict TDD.
- **Frontend**: una única SPA para la reserva pública (mobile-first) y el panel interno: React + Vite + TypeScript, React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS. Tests con Vitest + Testing Library; E2E con Playwright.
- **Email**: SMTP genérico vía Spring Mail; proveedor gratuito (Brevo o Resend) a elegir en C-14.
- **Hosting (planes gratuitos)**: API en Render, frontend en Vercel, Postgres en Neon. Desarrollo local con docker-compose (Postgres + captor de mails opcional, p. ej. Mailpit).
- **Repositorio**: monorepo con `backend/` y `frontend/` (más `knowledge-base/`, `docs/`, `openspec/`); CI con GitHub Actions.

**Contexto**: equipo de 4 con plazo acotado; el diferencial (DD-02) exige una garantía de no solapamiento robusta ante concurrencia.
**Justificación por capa**:
- Java/Spring Boot: ecosistema maduro, transacciones y seguridad (RBAC) integradas, y encaje con la formación del equipo.
- PostgreSQL: único motor de uso común con restricciones de exclusión sobre rangos horarios, que dan la garantía de no solapamiento aun con reservas simultáneas.
- Testcontainers: las pruebas de concurrencia y de la restricción de exclusión corren contra Postgres real, no contra un sustituto en memoria.
- React + Vite + TS: una sola base para la reserva mobile-first y el panel, con tipado y validación compartida de formularios (Zod).
- Planes gratuitos (Render/Vercel/Neon): costo cero para un trabajo académico.
**Alternativas consideradas**: TypeScript/NestJS, Python y C# en el backend; Angular, Next.js y plantillas renderizadas en servidor en el frontend; Docker/VPS propio o infraestructura de la facultad para el hosting. Se descartaron por menor afinidad del equipo, mayor complejidad de despliegue o falta de ventaja clara frente a la opción elegida.
**Trade-offs aceptados / consecuencias**:
- **Arranque en frío de los planes gratuitos**: los servidores se suspenden por inactividad, por lo que un `@Scheduled` dentro del proceso puede no dispararse. Propuesta (a decidir en C-18): el envío de recordatorios es una operación idempotente (sin emails duplicados) disparada por un cron externo gratuito contra un endpoint protegido; `@Scheduled` se usa en desarrollo local.
- **Front y API en dominios distintos** (Vercel y Render): complica las cookies de sesión (SameSite/CORS). El esquema de autenticación (cookie de sesión o JWT) se decide en C-04 (zona CRITICA), no ahora.
- Dependencia de PostgreSQL y de Docker en desarrollo y en CI (Testcontainers).
- Dependencia de los límites de los planes gratuitos (almacenamiento, horas de cómputo, cuota de email).
**Relacionado**: resuelve PA-02. PA-01 resuelta en DD-10.

### DD-09 — Base de datos MySQL 8 y desarrollo sin Docker (aprobado 2026-11-24)
**Decisión**: reemplaza la parte de datos, tests y entorno local de DD-08:
- **Base de datos**: MySQL 8 (InnoDB) + Flyway (`flyway-mysql`, `mysql-connector-j`), instalado de forma nativa en desarrollo y usado también en despliegue.
- **Tests de backend**: JUnit 5, Mockito, MockMvc, `@DataJpaTest` y `@SpringBootTest` con el perfil `test` sobre H2 en memoria (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`). Los tests de concurrencia y bloqueo se marcan `@Tag("mysql")` y corren además con el perfil `it-mysql` contra un MySQL 8 real (local, o el MySQL preinstalado en los runners `ubuntu-latest` de GitHub Actions).
- **Sin Docker**: no se usan Docker, `docker-compose` ni Testcontainers; tampoco un captor de mails en contenedor.
- **Anti-solapamiento (RN-TU-01/02/07, DD-02)**: la transacción que crea o mueve un turno bloquea con `SELECT ... FOR UPDATE` la fila del **profesional** y después la del **sillón** (siempre en ese orden; si hay varios del mismo tipo, por id ascendente). Con ambos bloqueos tomados verifica el solapamiento leyendo el último estado confirmado (aislamiento `READ COMMITTED` o lectura `FOR SHARE`) y recién entonces escribe. Índices `(profesional_id, estado, inicio, fin)` y `(sillon_id, estado, inicio, fin)` sobre `turno`. "Cualquier profesional" (PA-05) se resuelve probando un candidato por transacción.
- **Tipos**: PK `BIGINT AUTO_INCREMENT`, tiempos `DATETIME(6)` en UTC mapeados a `Instant`, estados como `VARCHAR` + `CHECK`; SQL de migraciones válido en MySQL 8 y en H2 `MODE=MySQL`.
- **Hosting de la base**: deja de aplicar el proveedor de DD-08; el proveedor gratuito de MySQL 8 se elige en C-19 (PA-14).

**Contexto**: las máquinas del equipo no tienen RAM suficiente para correr Docker (base en contenedor + Testcontainers) junto con el IDE y el navegador.
**Alternativas consideradas**:
- Mantener el motor de DD-08 instalado de forma nativa: se descartó por preferencia y experiencia del equipo con MySQL.
- Motor de DD-08 + H2 en su modo de compatibilidad: H2 no soporta la restricción de exclusión de DD-08, así que la garantía quedaba sin probar localmente.
- Tabla de franjas con `UNIQUE` por recurso: obliga a una granularidad fija que choca con duraciones variables por prestación (RN-TU-03).
- Bloqueo optimista con versión en el recurso: más reintentos y no más simple de probar.
**Justificación**: MySQL 8 nativo es liviano y conocido por el equipo; el bloqueo por PK del recurso serializa solo a quienes compiten por el mismo profesional o sillón y es predecible en InnoDB.
**Trade-offs aceptados / consecuencias**:
- La base no tiene una garantía declarativa: el anti-solapamiento depende de que **toda** escritura de turnos pase por el servicio transaccional de C-09. Se compensa con una única vía de escritura, revisión en PR y un test de concurrencia en `it-mysql` en cada change que cree o mueva turnos (C-09, C-12, C-16, C-17, C-20).
- H2 no reproduce el bloqueo de filas de InnoDB: un test de concurrencia en verde sobre H2 no cuenta como verificado.
- Reservas del mismo profesional se serializan; aceptable para 1 a 5 profesionales.
**Relacionado**: DD-02, DD-08, PA-14, C-02, C-09, C-10.

### DD-10 — Plazo y corte de entrega (aprobado 2026-11-24)
**Decisión**: la entrega del trabajo final es el **jueves 2026-11-26 a las 12:00** (resuelve PA-01). El corte de entrega incluye C-01, C-02, C-03, C-04, C-06, C-07, C-08, C-09, C-10, C-14, C-15, C-16, C-17 y C-19 (camino crítico más sus dependencias reales; C-06, C-07, C-14 y C-15 en versión mínima). Quedan fuera del corte, sin borrarse del roadmap, C-05 (alcanza el seed), C-11, C-12, C-13, C-18 y los diferenciadores C-20 a C-24.
**Contexto**: quedan menos de 48 h y el corte tiene una cadena serial de 10 changes.
**Alternativas consideradas**: entregar solo la planificación; MVP completo sin diferenciadores (no entra en el plazo).
**Trade-offs aceptados**: sin agenda interna (C-11), carga manual con sobreturno (C-12), asistencia (C-13) ni recordatorios programados (C-18) en la entrega.
**Riesgo y plan de degradación**: si el corte no llega completo, la prioridad es C-09 + C-10 (el diferencial DD-02: no solapamiento por profesional y sillón con concurrencia), mostrado con una demo local si no hay despliegue.
**Relacionado**: PA-01, `01_vision_y_objetivos.md` §Plazo y corte de entrega, `CHANGES.md`.

### DD-11 — Autenticación del personal con JWT Bearer (aprobado 2026-11-25)
**Decisión**: el personal ingresa con email y contraseña (hash BCrypt) y recibe un **JWT firmado con HMAC-SHA256** usando `AUTH_SECRET` (mínimo 32 bytes; sin clave válida el backend no arranca), con vigencia de **8 horas**, que el frontend envía en el header `Authorization: Bearer`. En cada request el backend relee el usuario por el `sub` del token: un usuario desactivado deja de operar y un cambio de rol se aplica en el request siguiente, sin revocación de tokens. El frontend guarda el token en `sessionStorage`. Todo endpoint exige autenticación salvo `/actuator/health`, `POST /api/auth/login`, la documentación OpenAPI y `/api/publico/**` (reserva online). Un solo rol por usuario (PA-09), con catálogo en la tabla `rol`.
**Contexto**: el frontend (Vercel) y la API (Render) quedan en sitios distintos; una cookie de sesión sería de terceros y Safari y Firefox la bloquean por defecto.
**Alternativas consideradas**: cookie de sesión con proxy de Vercel para hacerla first-party (suma CSRF, rewrites y sesiones que se pierden al reiniciar Render); tokens opacos guardados en la base.
**Trade-offs aceptados**: un token robado vale hasta 8 h (mitigado porque desactivar al usuario lo corta en el request siguiente); un XSS podría leer el token de `sessionStorage`; el límite de intentos de ingreso (5 fallos en 15 min por email) vive en memoria y se reinicia con el servicio.
**Relacionado**: C-04 (`openspec/changes/c-04-auth-usuarios-roles/design.md`), PA-09, `03_actores_y_roles.md` §RBAC, `08_arquitectura_propuesta.md` §Seguridad.

### DD-12 — Definiciones cerradas para el corte de entrega (aprobado 2026-11-25)
**Decisión** (comunicada por Florencia al equipo para que ningún change quede frenado por preguntas abiertas):
- **PA-13 / SU-03 (C-09)**: el email del paciente es obligatorio y el DNI es obligatorio y único por paciente.
- **PA-11 (C-06)**: cargar un bloqueo horario no cancela los turnos ya existentes; solo avisa si hay turnos en esa franja e impide reservas nuevas en ella.
- **Email (C-14)**: Spring Mail por SMTP estándar configurado con variables de entorno (`.env`), con Brevo o una contraseña de aplicación de Gmail; en los tests, adaptador simulado.
- **PA-12 (C-15)**: aceptación obligatoria del aviso de privacidad al reservar, registrando el instante, y un endpoint de anonimización/baja de los datos personales del paciente; los registros se conservan 5 años.
- **PA-04 (C-17)**: la anticipación mínima es configurable en los parámetros, por defecto 24 h y ajustable a 0 h para la demo (hoy C-07 acepta de 1 a 168: bajar a 0 requiere ampliar ese rango).
- **PA-14 (C-19)**: MySQL en Aiven (plan gratuito) como opción principal; si el 2026-11-26 a las 11:00 el tiempo no alcanza, se prioriza una demo local completa funcionando al 100%.
**Contexto**: entrega el 2026-11-26 a las 12:00 (DD-10).
**Relacionado**: PA-04, PA-11, PA-12, PA-13, PA-14, SU-03, C-06, C-09, C-14, C-15, C-17, C-19.

### DD-13 — Dockerfile solo para el build remoto de Render (aprobado 2026-11-25)
**Decisión**: la API se despliega en Render con un `backend/Dockerfile` multietapa (JDK 21 para compilar, JRE 21 para correr) que construye **solo Render, en sus servidores**. Es una excepción acotada a la regla "sin Docker" de DD-09: en desarrollo, tests y CI se sigue sin Docker, sin `docker-compose` y sin Testcontainers.
**Contexto**: Render no tiene runtime nativo de Java; el motivo de DD-09 es la RAM de las máquinas locales, que esta excepción no afecta.
**Alternativas consideradas**: otro hosting con buildpacks de Java (cambiar DD-08 a horas de la entrega); presentar solo la demo local.
**Trade-offs aceptados**: el build de producción difiere del local (imagen vs. `mvnw` nativo); se mitiga con el mismo JDK 21 y el smoke test después de cada deploy.
**Relacionado**: DD-08, DD-09, DD-12, C-19 (`openspec/changes/c-19-cierre-mvp-e2e-y-despliegue/design.md` D1).

## Supuestos inferidos

### SU-01 — Los pacientes prefieren reservar online sobre WhatsApp
**Supuesto**: hay adopción suficiente de la reserva online.
**Origen**: `discovery.md`, sección Riesgos (declarado sin probar).
**Riesgo si es falso**: el sistema se usa poco y el valor cae.
**Cómo validar**: entrevistas o piloto con un consultorio; medir proporción online vs recepción.

### SU-02 — El diferencial de sillón + solapamiento es real
**Supuesto**: ningún competidor lo ofrece realmente.
**Origen**: `informe-discovery.md` (sin demos).
**Riesgo si es falso**: la propuesta de valor se debilita.
**Cómo validar**: demos de Oclusar, DentalSoft, AgendaPro, Atiendo, Dentalink.

### SU-03 — Acceso del paciente por enlace con token; DNI como clave de paciente
**Supuesto**: el enlace del email contiene un token por turno y el paciente se identifica (y se deduplica) por DNI.
**Origen**: inferido de "sin registro, DNI y teléfono" (`discovery.md`, Supuestos aceptados).
**Riesgo si es falso**: duplicados de pacientes o acceso indebido a turnos.
**Cómo validar**: diseño de seguridad con el equipo.
**Validado el 2026-11-25 (DD-12)**: DNI obligatorio y único; email obligatorio.

### SU-04 — Modelo de datos conceptual
**Supuesto**: las entidades de `04_modelo_de_datos.md` son un punto de partida razonable.
**Origen**: derivado de reglas de negocio; no existe modelo previo.
**Riesgo si es falso**: retrabajo de esquema.
**Cómo validar**: revisión del equipo antes de implementar.

### SU-05 — Matriz RBAC propuesta
**Supuesto**: permisos de `03_actores_y_roles.md` (p. ej. recepción ve el panel de ausentismo; admin puede cargar sobreturno).
**Origen**: inferido de los casos de uso (solo se explicita que recepción carga sobreturnos).
**Cómo validar**: confirmar con el equipo/docente.

### SU-06 — Dentro de la ventana mínima, cancela la recepción
**Supuesto**: fuera de plazo, el paciente debe contactar al consultorio.
**Origen**: inferido de RN-CA-01.

### SU-07 — Datos mínimos del paciente
**Supuesto**: nombre, DNI, teléfono y email alcanzan para el MVP.
**Origen**: inferido de "DNI y teléfono" + necesidad de email para recordatorios.
**Riesgo si es falso**: no se pueden enviar recordatorios si falta el email.
**Cómo validar**: definir si el email es obligatorio en la reserva.

### SU-08 — Criterios de aceptación propuestos
**Supuesto**: los criterios de `06_funcionalidades.md` son propuestos por esta KB, no provienen de las fuentes.

### SU-09 — Zona horaria única
**Supuesto**: un consultorio, una zona horaria (America/Argentina/Buenos_Aires).
**Origen**: segmento "consultorio chico, una sede, Argentina".
