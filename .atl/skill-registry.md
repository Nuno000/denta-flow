# Registro de Skills

Este registro está versionado en el repositorio para que todo el equipo comparta el mismo mapeo skill→rol y las mismas reglas compactas. Cualquier agente que lance sub-agentes lo lee para resolver las reglas compactas y las inyecta directamente en los prompts de los sub-agentes. Los sub-agentes NO leen este registro ni los SKILL.md individuales.

Las rutas de las skills globales son relativas al home de cada persona (`~`). Para regenerarlo, ejecutar la skill `skill-registry` y luego volver a aplicar este formato (en español, sin rutas absolutas).

Proyecto: denta-flow (DentaFlow). Actualizado el 2026-11-24 (adaptado a entorno local liviano sin Docker).

## Stack del proyecto (DD-08 adaptado sin Docker)

Java 21 + Spring Boot 3 (Maven), Spring Data JPA / Hibernate, PostgreSQL 16 + Flyway (Neon en despliegue; H2 en modo PostgreSQL o PostgreSQL nativo en desarrollo y tests locales sin Docker), JUnit 5 / Mockito / MockMvc; React + Vite + TypeScript, TanStack Query, RHF + Zod, Tailwind, Vitest / Testing Library, Playwright; GitHub Actions. Desarrollo guiado por specs con OpenSpec; TDD estricto.

Restricción de entorno local: **NO usar Docker, `docker-compose` ni `Testcontainers`** por limitación de memoria RAM en desarrollo local.

Brechas de cobertura del stack (no hay skill dedicada instalada): migraciones Flyway multi-motor (H2/PostgreSQL), TanStack Query, RHF + Zod, Tailwind, Vitest / Testing Library, GitHub Actions. Aplicar las reglas generales de abajo y los archivos de convenciones del proyecto; no asumir que una skill los cubre.

## Relevancia por stack

Mapeo de relevancia para los delegadores. Las skills marcadas NO RELEVANTE se indexan por completitud y no deben asociarse a las tareas de este proyecto.

| Skill | Relevancia | Nota |
|-------|------------|------|
| java-spring-boot, java-springboot, springboot-patterns, springboot-tdd, springboot-security, spring-boot-test-patterns, spring-boot-rest-api-standards, spring-boot-security-jwt, spring-boot-openapi-documentation, spring-boot-cache | RELEVANTE (backend) | Backend Spring Boot 3 + JPA/Hibernate (ejecución nativa sin contenedores) |
| spring-boot-resilience4j | CONDICIONAL | Solo cuando una llamada externa (ej. SMTP) necesite retry / circuit breaker |
| spring-boot-event-driven-patterns | CONDICIONAL | Solo para eventos de dominio locales (`ApplicationEventPublisher`); Kafka no está en DD-08 |
| postgresql-table-design, postgres-patterns, database-schema-designer | RELEVANTE (datos) | Esquema PostgreSQL 16 + Flyway (Neon / Postgres nativo) y compatibilidad con perfil local H2 |
| rest-api-design | RELEVANTE (API) | Diseño de endpoints REST |
| typescript-best-practices | RELEVANTE (frontend) | Código TypeScript (.ts / .tsx) |
| playwright-best-practices, webapp-testing | RELEVANTE (e2e) | Tests E2E con Playwright contra servidor local nativo |
| web-accessibility, frontend-design-ui-ux | RELEVANTE (UI) | Accesibilidad de UI y specs de diseño |
| modern-web-development | PARCIAL | Solo guías genéricas de TS / React / Tailwind / Zod; ignorar las partes de Next.js 14, Supabase y GraphQL |
| git-workflow-and-versioning | RELEVANTE (proceso) | Commits, ramas, PRs |
| strict-tdd, springboot-tdd | RELEVANTE (proceso) | Modo TDD estricto sin contenedores. NOTA: el hook de strict-tdd apunta a `src/**/*.ts` solo con jest, por lo que NO fuerza TDD en el backend Java. La disciplina TDD en Java debe aplicarse manualmente con springboot-tdd y las Tres Leyes |
| openspec-explore, openspec-propose, openspec-apply-change, openspec-update-change, openspec-sync-specs, openspec-archive-change | RELEVANTE (proceso) | Flujo de trabajo OpenSpec |
| docker-patterns, docker-compose-orchestration | NO RELEVANTE | Descartadas por restricción de memoria local (no se usa Docker ni docker-compose) |
| supabase-postgres-best-practices | NO RELEVANTE | Específica de Supabase; el proyecto usa PostgreSQL 16 / Neon + H2 local |
| upstash-redis-js | NO RELEVANTE | No hay Redis en el stack |
| idempotent-financial-workflows | NO RELEVANTE | Fuera del alcance MVP (DD-01) |
| notebooklm | NO RELEVANTE | API de Google NotebookLM |
| active-orchestrator, agent-instruction, kb-creator, roadmap-generator, discovery-research, web-scraper, find-skills, skill-creator | ORQUESTACIÓN / META | Skills de fundación y herramientas, no se asocian a tareas de implementación |

## Skills del proyecto

Skills a nivel de proyecto (`.claude/skills`, `.agents/skills` en este repo). Estas tienen precedencia sobre las skills de OpenSpec de usuario con el mismo nombre.

| Trigger | Skill | Ruta |
|---------|-------|------|
| Entrar al modo explore de OpenSpec; "openspec explore", "opsx explore" | openspec-explore | .claude/skills/openspec-explore/SKILL.md |
| Proponer un nuevo change de OpenSpec con todos los artefactos; "openspec propose", "opsx propose" | openspec-propose | .claude/skills/openspec-propose/SKILL.md |
| Implementar las tareas de un change de OpenSpec; "openspec apply", "opsx apply" | openspec-apply-change | .claude/skills/openspec-apply-change/SKILL.md |
| Revisar los artefactos de planificación de un change; "openspec update change", "opsx update" | openspec-update-change | .claude/skills/openspec-update-change/SKILL.md |
| Sincronizar delta specs con las specs principales; "openspec sync", "opsx sync" | openspec-sync-specs | .claude/skills/openspec-sync-specs/SKILL.md |
| Archivar un change completado; "openspec archive", "opsx archive" | openspec-archive-change | .claude/skills/openspec-archive-change/SKILL.md |

## Reglas compactas

Reglas pre-digeridas por skill. Los delegadores copian los bloques que correspondan en los prompts de los sub-agentes como `## Project Standards (auto-resolved)`. Copiar solo los bloques marcados RELEVANTE (o CONDICIONAL cuando la tarea lo necesite) para la tarea en curso.

### java-spring-boot
- Inyección por constructor; dependencias `private final`; no usar inyección por campo
- Los controllers devuelven DTOs (records), nunca entidades JPA; `@Valid @RequestBody` para la entrada
- Errores vía `@RestControllerAdvice` con cuerpo estandarizado; 201 + Location al crear, 204 al borrar
- `@Transactional` en métodos de servicio con el alcance más acotado; servicios sin estado
- Repositorios Spring Data JPA; usar `@Query` o proyecciones para lecturas complejas
- Secretos desde variables de entorno, nunca hardcodeados; BCrypt para contraseñas

### java-springboot
- `pom.xml` de Maven; usar starters; organizar paquetes por feature/dominio, no por capa
- Inyección por constructor con `private final`; `@ConfigurationProperties` para configuración tipada
- Perfiles vía `application-{env}.yml` (`dev` con H2 en modo PostgreSQL o Neon; `prod` con PostgreSQL 16); secretos nunca en el código fuente
- No exponer entidades JPA; usar DTOs con Bean Validation
- SLF4J con logging parametrizado (`logger.info("x {}", id)`), nunca concatenación de strings
- Tests: unitarios con JUnit 5 + Mockito, slices `@WebMvcTest` / `@DataJpaTest` con H2 (`MODE=PostgreSQL`), sin Docker ni Testcontainers

### springboot-patterns
- Capas: controller -> service -> repository; los controllers se mantienen delgados
- Usar `ResponseEntity` con status explícito; paginación vía `Pageable` y `Page.map(Dto::from)`
- Los métodos de servicio que escriben son `@Transactional`; usar `@Query` para lecturas filtradas
- Usar records de Java para DTOs de request/response con anotaciones de validación Jakarta
- Usar perfiles para la configuración de `dev` / `test` / `prod`
- El caché y el trabajo asíncrono pasan por abstracciones de Spring, no por threads ad hoc

### springboot-tdd
- Escribir primero el test que falla, luego el código mínimo, luego refactorizar con tests en verde
- Unitarios: `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`, estructura AAA
- Evitar mocks parciales; usar stubbing explícito; `@ParameterizedTest` para variantes
- Capa web: `@WebMvcTest` con `@MockitoBean` y aserciones `jsonPath` de MockMvc
- Persistencia e integración: `@DataJpaTest` y `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` usando H2 en memoria (`MODE=PostgreSQL`), sin Testcontainers
- Exigir cobertura con JaCoCo (objetivo 80%+)
- Java NO tiene enforcement por hook de strict-tdd; aplicar las Tres Leyes manualmente

### springboot-security
- JWT sin estado o tokens opacos; validar en `OncePerRequestFilter` o resource server
- Cookies para sesiones: `HttpOnly`, `Secure`, `SameSite=Strict`
- Habilitar `@EnableMethodSecurity`; usar `@PreAuthorize`; denegar por defecto
- Bean Validation (`@Valid`, `@NotBlank`, `@Size`) en todos los DTOs; sanitizar HTML antes de renderizar
- Secretos vía entorno; limitar la tasa de los endpoints de auth; escanear dependencias por CVEs
- Configurar headers de seguridad y una política CORS explícita

### spring-boot-test-patterns
- Pirámide liviana sin Docker: unitarios (`@ExtendWith(MockitoExtension.class)`, <50ms), slices (`@DataJpaTest` con H2 `MODE=PostgreSQL`, `@WebMvcTest`), integración (`@SpringBootTest` con perfil `test`)
- No usar `@Testcontainers` ni `@ServiceConnection` con contenedores
- Preferir slices antes que el contexto completo; reservar `@SpringBootTest` para el cableado end-to-end
- Aserciones con AssertJ (`assertThat`)

### spring-boot-rest-api-standards
- Sustantivos en plural en las URLs; sin verbos (`/turnos`, no `/getTurnos`)
- Métodos: GET seguro; POST crea (201 + Location); PUT reemplaza; PATCH parcial; DELETE 204
- Códigos de estado: 400 inválido, 404 inexistente, 409 duplicado/solapado, 500 inesperado
- DTOs como records inmutables, separados de las entidades de dominio
- Errores globales vía `@RestControllerAdvice` con status, error, message, timestamp
- Pageable con page, size, sort; devolver metadatos de totales
- Configurar CORS y headers de seguridad de forma explícita

### spring-boot-security-jwt
- Dependencias: spring-boot-starter-security, oauth2-resource-server, JJWT 0.12.6 (api, impl, jackson)
- `jwt.secret` desde `${AUTH_SECRET}`, mínimo 256 bits, nunca hardcodeado
- Access token ~15 min; refresh token ~7 días; validar el issuer en cada token
- Autorización con `@PreAuthorize` (roles `ADMIN`, `RECEPCION`, `PROFESIONAL`)

### spring-boot-openapi-documentation
- Usar `springdoc-openapi-starter-webmvc-ui` para Spring MVC
- Docs en `/v3/api-docs`, UI en `/swagger-ui/index.html`
- Anotar con `@Tag`, `@Operation`, `@ApiResponse`, `@Schema`, `@SecurityRequirement`
- Paginación con `@ParameterObject` para Pageable
- Documentar las respuestas de error y los esquemas de seguridad

### spring-boot-cache
- Agregar `spring-boot-starter-cache` más un proveedor en memoria (Caffeine; sin Redis)
- `@EnableCaching` más un bean `CacheManager`
- `@Cacheable` para lecturas, `@CachePut` para escrituras, `@CacheEvict` para borrados

### postgresql-table-design
- Tablas con PK: `BIGINT GENERATED ALWAYS AS IDENTITY` o `UUID` estándar ANSI para compatibilidad H2/PostgreSQL
- Normalizar a 3FN; desnormalizar solo para lecturas medidas
- Tiempo: `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`); texto: `TEXT` / `VARCHAR`; evitar tipos propietarios innecesarios en migraciones compartidas
- Indexar manualmente las columnas FK
- Aislar extensiones y restricciones exclusivas de PostgreSQL (`btree_gist`, `EXCLUDE USING gist` sobre `tstzrange`) en `db/migration/postgresql` para que las migraciones base (`db/migration/common`) corran también en H2 local

### postgres-patterns
- Tipos de índice: B-tree por defecto; GiST para rangos horarios `tstzrange` en PostgreSQL 16
- Orden de índice compuesto: primero columnas de igualdad, después columnas de rango
- Mantener las transacciones cortas; usar solo consultas parametrizadas

### database-schema-designer
- Normalizar a 3FN para OLTP; desnormalizar solo con evidencia
- Toda tabla tiene clave primaria; FKs indexadas; NOT NULL y defaults razonables
- Usar `TIMESTAMP WITH TIME ZONE` para tiempo
- Validar el diseño contra el checklist de verificación antes de implementar

### rest-api-design
- Recursos en sustantivos, plural y minúsculas; anidar solo un nivel por pertenencia
- Usar los métodos HTTP según su semántica; sin verbos en las rutas
- Códigos de estado consistentes; errores con una única forma estable
- Documentar cada endpoint (request, response, errores)

### typescript-best-practices
- Modelar variantes con uniones discriminadas (literal `kind`); sin bolsas de campos opcionales
- Usar `unknown` para datos externos; sin casts con `as` salvo después de validar
- Parsear en los bordes con un schema (Zod) y luego confiar en los tipos internos
- Usar `satisfies` en lugar de `as`; exhaustividad vía `const _exhaustive: never = x`
- Derivar tipos con `Pick`, `Omit`, `ReturnType`, `z.infer` antes de declarar interfaces nuevas
- Sin `console.log` en código entregado; usar logging estructurado

### modern-web-development (PARCIAL)
- Usar interfaces de TypeScript; componentes funcionales; retornos tempranos y guard clauses
- Zod + React Hook Form para validación de formularios
- Tailwind CSS para estilos; diseño responsive mobile-first
- Minimizar `useEffect` y `setState`; preferir datos derivados
- Ignorar las reglas específicas de Next.js, Supabase, GraphQL y Shadcn (no están en DD-08)

### playwright-best-practices
- Usar Page Object Model para la estructura; mantener los selectores en roles / etiquetas visibles al usuario
- Usar aserciones web-first con auto-espera; evitar sleeps fijos
- Aislar los datos de test con fixtures; resetear el estado entre tests contra el servidor local nativo
- Ejecutar en CI con reintentos solo para casos inestables conocidos; capturar traces ante fallos

### webapp-testing
- Usar Playwright; lanzar chromium en headless contra el frontend/backend local nativo
- Esperar `networkidle` antes de hacer aserciones en páginas dinámicas
- Usar capturas de pantalla y logs de consola para depurar el comportamiento de la UI

### web-accessibility
- Clasificar primero la superficie que falla: semántica, teclado/foco, etiquetas/anuncios, reflow/contraste
- Separar los hallazgos automatizados (axe, Lighthouse) de las verificaciones manuales
- Mantener el comportamiento del foco explícito en apps con ruteo y modales

### frontend-design-ui-ux
- Skill solo de spec: producir `.ulpi/design/DESIGN.md` y la spec de la feature; nunca escribir código de UI de producción
- Comprometerse con una única estética distintiva antes de especificar pantallas; diseño mobile-first para la reserva pública

### git-workflow-and-versioning
- Trunk-based: mantener `main` desplegable; ramas de vida corta (1-3 días)
- Commitear temprano y seguido; cada commit es un cambio lógico y atómico
- El mensaje del commit y el PR deben describir el porqué; sin commits gigantes
- Nunca hacer force-push en ramas compartidas; pre-commit con tests en verde

### strict-tdd
- Red-Green-Refactor: sin código de producción sin un test que falle primero
- Triangular con al menos dos casos por comportamiento (camino feliz + caso borde)
- El hook apunta a `src/**/*.ts` solo con jest; NO se aplica al backend Java
- Aplicar las Tres Leyes manualmente al código Spring Boot vía springboot-tdd

### openspec-explore, openspec-propose, openspec-apply-change, openspec-update-change, openspec-sync-specs, openspec-archive-change
- `openspec-explore`: solo lectura e investigación; nunca escribir código ni artefactos sin confirmación explícita
- `openspec-propose`: generar `proposal.md`, delta `specs/`, `design.md` y `tasks.md` sin Docker ni Testcontainers; nunca implementar código en esta fase
- `openspec-apply-change`: implementar tarea por tarea siguiendo TDD estricto y marcar `- [x]` al finalizar cada una
- `openspec-update-change`: mantener coherentes los artefactos de planificación existentes ante cambios de diseño
- `openspec-sync-specs`: merge aditivo de delta specs hacia `openspec/specs/` preservando rutas de capability
- `openspec-archive-change`: verificar tareas completas y sincronizar specs antes de archivar

### docker-patterns, docker-compose-orchestration (NO RELEVANTE)
- Descartadas por restricción de hardware/memoria en desarrollo local. No crear `Dockerfile`, `docker-compose.yml` ni dependencias de `Testcontainers`.

### supabase-postgres-best-practices, upstash-redis-js, idempotent-financial-workflows, notebooklm (NO RELEVANTE)
- No aplican al stack ni al alcance MVP del proyecto.

### active-orchestrator, agent-instruction, kb-creator, roadmap-generator, discovery-research, web-scraper, find-skills, skill-creator (ORQUESTACIÓN / META)
- Skills de fundación y herramientas. No se inyectan en sub-agentes de implementación.

## Convenciones del proyecto

| Archivo | Ruta | Notas |
|---------|------|-------|
| Instrucciones de agentes | CLAUDE.md / AGENTS.md | Reglas del proyecto, arquitectura, restricciones y comandos locales |
| Config de OpenSpec | openspec/config.yaml | Contexto del stack sin Docker y reglas por artefacto |
| Índice de la base de conocimiento | knowledge-base/README.md | Índice de los 10 archivos de la KB (01 a 10) |
| Decisiones (stack DD-08) | knowledge-base/09_decisiones_y_supuestos.md | Fuente de decisiones arquitectónicas |
| Arquitectura | knowledge-base/08_arquitectura_propuesta.md | Capas, patrones, seguridad y variables de entorno |
| Reglas de negocio | knowledge-base/05_reglas_de_negocio.md | Reglas `RN-XX` por dominio |
| Roadmap de changes | CHANGES.md | Ítems de trabajo `C-01` a `C-22` |