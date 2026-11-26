# Design — C-20 `lista-de-espera`

## 1. Arquitectura y capas

Se respeta la separación en capas de `knowledge-base/08_arquitectura_propuesta.md`:

- **Dominio (`ar.dentaflow.dominio.listaespera`)**:
  - `ListaEspera` (entidad JPA / agregado): `id`, `pacienteId`, `prestacionId`, `profesionalId` (nullable), `fechaDesde` (`LocalDate`), `fechaHasta` (`LocalDate`), `horaDesde` (`LocalTime`, nullable), `horaHasta` (`LocalTime`, nullable), `estado` (`EstadoListaEspera`: `ACTIVA`, `OFRECIDA`, `ATENDIDA`, `VENCIDA`), `turnoOrigenId` (nullable), `profesionalOfrecidoId` (nullable), `inicioOfrecido` (`Instant`, nullable), `tokenAceptacion` (`String`, nullable, único), `ofrecidaEn` (`Instant`, nullable), `ofrecidaHasta` (`Instant`, nullable), `turnoCreadoId` (nullable), `creadoEn` (`Instant`), `creadoPorUsuarioId` (`Long`).
  - `EstadoListaEspera` con conversor JPA a minúsculas (`activa`, `ofrecida`, `atendida`, `vencida`).
  - `ListaEsperaRepositorio` (puerto de dominio) implementado en `infraestructura.persistencia.listaespera.ListaEsperaRepositorioJpa`.
- **Configuración (`ar.dentaflow.dominio.configuracion`)**:
  - `ClaveParametro.LISTA_ESPERA_PLAZO_OFERTA_HORAS("lista-espera-plazo-oferta-horas", ..., TipoParametro.ENTERO, "horas", 1, 72, null, "plazo de oferta de lista de espera")`.
- **Aplicación (`ar.dentaflow.aplicacion.listaespera`)**:
  - `ServicioDeListaDeEspera`:
    - `registrar(SolicitudListaEspera, long usuarioId)`: valida paciente (no anonimizado), prestación activa, profesional activo y habilitado si se especificó, rango de fechas/horas válido.
    - `listar(EstadoListaEspera estadoOpcional)`: consulta ordenada por `creadoEn` descendente enriquecida con datos de paciente, prestación y profesional.
    - `darDeBaja(long id)`: solo si está `ACTIVA`, la pasa a `VENCIDA`.
    - `vencerOfertasExpiradas()`: actualiza a `VENCIDA` las entradas `OFRECIDA` con `ofrecidaHasta <= reloj.instant()`.
  - `OferenteDeListaDeEspera`:
    - Escucha `TurnoCanceladoEvent` (publicado por `ServicioDeTurnos.cambiarEstado` cuando el nuevo estado es `CANCELADO`) mediante `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` con `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
    - Verifica en `MotorDeDisponibilidad` que el hueco liberado (`inicio` del turno cancelado, duración de la prestación, `profesionalId`) siga disponible.
    - Busca entradas `ACTIVA` compatibles (misma prestación, `profesionalId` nulo o igual, `fechaDesde <= fechaLocal <= fechaHasta`, y si `horaDesde`/`horaHasta` están presentes, `horaDesde <= horaLocal < horaHasta`), excluyendo pacientes anonimizados.
    - Marca cada entrada compatible como `OFRECIDA` con token seguro (`GeneradorDeTokensSeguro`), `ofrecidaHasta = ahora + plazoHoras` y dispara `ServicioDeNotificacionesListaEspera.enviarOferta(...)` tras el commit.
  - `AceptadorDeOfertaListaEspera`:
    - `consultarPorToken(String token)`: resuelve la entrada; si estaba `OFRECIDA` y venció, la marca `VENCIDA`.
    - `aceptar(String token)` (`@Transactional(isolation = Isolation.READ_COMMITTED)`):
      1. Bloquea la fila de `ListaEspera` (`SELECT ... FOR UPDATE` por `tokenAceptacion`).
      2. Verifica que esté `OFRECIDA` y no vencida (`ofrecidaHasta > ahora`). Si venció, la marca `VENCIDA` y lanza `ConflictoException("La oferta ya venció")`.
      3. Consulta `MotorDeDisponibilidad` para obtener un sillón libre (`CandidatoDeReserva`) para `(prestacionId, profesionalOfrecidoId, inicioOfrecido)` — si no hay candidatos, marca la entrada como `VENCIDA` y lanza `ConflictoException("El horario ya fue tomado")`.
      4. Llama a `ServicioDeTurnos.crear(...)` con `OrigenTurno.ONLINE` y `Actor.paciente()`, que adquiere el bloqueo pesimista `FOR UPDATE` en orden `profesional -> sillón` (`DD-09`). Si `ServicioDeTurnos.crear` falla por solapamiento (`TurnoSolapadoException` / `ConflictoException`), marca la entrada como `VENCIDA` y propaga `ConflictoException("El horario ya fue tomado")`.
      5. Si el turno se creó, marca la entrada como `ATENDIDA` (`turnoCreadoId = turno.id()`) y marca como `VENCIDA` todas las demás entradas `OFRECIDA` para ese mismo hueco (`turnoOrigenId` o mismo `profesionalOfrecidoId` + `inicioOfrecido`).
- **Notificaciones (`ar.dentaflow.aplicacion.notificaciones`)**:
  - `PlantillaOfertaListaEspera` + `ServicioDeNotificacionesListaEspera`: construye el enlace `{APP_BASE_URL}/lista-espera/aceptar?token={token}` y envía el email de forma asíncrona en `EjecutorDeEmails` (sin bloquear la cancelación).
- **Presentación (`ar.dentaflow.presentacion.listaespera` y `presentacion.publico`)**:
  - `ListaEsperaController` (`/api/lista-espera`, `@AdministradorORecepcion`): `GET`, `POST`, `DELETE /{id}`.
  - `ListaEsperaPublicoController` (`/api/publico/lista-espera/{token}`): `GET` (detalle de la oferta) y `POST /aceptar` (acepta la oferta). Permitido en `ConfiguracionSeguridad` sin autenticación.

## 2. Esquema de base de datos (`V020_1__crear_lista_de_espera.sql`)

SQL compatible con MySQL 8 (`InnoDB`, `BIGINT AUTO_INCREMENT`, `DATETIME(6)`) y H2 `MODE=MySQL`:

```sql
CREATE TABLE lista_espera (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id             BIGINT      NOT NULL,
    prestacion_id           BIGINT      NOT NULL,
    profesional_id          BIGINT      NULL,
    fecha_desde             DATE        NOT NULL,
    fecha_hasta             DATE        NOT NULL,
    hora_desde              TIME        NULL,
    hora_hasta              TIME        NULL,
    estado                  VARCHAR(20) NOT NULL,
    turno_origen_id         BIGINT      NULL,
    profesional_ofrecido_id BIGINT      NULL,
    inicio_ofrecido         DATETIME(6) NULL,
    token_aceptacion        VARCHAR(64) NULL,
    ofrecida_en             DATETIME(6) NULL,
    ofrecida_hasta          DATETIME(6) NULL,
    turno_creado_id         BIGINT      NULL,
    creado_por_usuario_id   BIGINT      NOT NULL,
    creado_en               DATETIME(6) NOT NULL,
    CONSTRAINT uk_lista_espera_token UNIQUE (token_aceptacion),
    CONSTRAINT ck_lista_espera_estado CHECK (estado IN ('activa', 'ofrecida', 'atendida', 'vencida')),
    CONSTRAINT ck_lista_espera_fechas CHECK (fecha_hasta >= fecha_desde),
    CONSTRAINT ck_lista_espera_horas CHECK (
        (hora_desde IS NULL AND hora_hasta IS NULL)
        OR (hora_desde IS NOT NULL AND hora_hasta IS NOT NULL AND hora_hasta > hora_desde)
    ),
    CONSTRAINT fk_lista_espera_paciente    FOREIGN KEY (paciente_id)             REFERENCES paciente (id),
    CONSTRAINT fk_lista_espera_prestacion  FOREIGN KEY (prestacion_id)           REFERENCES prestacion (id),
    CONSTRAINT fk_lista_espera_profesional FOREIGN KEY (profesional_id)          REFERENCES profesional (id),
    CONSTRAINT fk_lista_espera_prof_of     FOREIGN KEY (profesional_ofrecido_id) REFERENCES profesional (id),
    CONSTRAINT fk_lista_espera_turno_orig  FOREIGN KEY (turno_origen_id)         REFERENCES turno (id),
    CONSTRAINT fk_lista_espera_turno_crea  FOREIGN KEY (turno_creado_id)         REFERENCES turno (id),
    CONSTRAINT fk_lista_espera_creado_por  FOREIGN KEY (creado_por_usuario_id)   REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE INDEX ix_lista_espera_matching ON lista_espera (estado, prestacion_id, profesional_id, fecha_desde, fecha_hasta);
CREATE INDEX ix_lista_espera_vencimiento ON lista_espera (estado, ofrecida_hasta);

INSERT INTO parametro (clave, valor, provisorio) VALUES
    ('lista-espera-plazo-oferta-horas', '4', TRUE);
```

## 3. Frontend (`frontend/src/features/lista-espera/`)

- **Panel interno (`/panel/lista-espera`, `Administrador` y `Recepcion`)**:
  - Link "Lista de espera" en `BarraDelPanel.tsx` visible para `administrador` y `recepcion`.
  - Listado con filtro por estado (`Todas`, `Activas`, `Ofrecidas`, `Atendidas`, `Vencidas`), botón de baja para entradas activas y formulario de alta (búsqueda de paciente por DNI reutilizando `POST /api/pacientes/busqueda` o creación rápida, selector de prestación, profesional opcional, rango de fechas y rango horario opcional).
- **Pantalla pública (`/lista-espera/aceptar`, sin auth)**:
  - Lee `?token=...` (o `/lista-espera/aceptar/:token`), muestra los datos del hueco ofrecido (prestación, profesional, fecha y hora en `America/Argentina/Buenos_Aires`, vencimiento) y botón "Aceptar turno".
  - Maneja estados: oferta vigente, oferta ya tomada (`409`), oferta vencida o enlace inválido.

## 4. Concurrencia y pruebas

- Todos los tests de unidad e integración sobre H2 (`./mvnw test`).
- Test de concurrencia `@Tag("mysql")` (`AceptacionListaEsperaConcurrenciaMySqlTest`) ejecutado con `./mvnw verify -Pit-mysql` contra MySQL 8 nativo verificando que dos aceptaciones simultáneas sobre la misma oferta o sobre el mismo hueco liberado producen exactamente un turno creado y un `409`, sin deadlocks ni solapamientos.
- Tests de frontend en Vitest + Testing Library (`npm test` y `npm run build`).
