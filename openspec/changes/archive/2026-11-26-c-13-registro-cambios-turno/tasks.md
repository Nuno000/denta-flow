# Tasks — C-13 parte 2 `registro-cambios-turno` (US-025)

> **Gobernanza MEDIO:** lectura de auditoría solo para `administrador`. Rama: `feat/C-13-registro-cambios-turno`. Revisor del PR: Bruno.
>
> TDD estricto: **RED** → **GREEN** → **REFACTOR**. Sin Docker ni Testcontainers; perfil `test` (H2 `MODE=MySQL`) y perfil `it-mysql` (MySQL 8 real). No se modifican `ServicioDeTurnos` (C-09) ni `ServicioDeAuditoria` (C-08).

## 1. Caso de uso de consulta de historial (backend, aplicación)

- [x] 1.1 RED→GREEN `ConsultaDeHistorialDeTurno`: si el turno no existe lanza `RecursoNoEncontradoException`; si existe devuelve `EntradaDeHistorial` en el orden de `ServicioDeAuditoria.historialDe`, resolviendo `actorEmail` desde `UsuarioRepositorio` para `actor_tipo = usuario` (deduplicando ids) y `null` para `paciente` y `sistema`. Verificar con `ConsultaDeHistorialDeTurnoTest` (Mockito).

## 2. Endpoint REST de solo lectura y OpenAPI (backend, presentación)

- [x] 2.1 RED→GREEN `HistorialTurnoController` (`GET /api/turnos/{id}/historial` con `@SoloAdministrador`): 200 con lista cronológica real sin datos personales del paciente ni token, 403 para `recepcion` y `profesional`, 401 sin token, 404 para turno inexistente, 4xx en `DELETE`/`PUT`, y documentación OpenAPI (`@Tag("Auditoría")`, `@Operation`, `@ApiResponse` 200/401/403/404) verificada en `/v3/api-docs`. Verificar con `HistorialTurnoControllerTest` (MockMvc, H2) y `GuardasEnControladoresTest`.

## 3. Diálogo de historial en la agenda (frontend)

- [x] 3.1 RED→GREEN `useHistorialDeTurno` en `frontend/src/features/agenda/api.ts`: consulta `GET /api/turnos/{id}/historial` solo cuando `turnoId !== null`. Verificar con `api.test.tsx`.
- [x] 3.2 RED→GREEN `HistorialDeTurnoDialogo.tsx` y botón `"Ver historial"` en `TarjetaDeTurno.tsx`: visible únicamente para `administrador` (incluso en turnos `cancelado`), oculto para `recepcion` y `profesional`; muestra fecha/hora en `America/Argentina/Buenos_Aires`, actor, acción y valores anterior/nuevo en solo lectura (sin editar ni borrar), estado vacío y error con reintentar. Verificar con `HistorialDeTurnoDialogo.test.tsx` y `AgendaPage.test.tsx`.

## 4. Verificación integral y cierre

- [x] 4.1 Correr en verde `./mvnw verify` (H2), `./mvnw verify -Pit-mysql` (MySQL 8), `npm test`, `npm run lint`, `npm run build` y `openspec validate c-13-registro-cambios-turno --strict`. Guardar evidencia en `evidencia/verificacion.txt`.
- [x] 4.2 Archivar con `/openspec-archive-change c-13-registro-cambios-turno`, sincronizar spec `auditoria-turnos` y marcar **C-13** `[x]` completo en `CHANGES.md` (US-024 + US-025).
