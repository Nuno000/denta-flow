# Design — C-13 parte 2 `registro-cambios-turno` (US-025)

## Context

- **C-08 (`ServicioDeAuditoria`, `CambioDeTurno`):** ya expone `historialDe(long turnoId)` en orden cronológico ascendente con desempate estable por `id`, sin operaciones de edición ni borrado.
- **C-09 (`ServicioDeTurnos`):** expone `buscarPorId(long turnoId)` para verificar existencia antes de consultar el historial.
- **C-04 (`@SoloAdministrador`, `UsuarioRepositorio`):** guarda de método verificada en backend (`GuardasEnControladoresTest`) y resolución de email de usuarios internos (activos o inactivos).
- **C-11 y C-13 parte 1 (`TarjetaDeTurno`, `api.ts`, `Dialogo`):** la agenda interna ya muestra los turnos (incluso cancelados filtrando por estado) y el repo cuenta con `Dialogo.tsx` accesible (`<dialog>` nativo con `Escape` y restauración de foco).
- **Política visual del proyecto:** `off`. Decision: no (project policy).

## Goals / Non-Goals

**Goals:**
- Exponer el historial por turno en solo lectura exclusivamente para `administrador` (RBAC en backend y reflejado en UI).
- Mostrar al administrador quién, cuándo (en `America/Argentina/Buenos_Aires`), qué acción y qué valores cambiaron de forma clara y sin datos personales del paciente.
- Mantener cambios acotados y sin conflictos con C-12, C-16, C-17 ni C-19.

**Non-Goals:**
- Editar o borrar entradas de auditoría.
- Listado global de auditoría sin turno o filtros avanzados de fechas/actores.

## Decisions

### D1. Caso de uso `aplicacion/auditoria/ConsultaDeHistorialDeTurno`
- Anotado con `@Service` y `@Transactional(readOnly = true)`.
- Dependencias: `ServicioDeTurnos`, `ServicioDeAuditoria`, `UsuarioRepositorio`.
- `consultar(long turnoId)`:
  1. Verifica existencia con `turnos.buscarPorId(turnoId).orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"))`.
  2. Obtiene `List<CambioDeTurno> cambios = auditoria.historialDe(turnoId)`.
  3. Reúne los `usuarioId` distintos no nulos, resuelve su email en un mapa `Map<Long, String>` vía `usuarios.buscarPorId(id)` (si un usuario existió y está inactivo, `buscarPorId` igual lo encuentra porque no hay borrado físico).
  4. Mapea cada `CambioDeTurno` a `EntradaDeHistorial(id, ocurridoEn, actorTipo, actorEmail, accion, valorAnterior, valorNuevo)`.

### D2. Controller `presentacion/auditoria/HistorialTurnoController`
- `@RestController`, `@RequestMapping("/api/turnos")`, `@Tag(name = "Auditoría", description = "Registro de cambios de turnos (solo administrador)")`, `@SecurityRequirement(name = "bearerAuth")`.
- `@SoloAdministrador` + `@GetMapping("/{id}/historial")` → `List<CambioDeTurnoRespuesta>`.
- `CambioDeTurnoRespuesta(Long id, Instant ocurridoEn, String actorTipo, String actorEmail, String accion, Map<String, String> valorAnterior, Map<String, String> valorNuevo)`.
- OpenAPI con `@Operation` y `@ApiResponse` para `200`, `401`, `403`, `404`.
- Sin métodos `POST`, `PUT`, `PATCH` ni `DELETE`.

### D3. Frontend (`frontend/src/features/agenda/`)
- **`api.ts`:** agrega tipos `TipoActorHistorial`, `AccionHistorial`, `CambioDeTurno` y hook `useHistorialDeTurno(turnoId: number | null)` (`queryKey: ['turno-historial', turnoId]`, `enabled: turnoId !== null`, `apiClient.get<CambioDeTurno[]>('/api/turnos/${turnoId}/historial')`).
- **`HistorialDeTurnoDialogo.tsx`:**
  - Reutiliza `Dialogo` (`components/formulario/Dialogo.tsx`).
  - Formatea `ocurridoEn` en `America/Argentina/Buenos_Aires` (`dd/MM/aaaa HH:mm`) usando `Intl.DateTimeFormat('es-AR', { timeZone: ZONA_CONSULTORIO, ... })`.
  - Traduce actor: `usuario` → `actorEmail ?? 'Usuario del consultorio'`, `paciente` → `'Paciente'`, `sistema` → `'Sistema'`.
  - Traduce acción: `creacion` → `'Creación'`, `confirmacion` → `'Confirmación'`, `cancelacion` → `'Cancelación'`, `reprogramacion` → `'Reprogramación'`, `cambio_estado` → `'Cambio de estado'`.
  - Presenta los campos de `valorAnterior` y `valorNuevo` con etiquetas legibles (`estado`, `inicio`, `fin`, `origen`, `es_sobreturno`, etc.), convirtiendo estados a `NOMBRES_DE_ESTADO` e instantes ISO-8601 a fecha/hora del consultorio.
  - Muestra estado de carga, lista vacía, o alerta con botón `"Reintentar"` ante error.
- **`TarjetaDeTurno.tsx`:**
  - Lee `useSesion()` para saber si `usuario?.rol === 'administrador'`.
  - Solo para `administrador`, renderiza el botón `"Ver historial"` (visible incluso en turnos `cancelado`) y monta `HistorialDeTurnoDialogo` cuando está abierto.
  - En los tests existentes de `TarjetaDeTurno.test.tsx` (que se renderizan sin sesión), el botón no aparece y no rompe ningún test previo.

### D4. Tests
- **Backend unitarios (Mockito):** `ConsultaDeHistorialDeTurnoTest` (404 si turno no existe, orden preservado, resolución de emails para usuario y `null` para paciente/sistema, lista vacía).
- **Backend integración H2 (MockMvc):** `HistorialTurnoControllerTest` (200 para admin con historial real generado por `ServicioDeTurnos`, 403 para recepción y profesional, 401 sin token, 404 para turno inexistente, 4xx en DELETE/PUT, sin datos personales del paciente ni token, `/v3/api-docs`).
- **MySQL 8:** sin concurrencia ni DDL nuevo; se corre `./mvnw verify -Pit-mysql` completo para confirmar 0 regresiones.
- **Frontend (Vitest + Testing Library):** `HistorialDeTurnoDialogo.test.tsx` + tests en `TarjetaDeTurno.test.tsx` / `AgendaPage.test.tsx` (visibilidad solo para admin, apertura del diálogo, render de cambios en hora del consultorio, vacío, error con reintento, sin controles de edición/borrado).

## Risks / Trade-offs

- **[`valorAnterior` / `valorNuevo` en `creacion` contienen `paciente_id`, `profesional_id`, `sillon_id`]** → Son solo IDs técnicos numéricos, sin DNI, teléfono, email ni nombre del paciente (cumple Ley 25.326). La UI los presenta de forma legible sin exponer PII.
- **[Consulta de usuarios por id en `ConsultaDeHistorialDeTurno`]** → Un turno típico tiene 1 a 5 cambios; deduplicar los `usuarioId` evita consultas repetidas sin agregar métodos nuevos a `UsuarioRepositorio`.

## Migration Plan

Sin migraciones ni configuración nueva. Rollback: revertir el PR.
