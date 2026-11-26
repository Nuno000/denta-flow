# Design — C-15 `privacidad-ley-25326`

## Context

- **Gobernanza ALTA** (datos personales, Ley 25.326): este diseño se revisa y aprueba antes de escribir código.
- PA-12 resuelta el 2026-11-25 (DD-12): aceptación obligatoria con instante, endpoint de anonimización/baja, retención de 5 años. PA-13/SU-03: email obligatorio y DNI obligatorio y único.
- C-07 ya entrega los parámetros `aviso-privacidad-texto` y `aviso-privacidad-leyenda` y su lectura tipada para otros procesos.
- C-04 ya entrega JWT, roles, `/api/publico/**` público y errores RFC 9457.
- **C-09 no está mergeado**: la entidad `Paciente`, la tabla `paciente` y el servicio de turnos son de Renzo. Este diseño fija lo que C-15 necesita de ellos y separa el trabajo en dos bloques (ver `tasks.md`).

## Goals / Non-Goals

**Goals**
- Aviso público y componente de aceptación listos para que C-16 los use.
- Consentimiento verificado en el backend con instante en UTC, atómico con la reserva.
- Baja por anonimización que respeta la retención de turnos y auditoría.
- Ningún dato personal en logs ni en errores.

**Non-Goals**
- Purga automática a los 5 años (punto de extensión documentado en D6).
- Versionado del texto del aviso aceptado (riesgo aceptado, ver Riesgos).
- Listados o fichas de pacientes por rol (C-11/C-12).
- Texto legal definitivo (lo edita el administrador en parámetros).

## Decisions

### D1. Qué espera C-15 de C-09 (revisado contra C-09 mergeado, 2026-11-26)
C-09 entregó `dni VARCHAR(8) NOT NULL UNIQUE` con `ck_paciente_dni_largo` (7 u 8 caracteres), `consentimiento_privacidad_en` y `Paciente.registrarConsentimiento`. El paciente se registra en **su propia transacción, antes que el turno** (`ServicioDePacientes`), con el consentimiento en `DatosPaciente.consentimientoEn`. Por eso cambian D2, D3 y D4 (decisión de Bruno del 2026-11-26: DNI nulo al anonimizar). Lo que sigue es el contrato original, superado:
- Tabla `paciente` con `dni VARCHAR(20) NOT NULL UNIQUE`, `email VARCHAR(254) NOT NULL`, `telefono`, `nombre`, `apellido` NOT NULL, y `consentimiento_privacidad_en DATETIME(6) NULL`.
- Deduplicación por DNI dentro de la transacción de reserva.
- Si C-09 no crea `consentimiento_privacidad_en`, la agrega `V015_1`. El largo de 20 en `dni` alcanza para el sustituto `ANON-<id>` (D4).

### D2. Migración `V015_1__privacidad_paciente.sql`
```sql
ALTER TABLE paciente MODIFY dni VARCHAR(8) NULL;
ALTER TABLE paciente ADD CONSTRAINT ck_paciente_dni_o_anonimizado
  CHECK (dni IS NOT NULL OR anonimizado_en IS NOT NULL);
ALTER TABLE paciente ADD COLUMN anonimizado_en DATETIME(6) NULL;
ALTER TABLE paciente ADD COLUMN anonimizado_por BIGINT NULL;
ALTER TABLE paciente ADD CONSTRAINT fk_paciente_anonimizado_por
  FOREIGN KEY (anonimizado_por) REFERENCES usuario (id);
ALTER TABLE paciente ADD CONSTRAINT ck_paciente_anonimizado
  CHECK ((anonimizado_en IS NULL AND anonimizado_por IS NULL)
      OR (anonimizado_en IS NOT NULL AND anonimizado_por IS NOT NULL));
```
SQL válido en MySQL 8 y H2 `MODE=MySQL`. Sigue la numeración por change (DD-09) y debe mergearse después de C-09 (`V009_1`). Si una base de desarrollo ya tiene aplicada una migración de número mayor (por ejemplo `V016_1`), Flyway rechaza `V015_1` por fuera de orden: en ese caso se recrea la base local, y el PR lo avisa.

### D3. Consentimiento en el dominio (Java puro)
**Revisado (2026-11-26):** el consentimiento se registra con el paciente, no en la transacción del turno. `ServicioDePacientes.registrarDesdeReservaOnline` rechaza (`ValidacionDeDominioException`, sin crear ni modificar) un `DatosPaciente` sin `consentimientoEn`; C-16 pasa ahí el instante del `Clock` cuando `aceptaAvisoPrivacidad` es verdadero. `RegistroDeConsentimiento` con `MANDATORY` queda descartado. Texto original, superado:
- `Paciente.registrarConsentimiento(Instant ahora)`: guarda el instante; rechaza `null`.
- `ConsentimientoPrivacidad.exigir(boolean aceptado)`: lanza `ValidacionDeDominioException` con código `aviso-privacidad-no-aceptado` si es falso.
- `aplicacion.pacientes.RegistroDeConsentimiento.registrar(Paciente, boolean aceptado)` con `@Transactional(propagation = MANDATORY)`: exige la transacción de la reserva de C-16, toma la hora del `Clock` (C-04, `RelojConfig`). Si la reserva falla, el instante se revierte con ella.
- En el DTO de C-16, `aceptaAvisoPrivacidad` es `Boolean` con `@NotNull @AssertTrue`: el 400 lo da Bean Validation antes de tocar la base; el dominio lo vuelve a verificar (defensa en profundidad).

### D4. Anonimización
- `Paciente.anonimizar(long usuarioId, Instant ahora)` en el dominio: si ya está anonimizado no hace nada (idempotente); si no, reemplaza:
  - `nombre` → `Paciente`, `apellido` → `anonimizado`
  - `dni` → `NULL` (permitido solo con `anonimizado_en`; MySQL y H2 admiten varios `NULL` en un `UNIQUE`; libera el DNI original). Revisado el 2026-11-26: el `ANON-<id>` original no entra en `VARCHAR(8)` ni en el formato de C-09
  - `telefono` → `00000000`
  - `email` → `anonimizado-<id>@dentaflow.invalid` (RFC 2606: nunca entregable)
  - `consentimiento_privacidad_en` se conserva (prueba del consentimiento dado).
- `aplicacion.pacientes.ServicioDeAnonimizacion.anonimizar(id, usuarioActual)` (`@Transactional`): bloquea la fila del paciente (`PESSIMISTIC_WRITE`) para que una reserva simultánea con el mismo DNI no cree un turno nuevo en medio; consulta si hay turnos con estado `pendiente|confirmado` e `inicio > ahora` (puerto de lectura de C-09); si hay, lanza `ConflictoDeNegocioException` → 409.
- Los turnos y `turno_historial` se conservan. `turno_historial` guarda estados y fechas, no datos personales (C-08), así que no requiere cambios.
- `presentacion.pacientes.PacienteAdminController`: `POST /api/pacientes/{id}/anonimizar` con `@PreAuthorize("hasRole('ADMINISTRADOR')")`; responde 200 con `{id, anonimizadoEn}`.
- Log: `INFO paciente {id} anonimizado por usuario {usuarioId}` (solo ids).

### D5. Aviso público
- `presentacion.publico.AvisoPrivacidadController` → `GET /api/publico/aviso-privacidad` → record `AvisoPrivacidadDto(texto, leyenda)` desde la lectura tipada de C-07. Sin caché en el backend (el parámetro debe verse al instante); `Cache-Control: no-cache`.

### D6. Retención de 5 años
- Política documentada en `knowledge-base/08_arquitectura_propuesta.md` §Seguridad y en el README del backend: turnos y auditoría se conservan 5 años; la baja anonimiza al instante.
- Punto de extensión: `ServicioDeAnonimizacion.anonimizarInactivosDesde(Instant limite)` queda **sin implementar** y descrito en el Javadoc del servicio; podría dispararse con el mismo mecanismo de tareas de C-18.

### D7. Logs y errores sin datos personales
- El `GlobalExceptionHandler` de C-04 no incluye `rejectedValue` en los 400 de Bean Validation (solo campo y mensaje).
- `spring.jpa.show-sql=false` y sin logging de bodies en ningún perfil.
- Test `SinDatosPersonalesEnLogsTest` con un `ListAppender` de Logback sobre el logger raíz.

### D8. HTTPS detrás del proxy
- `server.forward-headers-strategy: framework` en `application.yml` (Render y Vercel terminan TLS en su proxy).
- HSTS con la configuración por defecto de Spring Security (solo se envía en requests seguros), `max-age` de un año, sin `preload`.

### D9. Frontend
- `features/reserva/AvisoPrivacidad.tsx`: recibe `register`/`control` de React Hook Form; muestra el texto en un bloque desplegable (`<details>`) y una casilla con la leyenda, `aria-describedby` al error y foco al error al enviar.
- `features/reserva/avisoPrivacidadApi.ts` (esquema y hook juntos; `avisoPrivacidad.ts` choca con `AvisoPrivacidad.tsx` en Windows): `aceptaAvisoPrivacidad: z.literal(true, { errorMap: () => ({ message: 'Tenés que aceptar el aviso de privacidad para reservar' }) })`.
- `useAvisoPrivacidad` (en el mismo archivo): `useQuery` sobre `apiClient.get('/api/publico/aviso-privacidad')`; estados de carga y error con reintento.

## Risks / Trade-offs

- **C-09 cambia el modelo de `paciente`** → el bloque B se ajusta; por eso D1 se acuerda con Renzo antes de aplicar.
- **No se guarda qué versión del texto se aceptó** → si el texto cambia, no se puede probar cuál aceptó cada paciente. Aceptado para el MVP; mejora futura: guardar un hash del texto junto con el instante.
- **Anonimizar exige cancelar antes los turnos futuros** → un paso más para el administrador, pero evita turnos activos sin forma de contacto.
- **Valores sustitutos en vez de `NULL`** → mantiene los NOT NULL de C-09; cualquier envío de email los descarta por `anonimizado_en`, no por el formato.
- **Numeración `V015_1`** con bases locales que ya tengan migraciones posteriores → se documenta recrear la base de desarrollo.

## Migration Plan

1. Bloque A (sin C-09): aviso público, componente, logs, HTTPS.
2. Merge de C-09 a `main`, rebase de `feat/C-15-privacidad-ley-25326`.
3. Bloque B: `V015_1`, consentimiento, anonimización.
4. Rollback: revertir el PR; `V015_1` solo agrega columnas nulas, sin pérdida de datos.

## UI Mockups

- Design system revision: no aplica
- Decision: no (project policy) — 2026-11-25 (`visual_design.mode = off`)
