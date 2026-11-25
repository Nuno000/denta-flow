# Design — C-03 `modelo-recursos-consultorio`

> **Gobernanza CRÍTICA.** Este diseño se implementa recién cuando Renzo lo aprueba explícitamente. Las decisiones marcadas con **[A CONFIRMAR]** tienen una opción recomendada (la que asumen `tasks.md`) y una alternativa; si Renzo elige la alternativa, se actualizan `design.md` y `tasks.md` antes del apply.

## Context

Ver `proposal.md` (Why). Estado observado en `main` tras C-02:

- `backend/` con Spring Boot 3.5.16, Spring Data JPA, Flyway (`flyway-core` + `flyway-mysql`), H2 solo en tests. `application.yml`: `ddl-auto=validate`, `open-in-view=false`, `hibernate.jdbc.time_zone=UTC`, `spring.flyway.locations=classpath:db/migration`. No existe `application-prod.yml`: el despliegue corre con la configuración común más variables de entorno.
- `db/migration/` solo tiene `README.md`: esta es la **primera migración real** del proyecto. Convención `V0NN_<n>__descripcion.sql` (DD-09) → C-03 usa `V003_1`.
- Perfiles: `dev` (MySQL local, `out-of-order=true`), `test` (H2 `MODE=MySQL;DATABASE_TO_LOWER=TRUE`, perfil por defecto de Surefire) e `it-mysql` (MySQL real, tests `@Tag("mysql")`). Por defecto `dev` e `it-mysql` apuntan a la **misma base** local `dentaflow`.
- `DominioSinFrameworksTest` falla si un archivo de `dominio/` importa `org.springframework`, `jakarta.persistence` u `org.hibernate` (regla dura de `CLAUDE.md`). La zona de `CHANGES.md` (`dominio/recursos/`) convive con esa regla solo si el mapeo JPA vive fuera de `dominio`.
- Patrón de tests de C-02 reutilizable: clase base abstracta con los tests + subclase H2 (`@ActiveProfiles("test")`) + subclase `@Tag("mysql")` (`@ActiveProfiles("it-mysql")`), como `IdaYVueltaUtcBase`.
- C-05 quedó fuera del corte de entrega (DD-10): sin ABM, la demo depende del seed de este change.
- El DDL de D3 se probó en una sesión aislada contra H2 2.2.224 `MODE=MySQL;DATABASE_TO_LOWER=TRUE`: acepta `ENGINE=InnoDB`, rechaza duración 0 y negativa (CHECK), duración nula (NOT NULL), FK inexistente y nombre repetido; admite varias matrículas `NULL`; `DEFAULT TRUE` funciona y `SELECT ... FOR UPDATE` se ejecuta. Falta verificarlo con la versión de H2 que gestiona Spring Boot 3.5.16 y contra MySQL 8 (tareas 3.x y 5.x).

## Goals / Non-Goals

**Goals:**
- Tablas e invariantes de los tres recursos listas para que C-04, C-06, C-09 y C-10 solo agreguen FKs y consultas, sin migraciones que alteren estas tablas.
- Modelo de dominio en Java puro, testeable sin Spring, que sea a la vez lo que persiste JPA (sin duplicar el modelo).
- Seed reproducible para la demo que nunca contamine `test` ni la configuración por defecto.

**Non-Goals:**
- API REST, DTOs, validación de entrada HTTP, RBAC y UI (C-05).
- Métodos de bloqueo pesimista y cualquier lógica de turnos (C-09). C-03 solo garantiza que las filas sean bloqueables.
- Reglas sobre qué pasa con turnos futuros al desactivar un recurso (C-05 / C-09).
- Auditoría de cambios de recursos (RN-AU-01 aplica a turnos; no se agrega `creado_en` ni historial).

## Decisions

### D1. Capas y paquetes
| Paquete | Contenido | Depende de |
|---|---|---|
| `ar.dentaflow.dominio.recursos` | `Profesional`, `Sillon`, `Prestacion` (invariantes) y los puertos `ProfesionalRepositorio`, `SillonRepositorio`, `PrestacionRepositorio` (interfaces) | solo JDK y `dominio.comun` |
| `ar.dentaflow.dominio.comun` | `ValidacionDeDominioException` (nueva, `RuntimeException`) y validaciones de texto compartidas | JDK |
| `ar.dentaflow.infraestructura.persistencia.recursos` | Repositorios Spring Data que implementan los puertos | Spring Data JPA |
| `src/main/resources/META-INF/orm/recursos.xml` | Mapeo JPA de las tres clases de dominio (D2) | — |
| `ar.dentaflow.infraestructura.seed` | `SeedRecursosDemo` (D9) | Spring |

`aplicacion/` y `presentacion/` no se tocan: los casos de uso y controladores de recursos son de C-05.

### D2. Mapeo JPA en XML sobre las clases de dominio **[A CONFIRMAR — Q1]**
**Recomendado:** las clases de `dominio.recursos` son las entidades persistidas, mapeadas con un archivo `orm.xml` (estándar JPA) en `META-INF/orm/recursos.xml`, registrado en `application.yml` con `spring.jpa.mapping-resources`. El dominio no importa nada de JPA; Hibernate accede por campo (`access="FIELD"`).
- Requisitos que impone a las clases de dominio (Java puro, sin anotaciones): constructor sin argumentos `protected`, clases y campos no `final`, `id` `Long` asignado por la base (`IDENTITY`). La creación válida es por fábricas estáticas (`Prestacion.nueva(...)`) que aplican las invariantes.
- Un archivo por dominio (`recursos.xml`; después `usuarios.xml`, `turnos.xml`, ...) para que los changes paralelos no editen el mismo archivo; cada uno agrega una línea a `spring.jpa.mapping-resources`.
- `ddl-auto=validate` detecta al arrancar cualquier desajuste entre el XML y el esquema (spec `configuracion-entorno`).
- **Alternativa:** entidades JPA anotadas separadas en `infraestructura.persistencia.recursos` (`ProfesionalJpa`, ...) más mappers dominio↔JPA y adaptadores que implementan los puertos. Es el patrón más conocido, pero duplica cada recurso (≈15 clases en lugar de ≈6) y agrega errores de mapeo, con la entrega el 2026-11-26. Como C-03 fija el patrón que copian C-04, C-06 y C-09, la elección es de Renzo.
- **Descartada:** anotaciones JPA en `dominio/` (rompe la regla dura y `DominioSinFrameworksTest`).

### D3. Esquema (`V003_1__crear_recursos_consultorio.sql`)
Una sola migración con las cuatro tablas. Nombres de tabla en singular, igual que las entidades y las FKs que ya usa la KB (`profesional_id`, `sillon_id`, `prestacion_id`) **[A CONFIRMAR — Q5]**. Restricciones con nombre explícito (`uk_`, `ck_`, `fk_`, `pk_`, `idx_`), únicos en todo el esquema porque H2 no permite repetirlos entre tablas.

```sql
CREATE TABLE profesional (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    apellido   VARCHAR(100) NOT NULL,
    matricula  VARCHAR(30)  NULL,
    email      VARCHAR(254) NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_profesional_email UNIQUE (email),
    CONSTRAINT uk_profesional_matricula UNIQUE (matricula)
) ENGINE=InnoDB;

CREATE TABLE sillon (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    activo BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_sillon_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE prestacion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    duracion_minutos INT          NOT NULL,
    activa           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_prestacion_nombre UNIQUE (nombre),
    CONSTRAINT ck_prestacion_duracion_positiva CHECK (duracion_minutos > 0)
) ENGINE=InnoDB;

CREATE TABLE profesional_prestacion (
    profesional_id BIGINT NOT NULL,
    prestacion_id  BIGINT NOT NULL,
    CONSTRAINT pk_profesional_prestacion PRIMARY KEY (profesional_id, prestacion_id),
    CONSTRAINT fk_profesional_prestacion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT fk_profesional_prestacion_prestacion FOREIGN KEY (prestacion_id) REFERENCES prestacion (id)
) ENGINE=InnoDB;

CREATE INDEX idx_profesional_prestacion_prestacion ON profesional_prestacion (prestacion_id);
```
- **Tipos (DD-09):** PK `BIGINT AUTO_INCREMENT`; estado como `BOOLEAN` (en MySQL es `TINYINT(1)`), porque es binario y no una máquina de estados (los estados de turno sí van en `VARCHAR` + `CHECK`). Sin columnas de fecha: no hay `DATETIME(6)` en este change.
- **Duración:** `INT NOT NULL` + `CHECK (duracion_minutos > 0)` como última línea de defensa de RN-TU-03; el dominio valida antes. Sin tope superior ni granularidad **[A CONFIRMAR — Q4]**.
- **Unicidad:** email de profesional, matrícula (varias `NULL` permitidas en MySQL y en H2), nombre de sillón y nombre de prestación **[A CONFIRMAR — Q3]**. El email es obligatorio porque la KB no lo marca como opcional.
- **Estado:** `activo` en profesional y sillón, `activa` en prestación, tal como lo nombra `04_modelo_de_datos.md` **[A CONFIRMAR — Q6]**.
- **FKs:** `ON DELETE` por defecto (`RESTRICT`): como no hay borrado (D5), cualquier `DELETE` sobre un recurso referenciado falla en la base.
- **Índices:** la PK compuesta cubre "prestaciones de un profesional"; `idx_profesional_prestacion_prestacion` cubre "profesionales que realizan la prestación X" (D6). No hace falta índice por `activo` con 3–5 filas por tabla.
- **Sin `CHARSET` ni `COLLATE` por tabla:** los fija la base (`utf8mb4`, ver `README.md` y CI).
- **Descartado:** `CHECK (TRIM(nombre) <> '')` en la base. Los textos en blanco los rechaza el dominio; agregarlo suma diferencias posibles entre motores sin cubrir un riesgo real (no hay otra vía de escritura).

### D4. Relación N:M como conjunto de ids de prestación
`Profesional` guarda `Set<Long> prestacionIds` (mapeado en XML como `element-collection` sobre `profesional_prestacion`, `fetch="EAGER"`), no un `Set<Prestacion>`.
- Cada recurso es su propio agregado y se referencia por id: la prestación tiene ciclo de vida propio (se desactiva sin tocar a los profesionales) y se evita cargar grafos con `open-in-view=false`.
- `habilitarPrestacion(Prestacion)` recibe la prestación para validar que esté **activa** y persistida (id no nulo), y guarda su id; `deshabilitarPrestacion(Long)`, `realiza(Long)` y `prestacionIds()` (copia inmodificable) completan la API.
- Desactivar una prestación **no** la quita de los profesionales: el motor de disponibilidad (C-10) filtra por prestación activa **[A CONFIRMAR — Q7]**.
- Quitar una asociación borra la fila de `profesional_prestacion`: es una relación, no un recurso, y "desactivar sin borrar" no aplica.
- `EAGER` con 3–5 profesionales no tiene costo; si crece, `listarActivos` usa `JOIN FETCH`.
- **Alternativa descartada:** `@ManyToMany Set<Prestacion>` con entidad asociativa. Más acoplamiento y riesgo de `LazyInitializationException`, sin beneficio para C-05 ni C-10.

### D5. Activo/inactivo y ausencia de borrado
- Todo recurso nace activo. `desactivar()` y `activar()` son idempotentes y no tocan otros datos ni las asociaciones.
- Los puertos del dominio **no** declaran borrado, y los repositorios Spring Data extienden `org.springframework.data.repository.Repository` (interfaz marcadora) en lugar de `JpaRepository`, así que tampoco hay `delete*` heredados que alguien pueda llamar por error.
- Un recurso inactivo sigue visible por id (los turnos pasados de C-09 lo referencian) y queda fuera de `listarActivos` / `listarActivas`.

### D6. Puertos de repositorio
| Puerto | Operaciones |
|---|---|
| `PrestacionRepositorio` | `guardar`, `buscarPorId`, `listarTodas`, `listarActivas` (por nombre) |
| `SillonRepositorio` | `guardar`, `buscarPorId`, `listarTodos`, `listarActivos` (por id ascendente: el mismo orden con el que C-09 bloquea y C-10 elige "el primer sillón libre", PA-06) |
| `ProfesionalRepositorio` | `guardar`, `buscarPorId`, `listarTodos`, `listarActivos` (por apellido y nombre), `listarActivosQueRealizan(Long prestacionId)` (por id ascendente) |

- Implementación: interfaces en `infraestructura.persistencia.recursos` que extienden `Repository<T, Long>` y el puerto del dominio. Declaran `save`/`findById` con la firma de Spring Data (las resuelve `SimpleJpaRepository`), y los métodos del puerto son `default` que delegan o llevan `@Query` JPQL (`select p from Profesional p join p.prestacionIds pid where pid = :prestacionId and p.activo = true order by p.id`).
- Las unicidades se aplican en la base; el puerto no ofrece "existe por email" hasta que C-05 lo necesite para dar un mensaje amable.

### D7. Preparado para el bloqueo pesimista de C-09
- `profesional` y `sillon` son tablas InnoDB con PK simple: `SELECT ... FOR UPDATE` bloquea exactamente una fila por recurso.
- Sin `@Version` (bloqueo optimista): C-09 decide la concurrencia con bloqueo pesimista y una versión agregaría incrementos y excepciones que nadie maneja.
- C-03 **no** agrega `bloquearPorId`; C-09 lo agrega a `ProfesionalRepositorio` y `SillonRepositorio` con `@Lock(LockModeType.PESSIMISTIC_WRITE)`, respetando el orden profesional → sillón (DD-09). C-03 solo deja un test de humo que bloquea un profesional y después un sillón con `EntityManager.find(..., PESSIMISTIC_WRITE)` en H2, para detectar temprano si el mapeo XML lo impide **[A CONFIRMAR — Q8]**.
- `equals`/`hashCode` por id (con `instanceof`, no `getClass()`, por los proxies de Hibernate), `hashCode` constante por clase.

### D8. Invariantes del dominio y excepción
- `ValidacionDeDominioException` (en `dominio.comun`) con mensaje en español que nombra el dato ("La duración debe ser mayor que cero"). C-05 la traduce a `400` en su `@RestControllerAdvice`.
- `Prestacion`: nombre obligatorio (recortado, ≤100), `duracionMinutos` `Integer` obligatorio y >0 (en la fábrica y en `cambiarDuracion`). Cambiar la duración no afecta turnos ya creados: C-09 guarda `fin` calculado al crear el turno.
- `Sillon`: nombre obligatorio (recortado, ≤50), `renombrar` con la misma validación.
- `Profesional`: nombre y apellido obligatorios (≤100), email obligatorio (≤254, formato mínimo `algo@algo.algo`, guardado en minúsculas), matrícula opcional (en blanco → `null`, ≤30).
- Los largos máximos son constantes del dominio iguales a los `VARCHAR` de D3, para que un texto largo falle con un mensaje claro y no con un error SQL.

### D9. Seed de demostración: componente condicionado por propiedad **[A CONFIRMAR — Q2]**
**Recomendado:** `SeedRecursosDemo` (`ApplicationRunner` en `infraestructura.seed`) con `@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")` (sin `matchIfMissing`, o sea, apagado si falta).
- `application-dev.yml` pone `app.seed.demo: true`; `application.yml` y `application-test.yml` no la definen → nunca corre en `test`, en `it-mysql` ni en el despliegue salvo que alguien exporte `APP_SEED_DEMO=true` (C-19 decide si la demo pública la activa, que es lo esperable porque C-05 no entra en el corte).
- Idempotente: en una sola transacción, si ya existe al menos un profesional, sillón o prestación, no hace nada y lo registra en el log (sin datos personales). Si no, crea todo usando las fábricas del dominio, así el seed pasa por las mismas invariantes.
- Por qué no una ubicación Flyway solo de `dev` (`classpath:db/seed` con `V003_2__seed...`): (1) `dev` e `it-mysql` comparten la base local por defecto, y Flyway en `it-mysql` fallaría al validar con la migración `003.2` aplicada pero no resuelta (`ignoreMigrationPatterns` por defecto no ignora *missing*); (2) el historial de Flyway diferiría entre entornos; (3) un `INSERT` en SQL queda desfasado cuando C-04/C-06 agreguen columnas, mientras que el runner deja de compilar y se corrige en el mismo PR; (4) DD-09 fija **una sola** ubicación de migraciones. No viola "nunca cambiar el esquema fuera de Flyway": el seed escribe datos, no esquema.
- **Alternativa:** la ubicación Flyway `db/seed` activada en `application-dev.yml`, más `spring.flyway.ignore-migration-patterns: "*:missing"` en `it-mysql`. Más simple de leer (SQL plano) pero con los costos de arriba.

Contenido (nombres y emails ficticios, dominio reservado `example.com`, RFC 2606):

| Profesional | Email | Matrícula | Prestaciones |
|---|---|---|---|
| Laura Ejemplo | laura.ejemplo@example.com | MP-0001 | todas |
| Martín Prueba | martin.prueba@example.com | — | Consulta, Limpieza, Obturación |
| Sofía Demo | sofia.demo@example.com | MP-0003 | Consulta, Extracción, Blanqueamiento |

| Sillón | | Prestación | Duración |
|---|---|---|---|
| Sillón 1 | | Consulta y diagnóstico | 30 min |
| Sillón 2 | | Limpieza (profilaxis) | 40 min |
| | | Obturación (arreglo) | 45 min |
| | | Extracción simple | 60 min |
| | | Blanqueamiento | 90 min |

Tres profesionales y dos sillones hacen visible en la demo la restricción por sillón (RN-TU-07); las asociaciones parciales dejan visible el filtro de "cualquier profesional" (PA-05).

### D10. Estrategia de tests
| Capa | Qué cubre | Cómo |
|---|---|---|
| Unitaria (Java puro) | Invariantes de `Prestacion`, `Sillon`, `Profesional`; habilitar prestaciones; activar/desactivar | JUnit 5 + AssertJ, `@ParameterizedTest` para triangular (0, -1, `null`; blancos), sin Spring |
| Arquitectura | `dominio/` sin Spring/JPA | `DominioSinFrameworksTest` existente (cubre los archivos nuevos sin cambios) |
| Esquema H2 | CHECK, NOT NULL, defaults, unicidades, FKs | `RestriccionesRecursosBase` (abstracta, `JdbcTemplate`, `@Transactional` para revertir) + `RestriccionesRecursosH2Test` (`@SpringBootTest`, perfil `test`) |
| Persistencia | Ida y vuelta de cada recurso, `prestacionIds`, listados, desactivar sin borrar, humo de bloqueo | `@DataJpaTest` + `@ActiveProfiles("test")` + **`@AutoConfigureTestDatabase(replace = NONE)`** (sin esto Spring reemplaza el DataSource por un H2 sin `MODE=MySQL` y la migración corre en otro dialecto) |
| MySQL real | Migración + `validate` de Hibernate en MySQL 8, CHECK aplicado (≥ 8.0.16) | `RestriccionesRecursosMySqlTest` (`@Tag("mysql")`, perfil `it-mysql`), subclase de la misma base; no asume tablas vacías (la base local puede tener el seed) y usa nombres únicos por ejecución |
| Seed | Condición de activación; contenido; idempotencia | `ApplicationContextRunner` para la condición; `@DataJpaTest` + `@Import(SeedRecursosDemo.class)` con la propiedad en `true` para contenido e idempotencia; `@SpringBootTest` perfil `test` para confirmar que el bean no existe |

Sin Docker ni Testcontainers; sin mocks de la base en tests de persistencia.

## Risks / Trade-offs

- [Hibernate `validate` en MySQL compara `BOOLEAN` (`TINYINT(1)`, que Connector/J informa como `BIT`) con el tipo que espera para `boolean`] → El test `@Tag("mysql")` arranca el contexto con `validate` contra MySQL 8 en CI; si falla, se fija el tipo en el XML (`column-definition`) sin tocar la migración, que todavía no está mergeada.
- [MySQL < 8.0.16 acepta el `CHECK` y lo ignora] → El test `@Tag("mysql")` inserta duración 0 y espera el rechazo; README y CI ya usan MySQL 8.0.x reciente. Además el dominio valida antes.
- [El mapeo XML es menos conocido por el equipo que las anotaciones] → Un solo archivo corto por dominio, comentado; `validate` falla al arrancar ante cualquier error; la alternativa D2 queda documentada.
- [`dev` e `it-mysql` comparten la base local: los tests MySQL ven los datos del seed] → Los tests no asumen tablas vacías, usan nombres únicos y revierten con `@Transactional`. Se recomienda (no se exige) una base `dentaflow_it` local.
- [Las clases de dominio deben tener constructor sin argumentos y campos no finales por JPA] → Constructor `protected`, sin setters públicos; las invariantes viven en fábricas y métodos de negocio.
- [Un profesional puede quedar asociado a una prestación luego desactivada] → Intencional (D4); C-10 filtra por prestación activa. Queda escrito para C-10.
- [Plazo DD-10] → Alcance acotado a modelo, persistencia y seed; sin API ni UI.

## Migration Plan

1. Al mergear, Flyway aplica `V003_1` al arrancar en cada entorno (primera migración real del repo).
2. En `dev`, el primer arranque con `app.seed.demo=true` carga el seed; los siguientes lo saltean.
3. Rollback antes del merge: revertir el PR y, en la base local, `DROP` de las cuatro tablas y borrar la fila `003.1` de `flyway_schema_history` (no hay datos reales). Después del merge, cualquier corrección es una migración nueva `V003_2__...` o del change que la necesite; nunca se edita `V003_1`.

## Open Questions

**Resueltas (aprobado por Renzo 2026-11-25): se eligieron todas las opciones recomendadas de Q1 a Q8.**

- Q1: mapeo JPA en XML sobre las clases de dominio puras.
- Q2: seed como componente condicionado por `app.seed.demo` (solo `dev`; C-19 decide activarlo en la demo).
- Q3: email de profesional obligatorio y único; matrícula única cuando existe; nombres de sillón y de prestación únicos.
- Q4: la duración solo exige > 0.
- Q5: tablas en singular.
- Q6: `activa` en prestación (`activo` en profesional y sillón).
- Q7: se rechaza asociar una prestación inactiva; desactivar una prestación conserva las asociaciones.
- Q8: los métodos de bloqueo quedan para C-09 (acá solo el test de humo).

Texto original de las preguntas, conservado como registro:

- **Q1 (D2)** — Mapeo JPA: ¿XML sobre las clases de dominio (recomendado) o entidades JPA separadas con mappers? Cambia la estructura de las tareas 4.x y el patrón que copian C-04, C-06 y C-09.
- **Q2 (D9)** — Seed: ¿componente condicionado por `app.seed.demo` (recomendado) o ubicación Flyway `db/seed` solo en `dev`? ¿Confirmamos que C-19 activa el seed en la demo pública, dado que C-05 no entra en el corte?
- **Q3 (D3)** — ¿Email del profesional obligatorio y único, y matrícula única cuando existe? ¿Nombres de prestación y de sillón únicos?
- **Q4 (D3)** — ¿Tope máximo de duración (p. ej. 480 min) o múltiplo de 5? Hoy solo se exige > 0, como dice la KB.
- **Q5 (D3)** — ¿Tablas en singular (`profesional`, recomendado) o en plural?
- **Q6 (D3)** — ¿`activa` en prestación (como la KB) o `activo` uniforme en las tres tablas?
- **Q7 (D4)** — Al asociar se rechaza una prestación inactiva; al desactivar una prestación no se quitan las asociaciones. ¿De acuerdo?
- **Q8 (D7)** — ¿Los métodos `bloquearPorId` (`PESSIMISTIC_WRITE`) quedan para C-09 (recomendado), con solo un test de humo acá?
- Fuera de este change (anotar para C-05/C-09): qué pasa al desactivar un profesional o sillón con turnos futuros.

## UI Mockups

- Design system revision: — (no existe `design-system/`)
- Decision: no (no UI) — 2026-11-25. El change no tiene pantallas; la UI de recursos es de C-05.
