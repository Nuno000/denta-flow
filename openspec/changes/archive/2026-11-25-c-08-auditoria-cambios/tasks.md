# Tasks — C-08 `auditoria-cambios`

> **Gobernanza CRÍTICA:** no empezar hasta que Bruno apruebe `design.md` explícitamente. Estas tareas asumen las decisiones del 2026-11-25 (SDD, `turno_id` sin FK, solo inserción en la aplicación, valores como texto JSON en `VARCHAR`). Si cambia alguna, actualizar primero `design.md` y esta lista.
>
> TDD estricto en cada tarea: **RED** (test que falla y se ve fallar), **GREEN** (código mínimo hasta verde), **REFACTOR** (con tests en verde). Nada requiere Docker ni Testcontainers. Comandos desde `backend/` (con `JAVA_HOME` apuntando al JDK 21): `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local). La evidencia va en `openspec/changes/c-08-auditoria-cambios/evidencia/`.

## 1. Línea base

- [x] 1.1 Correr `./mvnw verify` y `./mvnw verify -Pit-mysql` en `feat/C-08-auditoria-cambios` antes de tocar código y anotar los tests en verde en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa

## 2. Dominio puro `dominio.auditoria` (spec "Actor identificado", "Acción de un catálogo cerrado", "Valores anterior y nuevo legibles", "Registro de quién y cuándo")

- [x] 2.1 RED: `ActorTest`: `Actor.usuario(7)` tiene tipo `USUARIO` e id 7; `Actor.usuario(null)` y `Actor.usuario(0)` se rechazan con `ValidacionDeDominioException`; `Actor.paciente()` y `Actor.sistema()` no tienen id. GREEN: `TipoDeActor` y el record `Actor` (design D2). Verificar que pasa y que `DominioSinFrameworksTest` sigue en verde
- [x] 2.2 RED: `AccionDeTurnoTest`: cada acción tiene su código (`creacion`, `confirmacion`, `cancelacion`, `reprogramacion`, `cambio_estado`) y `desdeCodigo("borrado")` se rechaza. GREEN: enum `AccionDeTurno`. Verificar que pasa
- [x] 2.3 RED: `CambioDeTurnoTest`: `registrar(...)` con turno 42, actor usuario 7, instante fijo, `cambio_estado`, `{"estado":"pendiente"}` → `{"estado":"confirmado"}` conserva todo; turno no positivo, actor o acción nulos, instante nulo, sin valores, valor anterior vacío en una acción que no es `creacion` y claves en blanco se rechazan; `creacion` admite anterior vacío; los mapas devueltos son inmodificables y respetan el orden de inserción. GREEN: `CambioDeTurno` inmutable con constructor `protected` sin argumentos para JPA (design D2). Verificar que pasa
- [x] 2.4 Declarar el puerto `HistorialDeTurnoRepositorio` con solo `agregar` y `historialDe`, con Javadoc en español que diga que no hay edición ni borrado. Verificar que compila y que `DominioSinFrameworksTest` pasa

## 3. Migración `V008_1` y restricciones de la tabla (spec "Actor identificado" — base, "Acción de un catálogo cerrado" — base, "Valores anterior y nuevo legibles" — base)

- [x] 3.1 RED: `RestriccionesHistorialBase` con subclases `RestriccionesHistorialH2Test` y `RestriccionesHistorialMySqlTest` (`@Tag("mysql")`), insertando por JDBC: fila válida aceptada; `actor_tipo` desconocido, `usuario` sin `usuario_id`, `paciente` o `sistema` con `usuario_id`, acción `borrado`, ambos valores nulos y `usuario_id` inexistente rechazados, cada uno por su restricción con nombre. GREEN: `V008_1__crear_turno_historial.sql` tal como design D1. Verificar en verde con `./mvnw verify` y con `./mvnw verify -Pit-mysql`
- [x] 3.2 Actualizar `backend/src/main/resources/db/migration/README.md`: agregar `V008_1__crear_turno_historial.sql` a los ejemplos existentes y anotar que C-09 debe agregar `fk_turno_historial_turno`. Verificar que el ejemplo coincide con el nombre real del archivo

## 4. Persistencia solo-inserción (spec "Historial solo de inserción", "Consulta cronológica por turno", "Valores demasiado largos")

- [x] 4.1 RED: `ConversorValoresJsonTest`: `{estado=pendiente}` ↔ `{"estado":"pendiente"}` respetando el orden; `null` ↔ `null`; comillas y tildes se escapan y vuelven igual; un JSON de más de 4000 caracteres lanza `ValidacionDeDominioException`. GREEN: conversor en `infraestructura.persistencia.auditoria` (design D3). Verificar que pasa
- [x] 4.2 RED: `HistorialDeTurnoRepositorioTest` (`@DataJpaTest`, H2): `agregar` guarda y devuelve con id; `historialDe(42)` devuelve los registros del turno 42 ordenados por `ocurridoEn` y, a igual instante, por orden de alta, sin los de otro turno; turno sin registros → lista vacía; `agregar` de una entidad con id se rechaza; el instante vuelve exacto en UTC. GREEN: `META-INF/orm/auditoria.xml` con columnas `updatable="false"` y `HistorialDeTurnoRepositorioJpa extends Repository<CambioDeTurno, Long>` (design D4). Verificar que pasa
- [x] 4.3 RED: en el mismo test, modificar por reflexión la acción y los valores de un registro gestionado, hacer `flush` y `clear`, y releer: el registro sigue igual. Agregar `HistorialSoloInsercionTest` que verifique por reflexión que el puerto y el repositorio no tienen métodos `delete*`, `remove*`, `update*`, `actualizar*` ni `borrar*`. GREEN: ajustar el mapeo si hiciera falta. Verificar que pasa

## 5. Servicio de aplicación (spec "Registro atómico con el cambio auditado", "Registro de quién y cuándo")

- [x] 5.1 RED: `ServicioDeAuditoriaTest` (unitario, repositorio falso y `Clock.fixed` en `2026-12-01T13:15:00Z`): `registrarCambio` arma el `CambioDeTurno` con la hora del reloj y lo agrega; los errores de validación del dominio se propagan sin agregar nada; `historialDe` delega en el puerto. GREEN: `ServicioDeAuditoria` en `aplicacion.auditoria` (design D5). Verificar que pasa
- [x] 5.2 RED: `ServicioDeAuditoriaIntegracionTest` (`@SpringBootTest`, perfil `test`, sin `@Transactional` en la clase): (a) sin transacción, `registrarCambio` lanza `IllegalTransactionStateException` y la tabla queda vacía; (b) dentro de un `TransactionTemplate` que termina bien, el registro queda; (c) dentro de uno que lanza después de registrar, no queda nada; (d) con actor usuario 9999 la excepción sale de `registrarCambio`, no del commit; (e) valores de más de 4000 caracteres revierten todo. Limpiar la tabla entre casos. GREEN: `@Transactional(propagation = MANDATORY)`; el largo se valida en `agregar` antes del `save` y el `INSERT` con `IDENTITY` es inmediato (design D5, ajuste de implementación). Verificar que pasa
- [x] 5.3 Correr `./mvnw verify` y `./mvnw verify -Pit-mysql` completos y guardar los resultados en `evidencia/backend.txt` y `evidencia/it-mysql.txt` (solo líneas de resultado, sin URL ni credenciales de la base). Verificar exit 0 en ambos y que los tests de la línea base siguen en verde

## 6. Cierre

- [x] 6.1 Correr `openspec validate c-08-auditoria-cambios --strict` y repasar cada escenario de `specs/auditoria-turnos/spec.md` contra el test que lo cubre; anotar la tabla escenario → test en `evidencia/trazabilidad.md`. Verificar que ningún escenario queda sin test

## Workflow follow-up

- Abrir la PR de `feat/C-08-auditoria-cambios` a `main`; la aprueba Florencia (governance CRÍTICO, revisión independiente).
- Contrato para C-09: agregar `fk_turno_historial_turno` en su migración e invocar `ServicioDeAuditoria.registrarCambio` dentro de su transacción, después de tomar los bloqueos.
- Pendiente para C-19: usuario de MySQL de producción sin `UPDATE`/`DELETE` sobre `turno_historial`.
- Archivar con `/opsx:archive` después del merge.
