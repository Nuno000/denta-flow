# Design — C-07 `parametros-consultorio`

> Gobernanza **BAJO**: se implementa con las opciones recomendadas. Las decisiones marcadas **[Q#]** se listan al final para que Renzo las confirme o las cambie; cambiar una de ellas solo toca la tarea indicada.

## Context

- Motivación y alcance: ver `proposal.md`. Requisitos: `specs/parametros-consultorio/spec.md`.
- Estado del backend (C-02, C-03, C-04 mergeados): capas `dominio` (Java puro, verificado por `DominioSinFrameworksTest`), `aplicacion`, `infraestructura`, `presentacion`. Las clases de dominio se mapean a tablas con XML (`META-INF/orm/*.xml`, listado en `spring.jpa.mapping-resources`) y Hibernate solo valida (`ddl-auto=validate`). Los repositorios son puertos en `dominio` implementados por interfaces Spring Data que extienden `Repository` (sin `delete*`).
- Seguridad (C-04, DD-11): JWT Bearer, denegación por defecto, guardas de rol como anotaciones (`@SoloAdministrador`), `@AuthenticationPrincipal UsuarioAutenticado` con el id del usuario, y `GuardasEnControladoresTest`, que falla si un endpoint de `presentacion` no declara guarda. El usuario se relee en cada request: uno desactivado recibe 401.
- Errores (C-04): `ManejadorDeErrores` traduce `ValidacionDeDominioException` → 400, `RecursoNoEncontradoException` → 404 y errores de Bean Validation → 400 con el mapa `errores`, todo como `ProblemDetail` en español. C-07 no necesita tocarlo.
- Datos de referencia (README de `db/migration`): lo que debe existir en todo entorno va en la migración (como los tres roles de `V004_1`); los datos de demostración van en `SeedRecursosDemo` (solo `dev`).
- Hora actual: un único `Clock` (`RelojConfig`, UTC), reemplazable en tests.
- Tests de esquema: clase base abstracta con `JdbcTemplate` + subclase H2 (perfil `test`) + subclase MySQL (`@Tag("mysql")`, perfil `it-mysql`). La base H2 en memoria se comparte entre contextos de la misma JVM (`DB_CLOSE_DELAY=-1`), así que todo test que haga commit debe dejar las filas como estaban.

## Goals / Non-Goals

**Goals:**
- Un modelo clave-valor chico, con catálogo cerrado y validación tipada en el dominio, que los changes siguientes extiendan agregando una clave al catálogo y una fila en su propia migración.
- Una lectura tipada (`Duration`, texto) para C-17 y C-15 que se pueda reemplazar por un doble en sus tests unitarios.
- Cero cambios en la configuración de seguridad y en el manejo de errores compartidos.

**Non-Goals:**
- Pantalla de administración (Q4), endpoint público del aviso (Q5), historial de versiones del aviso (Q6), bloqueo optimista (Q7).
- Caché de parámetros: se leen de la base en cada uso (una fila por clave primaria; volumen de un consultorio chico).
- Parámetros de otros changes (recordatorios PA-07, reintento PA-08, estado inicial del turno): los agregan sus changes con el mismo mecanismo.

## Decisions

### D1. Capas y paquetes

| Capa | Paquete | Contenido |
|------|---------|-----------|
| dominio | `ar.dentaflow.dominio.configuracion` | `ClaveParametro` (catálogo), `TipoParametro`, `Parametro`, puerto `ParametroRepositorio` |
| aplicación | `ar.dentaflow.aplicacion.configuracion` | `ParametrosDelConsultorio` (interfaz de lectura tipada), `AvisoPrivacidad` (record), `ServicioDeParametros` |
| infraestructura | `ar.dentaflow.infraestructura.persistencia.configuracion` | `ParametroRepositorioJpa`; `META-INF/orm/configuracion.xml` |
| presentación | `ar.dentaflow.presentacion.configuracion` | `ParametrosController`, `ParametroRespuesta`, `ActualizarParametroSolicitud` |

La ficha de `CHANGES.md` nombra solo `aplicacion/configuracion/` y `db/migration/`; el modelo y el controlador van en sus capas para respetar `08_arquitectura_propuesta.md` y el patrón de C-03/C-04. Alternativa descartada: todo en `aplicacion/configuracion/` con anotaciones JPA (rompe `DominioSinFrameworksTest` y el patrón XML).

### D2. Clave-valor con catálogo cerrado y validación tipada

Una sola tabla `parametro (clave, valor)` con el valor como texto, como pide la ficha ("modelo de configuración clave-valor"). El tipo y las reglas de cada clave viven en el enum de dominio `ClaveParametro`:

| Código (`clave`) | Tipo | Regla | Valor inicial |
|---|---|---|---|
| `anticipacion-minima-horas` | `ENTERO` (unidad: horas) | entero de 1 a 168 **[Q2]** | `24`, provisorio **[Q3]** |
| `aviso-privacidad-texto` | `TEXTO` | obligatorio, recortado, ≤ 4000 caracteres | borrador provisorio (D5) |
| `aviso-privacidad-leyenda` | `TEXTO` | obligatorio, recortado, ≤ 200 caracteres | `Leí y acepto el aviso de privacidad`, provisorio |

- `ClaveParametro.desdeCodigo(String)` lanza `RecursoNoEncontradoException("No existe el parámetro <código>")` → 404 sin tocar la tabla.
- `ENTERO`: se recorta y debe cumplir `-?\d{1,9}` (rechaza `24.5`, `veinte`, vacío, `+48`); después se aplica el rango. Mensaje: `La anticipación mínima debe ser un número entero de horas entre 1 y 168`.
- `TEXTO`: reutiliza `ValidacionesDeTexto.obligatorio(texto, dato, maximo)` de `dominio.comun` (mensajes ya probados).
- Códigos en kebab-case: van tal cual en la URL (`/api/parametros/{clave}`) sin problemas de puntos ni mayúsculas.

Alternativas: (a) tabla tipada de una sola fila con una columna por parámetro (`configuracion_consultorio(anticipacion_minima_horas INT, ...)`): mejor tipado en la base, pero contradice la ficha y obliga a una migración `ALTER TABLE` por cada parámetro nuevo de otros changes; (b) clave-valor abierto (cualquier clave): sin validación posible. **[Q1]**

### D3. Modelo de dominio `Parametro`

Java puro, constructor `protected` sin argumentos para el mapeo y sin fábrica pública de alta (las filas las crea la migración, D5).

- Campos: `clave` (String, id), `valor` (String), `provisorio` (boolean), `actualizadoEn` (`Instant`, nulo hasta la primera edición), `actualizadoPor` (Long, id de usuario, nulo hasta la primera edición).
- `ClaveParametro clave()` → `ClaveParametro.desdeCodigo(clave)`. La clave se guarda como `String` porque JPA no aplica `AttributeConverter` a un `@Id`.
- `actualizar(String valorNuevo, Long autorId, Instant ahora)`: valida y normaliza según el tipo (D2); si falla lanza `ValidacionDeDominioException` **sin cambiar nada**; si pasa, guarda el valor normalizado, `provisorio = false`, autor e instante. Guardar el mismo valor también confirma (spec "Confirmación al guardar").
- `valorEntero()`: solo para claves `ENTERO`; `Duration` la arma el servicio (D6) para no mezclar unidades en el dominio.
- `equals`/`hashCode` por clave, como C-03 (D7 de C-03).

### D4. Esquema `V007_1__crear_parametros.sql`

```sql
CREATE TABLE parametro (
    clave           VARCHAR(60)   NOT NULL,
    valor           VARCHAR(4000) NOT NULL,
    provisorio      BOOLEAN       NOT NULL,
    actualizado_en  DATETIME(6)   NULL,
    actualizado_por BIGINT        NULL,
    CONSTRAINT pk_parametro PRIMARY KEY (clave),
    CONSTRAINT fk_parametro_actualizado_por FOREIGN KEY (actualizado_por) REFERENCES usuario (id),
    CONSTRAINT ck_parametro_autor_y_fecha CHECK (
        (actualizado_en IS NULL AND actualizado_por IS NULL)
        OR (actualizado_en IS NOT NULL AND actualizado_por IS NOT NULL))
) ENGINE=InnoDB;

INSERT INTO parametro (clave, valor, provisorio) VALUES
    ('anticipacion-minima-horas', '24', TRUE),
    ('aviso-privacidad-texto', '<borrador, ver D5>', TRUE),
    ('aviso-privacidad-leyenda', 'Leí y acepto el aviso de privacidad', TRUE);
```

- Clave natural como PK (mismo criterio que `rol.codigo` en `V004_1`): el catálogo es fijo y la clave es lo que viaja en la URL. No hace falta `BIGINT AUTO_INCREMENT`.
- `VARCHAR(4000)` y no `TEXT`: `TEXT` es `LONGVARCHAR` en MySQL y `CLOB` en H2, y `ddl-auto=validate` puede rechazar el mapeo de un `String` en uno de los dos motores. 4000 caracteres `utf8mb4` son ≤ 16 000 bytes, lejos del límite de fila de InnoDB (65 535).
- La FK a `usuario` no necesita índice extra (InnoDB y H2 lo crean). Los usuarios no se borran (C-04), así que la FK nunca bloquea una baja.
- `ck_parametro_autor_y_fecha` es la única regla de consistencia en la base; los tipos y rangos por clave se validan en el dominio (un `CHECK` por clave obligaría a reescribir el `CHECK` en cada change que agregue una clave).
- Archivo en UTF-8 (default de Flyway) por las tildes; el test de esquema verifica la ida y vuelta de `¿`, `á`, `ñ` en ambos motores.

### D5. Valores por defecto: datos de referencia en la migración **[Q1, Q3]**

La ficha dice "el valor por defecto se carga desde seed y se marca como provisorio". Se interpreta como dato de referencia en la migración, no como `SeedRecursosDemo`: C-17 necesita la anticipación también en despliegue y en el perfil `test`, y el seed de demo solo corre en `dev`. Es el mismo criterio que el catálogo `rol` (README de `db/migration`).

- Texto provisorio del aviso (sin comillas simples, para no escaparlas): identifica que es un **borrador pendiente de revisión legal**, que los datos (DNI, nombre, teléfono y email) se usan solo para gestionar turnos (confirmar, recordar, avisar cambios), que no se ceden a terceros, que el titular puede pedir acceso, rectificación o supresión (Ley 25.326, arts. 14 a 16) y que la Agencia de Acceso a la Información Pública es el órgano de control. El texto final lo define C-15 tras la revisión legal; el administrador lo reemplaza por la API.
- **Resolver PA-04 después**: una migración nueva `V007_2__anticipacion_minima_definitiva.sql` con `UPDATE parametro SET valor = '<N>' WHERE clave = 'anticipacion-minima-horas' AND provisorio = TRUE`, para no pisar un valor que el administrador ya confirmó. Nunca se edita `V007_1` después del merge.
- Alternativas: `ApplicationRunner` que inserta si falta (como `SeedAdministradorInicial`): agrega código y una carrera en el arranque sin beneficio; valor por defecto en `application.yml` con fila opcional: dos fuentes de verdad.

### D6. Servicio de aplicación y lectura tipada

- `ParametrosDelConsultorio` (interfaz en `aplicacion.configuracion`): `Duration anticipacionMinima()` y `AvisoPrivacidad avisoPrivacidad()` (`record AvisoPrivacidad(String texto, String leyendaAceptacion)`). C-17 y C-15 dependen de la interfaz y la reemplazan con Mockito en sus tests unitarios.
- `ServicioDeParametros implements ParametrosDelConsultorio` (`@Service`, `@Transactional`):
  - `listar()` (`readOnly`): las filas en el orden del catálogo (orden del enum), no el de la base.
  - `obtener(String clave)` (`readOnly`): `desdeCodigo` (404 si no existe) y la fila.
  - `actualizar(String clave, String valor, Long actorId)`: `desdeCodigo` → buscar → `parametro.actualizar(valor, actorId, Instant.now(reloj))` → guardar. Registra `Parámetro {} actualizado por el usuario {}` con SLF4J (sin el valor).
  - `anticipacionMinima()`: `Duration.ofHours(valorEntero())`, independiente de la zona horaria. `avisoPrivacidad()`: las dos filas de texto, aunque sigan provisorias.
  - Fila ausente para una clave del catálogo (solo si alguien borra a mano): `IllegalStateException("Falta el parámetro <código> en la base")` → 500 genérico. La previene `CatalogoDeParametrosConsistenciaTest` (D9).
- Inyecta el `Clock` de `RelojConfig` (spec zona-horaria-consultorio, "Hora actual controlable").
- Sin caché: cada lectura va a la base, así el cambio se ve de inmediato (spec "Cambio visible de inmediato").

### D7. API REST

Base `/api/parametros`, `@Tag(name = "Parámetros")`, `@SecurityRequirement(name = "bearerAuth")`, `@SoloAdministrador` en **cada** método (como `UsuariosController`).

| Método y ruta | Respuesta | Errores |
|---|---|---|
| `GET /api/parametros` | 200 `ParametroRespuesta[]` (orden del catálogo) | 401, 403 |
| `GET /api/parametros/{clave}` | 200 `ParametroRespuesta` | 401, 403, 404 |
| `PUT /api/parametros/{clave}` cuerpo `{"valor": "48"}` | 200 `ParametroRespuesta` actualizado | 400, 401, 403, 404 |

- `PUT` porque reemplaza el valor completo del recurso y es idempotente; no hay `POST` ni `DELETE` (catálogo cerrado).
- `ActualizarParametroSolicitud(@NotNull(message = "El valor es obligatorio") String valor)`. El valor viaja siempre como texto JSON, también el entero (Jackson acepta además un número y lo convierte a texto).
- `ParametroRespuesta(String clave, String descripcion, String tipo, String unidad, Integer minimo, Integer maximo, Integer longitudMaxima, String valor, boolean provisorio, Instant actualizadoEn, Long actualizadoPor)`: `tipo` en minúsculas (`entero`/`texto`); `unidad`, `minimo` y `maximo` solo para enteros; `longitudMaxima` solo para textos; `actualizadoEn` sale en ISO-8601 UTC (`2026-12-02T00:30:00Z`, default de Spring Boot).
- No se tocan `ConfiguracionSeguridad` (las rutas nacen protegidas) ni `ManejadorDeErrores`.
- Lectura solo para el administrador: recepción y profesional no necesitan los parámetros por HTTP; los procesos internos usan D6 **[Q5]**.

### D8. Ediciones simultáneas **[Q7]**

Última escritura gana. Cada `PUT` corre en su propia transacción y Hibernate escribe la fila completa (`UPDATE parametro SET valor, provisorio, actualizado_en, actualizado_por WHERE clave = ?`; sin `@DynamicUpdate`), que InnoDB aplica de forma atómica con bloqueo de fila: el resultado final es siempre una de las dos ediciones completas. Alternativa: columna `version` con bloqueo optimista y 409 ante un valor desactualizado; exige que el cliente envíe la versión y agrega un manejador en `ManejadorDeErrores` (archivo compartido) para un caso improbable con 1 administrador.

### D9. Estrategia de tests (TDD estricto, sin Docker)

| Nivel | Test | Cubre (spec) |
|---|---|---|
| Unitario | `ClaveParametroTest`, `ParametroTest` | catálogo, rango y tipo, textos, confirmación, "valor rechazado no confirma" |
| Unitario (Mockito, `Clock.fixed`) | `ServicioDeParametrosTest` | orden del listado, 404, instante UTC, `Duration`, aviso, fila ausente |
| Esquema H2 + MySQL | `RestriccionesParametrosBase` / `...H2Test` / `...MySqlTest` | valores por defecto, PK, `NOT NULL`, FK, `CHECK` autor-fecha, UTF-8 |
| Consistencia | `CatalogoDeParametrosConsistenciaTest` | "Catálogo y base coinciden" |
| JPA (`@DataJpaTest`, H2) | `ParametroRepositorioTest` | ida y vuelta con `Instant`, guardado de la fila completa |
| HTTP (`@SpringBootTest` + MockMvc, perfil `test`) | `ParametrosControllerTest` | lectura/edición, 400/404, 403 por rol, 401 sin token y con administrador desactivado, valor sin cambios tras un rechazo, instante con reloj fijo (`@TestConfiguration` con un `Clock` `@Primary`) |
| Concurrencia (`@Tag("mysql")`) | `EdicionSimultaneaParametrosMySqlTest` | "Dos administradores guardan a la vez" |
| Arquitectura (existentes) | `DominioSinFrameworksTest`, `GuardasEnControladoresTest` | dominio puro, guarda en cada endpoint |

Los tests con `@Transactional` revierten sus cambios; el de concurrencia hace commit, así que restaura el valor `24`, `provisorio = TRUE` y autor/fecha nulos en `@AfterEach` y borra los usuarios que creó.

## Risks / Trade-offs

- [`ddl-auto=validate` rechaza `BOOLEAN` o `VARCHAR(4000)` en MySQL] → C-03 ya validó `BOOLEAN` en MySQL 8; `VARCHAR` mapea directo a `String`. Si fallara, fijar `column-definition` en el XML y repetir `-Pit-mysql`.
- [El `CHECK` se ignora en MySQL < 8.0.16] → mismo requisito que C-03/C-04; el test MySQL lo verifica.
- [En MySQL la violación de `CHECK` llega como `UncategorizedSQLException`, no `DataIntegrityViolationException`] → como en C-03, el test exige `DataAccessException` cuyo mensaje nombre `ck_parametro_autor_y_fecha`.
- [Un test que hace commit deja la base H2 compartida con valores no provisorios y rompe "Base recién migrada"] → solo el test de concurrencia hace commit, y restaura en `@AfterEach`; los de valores por defecto no dependen del orden de ejecución.
- [El texto provisorio del aviso no tiene revisión legal] → queda marcado `provisorio` y C-15 lo bloquea por "revisión legal del texto del aviso"; no se presenta como definitivo.
- [24 h no es el valor que elija el equipo para PA-04] → es provisorio, editable por API y reemplazable con `V007_2` sin pisar un valor confirmado (D5).
- [Última escritura gana puede perder la edición de otro administrador] → aceptado para un consultorio con un administrador (D8, Q7).
- [Sin historial: no se sabe qué texto del aviso aceptó un paciente antes de un cambio] → C-15 registra la fecha del consentimiento y la fecha de la última modificación del aviso queda guardada; un historial queda como extensión (Q6).

## Migration Plan

1. `V007_1` se aplica sola al arrancar (todos los perfiles); en `dev` rige `spring.flyway.out-of-order=true` por si C-06/C-08 se mergean antes o después. Depende de `V004_1` (FK a `usuario`), ya en `main`.
2. Una línea en `application.yml` (`META-INF/orm/configuracion.xml`) y el ejemplo en `db/migration/README.md`, en un commit aparte (archivos compartidos).
3. Rollback: revertir el PR antes del despliegue. Si ya se aplicó en una base persistente, no se edita la migración: una nueva `V007_n` hace `DROP TABLE parametro` (no hay datos de pacientes en la tabla).

## Open Questions

Decisiones tomadas con la opción recomendada; cualquiera se puede cambiar antes de `/opsx:apply` (o después, con una tarea nueva):

- **Q1 (D2, D5)** — Modelo y origen del valor por defecto: tabla clave-valor con catálogo cerrado en el dominio y valores iniciales como datos de referencia en `V007_1` (recomendado) — alternativa: tabla tipada de una fila, o seed por `ApplicationRunner`. Afecta los grupos 2 y 3 de `tasks.md`.
- **Q2 (D2)** — Rango de la anticipación mínima: entero de **1 a 168 horas** (recomendado) — alternativa: permitir `0` ("se puede cancelar hasta el inicio") o ampliar a 336 h, o expresarla en minutos. Afecta solo la regla del enum y los tests 2.x.
- **Q3 (D5)** — Valor provisorio de PA-04: **24 horas** (recomendado, práctica habitual en consultorios) — alternativa: 12 o 48 horas. Afecta una línea de `V007_1` y un test.
- **Q4** — Pantalla de administración en el frontend: **fuera de C-07**, solo backend como dice la zona de la ficha y la versión mínima de DD-10; se edita desde Swagger UI (recomendado) — alternativa: una página `frontend/src/features/configuracion/` para el administrador (sube el tamaño a M).
- **Q5 (D7)** — Lectura pública del aviso: **la agrega C-15** bajo `/api/publico/...` usando `avisoPrivacidad()`; la API `/api/parametros` es solo para el administrador (recomendado) — alternativa: C-07 expone ya `GET /api/publico/aviso-privacidad` (texto y leyenda) para adelantar C-15/C-16.
- **Q6** — Versionado del aviso: **sin historial ni número de versión** en C-07; se guarda fecha y autor de la última modificación (recomendado) — alternativa: columna `version` + tabla `parametro_historial` para saber qué texto aceptó cada paciente (lo decidiría C-15).
- **Q7 (D8)** — Ediciones simultáneas: **última escritura gana**, fila atómica (recomendado) — alternativa: bloqueo optimista con `version` y 409.
