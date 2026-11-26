# Tasks — C-13 `registro-asistencia-turno`

> **Gobernanza MEDIO:** implementar con checkpoints y avisar las decisiones. Rama: `feat/C-13-registro-asistencia-turno`. Revisor del PR: Bruno.
>
> TDD estricto: **RED** (el test falla), **GREEN** (código mínimo) y **REFACTOR** (con todo en verde). Nada requiere Docker ni Testcontainers. No se modifica `ServicioDeTurnos` (C-09) ni código de C-10, C-16, C-17 o C-19.

## 1. Línea base

- [x] 1.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql` y, en `frontend/`, `npm test`. Anotar los totales en `openspec/changes/c-13-registro-asistencia-turno/evidencia/linea-base.txt`, contando los casos de los XML de surefire porque los `.txt` no cuentan los `@Nested`. Si algo falla, frenar y reportarlo.

## 2. Caso de uso de asistencia (backend, aplicación)

- [x] 2.1 RED→GREEN `RegistroDeAsistencia`: un estado que no es de asistencia (`confirmado`, `cancelado`, vacío, desconocido) → `ValidacionDeDominioException` sin repetir el valor; un turno inexistente → `RecursoNoEncontradoException`. Verificar con `RegistroDeAsistenciaTest` (Mockito).
- [x] 2.2 RED→GREEN regla de "propios": un profesional sin vínculo o con un turno de otro profesional → `AccessDeniedException`, sin llamar a `cambiarEstado`; recepción y administrador → permitido. Verificar con `RegistroDeAsistenciaTest`.
- [x] 2.3 RED→GREEN regla de momento con `Clock` fijo: `asistio` y `ausente` antes del inicio → `OperacionNoPermitidaException`; justo al inicio y después → permitido; `ausente_con_aviso` antes del inicio → permitido. Verificar con `RegistroDeAsistenciaTest`.
- [x] 2.4 RED→GREEN delegación: llama a `ServicioDeTurnos.cambiarEstado(id, estado, Actor.usuario(usuarioId))` y devuelve el turno resultante. Verificar con `RegistroDeAsistenciaTest`.

## 3. Endpoint (backend, presentación)

- [x] 3.1 RED→GREEN `AsistenciaController` `POST /api/turnos/{id}/asistencia` con `@PersonalInterno`: 200 con `{id, estado}` y una fila nueva en `turno_historial` con estado anterior, estado nuevo y usuario; la respuesta no contiene DNI, teléfono, email ni token. Verificar con `AsistenciaControllerTest` (MockMvc, H2; turnos con `Turno.nuevo` + `em.persist`).
- [x] 3.2 RED→GREEN casos de error: 409 por turno cancelado y por `asistio` en un turno futuro, 403 por profesional ajeno, 401 sin token, 400 por estado inválido o cuerpo vacío, 404 por turno inexistente; en ninguno cambia el estado ni se agrega historial. Verificar con `AsistenciaControllerTest`.
- [x] 3.3 RED→GREEN corrección e idempotencia: `asistio` → `ausente` audita la corrección; repetir `asistio` responde 200 sin fila nueva en el historial. Verificar con `AsistenciaControllerTest`.
- [x] 3.4 REFACTOR: anotaciones OpenAPI (`@Tag("Turnos")`, `@Operation`, `@ApiResponse` 200, 400, 401, 403, 404 y 409) y una aserción de que `/v3/api-docs` lista la ruta. Verificar `./mvnw verify` en verde.

## 4. Acciones en la agenda (frontend)

- [x] 4.1 RED→GREEN `useRegistrarAsistencia` en `features/agenda/api.ts`: envía `POST /api/turnos/{id}/asistencia` con `{ estado }` e invalida `['agenda']` al terminar bien. Verificar con `api.test.tsx` (`simularApi`, leyendo método, ruta y cuerpo del pedido).
- [x] 4.2 RED→GREEN `AccionesDeAsistencia` dentro de `TarjetaDeTurno` (normal y compacta): tres botones con `aria-pressed` para el estado actual; "Asistió" y "Ausente" deshabilitados con "Disponible desde las HH:mm" antes del inicio; sin acciones en un turno cancelado. Verificar con `AccionesDeAsistencia.test.tsx` (`vi.useFakeTimers({ toFake: ['Date'] })`).
- [x] 4.3 RED→GREEN flujo en la pantalla: tocar "Asistió" envía el pedido, refresca la agenda y anuncia "Asistencia registrada"; un 409 muestra el `detail` en la tarjeta; un 403 muestra la falta de permiso; el estado mostrado no cambia ante un error. Verificar con `AgendaPage.test.tsx`.
- [x] 4.4 REFACTOR y chequeo: sin `fetch` suelto ni `any`. Correr `npm test`, `npm run lint` y `npm run build` en verde.

## 5. Verificación y evidencia

- [x] 5.1 Prueba visual con Playwright MCP a 390x844 y 1440x900, con la API simulada en el navegador: turno que empezó, turno futuro con botones deshabilitados, estado actual presionado y error 409. Guardar las capturas en `evidencia/capturas/`, borrar `.playwright-mcp/` y detener `vite`.
- [x] 5.2 Prueba de punta a punta contra el backend real (perfil `test`, H2 en archivo en `target/`, administrador ficticio, sin `.env`): registrar asistencia desde la agenda y confirmar la fila en `turno_historial`. Anotar en `evidencia/punta-a-punta.md` y limpiar procesos y archivos al terminar.
- [x] 5.3 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm test`, `npm run lint`, `npm run build` y `openspec validate c-13-registro-asistencia-turno --strict`, y anotar los totales frente a la línea base en `evidencia/verificacion.txt`.

## Workflow follow-up

- Archivar con `/opsx:archive` sincronizando la spec `asistencia-turnos`.
- En `CHANGES.md`, dejar C-13 como **parcial**: US-024 hecha y US-025 pendiente. No marcarlo `[x]` hasta que exista el change de US-025.
- Abrir el PR a `main` con Bruno como revisor, sin línea `Co-Authored-By`.
