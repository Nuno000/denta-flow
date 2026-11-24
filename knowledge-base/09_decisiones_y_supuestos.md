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
**Trade-offs aceptados**: el WhatsApp del MVP no es automático. **Ver PA-03**: la decisión de si entra la API en el MVP sigue abierta.

### DD-06 — Mobile-first web, sin app nativa
**Decisión**: web adaptable al celular para el paciente.
**Justificación**: cubre el uso inicial con menor costo.

### DD-07 — Cumplimiento de Ley 25.326 desde el MVP
**Decisión**: aviso de privacidad y tratamiento acorde desde el inicio; Ley 26.529 recién con ficha clínica.

### DD-08 — Stack tecnológico (aprobado 2026-11-23)
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
**Relacionado**: resuelve PA-02; PA-01 (plazo) sigue abierta.

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
