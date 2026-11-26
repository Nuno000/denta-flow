# Proposal — C-10 `motor-disponibilidad`

## Why

La reserva online (C-16), la carga manual (C-12), la reprogramación (C-17) y la lista de espera (C-20) necesitan la misma respuesta a "¿qué horarios puedo ofrecer para esta prestación?" y la misma forma de asignar profesional y sillón (RN-DI-01, RN-TU-04, RN-TU-07, PA-05, PA-06). Si cada consumidor la calcula a su manera, las reglas se duplican y se rompe el diferencial de DD-02. C-10 (`CHANGES.md`, oleada 5, camino crítico, governance **CRÍTICO**) crea el **motor de disponibilidad único** (`08_arquitectura_propuesta.md` §Patrones) sobre los horarios y bloqueos de C-06 y la única vía de escritura de C-09.

**Plan de entrega**: esta propuesta se mergea sola a `main` (PR de documentación, como C-15/C-18/C-19) y se implementa **después de que C-06 (Jessica) esté en `main`**. Por eso está escrita contra el modelo real de la rama `feat/C-06-horarios-y-bloqueos` (commit `fce21ec`) y lista abajo qué asume de C-06, para verificarlo al rebasar.

## What Changes

- **Cálculo de huecos** (`CHANGES.md` C-10, US-010, Flujo 1 paso 2): dados una prestación, un profesional opcional (o "cualquiera", PA-05) y un rango de fechas del consultorio, devuelve los huecos = horario de atención − bloqueos − turnos activos del profesional **y** del sillón, solo donde entra la duración completa de la prestación (RN-DI-01, RN-TU-07). Huecos en una grilla fija (15 min por defecto, Q1), nunca en el pasado (Q11), con día y hora de `America/Argentina/Buenos_Aires` y almacenamiento UTC.
- **Asignación** (PA-05, PA-06): primero el profesional (el elegido, o los activos que realizan la prestación en orden de id, Q3) y después el primer sillón libre por id ascendente (Q4). Cada hueco lleva sus **candidatos ordenados** (profesional, sillón).
- **Reserva por candidato** (DD-09, Flujo 1 paso 4): un servicio de aplicación recalcula los candidatos del horario elegido y llama a `ServicioDeTurnos.crear` de C-09 **una vez por transacción** por candidato; si uno falla por solapamiento, bloqueo de agenda o interbloqueo, prueba el siguiente; si se agotan, responde conflicto (409) para que el consumidor ofrezca otros huecos.
- **Verificación final de horario y bloqueos** (RN-TU-04): una implementación del punto de extensión `RestriccionDeAgenda` de C-09 que, con los bloqueos de fila tomados, consulta `DisponibilidadDeRecursos` de C-06. Así **toda** creación y reprogramación (online, recepción, sobreturno) rechaza un horario fuera de atención o bloqueado, sin duplicar reglas ni saltear `ServicioDeTurnos`.
- **Consulta pública de huecos** de solo lectura `GET /api/publico/huecos` para la pantalla de reserva de C-16 (Q6), sin datos personales, sin exponer sillones.
- **Reprogramación por candidato** para C-17 (**opcional**, grupo 8 de `tasks.md`; Q15): misma estrategia sobre `ServicioDeTurnos.reprogramar`, excluyendo el propio turno.
- **Tests** (TDD estricto, sin Docker): hueco donde no entra la duración, bloqueo parcial, borde exacto de horario y de turno, sillón ocupado con profesional libre, bloqueo de consultorio, zona horaria y cambio de día UTC, reserva simultánea del mismo hueco contra MySQL 8 real (`@Tag("mysql")`, perfil `it-mysql`).
- **Sin migraciones**: C-10 no crea tablas; lee `horario_atencion` y `bloqueo` (C-06) y `turno` (C-09).
- **Fuera de alcance**: pantallas (C-11, C-12, C-16); endpoint de creación de la reserva pública, datos del paciente, aviso de privacidad y email (C-16, C-15, C-14); sobreturno y su permiso (C-12); ventana de anticipación de cancelación (C-17); ofertas de lista de espera (C-20); rate limiting de rutas públicas (C-16/C-19); caché (Q8); resolver turnos afectados por un bloqueo (PA-11 resuelta en DD-12: no se tocan).

### Qué asume C-10 de C-06

Verificar cada punto después de que C-06 llegue a `main` (tarea 0.2). Si alguno cambió, se ajustan `design.md`, specs y tareas antes de escribir código.

1. Paquetes `ar.dentaflow.dominio.disponibilidad` y `ar.dentaflow.aplicacion.disponibilidad`; C-10 agrega clases nuevas sin tocar las de C-06.
2. Interfaz de aplicación `DisponibilidadDeRecursos` con `boolean profesionalDisponible(Long profesionalId, Instant inicio, Instant fin)` y `boolean sillonDisponible(Long sillonId, Instant inicio, Instant fin)` (horario − bloqueos, inactivo = no disponible, rango en un único día del consultorio, intervalos `[inicio, fin)`; inexistente → `RecursoNoEncontradoException`; rango inválido → `ValidacionDeDominioException`), implementada por `ServicioDeDisponibilidad` (`@Transactional(readOnly = true)`, sin bloqueos de fila; participa de la transacción del llamador).
3. Regla pura `CalendarioDeDisponibilidad` con `profesionalDisponible(Long, boolean activo, Collection<HorarioAtencion>, Collection<Bloqueo>, Instant, Instant)`, `sillonDisponible(Long, boolean activo, Collection<Bloqueo>, Instant, Instant)` y `franjasDelDia(Collection<HorarioAtencion>, LocalDate)` → `List<FranjaLocal>` (contiguos unidos).
4. Record `FranjaLocal(LocalDate fecha, LocalTime inicio, LocalTime fin)` con `inicioInstante()` y `finInstante()`.
5. Puertos `HorarioAtencionRepositorio.listarDelProfesional(Long)` y `BloqueoRepositorio.listarQueAfectanAProfesional(Long, Instant, Instant)` / `listarQueAfectanASillon(Long, Instant, Instant)` (incluyen los de ámbito `CONSULTORIO`).
6. `Bloqueo` con `afectaAProfesional(Long)`, `afectaASillon(Long)`, `seSuperponeCon(Instant, Instant)`; `AmbitoBloqueo` = `PROFESIONAL | SILLON | CONSULTORIO`.
7. Horario semanal en hora local que no cruza la medianoche (máximo 23:59); bloqueos en instantes UTC; un bloqueo no cancela turnos existentes (DD-12).
8. Migración `V006_1__crear_horarios_y_bloqueos.sql` (tablas `horario_atencion`, `bloqueo`); C-10 no usa el número `V010_*` porque no tiene migración.
9. Capabilities de C-06: `horarios-atencion`, `bloqueos-agenda`, `disponibilidad-recursos`. C-10 no las modifica.
10. C-06 **no** registra un bean `RestriccionDeAgenda` (su rama es anterior a C-09). Si al rebasar Jessica lo agregó, C-10 descarta su grupo 3 y usa el de C-06 (Q12).
11. Seed `dev` de C-06: profesionales demo de lunes a viernes 09:00–13:00 y 14:00–18:00 (lo usa la verificación manual).

## Capabilities

### New Capabilities
- `motor-disponibilidad`: cálculo de huecos por prestación y rango, asignación ordenada de profesional y sillón, reserva por candidato con una transacción por intento, y consulta pública de huecos para la reserva online.

### Modified Capabilities
- `turnos`: se agrega el requisito de que crear o reprogramar un turno (incluido el sobreturno) exija que el profesional y el sillón estén disponibles según horarios y bloqueos (RN-TU-04), verificado dentro de la misma transacción con los bloqueos tomados.

## Impact

- **Código nuevo**: `dominio/disponibilidad/` (calculadora de huecos pura y excepciones de conflicto), `aplicacion/agenda/` (motor, servicio de reserva por candidato, restricción de agenda), `presentacion/publico/` (consulta de huecos) y sus tests.
- **Archivos compartidos (aditivos, commit aparte)**: `TurnoRepositorio` / `TurnoRepositorioJpa` de C-09 (dos lecturas de ocupación por rango con el mismo criterio que la verificación de solapamiento, Q14); `application.yml` (`app.disponibilidad.paso-minutos`, `app.disponibilidad.dias-maximos`); fixture de tests `EscenarioDeAgendaJdbc` y tests de C-09 que crean turnos por servicio (agregar horario a sus profesionales, porque la nueva restricción los alcanza).
- **API**: `GET /api/publico/huecos?prestacionId&profesionalId&desde&hasta` (pública, bajo el prefijo ya reservado para C-10/C-16 en `autorizacion-por-rol`). Sin cambios en `ConfiguracionSeguridad` ni en `ManejadorDeErrores` (los conflictos nuevos extienden `ConflictoException` → 409).
- **Dependencias**: C-06 (bloqueante para implementar), C-09, C-03/C-05 (recursos activos y `profesional_prestacion`), zona horaria y `Clock` de C-02/C-03. Sin dependencias nuevas en `pom.xml`.
- **Consumidores**: C-16 (`GET` de huecos + reserva por candidato, test `ReservaPublicaConcurrenciaMySqlTest` de C-19 que espera 201/409), C-12 (sugerir el hueco más cercano; su carga directa queda alcanzada por la restricción), C-17 (huecos excluyendo el propio turno y reprogramación por candidato), C-20 (reserva por candidato al aceptar una oferta).
- **Historias y reglas**: US-010, US-021 (parcial), US-030 (base); RN-DI-01, RN-DI-02, RN-DI-04, RN-TU-04, RN-TU-07, RN-GL-01, RN-GL-02; PA-05, PA-06 (resueltas), PA-11 (DD-12); DD-09, DD-12.
- **Governance**: CRÍTICO. No se escribe código sin la aprobación explícita de Renzo de Q1..Q15 de `design.md` y sin C-06 en `main`; revisa y aprueba la PR Florencia.
