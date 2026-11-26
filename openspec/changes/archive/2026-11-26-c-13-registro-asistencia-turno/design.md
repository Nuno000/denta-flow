# Design — C-13 `registro-asistencia-turno`

## Context

Ver `proposal.md` (Why). Lo que ya existe en `main` y condiciona el enfoque:

- **`ServicioDeTurnos.cambiarEstado(turnoId, nuevo, actor)`** (C-09) es la única vía de escritura de turnos.
  - Bloquea la fila del turno, valida la transición (`EstadoTurno.puedePasarA`: pendiente o confirmado → asistencia; corrección entre los tres estados de asistencia; `cancelado` final) y audita en la misma transacción con `AccionDeTurno.CAMBIO_ESTADO` (C-08).
  - Repetir el estado actual no cambia ni audita.
  - Exige `READ COMMITTED` y no se puede envolver en otra transacción con otro aislamiento.
- **Errores existentes:** `TransicionDeEstadoInvalidaException` extiende `OperacionNoPermitidaException` → 409. `RecursoNoEncontradoException` → 404. `AccessDeniedException` → 403. `ValidacionDeDominioException` → 400.
- **Hora actual:** `Clock` inyectable (`RelojConfig`, spec `zona-horaria-consultorio`).
- **Frontend (C-11):** `features/agenda/api.ts` es el único punto de contacto con la API. `TarjetaDeTurno` muestra cada turno, y `useTurnosDeAgenda` usa la clave `['agenda', rango, filtros]`.
- **Política visual del proyecto:** `off`. Decision: no (project policy).

## Goals / Non-Goals

**Goals:**
- Registrar la asistencia sin duplicar ninguna regla de transición ni de auditoría: todo pasa por `ServicioDeTurnos`.
- Que la regla de "propios" y la regla de momento vivan en el backend. La UI solo las refleja.

**Non-Goals:**
- Historial de cambios (US-025).
- Acciones masivas.
- Deshacer.
- Notificar al paciente.

## Decisions

### D1. Endpoint `POST /api/turnos/{id}/asistencia`

- **Cuerpo:** `{ "estado": "asistio" | "ausente" | "ausente_con_aviso" }`.
- **Respuesta 200:** `{ "id": 41, "estado": "asistio" }`.
- **Por qué un recurso de acción y no `PATCH /api/turnos/{id}`:**
  - Deja explícito que solo se aceptan los tres estados de asistencia.
  - Tiene guarda y regla de momento propias.
  - No abre una vía genérica de cambio de estado: confirmar y cancelar llegan con C-12 y C-17, con sus propias reglas.
- **Alternativa descartada:** tres endpoints, uno por estado. Multiplica código sin ganar claridad.
- **Por qué POST y no PUT:** la operación audita y no es un reemplazo de recurso, aunque repetirla con el mismo estado no cambie nada.

### D2. Caso de uso `aplicacion/turnos/RegistroDeAsistencia`

Recibe `(turnoId, estado, rol, usuarioId, profesionalDelUsuario)` y hace, en este orden:

1. Valida que el estado sea de asistencia (`EstadoTurno.ASISTIO`, `AUSENTE` o `AUSENTE_CON_AVISO`). Si no, lanza `ValidacionDeDominioException` con un mensaje fijo, sin repetir el valor recibido (como en C-15 y C-11).
2. Lee el turno con `ServicioDeTurnos.buscarPorId`. Si no existe, devuelve 404.
3. Si el rol es `PROFESIONAL`: sin vínculo, o con un turno de otro profesional → `AccessDeniedException` (403). Es la misma regla de "propios" que la agenda (C-11 D2) y los horarios (C-06).
4. Para `asistio` y `ausente`: si `turno.inicio > ahora(reloj)`, lanza `OperacionNoPermitidaException` (409) con "La asistencia se registra desde el inicio del turno".
5. Delega en `ServicioDeTurnos.cambiarEstado(turnoId, estado, Actor.usuario(usuarioId))`, que valida la transición, bloquea, guarda y audita.

**Transacción:** `RegistroDeAsistencia` **no** abre una transacción propia, porque `ServicioDeTurnos` exige ser el borde transaccional con `READ COMMITTED`. Las verificaciones de los pasos 2 a 4 se hacen con una lectura previa sin bloqueo.

**Riesgo aceptado:** entre esa lectura y el bloqueo, otro usuario puede reprogramar el turno a otro profesional u otro horario.
- *Mitigación:* el profesional de un turno solo cambia al reprogramar, y reprogramar exige `pendiente` o `confirmado`. Por eso la ventana real es un turno reprogramado justo en ese instante.
- *Por qué se acepta:* el impacto es bajo y queda auditado.
- *Alternativa:* agregar a `ServicioDeTurnos` un método que reciba un validador. Se descarta para no tocar el servicio CRÍTICO de C-09 el día de la entrega. Queda anotada como mejora.

### D3. Controller `presentacion/turnos/AsistenciaController`

- Guarda `@PersonalInterno` y `@AuthenticationPrincipal UsuarioAutenticado`.
- Cuerpo `SolicitudDeAsistencia(@NotBlank String estado)`. El estado viaja como `String` y lo valida el caso de uso, así un valor desconocido da 400 con un mensaje fijo en lugar de un error de deserialización de enum.
- Respuesta `AsistenciaRespuesta(Long id, String estado)`.
- OpenAPI: `@Tag("Turnos")` y `@ApiResponse` 200, 400, 401, 403, 404 y 409.

### D4. Frontend

- **`api.ts`:** agrega `ESTADOS_DE_ASISTENCIA` y `useRegistrarAsistencia()` (`useMutation` → `apiClient.post('/api/turnos/{id}/asistencia', { estado })`). Al terminar bien invalida `['agenda']`, así se refrescan día y semana.
- **`AccionesDeAsistencia`:** componente dentro de `TarjetaDeTurno`, también en la variante compacta de la semana. Tiene tres botones con `aria-pressed` para el estado actual.
  - "Asistió" y "Ausente" quedan `disabled` mientras `Date.now() < inicio`, con el texto "Disponible desde las HH:mm".
  - No se muestra en turnos `cancelado`.
  - Los errores se ven en la tarjeta con `role="alert"`: 403 → "No tenés permiso para registrar la asistencia de este turno"; 409 → el `detail` de la API, con `leerErrorDeApi` de `lib/erroresApi.ts`.
  - El éxito se anuncia en la región `aria-live` de `AgendaPage` como "Asistencia registrada".
- **Ahora en la UI:** se toma una vez por render con `Date.now()`. En los tests se fija con `vi.useFakeTimers({ toFake: ['Date'] })`.

### D5. Tests

- **Backend, unitarios (Mockito):** `RegistroDeAsistenciaTest` cubre estado inválido, 404, profesional ajeno o sin vínculo, regla de momento (antes, justo al inicio, después) y aviso previo, y que delega en el servicio con el actor correcto.
- **Backend, integración (MockMvc, H2):** `AsistenciaControllerTest` cubre 200 con auditoría real (una fila en `turno_historial` con estado anterior y nuevo), idempotencia (sin fila nueva), corrección, 409 por cancelado y por turno futuro, 403, 401, 400, 404, la ausencia de datos personales en la respuesta y la ruta en `/v3/api-docs`. Los turnos se crean con `Turno.nuevo` + `em.persist` (gotcha de zona horaria de C-11 D7).
- **MySQL 8:** no hay concurrencia nueva. El bloqueo de la fila y la auditoría ya los cubren los tests `@Tag("mysql")` de C-09. Igual se corre `-Pit-mysql` completo para confirmar que no hay regresiones.
- **Frontend (Vitest y Testing Library con `simularApi`):** botones según estado y momento, el pedido enviado (método, ruta y cuerpo), el refresco de la agenda, el anuncio, y los errores 403 y 409.

## Risks / Trade-offs

- **[Ventana entre la verificación de "propios" y el bloqueo]** → D2: impacto bajo y auditado. La mejora queda anotada para después de la entrega.
- **[La hora de la máquina del usuario está mal]** → La UI podría habilitar un botón antes de tiempo, pero el backend decide con su `Clock` y responde 409, que se muestra en la tarjeta.
- **[US-025 queda afuera de C-13]** → Se registra en `CHANGES.md` al archivar: C-13 parcial, con US-025 pendiente en un change aparte, para no marcar C-13 completo por error.

## Migration Plan

Sin migraciones ni configuración. Rollback: revertir el PR. La agenda vuelve a ser de solo lectura.
