# Design — C-10 `motor-disponibilidad`

> Gobernanza **CRÍTICO**: este documento es análisis. No se escribe código hasta que (1) C-06 esté en `main` y se haya revisado "Qué asume C-10 de C-06" (`proposal.md`, tarea 0.2) y (2) Renzo apruebe Q1..Q15. Cambiar una decisión solo toca las tareas que se indican.

## Context

- Motivación y alcance: `proposal.md`. Requisitos: `specs/motor-disponibilidad/spec.md` y `specs/turnos/spec.md`.
- **C-09 (en `main`)**: `ServicioDeTurnos` es la única vía de escritura. `crear` y `reprogramar` son `@Transactional(isolation = READ_COMMITTED)`, exigen ese aislamiento si se los envuelve (`exigirAislamientoReadCommitted` → `IllegalStateException`), toman `CerrojoDeRecursos.bloquearProfesional` y después `bloquearSillon`, validan recursos activos y `profesional.realiza(prestación)`, evalúan las `RestriccionDeAgenda` registradas (`ObjectProvider.orderedStream()`, también para sobreturnos) y después el solapamiento (`TurnoRepositorio.haySolapamientoDe{Profesional,Sillon}`, estados `ocupaRecursos()` = no cancelado, `es_sobreturno = FALSE`, `[inicio, fin)`). Conflicto → `TurnoSolapadoException(RecursoOcupado)` → 409. Contrato explícito: "quien prueba varios candidatos (C-10) llama a `crear` una vez por transacción y captura `TurnoSolapadoException` por candidato"; `PessimisticLockingFailureException` se propaga sin reintento. Hoy **no hay ningún bean `RestriccionDeAgenda`**: crear un turno fuera de horario funciona.
- **C-06 (rama `upstream/feat/C-06-horarios-y-bloqueos`, `fce21ec`, grupos 1–7 hechos, sin mergear, base anterior a C-09)**: ver la lista "Qué asume C-10 de C-06" en `proposal.md`. Lo relevante: `DisponibilidadDeRecursos` (`profesionalDisponible`, `sillonDisponible`, `franjasDeAtencion`, `bloqueosQueAfectan`), regla pura `CalendarioDeDisponibilidad`, puertos `HorarioAtencionRepositorio` y `BloqueoRepositorio`, capability `disponibilidad-recursos` (disponibilidad de **un recurso** en **un rango**). C-06 deja explícito que el cálculo de huecos por prestación y la asignación son de C-10, y que "C-10 agrega el motor en el mismo paquete" `dominio/disponibilidad`.
- **C-03/C-05**: `ProfesionalRepositorio.listarActivosQueRealizan(prestacionId)` (id ascendente), `SillonRepositorio.listarActivos()` (id ascendente, "el mismo orden con el que se bloquean y se elige el primer sillón libre"), `Prestacion.getDuracionMinutos()` / `isActiva()`.
- **Tiempo**: `ZonaHorariaConsultorio` (`ZONA`, `aInstante`, `aHoraLocal`, `hoy(Clock)`), `Clock` UTC único (`RelojConfig`).
- **Seguridad**: `/api/publico/**` ya es público (`ConfiguracionSeguridad`) y reservado para C-10/C-16 en `autorizacion-por-rol`; `GuardasEnControladoresTest` exime a los controladores del paquete `presentacion.publico`.
- **Consumidores** (fichas de `CHANGES.md`): C-16 muestra huecos y reserva (C-19 espera 201/409 en `ReservaPublicaConcurrenciaMySqlTest`); C-12 sugiere la alternativa más cercana; C-17 reprograma "solo a huecos válidos del motor"; C-20 reserva al aceptar una oferta.

## Goals / Non-Goals

**Goals:**
- Un solo cálculo de huecos y una sola estrategia de asignación, reutilizando la regla de C-06 (no reescribirla) y el criterio de ocupación de C-09.
- Que **ninguna** escritura de turnos pueda saltear horarios y bloqueos (RN-TU-04, RN-GL-02), enchufando la regla en `RestriccionDeAgenda` sin tocar `ServicioDeTurnos`.
- Una reserva "cualquiera" que pruebe candidatos sin retener bloqueos de recursos que no usa (DD-09).
- Contrato estable para C-12/C-16/C-17/C-20 aunque cambien detalles internos de C-06.

**Non-Goals:**
- Pantallas, endpoint de creación de la reserva pública, aviso de privacidad, email (C-11, C-12, C-14, C-15, C-16).
- Caché de huecos (Q8), reservas tentativas/"hold" del hueco, rate limiting.
- Huecos alineados al fin de cada turno (grilla fija, Q1), excepciones de horario por fecha (C-06 las deja fuera), preferencia de sillón por prestación.
- Permisos de sobreturno y anticipación mínima de cancelación (C-12, C-17).

## Decisions

### D1. Capas, paquetes y nombres (sin colisión con C-06)

| Capa | Paquete | Clases nuevas |
|---|---|---|
| Dominio | `dominio/disponibilidad/` | `CalculadoraDeHuecos` (regla pura), records `Hueco(Instant inicio, Instant fin, List<CandidatoDeReserva> candidatos)`, `CandidatoDeReserva(Long profesionalId, Long sillonId)`, `AgendaDelRango` (datos ya leídos), `AgendaNoDisponibleException extends ConflictoException`, `HuecoNoDisponibleException extends ConflictoException` |
| Dominio | `dominio/turnos/` (C-09, aditivo) | record `OcupacionDeRecurso(Long recursoId, Instant inicio, Instant fin)`; dos métodos de lectura en `TurnoRepositorio` (D4) |
| Aplicación | `aplicacion/agenda/` | `MotorDeDisponibilidad` (consulta), `ConsultaDeHuecos`, `ServicioDeReservaDeHuecos` (reserva por candidato), `SolicitudDeReservaDeHueco`, `RestriccionDeHorarioYBloqueos implements RestriccionDeAgenda`, `ReglasDeConsultaDeHuecos(pasoMinutos, diasMaximos)` |
| Infraestructura | `infraestructura/persistencia/turnos/` (aditivo), `infraestructura/agenda/` | dos `@Query` en `TurnoRepositorioJpa`; `PropiedadesDeDisponibilidad` (`@ConfigurationProperties("app.disponibilidad")`) que construye `ReglasDeConsultaDeHuecos` |
| Presentación | `presentacion/publico/` | `HuecosPublicosController`, record `HuecoPublicoRespuesta` |

Nombres elegidos para no chocar con `ServicioDeDisponibilidad`, `DisponibilidadDeRecursos` y `CalendarioDeDisponibilidad` de C-06. `aplicacion/agenda/` es la zona de la ficha. Capability nueva `motor-disponibilidad` (C-06 usa `disponibilidad-recursos`). Sin migración (no hay `V010_*`).

### D2. Regla pura `CalculadoraDeHuecos` (Java puro, sin Spring)

Entrada: `AgendaDelRango` con duración, fechas `desde..hasta`, `ahora`, paso, profesionales candidatos en orden, y por profesional: activo, horarios (`HorarioAtencion`), bloqueos que lo afectan; sillones activos en orden con sus bloqueos; ocupaciones de profesionales y de sillones. Salida: `List<Hueco>` ordenada por inicio.

```
para cada fecha en [desde, hasta]:
  para cada profesional P (en orden):
    para cada franja F en CalendarioDeDisponibilidad.franjasDelDia(horarios(P), fecha):     // C-06
      t = F.inicio
      mientras t + duración <= F.fin:
        inicio = ZonaHorariaConsultorio.aInstante(fecha, t); fin = inicio + duración (Instant)
        si inicio >= ahora
           y CalendarioDeDisponibilidad.profesionalDisponible(P, true, horarios(P), bloqueos(P), inicio, fin)   // C-06
           y ninguna ocupación de P se superpone:
          para cada sillón S (id ascendente):
            si CalendarioDeDisponibilidad.sillonDisponible(S, true, bloqueos(S), inicio, fin)                 // C-06
               y ninguna ocupación de S se superpone:
              agregar candidato (P, S) al hueco "inicio"
        t += paso
agrupar por inicio (orden de candidatos: profesional en orden de entrada, luego sillón); descartar huecos sin candidatos
```

- La regla de horario y bloqueos **es la de C-06** (`CalendarioDeDisponibilidad`); C-10 solo agrega la grilla, la intersección con turnos y el ordenamiento. La superposición con ocupaciones usa el mismo criterio semiabierto `a.inicio < b.fin && b.inicio < a.fin`.
- `fin = inicio + duración` como `Instant`, igual que `Turno.nuevo` (C-09), para que el hueco ofrecido y el turno creado coincidan.
- Un mismo método `candidatosEn(agenda, inicio)` (la iteración interna para un instante) lo usan la consulta y la reserva: un solo camino de código.
- Complejidad: ≤ 14 días × ≤ 5 profesionales × ~40 inicios × pocos sillones, todo en memoria.

### D3. `MotorDeDisponibilidad` (aplicación, solo lectura)

```java
List<Hueco> buscarHuecos(ConsultaDeHuecos consulta);
// ConsultaDeHuecos(Long prestacionId, Long profesionalId /* null = cualquiera */,
//                  LocalDate desde, LocalDate hasta, Long turnoAExcluir /* null */)
List<CandidatoDeReserva> candidatosPara(Long prestacionId, Long profesionalId, Instant inicio, Long turnoAExcluir);
```

- `@Transactional(readOnly = true)` sin aislamiento propio y **sin bloqueos de fila** (Q7): es una vista previa; la autoridad es la verificación final de C-09 + D5.
- Validaciones: prestación existe (404) y activa (400); profesional dado existe (404), activo y realiza la prestación (400); `desde <= hasta`, `hasta - desde < diasMaximos` (400); `desde` anterior a `hoy(reloj)` se recorta a hoy; si todo el rango quedó en el pasado → lista vacía.
- Lecturas por consulta (≈ 2 + 2·profesionales + sillones + 2): prestación, profesionales candidatos, `SillonRepositorio.listarActivos()`, `HorarioAtencionRepositorio.listarDelProfesional(id)`, `BloqueoRepositorio.listarQueAfectanAProfesional/ASillon(id, desdeUtc, hastaUtc)` (C-06), `TurnoRepositorio.listarOcupacionesDe{Profesionales,Sillones}` (D4). `desdeUtc = aInstante(desde 00:00)`, `hastaUtc = aInstante(hasta+1 00:00)` (día del consultorio, spec "Límite del rango").
- `candidatosPara` usa el mismo armado con `desde = hasta = fecha local de inicio` y devuelve `candidatosEn(inicio)`; no exige que `inicio` esté en la grilla (C-12/C-17 pueden pedir cualquier minuto).
- Reusa los puertos de C-06 directamente (no `DisponibilidadDeRecursos.profesionalDisponible` por hueco) para no hacer una consulta por inicio y sillón; la decisión sigue en `CalendarioDeDisponibilidad`.

### D4. Ocupaciones leídas con el criterio de C-09 (Q14)

Se agregan a `TurnoRepositorio` (puerto de C-09, cambio aditivo):

```java
List<OcupacionDeRecurso> listarOcupacionesDeProfesionales(Collection<Long> ids, Instant desde, Instant hasta, Long excluirTurnoId);
List<OcupacionDeRecurso> listarOcupacionesDeSillones(Collection<Long> ids, Instant desde, Instant hasta, Long excluirTurnoId);
```

Implementadas en `TurnoRepositorioJpa` con JPQL `select new ...OcupacionDeRecurso(t.profesionalId, t.inicio, t.fin)` y **los mismos filtros** que `contarSolapados*` (`estado in estadosActivos()`, `esSobreturno = false`, `inicio < :hasta and fin > :desde`, `id <> :excluir`), compartiendo `estadosActivos()` y `excluir()`. Usan los índices `(profesional_id, estado, inicio, fin)` y `(sillon_id, estado, inicio, fin)` de `V009_1`. Un test de repositorio verifica que para el mismo rango "hay solapamiento" ⇔ "la lista no está vacía".

### D5. Verificación final: `RestriccionDeHorarioYBloqueos` (Q12)

```java
@Component @Order(0)
class RestriccionDeHorarioYBloqueos implements RestriccionDeAgenda {
  void verificar(TurnoPropuesto p) {
    if (!disponibilidad.profesionalDisponible(p.profesionalId(), p.inicio(), p.fin()))
      throw new AgendaNoDisponibleException("El profesional no atiende o tiene un bloqueo en ese horario");
    if (!disponibilidad.sillonDisponible(p.sillonId(), p.inicio(), p.fin()))
      throw new AgendaNoDisponibleException("El sillón tiene un bloqueo en ese horario");
  }
}
```

- `disponibilidad` = `DisponibilidadDeRecursos` de C-06. `ServicioDeTurnos` la llama **después** de `FOR UPDATE` profesional → sillón y antes del solapamiento (C-09 D7), dentro de su transacción `READ_COMMITTED`: lee el último estado confirmado de horarios y bloqueos. `ServicioDeDisponibilidad` es `readOnly` y se une a la transacción existente (propagación `REQUIRED`): no abre otra ni cambia el aislamiento (test de integración lo comprueba, riesgo R3).
- Se aplica a **crear, reprogramar y sobreturno** (RN-GL-02 solo exime del solapamiento). `AgendaNoDisponibleException extends ConflictoException` → 409 por `ManejadorDeErrores` sin cambios (Q12 alternativa: 400).
- Los bloqueos de profesional/sillón de C-06 toman el mismo `FOR UPDATE` del recurso y quedan serializados con la reserva; el de consultorio no (riesgo aceptado por C-06).
- Si C-06 ya registró una `RestriccionDeAgenda` equivalente al rebasar, C-10 no agrega la suya (tarea 0.2) y solo conserva los tests de la spec `turnos`.

### D6. `ServicioDeReservaDeHuecos`: una transacción por candidato (Q13)

```java
TurnoCreado reservar(SolicitudDeReservaDeHueco s);
// (pacienteId, prestacionId, profesionalId|null, Instant inicio, OrigenTurno origen, Actor actor)
Turno reprogramar(long turnoId, Instant inicio, Long profesionalId /* null = cualquiera */, Actor actor); // opcional, Q15
```

- **No** es `@Transactional`: llama a `motor.candidatosPara(...)` (su propia transacción de lectura) y después, por cada candidato, a `ServicioDeTurnos.crear(new SolicitudDeTurno(paciente, P, S, prestación, inicio, origen, false, actor))`, que abre y cierra su transacción. Así no viola la guarda de aislamiento de C-09 ni retiene bloqueos de un candidato fallido.
- Captura por candidato: `TurnoSolapadoException`, `AgendaNoDisponibleException`, `PessimisticLockingFailureException` (C-09 D5: "C-10 prueba el siguiente") → siguiente. Cualquier otra excepción (paciente inexistente, inicio pasado, prestación inactiva, origen/actor incoherente) se propaga sin probar más.
- Sin candidatos o todos fallidos → `HuecoNoDisponibleException` (409, "El horario elegido ya no está disponible; elegí otro"). Log `info` con cantidad de intentos, sin datos personales.
- Nunca marca sobreturno (RN-DI-02); el sobreturno de C-12 va directo a `ServicioDeTurnos`.
- Reprogramar (opcional): `candidatosPara(prestación del turno, profesional, inicio, turnoAExcluir = turnoId)` y por candidato `ServicioDeTurnos.reprogramar`; mismas capturas. Si no llega al corte, C-17 usa `buscarHuecos(..., turnoAExcluir)` + `ServicioDeTurnos.reprogramar` directo.

### D7. Endpoint público `GET /api/publico/huecos` (Q6)

| Parámetro | Tipo | Regla |
|---|---|---|
| `prestacionId` | `Long` obligatorio | 404 si no existe |
| `profesionalId` | `Long` opcional | ausente = "cualquiera" |
| `desde`, `hasta` | `LocalDate` ISO (`AAAA-MM-DD`) en el consultorio, obligatorios | rango ≤ 14 días; 400 si no |

Respuesta 200: `[{ "inicio": "2026-11-30T09:00:00-03:00", "fin": "2026-11-30T09:30:00-03:00", "profesionalId": 3 | null }]` ordenada por inicio. `profesionalId` solo viene si se pidió (con "cualquiera" el profesional se asigna al reservar). Nunca sillón ni candidatos. Instantes como `OffsetDateTime` en la zona del consultorio (misma convención que C-06 D8). Errores `ProblemDetail` en español del `ManejadorDeErrores` (parámetro faltante o mal formado → 400). `Cache-Control: no-store`. OpenAPI con `@Tag("Reserva pública")`. El costo está acotado por el máximo de días; rate limiting queda en C-16/C-19.

No hay endpoint interno: C-12 y C-17 usan el servicio desde sus propios controladores (o este mismo GET).

### D8. Configuración

`application.yml`: `app.disponibilidad.paso-minutos: 15` y `app.disponibilidad.dias-maximos: 14`, validados (`paso` entre 5 y 60, `dias` entre 1 y 31) con `@Validated`; no van a los parámetros de C-07 porque no los edita el administrador en el MVP (Q1, Q2).

### D9. Estrategia de pruebas (TDD estricto, sin Docker)

| Nivel | Test | Cubre |
|---|---|---|
| Unitario puro | `CalculadoraDeHuecosTest` | todos los escenarios de cálculo de la spec: duración que no entra, bordes, contiguas, bloqueo parcial/consultorio/sillón, turnos y sobreturnos, sillón ocupado con profesional libre, orden de candidatos, cualquiera, pasado, zona horaria y día UTC, excluir turno |
| JPA (`@DataJpaTest`, H2) | `TurnoRepositorioTest` (ampliado) | ocupaciones: estados, sobreturno, exclusión, bordes, coherencia con `haySolapamiento*` |
| Unitario Mockito | `MotorDeDisponibilidadTest`, `ServicioDeReservaDeHuecosTest`, `RestriccionDeHorarioYBloqueosTest` | validaciones 400/404, lecturas una vez por recurso, recorte de fechas, bucle de candidatos y excepciones que se capturan o se propagan |
| Integración (`@SpringBootTest`, `test`) | `RestriccionDeHorarioYBloqueosIntegracionTest`, `MotorDeDisponibilidadIntegracionTest`, `ReservaDeHuecosIntegracionTest` | spec `turnos` completa vía `ServicioDeTurnos`; coherencia motor ↔ creación (cada hueco ofrecido se puede crear y, creado, desaparece); `reservar` sin transacción envolvente |
| HTTP | `HuecosPublicosControllerTest` (`@SpringBootTest` + MockMvc) | 200 sin token, formato `-03:00`, sin sillón, 400/404, lista vacía, OpenAPI |
| Concurrencia (`@Tag("mysql")`, `it-mysql`) | `ReservaSimultaneaDeHuecosMySqlTest` | un profesional + un sillón → 1 turno + 1 conflicto; "cualquiera" con 2×2 → 2 turnos en recursos distintos; `@RepeatedTest(10)`, `CountDownLatch` |
| Arquitectura (existentes) | `DominioSinFrameworksTest`, `GuardasEnControladoresTest`, `UnicaViaDeEscrituraDeTurnosTest` | dominio puro, controlador público exento por paquete, sin escrituras de turnos fuera de C-09 |

Ajuste de fixtures: al registrar la restricción, los tests de C-09 que crean turnos por servicio (`ServicioDeTurnosIntegracionTest`, `CreacionSimultaneaTurnosMySqlTest` y los que use `EscenarioDeAgendaJdbc`) necesitan horario. Se agrega `EscenarioDeAgendaJdbc.atiendeTodaLaSemana(profesionalId)` (siete filas 00:00–23:59) y se llama en esos tests, sin cambiar su intención.

## Decisiones a aprobar (Q1..Q15)

| # | Decisión | Recomendación (aplicada en specs y tareas) | Alternativa |
|---|---|---|---|
| Q1 | Granularidad de los huecos | Grilla fija de **15 min** desde el inicio de cada franja, configurable (`app.disponibilidad.paso-minutos`) | Paso = duración de la prestación (menos opciones, deja huecos sin usar) / 5 min (lista muy larga en celular) / alinear al fin de cada turno (más complejo) |
| Q2 | Rango de consulta | Fechas del consultorio `desde..hasta` inclusive, **máx. 14 días** configurable; fechas pasadas se recortan a hoy | 31 días (más carga en la ruta pública) / instantes con desplazamiento en vez de fechas |
| Q3 | Orden de "cualquiera" | Profesionales activos que realizan la prestación por **id ascendente** (determinístico, lo que ya devuelve C-03) | Menor cantidad de turnos ese día (reparte carga; una consulta más y orden no determinístico) |
| Q4 | Elección de sillón | **Primer sillón activo libre por id ascendente** (PA-06; mismo orden que el bloqueo) | Sillón menos usado del día / preferencia por prestación (no está en el modelo) |
| Q5 | Bloqueos por ámbito | La regla de C-06: `CONSULTORIO` quita todo; `PROFESIONAL` quita a ese profesional; `SILLON` quita ese sillón y se asigna otro. Los turnos ya existentes dentro de un bloqueo siguen activos y ocupando (DD-12) | Que un bloqueo libere también los turnos existentes para la disponibilidad (contradice DD-12) |
| Q6 | ¿REST en C-10? | **Sí, mínimo**: `GET /api/publico/huecos` de solo lectura, porque C-16 está en el corte y lo necesita; la reserva (POST) es de C-16 | Solo servicio de aplicación; C-16 crea el GET (riesgo de reglas de validación duplicadas) |
| Q7 | Bloqueo en la lectura | **Sin bloqueos** (`readOnly`, lectura confirmada); la garantía es la verificación final de C-09 + D5 | `FOR SHARE` sobre recursos (serializa consultas públicas con reservas, sin ganar garantía) |
| Q8 | Caché | **Ninguna** (volumen de un consultorio chico; los huecos cambian con cada reserva) | Caffeine con TTL corto (huecos viejos, invalidación compleja) |
| Q9 | Inactivos y `profesional_prestacion` | Excluidos: "cualquiera" usa `listarActivosQueRealizan`; sillones `listarActivos`; profesional elegido inactivo o que no realiza → 400; inexistente → 404; prestación inactiva → 400 | Profesional elegido inválido → lista vacía 200 (oculta errores del frontend) |
| Q10 | Sobreturnos en la disponibilidad | **No ocupan** el horario (igual que la verificación de C-09, Q5 de C-09): el motor no oculta lo que la creación aceptaría | Que ocupen (el motor ofrecería menos que lo que C-09 permite; requiere cambiar C-09) |
| Q11 | Horarios pasados | Se excluyen huecos con `inicio < ahora`; **sin** anticipación mínima de reserva | Parámetro "anticipación mínima de reserva online" (p. ej. 2 h) en C-07 |
| Q12 | Verificación de horario y bloqueos al escribir | **C-10 registra** `RestriccionDeHorarioYBloqueos` sobre `DisponibilidadDeRecursos`; aplica a crear, reprogramar y sobreturno; error **409** (`AgendaNoDisponibleException`) | La registra C-06 al rebasar (si ya lo hizo, C-10 la omite) / error 400 de validación / eximir al sobreturno del horario |
| Q13 | Reserva "cualquiera" | Recalcular candidatos al reservar y probar **todos** en orden, una transacción por intento; capturar solapamiento, agenda no disponible e interbloqueo; agotado → 409 `HuecoNoDisponibleException` | Usar los candidatos de la consulta (datos viejos) / un solo intento |
| Q14 | Lectura de turnos ocupados | Dos lecturas nuevas en `TurnoRepositorio` (C-09) con los mismos filtros que la verificación de solapamiento | Puerto propio en `disponibilidad` con su consulta (duplica el criterio de "turno que ocupa") |
| Q15 | Reprogramación por candidato | Incluida como **grupo opcional** (8) de `tasks.md`; si no llega, C-17 usa `buscarHuecos(..., turnoAExcluir)` + `ServicioDeTurnos.reprogramar` | Dejarla íntegra a C-17 |

## Risks / Trade-offs

- **R1** [C-06 cambia nombres o firmas antes de mergear] → lista "Qué asume C-10 de C-06" + tarea 0.2 antes de codear; el motor depende solo de 2 puertos, 1 interfaz y 1 clase pura de C-06.
- **R2** [Registrar la restricción rompe tests de C-09/C-08 que crean turnos sin horario] → red de seguridad (0.3) y ajuste de fixtures con `atiendeTodaLaSemana` en el mismo grupo (3); avisar a Bruno si toca tests de C-08.
- **R3** [`ServicioDeDisponibilidad` es `readOnly` y se une a la transacción `READ_COMMITTED` de `crear`] → test de integración que crea un turno válido con la restricción activa (pasa la guarda de aislamiento y el `INSERT` se confirma) y otro con un bloqueo confirmado en otra transacción justo antes.
- **R4** [Bloqueo de consultorio simultáneo con una reserva] → aceptado por C-06 (no toma filas de recursos); el turno quedaría listado en el aviso de una edición posterior.
- **R5** [Conflicto en archivos compartidos con C-06: C-06 agrega `bloquearPorId` a `ProfesionalRepositorio`/`SillonRepositorio` y C-09 ya usa `CerrojoDeRecursos`] → no lo resuelve C-10; C-10 no usa ninguno de los dos directamente. Señalarlo a Jessica al rebasar C-06.
- **R6** [Grilla de 15 min deja minutos sin ofrecer tras turnos de duración no múltiplo] → aceptado para el MVP (Q1); la carga manual (C-12) puede usar cualquier minuto.
- **R7** [Ruta pública como vector de abuso] → rango máximo de 14 días y validación de parámetros; rate limiting en C-16/C-19.
- **R8** [H2 no reproduce InnoDB] → la reserva simultánea solo cuenta verificada con `./mvnw verify -Pit-mysql` (evidencia en `evidencia/it-mysql.txt`).
- **R9** [Plazo 2026-11-26 12:00 y C-06 sin mergear] → orden de tareas: restricción (garantía) → motor → reserva → GET → MySQL; el grupo 8 es opcional. Si C-06 no llega, C-10 no se implementa (no se replican horarios ni bloqueos).

## Migration Plan

- Sin migraciones ni datos nuevos. Requiere `V006_1` (C-06) y `V009_1` (C-09) aplicadas.
- Al desplegar, la restricción empieza a rechazar turnos fuera de horario: en `dev` el seed de C-06 carga horarios demo; en producción el administrador debe cargar horarios antes de abrir la reserva (RN-GL-01, anotar en el manual de C-24/C-19).
- Archivos compartidos (`TurnoRepositorio`, `TurnoRepositorioJpa`, `application.yml`, `EscenarioDeAgendaJdbc`) en un commit aparte y aditivo.
- Rollback: revertir la PR. Sin efecto en datos; los turnos creados siguen siendo válidos.

## Open Questions

- Ninguna que cambie specs o tareas fuera de Q1..Q15 y de la revisión de supuestos de C-06 (tarea 0.2). Diferida sin impacto: si C-16 muestra el nombre del profesional asignado antes de confirmar (puede leerlo de la respuesta de la reserva).

## UI Mockups

Decision: no (sin UI; las pantallas son de C-11, C-12 y C-16) — 2026-11-26.
