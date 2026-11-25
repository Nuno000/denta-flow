# Design — C-06 `horarios-y-bloqueos`

> Gobernanza **MEDIO**: se implementa con checkpoints (al cerrar los grupos 4, 7 y 9 de `tasks.md`) y las decisiones marcadas **[Q#]** se informan al final para que el equipo las confirme. Cambiar una solo toca las tareas que indica.

## Context

- Motivación y alcance: ver `proposal.md`. Requisitos: `specs/horarios-atencion`, `specs/bloqueos-agenda`, `specs/disponibilidad-recursos`.
- Backend en `main` (C-02, C-03, C-04, C-07, C-08): capas `dominio` (Java puro, verificado por `DominioSinFrameworksTest`), `aplicacion`, `infraestructura`, `presentacion`. Las clases de dominio se mapean con XML (`META-INF/orm/*.xml`, listados en `spring.jpa.mapping-resources`), Hibernate solo valida (`ddl-auto=validate`) y los repositorios son puertos en `dominio` implementados por interfaces Spring Data que extienden `Repository`.
- Recursos (C-03): `Profesional` y `Sillon` con `activo`; hoy no hay API de recursos (C-05) ni método de puerto para bloquear su fila, solo la prueba de humo `BloqueoRecursosTest` con `EntityManager.find(..., PESSIMISTIC_WRITE)`.
- Seguridad (C-04): JWT, denegación por defecto, guardas `@SoloAdministrador` y `@PersonalInterno`, principal `UsuarioAutenticado(id, email, rol, profesionalId)`; `GuardasEnControladoresTest` exige guarda en cada endpoint. `ManejadorDeErrores` traduce `ValidacionDeDominioException` → 400, `RecursoNoEncontradoException` → 404, `ConflictoException`/`OperacionNoPermitidaException` → 409, `AccessDeniedException` → 403 y Bean Validation → 400, todo `ProblemDetail` en español.
- Tiempo (C-02/C-03): `ZonaHorariaConsultorio` (zona fija, `aInstante`, `aHoraLocal`), `Clock` único UTC de `RelojConfig`; instantes en `DATETIME(6)` UTC mapeados a `Instant`.
- No existe la tabla `turno` (C-09): el aviso de turnos afectados no puede leer turnos reales.

## Goals / Non-Goals

**Goals:**
- Una regla de disponibilidad por recurso en Java puro, testeable sin Spring, que C-09 y C-10 consuman en vez de reescribirla.
- Horarios sin superposición aun con altas simultáneas, usando el mismo bloqueo de fila del profesional que va a usar C-09 (orden profesional → sillón).
- Un punto de extensión para el aviso de PA-11 que C-09 completa sin tocar C-06.

**Non-Goals:**
- Pantalla del panel (Q1), resolución de PA-11 (Q2), excepciones de horario por fecha, cálculo de huecos (C-10), historial de cambios de horarios y bloqueos (la auditoría RN-AU-01 es de turnos; se guarda solo el último autor del bloqueo).
- Caché: se lee de la base en cada consulta (volumen de un consultorio chico).

## Decisions

### D1. Capas y paquetes

| Capa | Paquete | Contenido |
|---|---|---|
| Dominio | `dominio/disponibilidad/` | `HorarioAtencion`, `Bloqueo`, `AmbitoBloqueo`, `MotivoBloqueo`, `FranjaLocal` (record día + horas), `CalendarioDeDisponibilidad` (regla pura), puertos `HorarioAtencionRepositorio` y `BloqueoRepositorio` |
| Aplicación | `aplicacion/disponibilidad/` | `ServicioDeHorarios`, `ServicioDeBloqueos`, `DisponibilidadDeRecursos` (interfaz para C-09/C-10) y su implementación `ServicioDeDisponibilidad`, puerto `TurnosAfectadosPorBloqueo` + record `TurnoAfectado`, implementación provisoria `SinTurnosAfectados` |
| Infraestructura | `infraestructura/persistencia/disponibilidad/` | `HorarioAtencionRepositorioJpa`, `BloqueoRepositorioJpa`, conversores de enum; `META-INF/orm/disponibilidad.xml` |
| Presentación | `presentacion/disponibilidad/` | `HorariosController`, `BloqueosController`, DTOs record con Bean Validation |

La ficha nombra `dominio/disponibilidad/` como zona; C-10 agrega el motor en el mismo paquete. Alternativa: `dominio/agenda/` — se descarta para seguir `08_arquitectura_propuesta.md` (`disponibilidad/  motor de disponibilidad, horarios, bloqueos`).

### D2. Horario como regla semanal en hora local

`HorarioAtencion(id, profesionalId, DayOfWeek dia, LocalTime inicio, LocalTime fin)`. Es una regla de reloj de pared que se repite cada semana, no un instante: por eso se guarda como `LocalTime` + día y **no** se convierte a UTC (convertirla rompería si la zona tuviera horario de verano). La regla "nunca `LocalDateTime` para turnos" sigue intacta: los instantes concretos (bloqueos, consultas) son `Instant`. Validaciones en el dominio: `inicio < fin`, segundos y nanos en cero, profesional con id. El horario no cruza la medianoche (máximo hasta 23:59): un consultorio no atiende de noche.

Superposición: `seSuperponeCon(otro)` = mismo profesional, mismo día e `inicio < otro.fin && otro.inicio < fin`. La verificación contra los existentes vive en el dominio (`HorarioAtencion.verificarSinSuperposicion(nuevo, existentes)`, excluye el propio id en la edición) y lanza `ConflictoException` con el rango en conflicto.

Alternativa considerada: reemplazar la semana completa de un profesional con un `PUT` de lista **[Q3]**. Más cómodo para un formulario, pero la matriz RBAC pide C/R/U/D por rango y el alta individual da errores más claros; un `PUT` de lista se puede sumar después sobre el mismo servicio.

### D3. Bloqueo con ámbito, instantes UTC y último autor

`Bloqueo(id, AmbitoBloqueo ambito, Long profesionalId, Long sillonId, Instant inicio, Instant fin, MotivoBloqueo motivo, String detalle, Long modificadoPor, Instant modificadoEn)`. Fábricas `deProfesional`, `deSillon`, `delConsultorio` garantizan la coherencia ámbito-recurso; `reprogramar(inicio, fin, motivo, detalle, actor, instante)` edita sin cambiar ámbito ni recurso. `afectaAProfesional(id)` = ámbito `CONSULTORIO` o `PROFESIONAL` con ese id (idem sillón). `seSuperponeCon(inicio, fin)` con intervalos semiabiertos. Detalle opcional recortado con `ValidacionesDeTexto.opcional` (200).

Motivo: `VACACIONES | FERIADO | AUSENCIA | OTRO` **[Q4]** (la KB lista los tres primeros; `OTRO` + detalle cubre mantenimiento de un sillón u otros casos).

### D4. Esquema `V006_1__crear_horarios_y_bloqueos.sql`

Nombre por la convención DD-09 (`V0NN_<n>` con NN = número del change); la ficha dice "Migración 002", numeración previa a esa convención.

```sql
CREATE TABLE horario_atencion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    dia_semana     INT    NOT NULL,          -- ISO-8601: 1 = lunes ... 7 = domingo
    hora_inicio    TIME   NOT NULL,
    hora_fin       TIME   NOT NULL,
    CONSTRAINT fk_horario_atencion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT ck_horario_atencion_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_atencion_rango CHECK (hora_inicio < hora_fin)
) ENGINE=InnoDB;
CREATE INDEX idx_horario_atencion_profesional_dia ON horario_atencion (profesional_id, dia_semana, hora_inicio);

CREATE TABLE bloqueo (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ambito         VARCHAR(20)  NOT NULL,
    profesional_id BIGINT       NULL,
    sillon_id      BIGINT       NULL,
    inicio         DATETIME(6)  NOT NULL,
    fin            DATETIME(6)  NOT NULL,
    motivo         VARCHAR(20)  NOT NULL,
    detalle        VARCHAR(200) NULL,
    modificado_por BIGINT       NOT NULL,
    modificado_en  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_bloqueo_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT fk_bloqueo_sillon FOREIGN KEY (sillon_id) REFERENCES sillon (id),
    CONSTRAINT fk_bloqueo_modificado_por FOREIGN KEY (modificado_por) REFERENCES usuario (id),
    CONSTRAINT ck_bloqueo_ambito CHECK (ambito IN ('PROFESIONAL', 'SILLON', 'CONSULTORIO')),
    CONSTRAINT ck_bloqueo_motivo CHECK (motivo IN ('VACACIONES', 'FERIADO', 'AUSENCIA', 'OTRO')),
    CONSTRAINT ck_bloqueo_rango CHECK (inicio < fin),
    CONSTRAINT ck_bloqueo_recurso_segun_ambito CHECK (
        (ambito = 'PROFESIONAL' AND profesional_id IS NOT NULL AND sillon_id IS NULL) OR
        (ambito = 'SILLON'      AND sillon_id IS NOT NULL AND profesional_id IS NULL) OR
        (ambito = 'CONSULTORIO' AND profesional_id IS NULL AND sillon_id IS NULL))
) ENGINE=InnoDB;
CREATE INDEX idx_bloqueo_profesional_rango ON bloqueo (profesional_id, inicio, fin);
CREATE INDEX idx_bloqueo_sillon_rango ON bloqueo (sillon_id, inicio, fin);
CREATE INDEX idx_bloqueo_ambito_rango ON bloqueo (ambito, inicio, fin);
```

- `dia_semana INT` y no `TINYINT`: evita diferencias de tipo en `ddl-auto=validate` entre H2 y MySQL; el mapeo usa un conversor `DayOfWeek ↔ int`.
- `TIME` ↔ `LocalTime` sin zona (D2). `DATETIME(6)` ↔ `Instant` con `hibernate.jdbc.time_zone=UTC`, igual que C-08.
- FK sin acciones `ON DELETE`: MySQL prohíbe acciones referenciales en columnas usadas por un `CHECK`, y los recursos no se borran (C-03).
- No hay restricción de base contra horarios superpuestos (MySQL no tiene exclusión por rangos): la garantiza D6. Nombres de restricción únicos en todo el esquema (H2).

### D5. Regla de disponibilidad pura (`CalendarioDeDisponibilidad`)

Recibe los datos ya leídos y no toca la base:

- `profesionalDisponible(activo, horariosDelProfesional, bloqueosQueLoAfectan, inicio, fin)`: convierte con `ZonaHorariaConsultorio.aHoraLocal`; si `inicio` y `fin` locales son de fechas distintas → no disponible (salvo `fin` exactamente a las 00:00 del día siguiente, que tampoco se acepta porque el horario termina como máximo 23:59); une los rangos contiguos del día y exige que `[horaInicio, horaFin)` quede contenido en uno; ningún bloqueo superpuesto.
- `sillonDisponible(activo, bloqueosQueLoAfectan, inicio, fin)`: activo y sin bloqueos superpuestos.
- `franjasDelDia(horarios, fecha)`: rangos del día unidos, como `FranjaLocal`; base para C-10.

`ServicioDeDisponibilidad` (aplicación, `@Transactional(readOnly = true)`) implementa `DisponibilidadDeRecursos` con `profesionalDisponible(id, inicio, fin)`, `sillonDisponible(id, inicio, fin)`, `franjasDeAtencion(profesionalId, LocalDate)` y `bloqueosQueAfectan(profesionalId, LocalDate)`: lee recurso, horarios del día y bloqueos superpuestos (`BloqueoRepositorio.listarQueAfectanAProfesional(id, inicio, fin)` / `...ASillon`) y delega en el calendario. No toma bloqueos de fila: C-09 lo llama **después** de bloquear profesional y sillón dentro de su propia transacción, así lee el último estado confirmado.

### D6. Concurrencia de horarios: bloqueo de la fila del profesional

`ServicioDeHorarios.crear/editar` (`@Transactional`) primero bloquea la fila del profesional con `ProfesionalRepositorio.bloquearPorId(id)` (`@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`), después lee los rangos del día y verifica la superposición, y recién entonces guarda. Dos altas para el mismo profesional se serializan; profesionales distintos no se esperan. Es el mismo método y el mismo orden (profesional antes que sillón) que va a usar C-09, así que no introduce interbloqueos.

Alternativas: `UNIQUE (profesional_id, dia_semana, hora_inicio)` (no detecta superposición parcial); bloqueo optimista con `version` en `profesional` (agrega columna a una tabla de C-03 y reintentos).

Se agregan `bloquearPorId(Long)` a `ProfesionalRepositorio` y `SillonRepositorio` (archivos de C-03) porque C-06 y C-09 los necesitan; C-09 debe reutilizarlos y no crear otros.

### D7. Bloqueos: aviso de turnos afectados sin resolver PA-11 **[Q2]**

`ServicioDeBloqueos.crear/editar`: valida recurso (existe → si no 404; activo → si no `OperacionNoPermitidaException` 409), construye el bloqueo con el actor y `Instant.now(reloj)`, y para ámbitos `PROFESIONAL`/`SILLON` toma antes `bloquearPorId` del recurso, para serializarse con la creación de turnos de C-09 y que la lista de afectados sea coherente. Guarda y llama a `TurnosAfectadosPorBloqueo.listar(bloqueo)`; devuelve `ResultadoBloqueo(bloqueo, List<TurnoAfectado>)`.

`TurnosAfectadosPorBloqueo` es un puerto de aplicación; en C-06 lo implementa `SinTurnosAfectados` (lista vacía, `@Component`). C-09 lo reemplaza por la consulta real sobre `turno` (borra o desactiva la implementación provisoria). Ningún camino modifica turnos.

Alternativa: rechazar con 409 si hay turnos afectados salvo confirmación explícita. Sería decidir PA-11; con la lista vacía el comportamiento observable de C-06 es igual en ambos casos, así que se difiere.

### D8. API REST

| Método y ruta | Guarda | Respuesta |
|---|---|---|
| `GET /api/profesionales/{id}/horarios` | `@PersonalInterno` + "propio" si rol profesional | 200 lista ordenada (día ISO, `inicio`/`fin` `HH:mm`) |
| `POST /api/profesionales/{id}/horarios` | `@SoloAdministrador` | 201 + `Location`; 400 / 404 / 409 |
| `PUT /api/horarios/{id}` | `@SoloAdministrador` | 200; 400 / 404 / 409 |
| `DELETE /api/horarios/{id}` | `@SoloAdministrador` | 204; 404 |
| `GET /api/bloqueos?desde&hasta&profesionalId&sillonId` | `@PersonalInterno` (rol profesional: se fuerza su profesional + consultorio) | 200 lista por inicio, id |
| `GET /api/bloqueos/{id}` | `@PersonalInterno` + "propio o del consultorio" si rol profesional | 200; 403; 404 |
| `POST /api/bloqueos` | `@SoloAdministrador` | 201 `{bloqueo, turnosAfectados}`; 400 / 404 / 409 |
| `PUT /api/bloqueos/{id}` | `@SoloAdministrador` | 200 `{bloqueo, turnosAfectados}`; 400 / 404 |
| `DELETE /api/bloqueos/{id}` | `@SoloAdministrador` | 204; 404 |

- Horas de horario como `"HH:mm"` (`LocalTime` con `@JsonFormat`); día como `LUNES … DOMINGO` (enum propio de presentación ↔ `DayOfWeek`).
- Instantes de bloqueo como `OffsetDateTime` ISO-8601 **con desplazamiento obligatorio** en la entrada (un texto sin desplazamiento falla la deserialización → 400) y en la salida convertidos a la zona del consultorio (`-03:00`). Así la API no usa `LocalDateTime` y no hay ambigüedad.
- `PUT /api/bloqueos/{id}` acepta solo `inicio`, `fin`, `motivo`, `detalle`; si el cuerpo trae `ambito`, `profesionalId` o `sillonId` distintos del guardado → 400 (spec "Intento de cambiar el ámbito"). Se resuelve con un DTO que los acepta opcionales y el servicio compara.
- La regla "propio" se verifica en el controlador con el principal (`rol == PROFESIONAL` → `profesionalId` del token; si es nulo o distinto → `AccessDeniedException` → 403). La lógica vive en una clase chica `AccesoPorProfesional` en `presentacion/disponibilidad/` para probarla sola.
- Rango de consulta: `desde` < `hasta`, máximo 366 días → 400 en otro caso.
- Sin cambios en `ConfiguracionSeguridad` ni en `ManejadorDeErrores`.

### D9. Datos de demostración

`SeedRecursosDemo` (solo `dev`, `app.seed.demo`) agrega a cada profesional demo lunes a viernes 09:00–13:00 y 14:00–18:00, de forma idempotente (no duplica si ya existen). Sin datos personales reales. El perfil `test` no lo corre (`SeedNoCorreEnTestTest`).

### D10. Estrategia de tests (TDD estricto, sin Docker)

| Nivel | Test | Cubre (spec) |
|---|---|---|
| Unitario | `HorarioAtencionTest`, `BloqueoTest`, `CalendarioDeDisponibilidadTest` | validaciones, superposición y contigüidad, ámbitos, bordes semiabiertos, día del consultorio vs UTC, cruce de medianoche, inactivos |
| Unitario (Mockito, `Clock.fixed`) | `ServicioDeHorariosTest`, `ServicioDeBloqueosTest`, `ServicioDeDisponibilidadTest` | orden bloquear → leer → guardar, 404/409, último autor, aviso con un doble del puerto que devuelve un turno (spec "Turnos existentes no se tocan") |
| Esquema H2 + MySQL | `RestriccionesDisponibilidadBase` / `...H2Test` / `...MySqlTest` | `CHECK` de día, rango, ámbito-recurso, motivo; FK; `NOT NULL` |
| JPA (`@DataJpaTest`, H2) | `HorarioAtencionRepositorioTest`, `BloqueoRepositorioTest` | ida y vuelta `LocalTime`/`Instant`, consultas de superposición y de afectación |
| HTTP (`@SpringBootTest` + MockMvc) | `HorariosControllerTest`, `BloqueosControllerTest`, `AccesoPorProfesionalTest` | CRUD, 400/404/409, 401/403 por rol, vista del profesional, formato con desplazamiento, OpenAPI |
| Concurrencia (`@Tag("mysql")`, `it-mysql`) | `AltaSimultaneaHorariosMySqlTest` | "Dos altas superpuestas a la vez" (`@RepeatedTest(10)`, dos hilos con `CountDownLatch`, exactamente una gana) |
| Arquitectura (existentes) | `DominioSinFrameworksTest`, `GuardasEnControladoresTest` | dominio puro, guarda en cada endpoint |

El test de concurrencia hace commit: borra en `@AfterEach` los horarios, el profesional y los usuarios que creó.

## Risks / Trade-offs

- [Una reserva de C-09 y un bloqueo de consultorio simultáneos: el turno puede confirmarse en una franja recién bloqueada] → bloqueos de profesional o sillón toman el mismo `FOR UPDATE` que C-09 y se serializan; para el ámbito consultorio no se bloquean todos los recursos (un administrador, ventana de milisegundos) y el turno aparecería en el aviso de una edición posterior. Aceptado y documentado para C-09.
- [`ddl-auto=validate` rechaza `TIME` o `INT` frente a `LocalTime`/conversor en MySQL] → la subclase MySQL del test de esquema arranca el contexto con `validate`; si falla, fijar `column-definition` en el XML.
- [En MySQL la violación de `CHECK` llega como `UncategorizedSQLException`] → como en C-03/C-07, el test exige `DataAccessException` cuyo mensaje nombre la restricción.
- [H2 no reproduce el `FOR UPDATE` de InnoDB] → la superposición simultánea solo cuenta como verificada con `-Pit-mysql` (DD-09).
- [Tocar puertos y seed de C-03 choca con C-05 en paralelo] → cambios aditivos (un método nuevo por puerto, un bloque nuevo en el seed), en un commit aparte y avisados a Renzo (C-05/C-09).
- [El profesional sin `profesionalId` en su usuario] → recibe 403 en las consultas "propias"; no ve datos de otros.
- [La lista de turnos afectados siempre vacía hasta C-09 puede dar falsa tranquilidad] → el campo se documenta en OpenAPI como "se completa a partir de C-09" y la ficha de C-09 lo lista como tarea.

## Migration Plan

1. `V006_1` se aplica sola al arrancar en todos los perfiles; depende de `V003_1` (recursos) y `V004_1` (usuarios), ya en `main`. En `dev` rige `spring.flyway.out-of-order=true` (V007 y V008 ya aplicadas en bases locales).
2. Archivos compartidos (`application.yml`, `db/migration/README.md`, puertos de C-03, `SeedRecursosDemo`) en un commit aparte.
3. Rollback: revertir el PR antes del despliegue. Si ya se aplicó en una base persistente, no se edita la migración: una `V006_2` nueva hace `DROP TABLE bloqueo; DROP TABLE horario_atencion;` (no hay datos de pacientes).

## Open Questions

Decisiones tomadas con la opción recomendada; cualquiera se puede cambiar antes de `/opsx:apply`:

- **Q1** — Pantalla del panel: **fuera de C-06**, solo backend como dice la zona de la ficha; se opera desde Swagger UI (recomendado) — alternativa: `frontend/src/features/agenda/` con formulario semanal y lista de bloqueos (sube el tamaño a L). Afectaría un grupo nuevo de tareas.
- **Q2 (D7)** — PA-11: **guardar el bloqueo, listar los turnos afectados y no tocarlos** (recomendado; no decide la resolución) — alternativa: rechazar con 409 salvo confirmación. Con turnos inexistentes hasta C-09 no cambia el comportamiento observable de C-06.
- **Q3 (D2)** — Edición del horario **por rango individual** (recomendado) — alternativa o agregado: `PUT /api/profesionales/{id}/horarios` con la semana completa.
- **Q4 (D3)** — Motivos `VACACIONES | FERIADO | AUSENCIA | OTRO` + detalle libre (recomendado) — alternativa: solo los tres de la KB.
- **Q5** — Recursos inactivos: **no se aceptan horarios ni bloqueos nuevos** (409) y la disponibilidad los considera no disponibles (recomendado) — alternativa: permitir bloqueos sobre inactivos.
- **Q6 (D8)** — Instantes con desplazamiento obligatorio en la API (recomendado) — alternativa: aceptar fecha-hora local sin desplazamiento interpretada en la zona del consultorio.

## UI Mockups

- Design system revision: — (`design-system/README.md`: `Mode: off`)
- Decision: no (no UI; además política del proyecto `off`) — 2026-11-25
