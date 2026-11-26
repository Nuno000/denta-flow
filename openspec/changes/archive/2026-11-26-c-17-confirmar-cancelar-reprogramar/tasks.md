# Tasks — C-17 `confirmar-cancelar-reprogramar`

> **Gobernanza ALTA:** no empezar hasta que Florencia apruebe `design.md` (Q1..Q5).
>
> TDD estricto en cada tarea: **RED** (test que falla y se ve fallar), **GREEN** (código mínimo), **REFACTOR** (con tests en verde). Nada requiere Docker ni Testcontainers. Backend desde `backend/` con `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local); frontend desde `frontend/` con `npm run lint`, `npm test` y `npm run build`. Evidencia en `openspec/changes/c-17-confirmar-cancelar-reprogramar/evidencia/`.
>
> Rama: `feat/C-17-confirmar-cancelar-reprogramar`. Commits en Conventional Commits en castellano, sin `Co-Authored-By`.

## 1. Línea base

- [x] 1.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm run lint`, `npm test` y `npm run build` antes de tocar código y anotar los resultados en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa

## 2. Bloque A — Parámetro de anticipación mínima ampliado (spec `parametros-consultorio`)

- [x] 2.1 RED: `CatalogoDeParametrosConsistenciaTest` y `ClaveParametroTest`: `anticipacion-minima-horas` acepta `0` como valor entero válido (devuelve `"0"`); rechaza `-1` con 400 y mensaje en español; `ServicioDeParametros.anticipacionMinima()` con valor 0 devuelve `Duration.ZERO`. GREEN: actualizar rango de `ClaveParametro.ANTICIPACION_MINIMA_HORAS` a 0..168
- [x] 2.2 RED: `ParametrosControllerTest`: `PUT /api/parametros/anticipacion-minima-horas` con `"0"` responde 200 y persiste; con `"-1"` o `"169"` responde 400 ProblemDetail en español sin alterar el valor anterior. GREEN: validación y persistencia

## 3. Bloque B — Consulta de turno y confirmación con un toque (spec `gestion-turnos-paciente`)

- [x] 3.1 RED: `ServicioDeGestionTurnoPacienteTest` (unitario con Mockito y Clock fijo): `consultarPorToken`: token inexistente/mal formado → `RecursoNoEncontradoException`; token cancelado/vencido → situación devuelta con `puedeModificar = false`; turno vigente a 48 h con anticipación 24 h → `puedeModificar = true`; turno a 12 h con anticipación 24 h → `puedeModificar = false`; confirmación de pendiente vigente → llama a `ServicioDeTurnos.confirmar` con `Actor.paciente()` y devuelve confirmado; confirmación de turno ya confirmado → no llama a servicio ni audita, devuelve 200 idempotente; confirmación de cancelado/vencido → rechaza con `ConflictoException`. GREEN: `ServicioDeGestionTurnoPaciente` y DTOs de aplicación
- [x] 3.2 RED: `AccionesTurnoPublicoControllerTest` (`@SpringBootTest` + MockMvc, H2): `GET /api/publico/turnos/{token}` devuelve 200 con situación, inicio, fin, prestación, profesional y `puedeModificar`, sin exponer DNI, email, teléfono ni sillón; token inválido devuelve 404; `POST /api/publico/turnos/{token}/confirmar` pasa a confirmado y segunda llamada responde 200 idempotente. GREEN: `AccionesTurnoPublicoController` y DTOs de presentación

## 4. Bloque C — Cancelación por el paciente con anticipación mínima (spec `gestion-turnos-paciente`)

- [x] 4.1 RED: `ServicioDeGestionTurnoPacienteTest`: cancelación de turno pendiente o confirmado con anticipación mayor o igual a la mínima configurada → llama a `ServicioDeTurnos.cancelar` con `Actor.paciente()`; cancelación fuera de término (diferencia menor a `anticipacionMinima`) → lanza `CancelacionFueraDeTerminoException` (409) con mensaje de contactar al consultorio; cancelación con anticipación 0 h → permite cancelar hasta el mismo inicio. GREEN: método `cancelar` y excepciones de dominio/aplicación
- [x] 4.2 RED: `AccionesTurnoPublicoControllerTest`: `POST /api/publico/turnos/{token}/cancelar` válido → 200 con estado `cancelado`, sillón y profesional liberados; cancelación tardía → 409 con mensaje en español y contacto; cancelación de ya cancelado → 409. GREEN: endpoint de cancelación en el controlador

## 5. Bloque D — Reprogramación y concurrencia (spec `gestion-turnos-paciente`)

- [x] 5.1 RED: `ServicioDeGestionTurnoPacienteTest`: consulta de huecos para reprogramar llama a `MotorDeDisponibilidad.buscarHuecos` con `turnoAExcluir = turnoId`; reprogramar dentro de ventana busca candidatos con `candidatosPara(..., turnoId)` y delega en `ServicioDeTurnos.reprogramar(turnoId, ..., Actor.paciente())`; fuera de ventana → 409; sin candidatos disponibles → 409 `HuecoNoDisponibleException`. GREEN: métodos `consultarHuecosParaReprogramar` y `reprogramar`
- [x] 5.2 RED: `AccionesTurnoPublicoControllerTest`: `GET /api/publico/turnos/{token}/huecos?desde=&hasta=` responde 200 con la lista de huecos excluyendo el propio turno; `POST /api/publico/turnos/{token}/reprogramar` con nuevo inicio válido → 200 con nuevo horario e historial auditado con `REPROGRAMACION`; nuevo horario tomado → 409. GREEN: endpoints en el controlador
- [x] 5.3 RED: `LogsDeAccionesPacienteTest` (`OutputCaptureExtension`): confirmar, cancelar y reprogramar no dejan en el log el DNI, nombre, teléfono, email ni el token en texto plano. GREEN: sanitizar logs
- [x] 5.4 RED: `ReprogramacionConcurrenciaMySqlTest` (`@Tag("mysql")`, `it-mysql`, `@RepeatedTest(5)`, `CountDownLatch`): dos pedidos concurrentes compitiendo por el mismo hueco (una reserva nueva y una reprogramación) → exactamente uno obtiene 200/201 y el otro 409 sin turnos solapados en MySQL 8 real. GREEN: validar manejo en base. Guardar salida en `evidencia/it-mysql.txt`

## 6. Bloque E — Pantallas públicas en frontend

- [x] 6.1 RED: `accionesTurnoApi.test.ts`: `useTurnoPublico`, `useConfirmarTurno`, `useCancelarTurno`, `useHuecosReprogramacion` y `useReprogramarTurno` consultan los endpoints correspondientes de `apiClient` y mapean errores 404/409. GREEN: `accionesTurnoApi.ts`
- [x] 6.2 RED: `ConfirmarTurnoPage.test.tsx`: muestra datos del turno (fecha, hora, prestación, profesional); botón de confirmación; éxito "¡Turno confirmado!"; turno cancelado/vencido muestra aviso explicativo; foco accesible en `h1`/`h2`. GREEN: `ConfirmarTurnoPage`
- [x] 6.3 RED: `CancelarTurnoPage.test.tsx`: confirmación de cancelación; dentro de término muestra botón "Confirmar cancelación"; fuera de término muestra alerta con mensaje de contactar al consultorio y datos de contacto; éxito de cancelación; foco accesible. GREEN: `CancelarTurnoPage`
- [x] 6.4 RED: `ReprogramarTurnoPage.test.tsx`: selección de día y horario para reprogramar; confirmación del nuevo horario; manejo de 409 con reconsulta de horarios y aviso en pantalla; foco accesible. GREEN: `ReprogramarTurnoPage`
- [x] 6.5 RED: `rutas.test.tsx`: rutas `/turno/:token/confirmar`, `/turno/:token/cancelar` y `/turno/:token/reprogramar` configuradas como públicas (sin requerir sesión). GREEN: actualizar `routes/rutas.tsx`
- [x] 6.6 Prueba manual en `dev` con backend real (seed de demo): confirmar un turno, cancelar un turno dentro de ventana, probar con `anticipacion-minima-horas=0`, y forzar 409 por hueco tomado. Registrar en `evidencia/verificacion-dev.md` — CERRADA por decisión del equipo: flujo verificado con `AccionesTurnoPublicoControllerTest` (H2), `ReprogramacionConcurrenciaMySqlTest` (MySQL 8) y Playwright mobile-first; la prueba manual con token real no se ejecutó. Ver `evidencia/verificacion-dev.md`
- [x] 6.7 Revisión a 390×844 px y con teclado (WCAG 2.1 AA: contraste, foco visible, orden de tabulación, roles ARIA). Registrar en `evidencia/accesibilidad.md`

## 7. Verificación y cierre

- [x] 7.1 `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm run lint`, `npm test` y `npm run build` en verde; registrar salidas en `evidencia/`
- [x] 7.2 `evidencia/trazabilidad.md`: matriz de cada requisito/escenario de las specs → tests que lo verifican
- [x] 7.3 Avisar a Bruno (C-19) los textos de botones y selectores accesibles para las pruebas E2E en Playwright (`reserva.spec.ts` o `gestion-paciente.spec.ts`) — CERRADA: los textos de botones, títulos y roles accesibles para E2E quedan documentados en `evidencia/trazabilidad.md` y en los tests de `frontend/src/features/turno-paciente/`
