# Design — C-08 `auditoria-cambios`

> Governance **CRÍTICO** (audit trail): este diseño requiere la aprobación explícita de Bruno antes de `/opsx:apply`.
> **Aprobación: aprobado (2026-11-25, por Bruno).** Decisiones de base tomadas por Bruno el 2026-11-25: SDD, `turno_id` sin FK (la agrega C-09), solo inserción garantizado en la aplicación y valores como texto JSON en `VARCHAR`. Motivación y alcance: ver `proposal.md`. Comportamiento exigido: ver `specs/auditoria-turnos`.

## Context

- C-03 y C-04 fijaron los patrones que este change replica: dominio en Java puro (lo controla `DominioSinFrameworksTest`), mapeo JPA en XML por dominio (`META-INF/orm/<dominio>.xml`), repositorios Spring Data que extienden `Repository` (sin `delete*`) e implementan el puerto del dominio, restricciones con nombre único y tests de restricciones duplicados en H2 y MySQL (`Restricciones…Base` con subclase `@Tag("mysql")`).
- C-04 dejó `UsuarioActual` (quién opera en el request) pensado para C-08, la tabla `usuario` y un único `Clock` en `RelojConfig`.
- La tabla `turno` todavía no existe: nace en C-09, que depende de C-08.
- No hay endpoints de auditoría en este change; la consulta para el administrador (US-025) es de C-13.

## Goals / Non-Goals

**Goals:**
- Un contrato chico y estable para C-09, C-11, C-12 y C-13: `registrarCambio(...)` dentro de la transacción del cambio y `historialDe(turnoId)`.
- Que sea imposible registrar fuera de una transacción y difícil modificar un registro desde la aplicación.

**Non-Goals:**
- Endpoint REST, pantalla y permisos de consulta (C-13).
- Auditoría de otras entidades (usuarios, recursos, configuración).
- Inmutabilidad a nivel de base (permisos del usuario de MySQL o triggers): queda anotada para C-19.
- Resolver el actor automáticamente: quien llama lo arma (ver D6).

## Decisions

### D1. Tabla `turno_historial` sin FK a `turno`

Migración `V008_1__crear_turno_historial.sql` (convención DD-09; `CHANGES.md` dice "Migración 003", que es anterior a esa convención):

```sql
CREATE TABLE turno_historial (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_id       BIGINT        NOT NULL,
    actor_tipo     VARCHAR(10)   NOT NULL,
    usuario_id     BIGINT        NULL,
    ocurrido_en    DATETIME(6)   NOT NULL,
    accion         VARCHAR(20)   NOT NULL,
    valor_anterior VARCHAR(4000) NULL,
    valor_nuevo    VARCHAR(4000) NULL,
    CONSTRAINT ck_turno_historial_actor_tipo CHECK (actor_tipo IN ('usuario', 'paciente', 'sistema')),
    CONSTRAINT ck_turno_historial_actor_usuario CHECK (
        (actor_tipo = 'usuario' AND usuario_id IS NOT NULL)
        OR (actor_tipo <> 'usuario' AND usuario_id IS NULL)),
    CONSTRAINT ck_turno_historial_accion CHECK (accion IN
        ('creacion', 'confirmacion', 'cancelacion', 'reprogramacion', 'cambio_estado')),
    CONSTRAINT ck_turno_historial_valores CHECK (valor_anterior IS NOT NULL OR valor_nuevo IS NOT NULL),
    CONSTRAINT fk_turno_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;
CREATE INDEX ix_turno_historial_turno ON turno_historial (turno_id, ocurrido_en, id);
```

- **Por qué sin FK**: la tabla `turno` no existe hasta C-09. C-09 agrega `fk_turno_historial_turno` en su migración (`ALTER TABLE ... ADD CONSTRAINT`), y como los turnos no se borran (se cancelan), la FK nunca bloquea un borrado.
- **Índice** `(turno_id, ocurrido_en, id)`: sirve a la única consulta (historial de un turno en orden) y da el desempate estable por `id`.
- **Alternativa descartada**: tabla genérica `evento_auditoria(entidad, entidad_id, ...)`; reutilizable, pero nunca podría tener FK y se aparta del modelo de la KB (`TurnoHistorial`). Elegida por Bruno el 2026-11-25.
- **Acción con `CHECK`** (no solo validada en Java): el catálogo es parte del contrato de auditoría. Si un change futuro necesita otra acción, agrega una migración que reemplace el `CHECK`. `sobreturno` no es una acción: es una `creacion` con `es_sobreturno` en el valor nuevo (RN-SO-02 queda cubierto por actor y fecha).

### D2. Dominio puro `dominio.auditoria`

- `CambioDeTurno`: inmutable (sin setters; los campos no son `final` porque JPA los carga por reflexión), con `id`, `turnoId`, `actor`, `ocurridoEn` (`Instant`), `accion`, `valorAnterior` y `valorNuevo` (`Map<String, String>` ordenado e inmodificable). Una fábrica `registrar(...)` valida: `turnoId` positivo, al menos un valor con contenido y valor anterior vacío solo en `creacion`, claves no vacías. Lanza `ValidacionDeDominioException` (ya existe en `dominio.comun`).
- `Actor`: record con `tipo` (`TipoDeActor` = `USUARIO | PACIENTE | SISTEMA`) y `usuarioId` opcional, con fábricas `Actor.usuario(id)`, `Actor.paciente()` y `Actor.sistema()` que hacen imposible un actor incoherente.
- `AccionDeTurno`: enum con código persistido (`creacion`, `confirmacion`, ...).
- `HistorialDeTurnoRepositorio` (puerto): solo `agregar(CambioDeTurno)` y `historialDe(long turnoId)`. No hay `guardar` genérico, ni `actualizar`, ni `borrar`.

### D3. Valores como texto JSON, convertidos en infraestructura

- El dominio maneja `Map<String, String>`; el JSON es un detalle de persistencia. Un `AttributeConverter` en `infraestructura.persistencia.auditoria` serializa con el `ObjectMapper` de Jackson (ya está en el classpath por Spring Web), con claves en orden de inserción para que el texto sea estable y legible (`{"estado":"confirmado"}`).
- Si el JSON resultante supera 4000 caracteres, el conversor lanza `ValidacionDeDominioException` **antes** del `INSERT`: nunca se trunca, y como el registro está en la transacción del cambio, el cambio tampoco se confirma (spec "Valores demasiado largos").
- Los instantes se pasan ya formateados en UTC ISO-8601 (`Instant.toString()`); mostrarlos en hora del consultorio es de quien consulta (C-13), con `ZonaHorariaConsultorio`.
- **Alternativa descartada**: una fila por campo cambiado (más fácil de filtrar, más filas y más difícil de leer un cambio completo). Elegida por Bruno el 2026-11-25. Tampoco se usa el tipo `JSON` nativo: H2 `MODE=MySQL` no lo trata igual que MySQL.

### D4. Solo inserción garantizado en la aplicación

- `META-INF/orm/auditoria.xml` mapea `CambioDeTurno` con todas las columnas `updatable="false"`: aunque alguien modifique la entidad gestionada por reflexión o un cambio futuro le agregue un setter, Hibernate no emite `UPDATE`.
- El repositorio Spring Data extiende `Repository<CambioDeTurno, Long>` (no `CrudRepository`): no hereda `delete*` ni `saveAll`. `agregar` usa `save` sobre una entidad siempre nueva (`id` nulo); si llega una con `id`, se rechaza.
- Un test de arquitectura verifica que el puerto y el repositorio no exponen métodos de modificación ni borrado.
- **Alternativa descartada**: triggers de MySQL que rechacen `UPDATE`/`DELETE`. H2 no corre triggers con cuerpo SQL, así que habría que excluirlos del perfil `test` y probarlos solo en `it-mysql`, con dos esquemas distintos. Los permisos del usuario de la base en producción se anotan para C-19.

### D5. Servicio con `Propagation.MANDATORY`

- `aplicacion.auditoria.ServicioDeAuditoria`:
  - `registrarCambio(long turnoId, AccionDeTurno accion, Actor actor, Map<String,String> anterior, Map<String,String> nuevo)` con `@Transactional(propagation = MANDATORY)`: fuera de una transacción, Spring lanza `IllegalTransactionStateException` y no se escribe nada. Toma `ocurridoEn` del `Clock` inyectado, nunca de quien llama.
  - `historialDe(long turnoId)` con `@Transactional(readOnly = true)`: lista ordenada por `ocurridoEn` y después `id`.
- **Por qué `MANDATORY` y no `REQUIRED`**: con `REQUIRED` un llamador distraído abriría una transacción propia y el registro quedaría guardado aunque el cambio falle después. `REQUIRES_NEW` sería peor: el registro sobreviviría al rollback del cambio.
- Los errores salen dentro de `registrarCambio`, no al confirmar la transacción del llamador, sin necesidad de `flush` explícito (ajuste de implementación, 2026-11-25): con id `IDENTITY` Hibernate ejecuta el `INSERT` en el `save`, así que la FK `fk_turno_historial_usuario` rechaza un usuario inexistente en ese momento. El largo de los valores lo valida `agregar` con el mismo conversor antes del `save`, porque si lo detectara Hibernate al convertir lo envolvería en una `JpaSystemException` en vez de la `ValidacionDeDominioException` que pide la spec.
- Un `agregar` con un registro que ya tiene id lanza `IllegalArgumentException`, que Spring traduce a `InvalidDataAccessApiUsageException` (es un error de programación, no de datos).

### D6. El actor lo arma quien llama

- C-09 y los demás changes construyen el `Actor` con `UsuarioActual` (personal), `Actor.paciente()` (endpoints públicos con token) o `Actor.sistema()` (tareas programadas). C-08 no decide cuál corresponde a cada endpoint porque esos endpoints todavía no existen.
- **Alternativa descartada**: que el servicio lea `UsuarioActual` solo; acoplaría `aplicacion` a Spring Security y no sabría distinguir paciente de sistema en requests sin autenticación.

## Risks / Trade-offs

- [Alguien con acceso directo a la base puede editar o borrar registros] → Fuera del alcance de la aplicación; C-19 crea el usuario de MySQL de producción sin `UPDATE`/`DELETE` sobre `turno_historial`.
- [C-09 se olvida de agregar la FK a `turno`] → Queda en la sección "Contrato" de `tasks.md` y en el README de migraciones; Florencia revisa C-09.
- [Un change futuro necesita una acción nueva] → Migración que reemplace `ck_turno_historial_accion` y valor nuevo en el enum; el test de restricciones lo detecta si se olvida uno de los dos.
- [El `INSERT` inmediato depende de la generación `IDENTITY`] → Si un change futuro cambia la estrategia de id, el test "usuario inexistente falla en el registro y no al confirmar" lo detecta.
- [4000 caracteres pueden no alcanzar] → Con los campos de un turno (estado, inicio, fin, ids, sobreturno) un cambio ocupa menos de 500; el límite se valida y falla de forma visible.

## Migration Plan

- Flyway aplica `V008_1` al arrancar; la tabla nace vacía. No hay datos previos que migrar.
- Rollback: como la migración ya mergeada no se edita, un rollback es una migración nueva que borra la tabla (solo mientras ningún change la use).
- Coordinación: si C-05, C-06 o C-07 se mergean antes, sus versiones (`V005_*` a `V007_*`) son menores; en `dev` está `out-of-order=true`.

## UI Mockups

Decision: no (no UI) — 2026-11-25.
