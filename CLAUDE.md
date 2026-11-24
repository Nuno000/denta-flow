<!-- active-stack:agent-instruction -->
# DentaFlow — Instrucciones para Agentes

> Este archivo es lo PRIMERO que todo agente lee al entrar al repo.
> Generado a partir de `knowledge-base/` y `CHANGES.md`. Si cambian la KB o el roadmap, regeneralo con `agent-instruction`.

DentaFlow es un sistema de turnos online para consultorios odontológicos chicos de Argentina, con una sola sede: reserva online, confirmación por email, agenda con prevención de solapamientos por profesional y por sillón, asistencia y auditoría. Es el trabajo final de Desarrollo de Software (UTN) de un equipo de 4.

---

## Stack Tecnológico

Decidido el 2026-11-23 (DD-08) y actualizado el 2026-11-24 (DD-09: MySQL 8 sin Docker).

| Capa | Tecnología | Estado |
|------|------------|--------|
| Frontend (web, mobile-first) | React + Vite + TypeScript (SPA única para reserva pública y panel interno), React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS; Vitest + Testing Library; E2E con Playwright | Decidido |
| Backend / API | Java 21 + Spring Boot 3 (Web, Security, Data JPA, Validation, Mail), Maven; REST + OpenAPI (springdoc-openapi); JUnit 5, Mockito, MockMvc; perfil `test` con H2 `MODE=MySQL` y perfil `it-mysql` contra MySQL real | Decidido |
| Base de datos | MySQL 8 (InnoDB) nativo + Flyway; anti-solapamiento con bloqueo pesimista del profesional y después del sillón (DD-09) | Decidido |
| Email transaccional | SMTP genérico vía Spring Mail; proveedor gratuito (Brevo o Resend) a elegir en C-14 | Proveedor pendiente |
| Tareas programadas | Operación idempotente disparada por cron externo; `@Scheduled` en local (se decide en C-18) | Propuesto |
| Hosting | Render (API), Vercel (frontend), planes gratuitos; proveedor de MySQL a elegir en C-19 (PA-14); desarrollo local nativo, sin contenedores; CI con GitHub Actions | API y frontend decididos; base pendiente |

El esquema de autenticación (cookie de sesión o JWT) se decide en C-04, porque el frontend y la API quedan en dominios distintos.

Detalle completo: [knowledge-base/02_descripcion_general.md](knowledge-base/02_descripcion_general.md), [knowledge-base/08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) y [knowledge-base/09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) (DD-09, DD-10).

---

## Base de Conocimiento

La fuente de verdad del dominio vive en `knowledge-base/`. **Leé el archivo relevante ANTES de implementar.**

| Archivo | Cuándo leerlo |
|---------|---------------|
| [01_vision_y_objetivos.md](knowledge-base/01_vision_y_objetivos.md) | Propósito, alcance del MVP y lo que queda afuera |
| [03_actores_y_roles.md](knowledge-base/03_actores_y_roles.md) | Auth, matriz RBAC, rutas públicas |
| [04_modelo_de_datos.md](knowledge-base/04_modelo_de_datos.md) | Entidades, relaciones, migraciones, seed |
| [05_reglas_de_negocio.md](knowledge-base/05_reglas_de_negocio.md) | Reglas RN-XX (solapamientos, estados, cancelación, auditoría) |
| [06_funcionalidades.md](knowledge-base/06_funcionalidades.md) | Historias de usuario por épica |
| [07_flujos_principales.md](knowledge-base/07_flujos_principales.md) | Flujos de punta a punta |
| [08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) | Patrones, estructura del monorepo, seguridad, variables de entorno |
| [09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) | Decisiones DD-XX y supuestos SU-XX |
| [10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md) | ⚠️ Preguntas abiertas a resolver ANTES de codear |

> ⚠️ Resolver las preguntas abiertas de `10_preguntas_abiertas.md` antes de arrancar el change que bloquean (hoy no queda ninguna de prioridad Alta; PA-14, hosting de MySQL, se decide en C-19).

---

## Skills Disponibles

| Agente | Rol | Skills que carga |
|--------|-----|------------------|
| **Backend** | Spring Boot: dominio, casos de uso, REST, seguridad | `springboot-patterns`, `java-springboot`, `spring-boot-rest-api-standards`, `spring-boot-openapi-documentation`, `springboot-security`, `spring-boot-security-jwt`, `rest-api-design` |
| **Backend — tests** | TDD en JUnit 5 / Mockito / MockMvc; H2 `MODE=MySQL` y perfil `it-mysql` | `springboot-tdd`, `spring-boot-test-patterns` |
| **Datos** | Esquema MySQL 8 compatible con H2 `MODE=MySQL`, índices, migraciones Flyway, bloqueo pesimista | `database-schema-designer` |
| **Frontend** | React + Vite + TS, Tailwind, UI/UX, accesibilidad | `typescript-best-practices`, `modern-web-development`, `frontend-design-ui-ux`, `web-accessibility` |
| **E2E / QA** | Playwright sobre el flujo de reserva | `playwright-best-practices`, `webapp-testing`, `strict-tdd` (su hook solo cubre `src/**/*.ts` con jest) |
| **Git** | Ramas, commits, PR | `git-workflow-and-versioning` |
| **Spec / orquestación** | OpenSpec y fundación | `openspec-explore`, `openspec-propose`, `openspec-apply-change`, `openspec-archive-change`, `kb-creator`, `roadmap-generator`, `agent-instruction`, `skill-registry` |

Cargá la skill correspondiente al contexto ANTES de escribir código. Sin skill dedicada (aplicar las reglas generales y la KB): MySQL 8 / Flyway, TanStack Query, React Hook Form + Zod, Tailwind, Vitest / Testing Library, GitHub Actions.

> Las reglas compactas de cada skill las resuelve el orquestador desde [.atl/skill-registry.md](.atl/skill-registry.md) (generado por `skill-registry` y versionado en el repo para todo el equipo; incluye cómo instalar las skills que falten). Esta tabla solo mapea skill→rol.

---

## Roadmap de Changes

El plan de implementación completo está en [CHANGES.md](CHANGES.md). Resumen:

- **Total**: 24 changes en 6 fases. MVP = C-01 a C-19; diferenciales opcionales = C-20 a C-24 (fase 5).
- **Entrega**: jueves 2026-11-26, 12:00 (DD-10). Corte de entrega: C-01 a C-04, C-06 a C-10, C-14 a C-17 y C-19 (columna `Corte 26/11` de `CHANGES.md`). Si no llega completo, prioridad C-09 + C-10.
- **Reparto y protocolo del equipo**: [docs/plan-trabajo-equipo.md](docs/plan-trabajo-equipo.md).
- **Camino crítico** (10): `C-01 → C-02 → C-03 → C-04 → C-08 → C-09 → C-10 → C-16 → C-17 → C-19`.
- **Primer change elegible**: `C-02` (`scaffolding-proyecto`). C-01 cerró el stack (DD-09), el plazo (DD-10) y PA-03, PA-05, PA-06 y PA-10.
- **Governance CRÍTICO**: C-03, C-04, C-08, C-09, C-10, C-12. **ALTO**: C-14 a C-18 (incluye privacidad Ley 25.326 en C-15).

**Antes de iniciar un change**: leé [CHANGES.md](CHANGES.md), identificá sus dependencias, el campo "Bloqueado por" y los archivos de "Leer antes".

---

## Reglas Duras (específicas del proyecto)

> Reglas **globales** ya definidas en el archivo global de instrucciones del cliente activo (orquestador OPSX, governance, TDD estricto, engram): el proyecto las **hereda**, no se repiten acá. Acá viven solo las reglas **específicas de este proyecto** + las universales que el global no cubre.

Son contrato; romperlas es un defecto.

### Git

- NUNCA commitear ni pushear sin pedido explícito → proponer el commit (mensaje y archivos) y esperar.
- NUNCA mensajes de commit libres → Conventional Commits en castellano (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`).
- NUNCA trabajar directo sobre `main` → una rama por change de `CHANGES.md` con el formato `feat/C-XX-nombre-corto` (p. ej. `feat/C-04-auth-usuarios-roles`) y PR a `main` aprobado según `docs/plan-trabajo-equipo.md`.
- NUNCA agregar la línea `Co-Authored-By` de un asistente de IA en los commits ni en las PR.

### Backend — datos y tests

- NUNCA cambiar el esquema fuera de Flyway ni editar una migración ya mergeada → crear una migración nueva `V0NN_<n>__descripcion.sql` en `backend/src/main/resources/db/migration/`.
- NUNCA escribir turnos por fuera del servicio transaccional de C-09 ni confiar solo en la UI para evitar solapamientos → `SELECT ... FOR UPDATE` sobre el profesional y después sobre el sillón (siempre en ese orden), verificación leyendo el último estado confirmado, y test de concurrencia `@Tag("mysql")` en el perfil `it-mysql` (DD-09).
- NUNCA usar Docker, `docker-compose` ni Testcontainers, ni mocks de la base en tests de persistencia → H2 en memoria `MODE=MySQL;DATABASE_TO_LOWER=TRUE` (perfil `test`); la concurrencia solo cuenta como verificada contra MySQL 8 real (perfil `it-mysql`).
- NUNCA usar sintaxis exclusiva de un motor en las migraciones → SQL válido en MySQL 8 y en H2 `MODE=MySQL` (`BIGINT AUTO_INCREMENT`, `DATETIME(6)`, `VARCHAR` + `CHECK`), con versión `V0NN_<n>__descripcion.sql` por change.
- NUNCA usar `LocalDateTime` ni la hora del servidor para turnos → `DATETIME(6)` guardado en UTC y mapeado a `Instant` (u `OffsetDateTime`), interpretado en `America/Argentina/Buenos_Aires`.

### Backend — seguridad y dominio

- NUNCA autorizar solo en la UI → verificar el rol en el backend (Spring Security) en cada endpoint.
- NUNCA exponer entidades JPA en la API → DTOs (records) con Bean Validation en todo input.
- NUNCA cambiar el estado de un turno por fuera de la máquina de estados → cada cambio registra quién y cuándo (auditoría).
- NUNCA poner datos personales reales en seeds, fixtures o logs, ni secretos en el repo (Ley 25.326) → datos ficticios y variables de entorno.
- NUNCA importar Spring ni JPA en el paquete `dominio/` → reglas de negocio en Java puro, testeables sin contexto de Spring.

### Frontend

- NUNCA usar `any` → `tsconfig` en modo `strict`, tipar todo; componentes en PascalCase.
- NUNCA hacer `fetch` suelto en componentes → cliente API en `frontend/src/lib/` + TanStack Query; formularios con React Hook Form + Zod.
- La reserva pública se diseña primero para celular (mobile-first) y cumple WCAG 2.1 AA.

---

## Flujo de Trabajo

```
1. Leer la KB relevante (knowledge-base/)        → entender el dominio
2. Identificar el change en CHANGES.md           → respetar dependencias y "Bloqueado por"
3. Acordar alcance y criterios de aceptación     → /opsx:propose
4. Implementar con TDD estricto y verificar      → /opsx:apply, en la rama del change
5. Registrar la evidencia y actualizar el estado en CHANGES.md → /opsx:archive
```

Aplicar TODAS las reglas duras en cada paso. Ante conflicto entre la KB y este archivo, las reglas duras prevalecen.
<!-- /active-stack:agent-instruction -->
