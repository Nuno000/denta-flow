# Design — C-09 `modelo-turno-paciente`

> Governance **CRÍTICO**: este diseño requiere la aprobación explícita de Renzo de las decisiones **Q1..Q14** (sección "Decisiones a aprobar") antes de `/opsx:apply`. Revisa la PR Florencia.
> **Aprobación: pendiente.** Hecho fijo (no se somete a aprobación): **PA-13 resuelta por el equipo el 2026-11-25** — email del paciente obligatorio y DNI obligatorio y único.
> Motivación y alcance: ver `proposal.md`. Comportamiento exigido: ver `specs/turnos`, `specs/pacientes` y `specs/auditoria-turnos`.

## Context

- **Patrones heredados (C-03, C-04, C-08)**: dominio en Java puro controlado por `DominioSinFrameworksTest`; mapeo JPA en XML por dominio (`META-INF/orm/<dominio>.xml`, listado en `spring.jpa.mapping-resources`); repositorios Spring Data que extienden `Repository` (sin `delete*`) e implementan el puerto del dominio; restricciones con nombre único en todo el esquema; tests de restricciones duplicados en H2 y MySQL (`Restricciones…Base` + subclase `@Tag("mysql")`); ids de otras entidades como `Long` (no asociaciones JPA), como `Profesional.prestacionIds` y `CambioDeTurno.turnoId`.
- **Recursos (C-03)**: `profesional`, `sillon`, `prestacion` (con `duracion_minutos`), `profesional_prestacion`. `BloqueoRecursosTest` ya probó que las filas de profesional y sillón se pueden bloquear con `PESSIMISTIC_WRITE` en H2; la concurrencia real queda para este change.
- **Auditoría (C-08)**: `ServicioDeAuditoria.registrarCambio(long turnoId, AccionDeTurno, Actor, Map valorAnterior, Map valorNuevo)` con `Propagation.MANDATORY`, hora del `Clock`, actor armado por quien llama (`Actor.usuario(id)`, `Actor.paciente()`, `Actor.sistema()`), acciones `creacion | confirmacion | cancelacion | reprogramacion | cambio_estado`. `turno_historial.turno_id` no tiene FK: este change la agrega. Los tests de integración de C-08 insertan historial con `turno_id` 42 sin turno real.
- **Errores**: `dominio/comun` tiene `ValidacionDeDominioException` (400), `RecursoNoEncontradoException` (404), `ConflictoException` y `OperacionNoPermitidaException` (409), traducidas por `ManejadorDeErrores` a `ProblemDetail`.
- **Tiempo**: un único `Clock` (`RelojConfig`), instantes `Instant` en UTC (`hibernate.jdbc.time_zone=UTC`), zona `ZonaHorariaConsultorio.ZONA`.
- **Migraciones presentes**: `V003_1`, `V004_1`, `V007_1`, `V008_1`. C-05 y C-06 no están mergeados: no hay horarios ni bloqueos todavía.

## Goals / Non-Goals

**Goals:**
- Un contrato estable para C-10..C-18: `ServicioDeTurnos` (escritura) y `ServicioDePacientes` (alta/deduplicación), que los consumidores invocan sin reescribir.
- Garantía de anti-solapamiento demostrada contra MySQL 8 real, sin interbloqueos.
- Integridad redundante en la base (CHECK, FK, UNIQUE) para lo que no depende de concurrencia.

**Non-Goals:**
- Endpoints REST, DTOs y guardas de rol (C-11, C-12, C-13, C-16, C-17). La matriz RBAC la aplica cada consumidor con las guardas de C-04.
- Horarios de atención y bloqueos (RN-TU-04): C-06/C-10 los enchufan con `RestriccionDeAgenda` (D7).
- Ventana de anticipación mínima (C-17), consultas de agenda por rango (C-11), emails y armado de enlaces (C-14), aviso de privacidad obligatorio en la reserva (C-15).
- Retención y baja de pacientes (PA-12), documentos extranjeros (Q13).

## Decisions

### D1. Migración `V009_1__crear_paciente_y_turno.sql`

```sql
CREATE TABLE paciente (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    dni                          VARCHAR(8)   NOT NULL,
    nombre                       VARCHAR(100) NOT NULL,
    apellido                     VARCHAR(100) NOT NULL,
    telefono                     VARCHAR(16)  NOT NULL,
    email                        VARCHAR(254) NOT NULL,
    consentimiento_privacidad_en DATETIME(6)  NULL,
    creado_en                    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_paciente_dni UNIQUE (dni),
    CONSTRAINT ck_paciente_dni_largo CHECK (CHAR_LENGTH(dni) BETWEEN 7 AND 8)
) ENGINE=InnoDB;

CREATE TABLE turno (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id           BIGINT      NOT NULL,
    profesional_id        BIGINT      NOT NULL,
    sillon_id             BIGINT      NOT NULL,
    prestacion_id         BIGINT      NOT NULL,
    inicio                DATETIME(6) NOT NULL,
    fin                   DATETIME(6) NOT NULL,
    estado                VARCHAR(20) NOT NULL,
    origen                VARCHAR(10) NOT NULL,
    es_sobreturno         BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_por_usuario_id BIGINT      NULL,
    creado_en             DATETIME(6) NOT NULL,
    token_acceso          VARCHAR(64) NOT NULL,
    CONSTRAINT uk_turno_token_acceso UNIQUE (token_acceso),
    CONSTRAINT ck_turno_estado CHECK (estado IN
        ('pendiente', 'confirmado', 'cancelado', 'asistio', 'ausente', 'ausente_con_aviso')),
    CONSTRAINT ck_turno_origen CHECK (origen IN ('online', 'recepcion')),
    CONSTRAINT ck_turno_rango CHECK (fin > inicio),
    CONSTRAINT ck_turno_sobreturno_recepcion CHECK (es_sobreturno = FALSE OR origen = 'recepcion'),
    CONSTRAINT ck_turno_origen_creador CHECK (
        (origen = 'online' AND creado_por_usuario_id IS NULL)
        OR (origen = 'recepcion' AND creado_por_usuario_id IS NOT NULL)),
    CONSTRAINT fk_turno_paciente    FOREIGN KEY (paciente_id)           REFERENCES paciente (id),
    CONSTRAINT fk_turno_profesional FOREIGN KEY (profesional_id)        REFERENCES profesional (id),
    CONSTRAINT fk_turno_sillon      FOREIGN KEY (sillon_id)             REFERENCES sillon (id),
    CONSTRAINT fk_turno_prestacion  FOREIGN KEY (prestacion_id)         REFERENCES prestacion (id),
    CONSTRAINT fk_turno_creado_por  FOREIGN KEY (creado_por_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

-- Igualdad primero, rango después (ficha C-09 y DD-09).
CREATE INDEX ix_turno_profesional_solapamiento ON turno (profesional_id, estado, inicio, fin);
CREATE INDEX ix_turno_sillon_solapamiento      ON turno (sillon_id, estado, inicio, fin);
CREATE INDEX ix_turno_paciente                 ON turno (paciente_id);
CREATE INDEX ix_turno_estado_inicio            ON turno (estado, inicio);
CREATE INDEX ix_turno_prestacion               ON turno (prestacion_id);
CREATE INDEX ix_turno_creado_por               ON turno (creado_por_usuario_id);
```

- Columnas `inicio`/`fin` (nombres de la ficha y de `04_modelo_de_datos.md`), no `fecha_hora_inicio`.
- Sin `CHECK` con expresiones regulares: el formato de DNI, teléfono y email se valida en el dominio (portabilidad H2/MySQL); la base garantiza nulos, largo del DNI y unicidad.
- MySQL 8 no admite `CHECK` sobre columnas con acciones referenciales; aquí no hay `ON DELETE/UPDATE` (igual que `usuario` en C-04).
- Las FK de profesional/sillón usan los índices compuestos (columna líder); MySQL descarta el índice automático de la FK al crearse uno utilizable.

### D2. Migración `V009_2__fk_turno_historial_turno.sql` (cierra el pendiente de C-08)

```sql
ALTER TABLE turno_historial
    ADD CONSTRAINT fk_turno_historial_turno FOREIGN KEY (turno_id) REFERENCES turno (id);
```

- Archivo separado para que la integración con C-08 sea explícita y reversible por sí misma. El índice `ix_turno_historial_turno (turno_id, ocurrido_en, id)` ya sirve a la FK.
- **No borra historial huérfano**: en producción no puede haberlo (la tabla `turno` nace acá). Si una base local de desarrollo tiene filas de prueba sueltas, la migración falla de forma visible; se limpian a mano en esa base (anotado en el README). Nunca se borra auditoría desde una migración (Q12).
- Consecuencia: los tests de integración de C-08 que usan `turno_id` 42 sin turno pasan a crear un turno real con el fixture de test `EscenarioDeAgendaJdbc` (D11). Se mantiene la intención de cada test; se avisa a Bruno.

### D3. Dominio puro `dominio/pacientes`

- `Dni` (normalización estática: quita `.`, espacios y `-`; exige 7 u 8 dígitos).
- `Paciente`: `nuevo(dni, nombre, apellido, telefono, email, creadoEn)` valida todo lo obligatorio (PA-13), normaliza teléfono (`+` opcional y 8..15 dígitos) y email (minúsculas, mismo formato que `Profesional`); `actualizarContacto(nombre, apellido, telefono, email)`; `registrarConsentimiento(Instant)`. Constructor `protected` sin argumentos para JPA.
- Puerto `PacienteRepositorio`: `guardar`, `buscarPorId`, `buscarPorDni` (sin borrado).

### D4. Dominio puro `dominio/turnos`

- `EstadoTurno` (enum con código de base) con la **tabla de transiciones** como única fuente: `puedePasarA(EstadoTurno)` y `ocupaRecursos()` (`false` solo para `CANCELADO`). Tabla (Q6):

  | Desde \ Hacia | pendiente | confirmado | cancelado | asistio | ausente | ausente_con_aviso |
  |---|---|---|---|---|---|---|
  | pendiente | = | ✔ | ✔ | ✔ | ✔ | ✔ |
  | confirmado | ✘ | = | ✔ | ✔ | ✔ | ✔ |
  | cancelado | ✘ | ✘ | = | ✘ | ✘ | ✘ |
  | asistio | ✘ | ✘ | ✘ | = | ✔ | ✔ |
  | ausente | ✘ | ✘ | ✘ | ✔ | = | ✔ |
  | ausente_con_aviso | ✘ | ✘ | ✘ | ✔ | ✔ | = |

  `=` es idempotente (sin cambio ni auditoría). Si se marca asistencia antes del inicio lo decide C-13.
- `OrigenTurno` (`ONLINE`, `RECEPCION`).
- `Turno`: `nuevo(pacienteId, profesionalId, sillonId, prestacionId, inicio, duracionMinutos, origen, esSobreturno, creadoPorUsuarioId, creadoEn, tokenAcceso)` calcula `fin`, nace `PENDIENTE`, valida precisión de minuto y coherencia origen/creador/sobreturno; `cambiarEstado(EstadoTurno)` devuelve un `CambioDeEstado` (anterior/nuevo, o vacío si es idempotente) y lanza `TransicionDeEstadoInvalidaException`; `reprogramar(inicio, duracionMinutos, profesionalId, sillonId)` solo en `PENDIENTE`/`CONFIRMADO` y devuelve los campos que cambiaron; `valoresDeAlta()` arma el mapa auditable **sin token ni datos personales**.
- Excepciones: `TurnoSolapadoException extends ConflictoException` con `RecursoOcupado` (`PROFESIONAL` | `SILLON`) y mensaje en español; `TransicionDeEstadoInvalidaException extends OperacionNoPermitidaException`. Ambas salen como 409 por `ManejadorDeErrores` sin tocarlo (Q7).
- Puertos: `TurnoRepositorio` (`guardar`, `buscarPorId`, `buscarPorIdParaActualizar` (bloqueo de la fila del turno), `buscarPorToken`, `haySolapamientoDeProfesional(profesionalId, inicio, fin, excluirTurnoId)`, `haySolapamientoDeSillon(...)`), `CerrojoDeRecursos` (`bloquearProfesional(id)`, `bloquearSillon(id)`; lanzan `RecursoNoEncontradoException` si la fila no existe), `GeneradorDeTokens` (`nuevoToken()`), `RestriccionDeAgenda` (D7).

### D5. Bloqueo y aislamiento (DD-09) (Q3)

Orden fijo dentro de cada transacción de escritura:

```
crear:        [lecturas sin bloqueo de prestación] → FOR UPDATE profesional → FOR UPDATE sillón
              → validar recursos (activos, realiza la prestación) → RestriccionDeAgenda
              → verificar solapamiento (si no es sobreturno) → INSERT turno → registrarCambio(C-08)
reprogramar:  leer turno → FOR UPDATE profesional destino → FOR UPDATE sillón destino
              → FOR UPDATE turno (re-validar estado) → validar → verificar excluyendo el propio id
              → UPDATE → registrarCambio
cambiarEstado: FOR UPDATE turno → transición → UPDATE → registrarCambio   (no toma recursos)
```

- **Cerrojo**: `CerrojoDeRecursosJpa` ejecuta `SELECT id FROM profesional WHERE id = ? FOR UPDATE` (ídem sillón) como consulta nativa del `EntityManager` (misma transacción; SQL válido en MySQL 8 y H2). Se prefiere a `em.find(..., PESSIMISTIC_WRITE)` porque no arrastra la colección EAGER `prestacionIds` al `FOR UPDATE` y deja explícito qué fila se bloquea.
- **Lectura del último estado confirmado**: los métodos de escritura declaran `@Transactional(isolation = READ_COMMITTED)`; la verificación es una consulta común (`COUNT` con `estado IN (activos)`, `es_sobreturno = FALSE`, `inicio < :fin AND fin > :inicio`, `id <> :excluir`). Con `READ COMMITTED` cada sentencia ve lo último confirmado aunque antes se haya leído algo, y InnoDB no toma *gap locks*, lo que evita el interbloqueo entre una creación y un cambio de estado concurrente.
- **Guarda de aislamiento**: al entrar, el servicio exige `TransactionSynchronizationManager.getCurrentTransactionIsolationLevel() == READ_COMMITTED`; si un consumidor lo envuelve en una transacción externa con otro aislamiento (Spring ignoraría el declarado), falla con `IllegalStateException` en vez de verificar con una foto vieja. No requiere cambiar la configuración global del `JpaTransactionManager`.
- **Interbloqueos**: el orden profesional → sillón → turno es global; `cambiarEstado` solo toma el turno y nunca espera recursos, así que no hay ciclo. Si aun así MySQL detecta uno o vence `innodb_lock_wait_timeout`, la excepción de Spring (`PessimisticLockingFailureException`) se propaga sin reintento; el consumidor decide (C-10 prueba el siguiente candidato).
- **Sobreturno** (Q5): toma los dos bloqueos (serializa con las verificaciones de otros) pero no verifica; la verificación de turnos comunes excluye a los sobreturnos existentes.

### D6. Servicio de aplicación `aplicacion/turnos/ServicioDeTurnos` (única vía de escritura)

```java
TurnoCreado crear(SolicitudDeTurno solicitud);            // record: pacienteId, profesionalId, sillonId,
                                                          // prestacionId, inicio, origen, esSobreturno, actor
Turno confirmar(long turnoId, Actor actor);               // acción confirmacion
Turno cancelar(long turnoId, Actor actor);                // acción cancelacion
Turno cambiarEstado(long turnoId, EstadoTurno nuevo, Actor actor); // asistencia y resto: cambio_estado
Turno reprogramar(long turnoId, SolicitudDeReprogramacion r, Actor actor); // acción reprogramacion
Optional<Turno> buscarPorId(long turnoId);                // readOnly
Optional<Turno> buscarPorToken(String token);             // readOnly; formato inválido → vacío
```

- `TurnoCreado` = turno + token de acceso (C-14 arma el enlace sin volver a leer).
- Validaciones en `crear`: paciente existe; prestación, profesional y sillón existen y están activos; el profesional realiza la prestación; `inicio >= reloj.instant()`; origen/actor coherentes (`ONLINE` ⇔ `Actor.paciente()`, `RECEPCION` ⇔ `Actor.usuario(id)`, que pasa a `creado_por_usuario_id`); sobreturno solo en `RECEPCION`. El **rol** (recepción/administrador, no profesional, RN-SO-01) lo verifica el consumidor (C-12) con las guardas de C-04.
- Auditoría: siempre después del `INSERT`/`UPDATE`, dentro de la misma transacción, vía `ServicioDeAuditoria.registrarCambio` (C-08, `MANDATORY`). Valores: alta → `paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin, estado, origen, es_sobreturno`; estado → `{"estado": ...}`; reprogramación → solo los campos que cambiaron (`inicio`, `fin`, `profesional_id`, `sillon_id`). Instantes en ISO-8601 UTC. Nunca token ni datos personales.
- Idempotencia: si `cambiarEstado` no cambia nada, no hay `UPDATE` ni registro.

### D7. Punto de extensión `RestriccionDeAgenda`

Interfaz del dominio `void verificar(TurnoPropuesto propuesto)` (profesional, sillón, prestación, inicio, fin, esSobreturno) que el servicio evalúa con los bloqueos tomados, antes de verificar el solapamiento. C-09 no registra ninguna; C-06/C-10 agregan "dentro del horario de atención" y "fuera de bloqueos" como beans sin tocar el servicio. Se inyecta con `ObjectProvider<RestriccionDeAgenda>.orderedStream()` (Spring no inyecta una `List` vacía en constructor).

### D8. Servicio `aplicacion/pacientes/ServicioDePacientes` (Q2, Q11)

```java
Paciente registrarDesdeReservaOnline(DatosPaciente datos); // existente: no pisa contacto
Paciente registrarDesdeRecepcion(DatosPaciente datos);     // existente: reemplaza contacto
Optional<Paciente> buscarPorDni(String dni);               // normaliza antes de buscar
```

- `DatosPaciente` (record): dni, nombre, apellido, teléfono, email, `consentimientoEn` opcional (si viene, reemplaza la fecha; lo usa C-15).
- Corre en **su propia transacción**, antes y separada de `ServicioDeTurnos.crear`. Carrera de dos altas del mismo DNI nuevo: la segunda choca con `uk_paciente_dni`; el servicio captura la `DataIntegrityViolationException` fuera de la transacción fallida (`TransactionTemplate`), relee por DNI en una nueva y aplica la política. Un reintento; si vuelve a fallar se propaga.
- El paciente puede quedar registrado aunque después la creación del turno falle (p. ej. por conflicto): aceptado; la retención la define PA-12/C-15.

### D9. Token de acceso (Q1)

- `GeneradorDeTokensSeguro` (`infraestructura/turnos`): 32 bytes de un `SecureRandom` único → Base64 URL sin relleno → 43 caracteres `[A-Za-z0-9_-]` (256 bits).
- Se guarda **en claro** en `turno.token_acceso` (`UNIQUE`), porque C-14 (confirmación) y C-18 (recordatorio días después) necesitan volver a armar el mismo enlace; el token no viaja al historial, no se loguea y `Turno` no lo expone en `toString`.
- `buscarPorToken` rechaza sin consultar los textos que no cumplen el formato. Ante colisión (probabilidad despreciable) el `UNIQUE` aborta la transacción.

### D10. Persistencia

- `META-INF/orm/pacientes.xml` y `META-INF/orm/turnos.xml` (acceso por campo, conversores de `EstadoTurno` y `OrigenTurno` a código, como `ConversorAccionDeTurno`); se agregan a `spring.jpa.mapping-resources` (cambio aditivo, commit aparte).
- `PacienteRepositorioJpa`, `TurnoRepositorioJpa` (`extends Repository`, sin `delete*`), `buscarPorIdParaActualizar` con `@Lock(PESSIMISTIC_WRITE)`.
- `CerrojoDeRecursosJpa` en `infraestructura/persistencia/turnos`.

### D11. Estrategia de pruebas

- Dominio: JUnit puro, `@ParameterizedTest` con las 36 combinaciones de la tabla de estados.
- Persistencia: `@DataJpaTest` (H2) para mapeos, consultas de solapamiento (bordes contiguos, cancelado, sobreturno, exclusión del propio id, medianoche local) y búsqueda por token; `RestriccionesPacienteBase`/`RestriccionesTurnoBase` con subclases H2 y MySQL; índices verificados en MySQL vía `information_schema.statistics`.
- Fixture de test `EscenarioDeAgendaJdbc` (inserta profesional, sillón, prestación, paciente y turno por JDBC con datos ficticios) reutilizado por los tests de C-08 y C-09.
- Servicio: unitarios con Mockito (`InOrder` para el orden profesional → sillón → restricciones → verificación → guardado → auditoría) y de integración `@SpringBootTest` perfil `test` (atomicidad con auditoría, guarda de aislamiento).
- Concurrencia: `@Tag("mysql")`, `@SpringBootTest` perfil `it-mysql`, `CountDownLatch` + 2 hilos, `@RepeatedTest(10)`, limpieza por sufijo en `@AfterEach` (patrón `EdicionSimultaneaParametrosMySqlTest`).
- Arquitectura: test que recorre `src/main/java` y verifica que solo `ServicioDeTurnos` usa los métodos de escritura de `TurnoRepositorio` (RN-GL-02).

## Decisiones a aprobar (Q1..Q14)

| # | Decisión | Recomendación (aplicada en specs y tareas) | Alternativa |
|---|---|---|---|
| Q1 | Formato y guardado del token | 256 bits `SecureRandom`, Base64 URL (43 car.), **en claro** con `UNIQUE`, fuera de logs e historial (D9) | Guardar solo SHA-256 y devolver el token una vez; obliga a C-14/C-18 a rotarlo en cada email (los enlaces previos dejan de andar) |
| Q2 | Política de actualización al deduplicar por DNI | Online **no pisa** nombre, apellido, teléfono ni email existentes; recepción **reemplaza** (personal autenticado). Consentimiento: siempre se actualiza si viene | Último gana siempre (riesgo: cualquiera con un DNI ajeno cambia el email de contacto) / rechazar si difiere |
| Q3 | Bloqueo y aislamiento | `READ COMMITTED` declarado + guarda; `SELECT id … FOR UPDATE` nativo sobre profesional y después sillón; verificación con lectura común; sin reintentos internos (D5) | `REPEATABLE READ` + verificación `FOR SHARE` (más *gap locks*, riesgo de interbloqueo con cambios de estado) |
| Q4 | `fin` guardado o calculado | Guardado al escribir (`inicio + duración` de la prestación vigente); indexable y estable si la prestación cambia luego | Calcularlo al leer (rompe índices y verificación) |
| Q5 | Semántica del sobreturno | No se verifica **ni bloquea** a otros turnos; toma igual los bloqueos; solo `recepcion` + usuario; el rol lo controla C-12 | Que los sobreturnos existentes sí ocupen el horario para turnos comunes |
| Q6 | Tabla de transiciones | La de D4: `cancelado` final; asistencia corregible entre `asistio`/`ausente`/`ausente_con_aviso`; mismo estado = no-op sin auditoría | Estados de asistencia finales; repetir estado = error |
| Q7 | Tipo de error de conflicto | `TurnoSolapadoException extends ConflictoException` con recurso `PROFESIONAL`/`SILLON`; `TransicionDeEstadoInvalidaException extends OperacionNoPermitidaException`; ambas 409 sin tocar `ManejadorDeErrores` | `ConflictoException` genérica (C-10 no distingue el recurso) |
| Q8 | ¿REST en C-09? | **No**: solo dominio, servicios y migraciones (Zona de la ficha); los endpoints son de los consumidores | Endpoints internos mínimos de creación/consulta |
| Q9 | Estado inicial en recepción | Siempre `pendiente`; C-12 llama a `confirmar` si corresponde (queda auditado) | Recepción elige `pendiente` o `confirmado` al crear |
| Q10 | Reprogramación en C-09 | Incluida (es escritura con bloqueos): solo `pendiente`/`confirmado`, conserva estado, paciente y token, recalcula `fin` | Dejarla a C-17 (duplicaría el bloqueo y la verificación) |
| Q11 | Transacción del paciente | Separada y previa a la del turno, con reintento ante DNI duplicado concurrente (D8) | Misma transacción del turno (un DNI duplicado concurrente aborta la reserva) |
| Q12 | FK del historial y datos huérfanos | `V009_2` separada, sin borrar historial; ajustar los tests de C-08 con un turno real | Que la migración borre filas huérfanas (borra auditoría) |
| Q13 | Formato del DNI | Solo DNI argentino de 7 u 8 dígitos normalizados | Columna `tipo_documento` para pasaporte/extranjeros |
| Q14 | Reglas de tiempo en C-09 | Precisión de minuto e inicio no anterior a "ahora"; horarios y bloqueos (RN-TU-04) vía `RestriccionDeAgenda` en C-06/C-10 | Esperar a C-06 e incluirlos acá (C-06 no está mergeado) |

## Risks / Trade-offs

- [H2 no reproduce el bloqueo de filas de InnoDB] → La concurrencia solo cuenta verificada con `./mvnw verify -Pit-mysql`; la evidencia se guarda en `evidencia/it-mysql.txt`.
- [Un consumidor envuelve el servicio en una transacción con otro aislamiento] → Guarda de aislamiento (D5) que falla de forma visible; documentado en el Javadoc del servicio.
- [Bloqueos de C-06 escritos en paralelo a una reserva] → Fuera de C-09: C-06 debe bloquear el profesional/sillón al crear un bloqueo para serializar con las reservas (anotado como contrato).
- [Los tests de C-08 cambian de fixture] → Solo se reemplaza el `turno_id` fijo por un turno real; se revisa con Bruno en la PR.
- [Base local con historial huérfano] → `V009_2` falla visible; limpieza manual en esa base de desarrollo (README).
- [Token en claro en la base] → Quien lee la base ya ve los datos personales y puede cambiar turnos; el token no sale en logs ni historial. Alternativa en Q1.
- [Paciente sin turno tras un conflicto] → Aceptado (D8); PA-12 define retención.
- [Plazo 2026-11-26 12:00] → Orden de tareas que deja primero creación + anti-solapamiento + concurrencia MySQL; reprogramación y la prueba de arquitectura son los últimos grupos y se pueden mover a C-17 si no llega (con aprobación).

## Migration Plan

- Flyway aplica `V009_1` y `V009_2` al arrancar; tablas nuevas vacías, sin datos que migrar. En `dev` sigue `out-of-order=true`.
- Antes de aplicar en una base local: `SELECT COUNT(*) FROM turno_historial` debe ser 0 o solo filas de turnos que existen.
- Rollback: migraciones nuevas (nunca editar las mergeadas) que quiten `fk_turno_historial_turno` y luego `turno` y `paciente`, solo mientras ningún change las use.
- Archivos compartidos (`application.yml`, `db/migration/README.md`) en un commit aparte y aditivo.

## Open Questions

- Ninguna que cambie specs o tareas fuera de Q1..Q14. Diferidas sin impacto: si C-13 permite marcar asistencia antes del inicio; política de retención de pacientes (PA-12, C-15).

## UI Mockups

Decision: no (no UI) — 2026-11-25.
