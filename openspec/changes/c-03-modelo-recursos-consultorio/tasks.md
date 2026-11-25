# Tasks — C-03 `modelo-recursos-consultorio`

> **Gobernanza CRÍTICA:** no empezar hasta que Renzo apruebe `design.md` y responda Q1–Q8. Estas tareas asumen las opciones recomendadas; si cambia alguna, actualizar primero `design.md` y esta lista.
>
> TDD estricto en cada tarea: **RED** (test que falla y se ve fallar), **GREEN** (código mínimo hasta verde), **REFACTOR** (con tests en verde después de cada paso). Nada requiere Docker ni Testcontainers. Comandos desde `backend/`: `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local ≥ 8.0.16).

## 1. Línea base

- [x] 1.1 Red de seguridad: correr `./mvnw verify` en `feat/C-03-modelo-recursos-consultorio` antes de tocar código y anotar "N tests en verde" en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa (no arreglarlo acá)

## 2. Dominio puro (`dominio.recursos`, spec "Duración obligatoria", "Datos obligatorios", "Identificación del sillón", "Desactivar sin borrar", "Prestaciones que realiza cada profesional")

- [x] 2.1 RED: `PrestacionTest` — `Prestacion.nueva("Limpieza", 40)` queda activa con 40 min; duración `null`, `0` y `-15` lanzan `ValidacionDeDominioException` con mensaje de duración (`@ParameterizedTest`). GREEN: `ValidacionDeDominioException` en `dominio.comun` y `Prestacion` mínima (design D8). Verificar que el test pasa y que `DominioSinFrameworksTest` sigue en verde
- [x] 2.2 RED: `PrestacionTest` — nombre `null`, vacío y solo espacios rechazados; nombre recortado; más de 100 caracteres rechazado; `cambiarDuracion` aplica la misma regla y no cambia la prestación si falla. GREEN: completar `Prestacion`. Verificar que pasa
- [x] 2.3 RED: `PrestacionTest` — `desactivar()` deja `activa=false` sin tocar nombre ni duración; desactivar dos veces no falla; `activar()` la reactiva. GREEN: implementar. Verificar que pasa
- [x] 2.4 RED: `SillonTest` — `Sillon.nuevo("Sillón 1")` activo; nombre en blanco o de más de 50 caracteres rechazado; `renombrar`; desactivar/activar idempotentes. GREEN: `Sillon`. Verificar que pasa
- [x] 2.5 RED: `ProfesionalTest` — alta con nombre, apellido y email (sin matrícula → `null`; matrícula en blanco → `null`); nombre/apellido/email en blanco rechazados; email sin formato `algo@algo.algo` rechazado; email guardado en minúsculas; largos máximos 100/100/254/30. GREEN: `Profesional` sin prestaciones. Verificar que pasa
- [x] 2.6 RED: `ProfesionalTest` — `habilitarPrestacion` con prestación activa y con id agrega su id; repetirla no duplica; prestación inactiva o sin id rechazada sin cambiar el profesional; `deshabilitarPrestacion` la quita; `realiza(id)`; `prestacionIds()` no se puede modificar desde afuera; desactivar y reactivar conserva las prestaciones. Para dar id a una prestación en el test usar un ayudante de test por reflexión (no agregar setters públicos). GREEN: implementar (design D4). Verificar que pasa
- [x] 2.7 RED: tests de `equals`/`hashCode` por id (dos instancias con el mismo id son iguales; sin id solo es igual a sí misma). GREEN: implementar en las tres clases (design D7). REFACTOR: extraer las validaciones de texto repetidas a un ayudante de `dominio.comun`, agregar constructor `protected` sin argumentos y fábricas estáticas como único camino de creación. Verificar `./mvnw verify` en verde, incluido `DominioSinFrameworksTest`
- [x] 2.8 Declarar los puertos `PrestacionRepositorio`, `SillonRepositorio` y `ProfesionalRepositorio` en `dominio.recursos` con las operaciones de design D6, sin métodos de borrado y con Javadoc en español del orden de cada listado. Verificar que compila y que `DominioSinFrameworksTest` pasa (los implementa el grupo 4)

## 3. Migración `V003_1` sobre H2 (spec "Duración obligatoria" — base, "Datos obligatorios", "Identificación del sillón", "Prestaciones que realiza cada profesional" — FK)

- [x] 3.1 RED: `RestriccionesRecursosBase` (abstracta, `JdbcTemplate`, `@Transactional`) + `RestriccionesRecursosH2Test` (`@SpringBootTest`, perfil `test`): las cuatro tablas existen; `INSERT` en `prestacion` con duración `0` y `-5` falla por CHECK y sin duración falla por NOT NULL; `activo`/`activa` valen `TRUE` por defecto. GREEN: crear `db/migration/V003_1__crear_recursos_consultorio.sql` con el DDL de design D3. Verificar que pasa con `./mvnw verify`
- [x] 3.2 RED: en la misma base — email de profesional repetido, nombre de sillón repetido y nombre de prestación repetido fallan; dos profesionales sin matrícula se aceptan; asociación en `profesional_prestacion` hacia una prestación inexistente falla por FK; la misma asociación dos veces falla por PK; `DELETE` de una prestación asociada falla. GREEN: ajustar el DDL solo si algún caso no pasa. Verificar que pasa
- [x] 3.3 Actualizar `db/migration/README.md`: ejemplo real `V003_1__crear_recursos_consultorio.sql` y una línea sobre el seed (no es una migración, design D9). Verificar a mano que el README coincide con el archivo creado

## 4. Mapeo JPA y repositorios (spec "Desactivar sin borrar", "Prestaciones que realiza cada profesional", "Profesionales activos por prestación", "Recursos bloqueables")

- [x] 4.1 RED: `PrestacionRepositorioTest` (`@DataJpaTest`, `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`): guardar y recuperar por id con todos los campos; `listarActivas` excluye la desactivada y ordena por nombre; desactivar + guardar conserva la fila (`buscarPorId` la devuelve inactiva) y `listarTodas` la incluye. GREEN: `META-INF/orm/recursos.xml` con `Prestacion` (acceso por campo, `IDENTITY`), `spring.jpa.mapping-resources` en `application.yml` e interfaz `PrestacionRepositorioJpa extends Repository<Prestacion, Long>, PrestacionRepositorio` (design D2, D6). Verificar que pasa y que el contexto arranca con `validate`
- [x] 4.2 RED: `SillonRepositorioTest` con los mismos casos; `listarActivos` en orden de id ascendente. GREEN: mapear `Sillon` y `SillonRepositorioJpa`. Verificar que pasa
- [x] 4.3 RED: `ProfesionalRepositorioTest` — ida y vuelta con matrícula `null` y con `prestacionIds`; quitar una prestación y guardar borra solo esa asociación; desactivar + guardar conserva las prestaciones; `listarActivos` por apellido y nombre. GREEN: mapear `Profesional` con `element-collection` sobre `profesional_prestacion` (`EAGER`) y `ProfesionalRepositorioJpa`. Verificar que pasa
- [x] 4.4 RED: `ProfesionalRepositorioTest` — `listarActivosQueRealizan(prestacionId)` con el escenario de la spec (dos activos que la realizan, uno inactivo que la realiza, uno activo que no) devuelve exactamente los dos, por id ascendente. GREEN: `@Query` JPQL de design D6. Verificar que pasa
- [x] 4.5 RED: `BloqueoRecursosTest` (`@DataJpaTest`, mismas anotaciones que 4.1): dentro de una transacción, `EntityManager.find(Profesional.class, id, PESSIMISTIC_WRITE)` y después `find(Sillon.class, id, PESSIMISTIC_WRITE)` devuelven las entidades sin error (humo de design D7; la concurrencia real es de C-09). GREEN: ajustar el mapeo solo si falla. Verificar que pasa
- [x] 4.6 Verificar que ningún repositorio expone borrado: los repositorios Spring Data extienden `Repository` (no `JpaRepository`/`CrudRepository`) y una búsqueda de `delete` en `infraestructura/persistencia/recursos` y `dominio/recursos` no devuelve resultados. REFACTOR: comentarios del XML, nombres y duplicación en los tests (ayudantes de construcción). Verificar `./mvnw verify` en verde

## 5. MySQL 8 real (perfil `it-mysql`)

- [x] 5.1 RED/GREEN: `RestriccionesRecursosMySqlTest` (`@Tag("mysql")`, `@SpringBootTest`, perfil `it-mysql`) que hereda de `RestriccionesRecursosBase`; ajustar la base para que no asuma tablas vacías (nombres únicos por ejecución, `@Transactional` revierte). El arranque del contexto valida la migración y el mapeo XML con `validate` en MySQL. Verificar con `./mvnw verify -Pit-mysql` contra el MySQL local; si `validate` falla por el tipo booleano, fijarlo en el XML (design, Riesgos) y repetir
  - **hecho 2026-11-25:** `-Pit-mysql` contra MySQL 8 local, 13/13 en verde; `validate` aceptó el `BOOLEAN` (el riesgo no se dio). Ajuste: en MySQL la violación del CHECK (error 3819, SQL state HY000) llega como `UncategorizedSQLException` y no como `DataIntegrityViolationException`, así que `rechazaDuracionCeroONegativa` exige un `DataAccessException` que nombre `ck_prestacion_duracion_positiva` (decidido por Renzo: ajustar el test, sin mapeo de códigos en producción).
- [x] 5.2 Verificar que `./mvnw verify` (sin `-Pit-mysql`) no ejecuta `RestriccionesRecursosMySqlTest` y que `./mvnw verify -Pit-mysql` sí, con el CHECK de duración rechazando `0` en MySQL
  - **hecho:** `./mvnw verify` no ejecuta `RestriccionesRecursosMySqlTest`; `./mvnw verify -Pit-mysql` sí, y el CHECK rechaza `0` y `-5` en MySQL.

## 6. Seed de demostración (spec "Datos de demostración")

- [x] 6.1 RED: `SeedRecursosDemoCondicionTest` con `ApplicationContextRunner`: sin la propiedad `app.seed.demo` y con `false` no existe el bean `SeedRecursosDemo`; con `true` existe. GREEN: `SeedRecursosDemo` (`ApplicationRunner`, `@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")`) en `infraestructura.seed`, con `run` vacío. Verificar que pasa
- [x] 6.2 RED: `SeedRecursosDemoTest` (`@DataJpaTest`, perfil `test`, `replace = NONE`, `@Import(SeedRecursosDemo.class)`, `app.seed.demo=true`): sobre base vacía, `run` crea 3 profesionales, 2 sillones y 5 prestaciones activos, con 5 duraciones distintas, cada profesional con al menos una prestación y todos los emails en `@example.com`. GREEN: implementar con las fábricas del dominio y los datos de design D9, en una transacción. Verificar que pasa
- [x] 6.3 RED: `SeedRecursosDemoTest` — correr `run` dos veces no duplica nada; con un sillón preexistente no crea nada y no lo modifica. GREEN: guarda de idempotencia (si existe algún recurso, se saltea y se registra en el log sin datos personales). Verificar que pasa
- [x] 6.4 RED: `SeedNoCorreEnTestTest` (`@SpringBootTest`, perfil `test`): el contexto no tiene el bean `SeedRecursosDemo` y las tres tablas están vacías. GREEN: no definir la propiedad en `application.yml` ni en `application-test.yml`. Verificar que pasa
- [x] 6.5 Configurar `app.seed.demo: true` en `application-dev.yml` y documentar `APP_SEED_DEMO` (comentada, `false` por defecto) en `.env.example`. Verificar a mano: arrancar en `dev` contra el MySQL local con la base sin recursos → 3/2/5 filas; reiniciar → mismas filas (consultas `SELECT COUNT(*)` guardadas en `evidencia/seed-dev.txt`)
  - **hecho:** `app.seed.demo: true` en `application-dev.yml` y `APP_SEED_DEMO` comentada en `.env.example` (agregada por Renzo a mano, 2026-11-25). Verificación manual en `dev` contra MySQL local hecha por Renzo: 3/2/5 filas (`evidencia/seed-dev.txt`); la no duplicación al reiniciar la cubre además `SeedRecursosDemoTest` (6.3).

## 7. Verificación integral

- [x] 7.1 Ejecutar `./mvnw verify` y `./mvnw verify -Pit-mysql`; guardar los resúmenes en `openspec/changes/c-03-modelo-recursos-consultorio/evidencia/`. Verificar que el número de tests es la línea base de 1.1 más los nuevos, todos en verde
  - **hecho:** `verify` (H2) 108/108 en verde (`evidencia/verificacion-h2.txt`); `verify -Pit-mysql` 16/16 tests `mysql` en verde (`evidencia/verificacion-mysql.txt`).
- [x] 7.2 Verificar con búsquedas que: no hay `Dockerfile`, `docker-compose*` ni `testcontainers`; `dominio/` no importa Spring, JPA ni Hibernate; el seed no tiene datos personales reales; no se tocaron `presentacion/` ni `aplicacion/`; no hay sintaxis exclusiva de un motor en `V003_1`
- [ ] 7.3 Marcar C-03 `[x]` en la ficha y en la fila de `CHANGES.md` (con la evidencia) y corregir en la ficha "Migración 001" por `V003_1__crear_recursos_consultorio.sql`. Verificar con `git diff CHANGES.md` que solo cambió C-03
  - **no aplicado en este apply:** `CHANGES.md` queda fuera del alcance (lo toma el orquestador/Renzo).

## Workflow follow-up

- Proponer el/los commit(s) en `feat/C-03-modelo-recursos-consultorio` (Conventional Commits en castellano, sin `Co-Authored-By`) y esperar el pedido explícito antes de commitear y pushear; PR a `main` que aprueba Florencia, con los jobs `backend` e `it-mysql` en verde.
- Con el PR aprobado: `/opsx:archive c-03-modelo-recursos-consultorio` (crea la spec principal `recursos-consultorio`), merge y avisar a Bruno que C-04 queda desbloqueado (FK `usuario.profesional_id → profesional.id`).
- Para C-19: decidir si la demo pública exporta `APP_SEED_DEMO=true`.
