# Registro de Skills

Este registro está versionado en el repositorio para que todo el equipo comparta el mismo mapeo skill→rol y las mismas reglas compactas. Cualquier agente que lance sub-agentes lo lee para resolver las reglas compactas y las inyecta directamente en los prompts de los sub-agentes. Los sub-agentes NO leen este registro ni los SKILL.md individuales.

Las rutas de las skills globales son relativas al home de cada persona (`~`). Para regenerarlo, ejecutar la skill `skill-registry` y luego volver a aplicar este formato (en español, sin rutas absolutas).

Proyecto: denta-flow (DentiFlow). Generado el 2026-11-23 con la skill `skill-registry` (variante `index`).

## Stack del proyecto (DD-08)

Java 21 + Spring Boot 3 (Maven), PostgreSQL 16 + Flyway, JUnit 5 / Mockito / Testcontainers; React + Vite + TypeScript, TanStack Query, RHF + Zod, Tailwind, Vitest / Testing Library, Playwright; docker-compose; GitHub Actions. Desarrollo guiado por specs con OpenSpec; TDD estricto.

Brechas de cobertura del stack (no hay skill dedicada instalada): migraciones Flyway, TanStack Query, RHF + Zod, Tailwind, Vitest / Testing Library, GitHub Actions. Aplicar las reglas generales de abajo y los archivos de convenciones del proyecto; no asumir que una skill los cubre.

## Relevancia por stack

Mapeo de relevancia para los delegadores. Las skills marcadas NO RELEVANTE se indexan por completitud y no deben asociarse a las tareas de este proyecto.

| Skill | Relevancia | Nota |
|-------|------------|------|
| java-spring-boot, java-springboot, springboot-patterns, springboot-tdd, springboot-security, spring-boot-test-patterns, spring-boot-rest-api-standards, spring-boot-security-jwt, spring-boot-openapi-documentation, spring-boot-cache | RELEVANTE (backend) | Backend Spring Boot 3 |
| spring-boot-resilience4j | CONDICIONAL | Solo cuando una llamada entre servicios necesita circuit breaker / retry |
| spring-boot-event-driven-patterns | CONDICIONAL | Solo para eventos de dominio / outbox; Kafka no está en DD-08 |
| postgresql-table-design, postgres-patterns, database-schema-designer | RELEVANTE (datos) | Diseño de esquema PostgreSQL 16, índices, restricciones |
| docker-patterns, docker-compose-orchestration | RELEVANTE (infra) | docker-compose para el stack local |
| rest-api-design | RELEVANTE (API) | Diseño de endpoints REST |
| typescript-best-practices | RELEVANTE (frontend) | Código TypeScript (.ts / .tsx) |
| playwright-best-practices, webapp-testing | RELEVANTE (e2e) | Tests E2E con Playwright |
| web-accessibility, frontend-design-ui-ux | RELEVANTE (UI) | Accesibilidad de UI y specs de diseño |
| modern-web-development | PARCIAL | Solo guías genéricas de TS / React / Tailwind / Zod; ignorar las partes de Next.js 14, Supabase y GraphQL |
| git-workflow-and-versioning | RELEVANTE (proceso) | Commits, ramas, PRs |
| strict-tdd, springboot-tdd | RELEVANTE (proceso) | Modo TDD estricto. NOTA: el hook de strict-tdd apunta a `src/**/*.ts` solo con jest, por lo que NO fuerza TDD en el backend Java. La disciplina TDD en Java debe aplicarse manualmente con springboot-tdd y las Tres Leyes |
| openspec-explore, openspec-propose, openspec-apply-change, openspec-update-change, openspec-sync-specs, openspec-archive-change | RELEVANTE (proceso) | Flujo de trabajo OpenSpec |
| supabase-postgres-best-practices | NO RELEVANTE | Específica de Supabase; el proyecto usa PostgreSQL plano + Flyway |
| upstash-redis-js | NO RELEVANTE | No hay Redis en el stack |
| idempotent-financial-workflows | NO RELEVANTE | No se asocia a las tareas de este proyecto |
| notebooklm | NO RELEVANTE | API de Google NotebookLM |
| active-orchestrator, agent-instruction, kb-creator, roadmap-generator, discovery-research, web-scraper, find-skills, skill-creator | ORQUESTACIÓN / META | Skills de fundación y herramientas, no se asocian a tareas de implementación |

## Skills de usuario (globales)

Alcance: global. Las rutas son relativas al home de cada persona (`~`). Las que están en `~/.claude/skills` como symlinks resuelven a `~/.agents/skills`; se muestra la ruta resuelta. Deduplicadas por nombre. La columna "Instalación" indica cómo instalar la skill en otra máquina (`—` = fuente desconocida; "Active Stack" = viene con el instalador de Active Stack).

| Trigger | Skill | Ruta | Instalación |
|---------|-------|------|-------------|
| Recomendar una ruta directa o coordinar la fundación del proyecto; /active-orchestrator init, iniciar o retomar la fundación | active-orchestrator | ~/.claude/skills/active-orchestrator/SKILL.md | Active Stack |
| Generar AGENTS.md / CLAUDE.md a partir de la KB y el roadmap; "generar claude.md", "instrucciones para los agentes" | agent-instruction | ~/.claude/skills/agent-instruction/SKILL.md | Active Stack |
| Diseñar esquemas SQL/NoSQL robustos; "design schema", "create tables", "database design" | database-schema-designer | ~/.agents/skills/database-schema-designer/SKILL.md | `npx skills add softaworks/agent-toolkit@database-schema-designer -g -y` |
| Fase de Discovery/Investigación previa a la KB; investigación de competidores | discovery-research | ~/.claude/skills/discovery-research/SKILL.md | Active Stack |
| Encontrar e instalar skills de agentes; "find a skill for X", "is there a skill that can..." | find-skills | ~/.claude/skills/find-skill/SKILL.md | Active Stack |
| Lenguaje de diseño distintivo y bloqueado y spec UX/UI en .ulpi/design/ (solo spec, nunca código de UI de producción) | frontend-design-ui-ux | ~/.agents/skills/frontend-design-ui-ux/SKILL.md | `npx skills add ulpi-io/skills@frontend-design-ui-ux -g -y` |
| Flujo de trabajo git: commits, ramas, conflictos, PRs, versionado, changelog | git-workflow-and-versioning | ~/.agents/skills/git-workflow-and-versioning/SKILL.md | `npx skills add addyosmani/agent-skills@git-workflow-and-versioning -g -y` |
| Construir una KB de proyecto de 10 archivos en knowledge-base/; "crear KB", "documentar proyecto" | kb-creator | ~/.claude/skills/kb-creator/SKILL.md | Active Stack |
| API de Google NotebookLM; /notebooklm, "create a podcast about X" | notebooklm | ~/.claude/skills/notebooklm/SKILL.md | Active Stack |
| Diseñar APIs RESTful: modelado de recursos, métodos, códigos de estado, versionado | rest-api-design | ~/.agents/skills/rest-api-design/SKILL.md | `npx skills add aj-geddes/useful-ai-prompts@rest-api-design -g -y` |
| Generar el roadmap CHANGES.md de ítems de trabajo; "armar roadmap" | roadmap-generator | ~/.claude/skills/roadmap-generator/SKILL.md | Active Stack |
| Crear o mejorar skills, ejecutar evals, optimizar descripciones de skills | skill-creator | ~/.claude/skills/skill-creator/SKILL.md | Active Stack |
| Forzar TDD Red-Green-Refactor; /strict-tdd | strict-tdd | ~/.agents/skills/strict-tdd/SKILL.md | `npx skills add proffesor-for-testing/agentic-qe@strict-tdd -g -y` |
| Remediación y verificación de accesibilidad web (WCAG, axe, Lighthouse, teclado, foco, etiquetas) | web-accessibility | ~/.agents/skills/web-accessibility/SKILL.md | `npx skills add akillness/jeo-skills@web-accessibility -g -y` |
| Investigar competidores a partir de una URL en notas con esquema fijo | web-scraper | ~/.claude/skills/web-scraper/SKILL.md | Active Stack |
| Orquestación multi-contenedor con Docker Compose, redes, volúmenes | docker-compose-orchestration | ~/.agents/skills/docker-compose-orchestration/SKILL.md | — |
| Patrones de Docker y Compose, seguridad de contenedores, orquestación multi-servicio | docker-patterns | ~/.agents/skills/docker-patterns/SKILL.md | — |
| Flujos financieros seguros ante reintentos (pagos, payouts, webhooks, outbox) | idempotent-financial-workflows | ~/.agents/skills/idempotent-financial-workflows/SKILL.md | — |
| Construir apps Spring Boot de producción: REST, Security, Data, Actuator | java-spring-boot | ~/.agents/skills/java-spring-boot/SKILL.md | — |
| Buenas prácticas de Spring Boot para el desarrollo | java-springboot | ~/.agents/skills/java-springboot/SKILL.md | — |
| Desarrollo web moderno: TypeScript, Next.js 14, RSC, Supabase, GraphQL, Tailwind | modern-web-development | ~/.agents/skills/modern-web-development/SKILL.md | — |
| Tests con Playwright: POM, tests inestables, CI, mocking, auth, tests de a11y | playwright-best-practices | ~/.agents/skills/playwright-best-practices/SKILL.md | — |
| Optimización de consultas PostgreSQL, diseño de esquema, índices, RLS | postgres-patterns | ~/.agents/skills/postgres-patterns/SKILL.md | — |
| Diseño de tablas específico de PostgreSQL: tipos, restricciones, índices, particionado | postgresql-table-design | ~/.agents/skills/postgresql-table-design/SKILL.md | — |
| Caché en Spring Boot: @Cacheable, @CacheEvict, @CachePut, TTL con Caffeine/Redis | spring-boot-cache | ~/.agents/skills/spring-boot-cache/SKILL.md | — |
| Patrones orientados a eventos: eventos de dominio, @TransactionalEventListener, Kafka, outbox | spring-boot-event-driven-patterns | ~/.agents/skills/spring-boot-event-driven-patterns/SKILL.md | — |
| SpringDoc OpenAPI 3 y Swagger UI en Spring Boot 3 | spring-boot-openapi-documentation | ~/.agents/skills/spring-boot-openapi-documentation/SKILL.md | — |
| Resilience4j: circuit breaker, retry, rate limiter, bulkhead | spring-boot-resilience4j | ~/.agents/skills/spring-boot-resilience4j/SKILL.md | — |
| Estándares de API REST para Spring Boot: DTOs, errores, paginación, headers | spring-boot-rest-api-standards | ~/.agents/skills/spring-boot-rest-api-standards/SKILL.md | — |
| Auth JWT y RBAC para Spring Boot 3.5 con Spring Security 6 | spring-boot-security-jwt | ~/.agents/skills/spring-boot-security-jwt/SKILL.md | — |
| Testing en Spring Boot: JUnit 5, Mockito, slices, Testcontainers | spring-boot-test-patterns | ~/.agents/skills/spring-boot-test-patterns/SKILL.md | — |
| Patrones de arquitectura Spring Boot: REST, servicios en capas, acceso a datos, caché | springboot-patterns | ~/.agents/skills/springboot-patterns/SKILL.md | — |
| Revisión de Spring Security: authn/authz, validación, CSRF, secretos, headers | springboot-security | ~/.agents/skills/springboot-security/SKILL.md | — |
| TDD para Spring Boot con JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo | springboot-tdd | ~/.agents/skills/springboot-tdd/SKILL.md | — |
| Buenas prácticas de Postgres mantenidas por Supabase (específicas de Supabase) | supabase-postgres-best-practices | ~/.agents/skills/supabase-postgres-best-practices/SKILL.md | — |
| Buenas prácticas de TypeScript; leer o editar cualquier archivo .ts o .tsx | typescript-best-practices | ~/.agents/skills/typescript-best-practices/SKILL.md | — |
| Uso del SDK @upstash/redis: caché, sesiones, contadores, colas | upstash-redis-js | ~/.agents/skills/upstash-redis-js/SKILL.md | — |
| Toolkit de Playwright para probar apps web locales, capturas, logs del navegador | webapp-testing | ~/.agents/skills/webapp-testing/SKILL.md | — |

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
- Secretos desde variables de entorno o vault, nunca hardcodeados; BCrypt para contraseñas

### java-springboot
- `pom.xml` de Maven; usar starters; organizar paquetes por feature/dominio, no por capa
- Inyección por constructor con `private final`; `@ConfigurationProperties` para configuración tipada
- Perfiles vía `application-{env}.yml`; secretos nunca en el código fuente
- No exponer entidades JPA; usar DTOs con Bean Validation
- SLF4J con logging parametrizado (`logger.info("x {}", id)`), nunca concatenación de strings
- Tests: unitarios con JUnit 5 + Mockito, slices `@WebMvcTest` / `@DataJpaTest`, Testcontainers para BD real

### springboot-patterns
- Capas: controller -> service -> repository; los controllers se mantienen delgados
- Usar `ResponseEntity` con status explícito; paginación vía `Pageable` y `Page.map(Dto::from)`
- Los métodos de servicio que escriben son `@Transactional`; usar `@Query` para lecturas filtradas
- Usar records de Java para DTOs de request/response con anotaciones de validación Jakarta
- Usar perfiles para la configuración de dev / staging / prod
- El caché y el trabajo asíncrono pasan por abstracciones de Spring, no por threads ad hoc

### springboot-tdd
- Escribir primero el test que falla, luego el código mínimo, luego refactorizar con tests en verde
- Unitarios: `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`, estructura AAA
- Evitar mocks parciales; usar stubbing explícito; `@ParameterizedTest` para variantes
- Capa web: `@WebMvcTest` con `@MockBean` y aserciones `jsonPath` de MockMvc
- Integración: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- Exigir cobertura con JaCoCo (objetivo 80%+)
- Java NO tiene enforcement por hook de strict-tdd; aplicar las Tres Leyes manualmente

### springboot-security
- JWT sin estado o tokens opacos; validar en `OncePerRequestFilter` o resource server
- Cookies para sesiones: `HttpOnly`, `Secure`, `SameSite=Strict`
- Habilitar `@EnableMethodSecurity`; usar `@PreAuthorize`; denegar por defecto
- Bean Validation (`@Valid`, `@NotBlank`, `@Size`) en todos los DTOs; sanitizar HTML antes de renderizar
- Secretos vía entorno o Vault; limitar la tasa de los endpoints de auth; escanear dependencias por CVEs
- Configurar headers de seguridad y una política CORS explícita

### spring-boot-test-patterns
- Pirámide: unitarios (`@ExtendWith(MockitoExtension.class)`, <50ms), slices (`@DataJpaTest`, `@WebMvcTest`), integración (`@SpringBootTest`)
- BD real vía Testcontainers; usar `@ServiceConnection` (Boot 3.5+) o `@DynamicPropertySource`
- Usar `@Testcontainers` para el ciclo de vida de los contenedores
- Preferir slices antes que el contexto completo; reservar `@SpringBootTest` para el cableado end-to-end
- Aserciones con AssertJ (`assertThat`)

### spring-boot-rest-api-standards
- Sustantivos en plural en las URLs; sin verbos (`/users`, no `/getUsers`)
- Métodos: GET seguro; POST crea (201 + Location); PUT reemplaza; PATCH parcial; DELETE 204
- Códigos de estado: 400 inválido, 404 inexistente, 409 duplicado, 500 inesperado
- DTOs como records inmutables, separados de las entidades de dominio
- Errores globales vía `@RestControllerAdvice` con status, error, message, timestamp
- Pageable con page, size, sort; devolver metadatos de totales
- Configurar CORS y headers de seguridad de forma explícita

### spring-boot-security-jwt
- Dependencias: spring-boot-starter-security, oauth2-resource-server, JJWT 0.12.6 (api, impl, jackson)
- `jwt.secret` desde `${JWT_SECRET}`, mínimo 256 bits, nunca hardcodeado
- Access token ~15 min; refresh token ~7 días; validar el issuer en cada token
- Auth con cookies: `HttpOnly` y `Secure` en true en producción
- Revocar o poner en lista negra los tokens al cerrar sesión y en la rotación
- Autorización con `@PreAuthorize` (roles, authorities, `hasPermission`, chequeos por bean)

### spring-boot-openapi-documentation
- Usar `springdoc-openapi-starter-webmvc-ui` para Spring MVC
- Docs en `/v3/api-docs`, UI en `/swagger-ui/index.html`
- Anotar con `@Tag`, `@Operation`, `@ApiResponse`, `@Schema`, `@SecurityRequirement`
- Paginación con `@ParameterObject` para Pageable
- Documentar las respuestas de error y los esquemas de seguridad

### spring-boot-cache
- Agregar `spring-boot-starter-cache` más un proveedor (Caffeine, Redis o Ehcache)
- `@EnableCaching` más un bean `CacheManager`
- `@Cacheable` para lecturas, `@CachePut` para escrituras, `@CacheEvict` para borrados
- Configurar el TTL vía propiedades `spring.cache.*`; armar las keys con SpEL; usar `condition` / `unless`
- Validar con un test de integración (la segunda llamada pega en el caché) y Actuator `/actuator/caches`

### spring-boot-resilience4j (CONDICIONAL)
- Dependencias: resilience4j-spring-boot3 2.2.0, starter-aop, starter-actuator
- Combinar circuit breaker + retry (backoff exponencial) + timeout + fallback donde las llamadas crucen servicios
- Configurar vía propiedades; exponer health y métricas de Actuator
- Usar fallbacks que devuelvan valores por defecto seguros, no un éxito silencioso

### spring-boot-event-driven-patterns (CONDICIONAL)
- Los eventos de dominio extienden un `DomainEvent` base con eventId, occurredAt, correlationId
- Local: `ApplicationEventPublisher`; usar `@TransactionalEventListener(phase = AFTER_COMMIT)`
- Distribuido: Kafka vía `KafkaTemplate` / `@KafkaListener` (Kafka no está en DD-08; confirmar antes de adoptarlo)
- Para confiabilidad usar el transactional outbox (escribir el evento en la misma transacción, publicar después)

### postgresql-table-design
- Las tablas de referencia llevan PK: `BIGINT GENERATED ALWAYS AS IDENTITY` (UUID solo para IDs opacos o distribuidos)
- Normalizar a 3FN; desnormalizar solo para lecturas medidas
- Dinero: `NUMERIC(p,s)`; tiempo: `TIMESTAMPTZ`; texto: `TEXT` + `CHECK`; nunca `timestamp`, `money`, `char(n)`, `varchar(n)`, `serial`
- Indexar manualmente las columnas FK (PostgreSQL no las indexa automáticamente)
- Usar `UNIQUE NULLS NOT DISTINCT` (PG15+) cuando solo se permite un NULL
- Definir `ON DELETE` en cada FK; `DEFERRABLE INITIALLY DEFERRED` para FKs circulares
- Usar `CREATE TYPE ... AS ENUM` solo para conjuntos chicos y estables; si no, TEXT + CHECK o tabla de lookup

### postgres-patterns
- Tipos de índice: B-tree por defecto; GIN para jsonb / full-text; BRIN para rangos de series temporales
- Orden de índice compuesto: primero columnas de igualdad, después columnas de rango
- Usar índices parciales para filas con borrado lógico (`WHERE deleted_at IS NULL`) e `INCLUDE` de cobertura cuando sirva
- Usar IDs `bigint`, `text`, `timestamptz`, `numeric(10,2)` para dinero, `boolean` para flags
- Mantener las transacciones cortas; usar solo consultas parametrizadas

### database-schema-designer
- Normalizar a 3FN para OLTP; desnormalizar solo con evidencia
- Toda tabla tiene clave primaria; FKs indexadas; NOT NULL y defaults razonables
- Usar DECIMAL para dinero, TIMESTAMP WITH TIME ZONE para tiempo
- Agregar índices para caminos de acceso reales; evitar el exceso de índices en tablas con muchas escrituras
- Validar el diseño contra el checklist de verificación antes de implementar

### docker-patterns
- Usar Dockerfiles multi-stage; elegir el target `dev` para desarrollo local
- Fijar las imágenes a una versión (p. ej. `postgres:16-alpine`); agregar healthchecks; usar `depends_on` con `condition: service_healthy`
- Volúmenes nombrados para datos; bind mounts solo para hot reload local
- No exponer puertos innecesarios; usar redes personalizadas
- Secretos vía entorno o Docker secrets, nunca incluidos en las imágenes
- Agregar `.dockerignore`; ejecutar los contenedores como no-root

### docker-compose-orchestration
- Los servicios de Compose se comunican por nombre de servicio en redes de usuario
- Volúmenes nombrados para datos persistentes; usar `depends_on` con condiciones de health para el orden de arranque
- Separar las preocupaciones de dev y prod con archivos de override
- Usar healthchecks en cada servicio con estado (BD, caché)
- Definir redes y volúmenes de forma explícita; evitar `network_mode: host` salvo que sea necesario

### rest-api-design
- Recursos en sustantivos, plural y minúsculas; anidar solo un nivel por pertenencia (`/users/{id}/orders`)
- Usar los métodos HTTP según su semántica; sin verbos en las rutas
- Códigos de estado consistentes; errores con una única forma estable
- Versionar la API explícitamente (prefijo en la URL) cuando se esperen cambios incompatibles
- Documentar cada endpoint (request, response, errores)

### typescript-best-practices
- Modelar variantes con uniones discriminadas (literal `kind`); sin bolsas de campos opcionales
- Usar `unknown` para datos externos; sin casts con `as` salvo después de validar
- Parsear en los bordes con un schema (Zod según el stack del proyecto) y luego confiar en los tipos internos
- Usar `satisfies` en lugar de `as`; exhaustividad vía `const _exhaustive: never = x`
- Los type guards deben verificar lo que afirman; nombrarlos `isX` / `hasX`
- Derivar tipos con `Pick`, `Omit`, `ReturnType`, `z.infer` antes de declarar interfaces nuevas
- Sin `console.log` en código entregado; usar logging estructurado

### modern-web-development (PARCIAL)
- Usar interfaces de TypeScript; componentes funcionales; retornos tempranos y guard clauses
- Zod para validación de formularios (aplica a RHF + Zod en este proyecto)
- Tailwind CSS para estilos; diseño responsive mobile-first
- Minimizar `useEffect` y `setState`; preferir datos derivados
- Envolver la UI asíncrona en Suspense con fallbacks cuando corresponda
- Ignorar las reglas específicas de Next.js, Supabase, GraphQL y Shadcn (no están en DD-08)

### playwright-best-practices
- Usar Page Object Model para la estructura; mantener los selectores en roles / etiquetas visibles al usuario
- Usar aserciones web-first con auto-espera; evitar sleeps fijos
- Aislar los datos de test con fixtures; resetear el estado entre tests
- Mockear APIs solo donde haga falta; preferir el backend real en E2E
- Ejecutar en CI con reintentos solo para casos inestables conocidos; capturar traces ante fallos
- Incluir chequeos de accesibilidad con axe-core donde la UI sea crítica

### webapp-testing
- Usar Playwright (helper en Python de la skill); lanzar chromium en headless
- Iniciar los servidores con `scripts/with_server.py --help` antes de escribir código de arranque propio
- Esperar `networkidle` antes de hacer aserciones en páginas dinámicas
- Usar capturas de pantalla y logs de consola para depurar el comportamiento de la UI
- Mantener los scripts limitados a la lógica de Playwright; los servidores los gestiona el helper

### web-accessibility
- Clasificar primero la superficie que falla: semántica, teclado/foco, etiquetas/anuncios, reflow/contraste, medios, feedback de navegación
- Separar los hallazgos automatizados (axe, Lighthouse) de las verificaciones manuales y con lector de pantalla
- Corregir en la capa correcta; mantener el comportamiento del foco explícito en apps con ruteo y modales
- Producir un único paquete de remediación con un paso de verificación, no un volcado crudo del scanner
- Derivar la API de primitivas y la gobernanza de layout a otras skills, no a esta

### frontend-design-ui-ux
- Skill solo de spec: producir `.ulpi/design/DESIGN.md` y la spec de la feature; nunca escribir código de UI de producción
- Comprometerse con una única estética distintiva antes de especificar pantallas; evitar el AI-slop genérico
- Releer `DESIGN.md` antes de cada feature; mantener la variación dentro de la identidad bloqueada
- Incluir cobertura de estados, flujos y casos borde, y restricciones de accesibilidad
- Entregar la construcción a un agente de ingeniería con criterios de aceptación

### git-workflow-and-versioning
- Trunk-based: mantener `main` desplegable; ramas de vida corta (1-3 días)
- Commitear temprano y seguido; cada commit es un cambio lógico y atómico
- El mensaje del commit y el PR deben describir el porqué; sin commits gigantes
- Nunca hacer force-push en ramas compartidas; resolver conflictos haciendo rebase sobre el último main
- Pre-commit: los tests pasan y el diff fue revisado

### strict-tdd
- Red-Green-Refactor: sin código de producción sin un test que falle primero
- Triangular con al menos dos casos por comportamiento (camino feliz + caso borde)
- El hook apunta a `src/**/*.ts` solo con jest; NO se aplica al backend Java
- Aplicar las Tres Leyes manualmente al código Spring Boot vía springboot-tdd
- Alcance de sesión; activar con /strict-tdd

### openspec-explore
- El modo explore es para pensar, no para implementar: solo leer código y ejecutar comandos de solo lectura
- Fundamentar las preguntas en el código real y en los artefactos de OpenSpec antes de preguntar
- Hacer una pregunta puntual a la vez, explicando qué decisión destraba
- No crear ni modificar artefactos del change salvo que el usuario pida capturar el resultado
- Si no existe una raíz de OpenSpec, no ejecutar `openspec init` sin que el usuario lo pida

### openspec-propose
- Crea solo artefactos de planificación (proposal, delta specs, design, tasks); nunca implementar código acá
- Derivar un nombre de change en kebab-case a partir del pedido
- Usar `openspec status --change <name> --json` para determinar el próximo artefacto listo
- Preservar las rutas de capability existentes al escribir delta specs
- No ejecutar `openspec init` como efecto colateral; preguntar antes si no existe una raíz

### openspec-apply-change
- Anunciar el change seleccionado ("Using change: <name>") y cómo cambiarlo
- Leer `openspec status --change <name> --json` y las instrucciones de apply antes de editar
- Implementar una tarea a la vez; marcar cada tarea como completa al terminarla
- Detenerse y preguntar ante bloqueos o desajustes de diseño en lugar de adivinar
- Seguir las reglas de strict-tdd cuando el proyecto tenga TDD habilitado

### openspec-update-change
- Revisar solo artefactos de planificación existentes; nunca editar código
- Mantener coherentes proposal, design, specs y tasks tras cualquier edición
- Si falta un artefacto, mostrar el próximo listo según `openspec status`
- Pedirle al usuario que elija un change cuando haya más de uno activo

### openspec-sync-specs
- Merge dirigido por el agente de las delta specs hacia las specs principales en `openspec/specs/`
- Preservar la ruta completa de capability de cada delta spec
- Mergear los escenarios de forma aditiva; no sobrescribir requisitos no relacionados
- No archivar el change; solo sincronizar

### openspec-archive-change
- Archivar solo changes completados; confirmar que las tareas estén hechas antes de archivar
- Sincronizar las delta specs (si están pendientes) antes de mover el change a `openspec/changes/archive/YYYY-MM-DD-<name>/`
- Anunciar el change seleccionado y cómo cambiarlo
- Archivar es destructivo para la lista de trabajo; confirmar con el usuario primero

### supabase-postgres-best-practices (NO RELEVANTE)
- Guía de Postgres específica de Supabase; no asociar a este proyecto (PostgreSQL 16 plano + Flyway)
- Si se aplica, preferir las reglas de postgresql-table-design para trabajo de esquema genérico

### upstash-redis-js (NO RELEVANTE)
- SDK @upstash/redis; no hay Redis en DD-08. No asociar a tareas.

### idempotent-financial-workflows (NO RELEVANTE)
- Flujos que mueven dinero seguros ante reintentos; no se asocian a las tareas de este proyecto por indicación del usuario.

### notebooklm (NO RELEVANTE)
- API de Google NotebookLM; no se asocia a tareas de implementación.

### active-orchestrator, agent-instruction, kb-creator, roadmap-generator, discovery-research, web-scraper, find-skills, skill-creator (ORQUESTACIÓN / META)
- Skills de fundación y herramientas. No se inyectan en sub-agentes de implementación.
- Generan la KB, CHANGES.md, AGENTS.md / CLAUDE.md y catálogos de skills; ejecutarlas solo cuando el usuario lo pida.

## Convenciones del proyecto

| Archivo | Ruta | Notas |
|---------|------|-------|
| Config de openspec | openspec/config.yaml | Configuración del proyecto OpenSpec |
| Índice de la base de conocimiento | knowledge-base/README.md | Índice de los 10 archivos de la KB (01 a 10) |
| Decisiones (stack DD-08) | knowledge-base/09_decisiones_y_supuestos.md | Fuente del stack de este registro |
| Arquitectura | knowledge-base/08_arquitectura_propuesta.md | Arquitectura propuesta |
| Reglas de negocio | knowledge-base/05_reglas_de_negocio.md | Reglas de negocio |
| Roadmap de changes | CHANGES.md | Ítems de trabajo de implementación |

No existe AGENTS.md, CLAUDE.md, .cursorrules, GEMINI.md ni copilot-instructions.md en la raíz del proyecto. No se encontró un archivo índice de convenciones, por lo que no se extrajo ninguna ruta referenciada.

Leer los archivos de convenciones listados arriba para conocer los patrones y reglas específicos del proyecto.
