# Design — C-05 `gestion-recursos-admin`

## Context

Motivación y alcance: ver `proposal.md`. Requisitos: `specs/gestion-recursos/spec.md` y el delta de `specs/autorizacion-por-rol/spec.md`.

Lo que ya existe y este change aprovecha:

- **Dominio (C-03)**: `dominio/recursos/` tiene `Profesional`, `Sillon` y `Prestacion` en Java puro, mapeados por XML ORM. Ya validan el alta, activan y desactivan, `Sillon.renombrar`, `Prestacion.cambiarDuracion` y `Profesional.habilitarPrestacion`/`deshabilitarPrestacion`. Falta editar los datos del profesional y el nombre de la prestación.
- **Puertos**: `ProfesionalRepositorio`, `SillonRepositorio` y `PrestacionRepositorio` (guardar, buscarPorId, listarTodos, listarActivos; sin borrado). No necesitan métodos nuevos.
- **Esquema (`V003_1`)**: `uk_profesional_email`, `uk_profesional_matricula`, `uk_sillon_nombre`, `uk_prestacion_nombre`, `ck_prestacion_duracion_positiva` y la PK compuesta de `profesional_prestacion`. **No hace falta ninguna migración.**
- **Seguridad (C-04)**: guardas `@SoloAdministrador` y `@PersonalInterno`; `GuardasEnControladoresTest` exige que cada endpoint tenga una guarda; `ManejadorDeErrores` traduce `ValidacionDeDominioException` → 400, `RecursoNoEncontradoException` → 404 y `ConflictoException` → 409 en formato de problema, con `errores` por campo para Bean Validation.
- **Frontend (C-04)**: `apiClient`, `RutaProtegida` con roles, `BarraDelPanel` con `SECCIONES`, la pantalla de usuarios como referencia (tabla responsiva, `Dialogo`, `Campo`, formularios RHF + Zod) y el ayudante de tests `test/apiSimulada`.
- **Política visual**: `design-system/README.md` y `.active-orchestrator-state.json` tienen `Mode: off`.

## Goals / Non-Goals

**Goals:**
- Tres recursos con el mismo patrón de API y de pantalla, así se implementan en serie y se revisan rápido.
- Reutilizar la lógica de validación del dominio. El backend es la única autoridad: el frontend replica las reglas solo por usabilidad.
- Los duplicados dan 409 de forma determinística en MySQL 8 y en H2.

**Non-Goals:**
- Paginación, búsqueda o filtros en los listados: un consultorio chico tiene menos de 20 recursos de cada tipo.
- Control de concurrencia optimista entre dos administradores. Gana la última escritura, porque hay uno o dos administradores por consultorio.
- Avisar de turnos futuros al desactivar un profesional o un sillón: todavía no hay turnos (C-09) y el motor de C-10 excluirá los inactivos.
- Desactivar el usuario vinculado cuando se desactiva un profesional: se mantiene el comportamiento de C-04.

## Decisions

### D1. Tres servicios de aplicación en `aplicacion/recursos/`
`ServicioDeProfesionales`, `ServicioDeSillones` y `ServicioDePrestaciones` (`@Service`, `@Transactional`; las lecturas con `readOnly = true`). Cada uno orquesta: busca o devuelve 404, llama al dominio y verifica duplicados antes de guardar. Devuelven entidades de dominio, o un record `ProfesionalDetalle` cuando hace falta enriquecer, y los controllers las convierten a DTO.
- *Alternativa descartada*: un único `ServicioDeRecursos`. Mezclaría tres agregados y haría tests más grandes. Separados copian la forma de `ServicioDeUsuarios`.

### D2. Edición en el dominio con la misma validación del alta
- `Profesional.actualizarDatos(nombre, apellido, matricula, email)`: valida todo con los mismos ayudantes del constructor antes de asignar. Si un dato falla, no cambia nada (todo o nada). No toca `activo` ni `prestacionIds`.
- `Prestacion.renombrar(nombre)`, más `actualizar(nombre, duracion)`, que valida ambos y después asigna.
- `Sillon.renombrar` ya existe.
- Tests de dominio sin Spring (`DominioSinFrameworksTest` sigue en verde).

### D3. API REST

Prefijos `/api/profesionales`, `/api/sillones` y `/api/prestaciones`. El mismo patrón en los tres:

| Método y ruta | Guarda | Respuesta |
|---|---|---|
| `GET /` | `@PersonalInterno` | 200, lista completa (activos e inactivos) |
| `GET /{id}` | `@PersonalInterno` | 200 / 404 |
| `POST /` | `@SoloAdministrador` | 201 + `Location` / 400 / 409 |
| `PUT /{id}` | `@SoloAdministrador` | 200 / 400 / 404 / 409 |
| `POST /{id}/desactivar` | `@SoloAdministrador` | 200 (idempotente) / 404 |
| `POST /{id}/activar` | `@SoloAdministrador` | 200 (idempotente) / 404 |
| `PUT /api/profesionales/{id}/prestaciones` | `@SoloAdministrador` | 200 / 400 / 404 |

- La activación y la desactivación usan `POST .../desactivar` y `POST .../activar`, como en `UsuariosController`. No hay mapeo `DELETE`: Spring responde 405, que el manejador ya devuelve como problema.
- Orden de los listados: el de los puertos (profesionales por apellido y nombre, sillones por id, prestaciones por nombre).
- **DTO (records en `presentacion/recursos/`)**:
  - Entrada: `ProfesionalSolicitud(nombre, apellido, matricula, email)`, `SillonSolicitud(nombre)`, `PrestacionSolicitud(nombre, duracionMinutos)` y `AsignarPrestacionesSolicitud(List<Long> prestacionIds)`. Llevan `@NotBlank`, `@Size` con los máximos del dominio, `@Email`, `@NotNull` y `@Positive`.
  - Salida: `ProfesionalRespuesta(id, nombre, apellido, matricula, email, activo, prestacionIds)` (ids ordenados), `SillonRespuesta(id, nombre, activo)` y `PrestacionRespuesta(id, nombre, duracionMinutos, activa)`.
  - El alta y la edición usan el mismo DTO.
- *Alternativa descartada*: devolver en el profesional los objetos prestación completos. El frontend ya carga las prestaciones y cruza los ids. Así el DTO es plano y no hay consultas N+1.

### D4. Duplicados: verificación previa sin distinguir mayúsculas, más el respaldo de la constraint
- Antes de guardar, el servicio compara contra `listarTodos()` y excluye el propio id en las ediciones:
  - Sillón y prestación: el nombre recortado, con `equalsIgnoreCase` (`Locale.ROOT`).
  - Profesional: el email (el dominio ya lo pasa a minúsculas) y la matrícula, si se informa.
  - Si hay coincidencia, lanza `ConflictoException` con un mensaje en español ("Ya existe un sillón con ese nombre").
  - Los listados son chicos, así que recorrerlos en memoria cuesta poco.
- **Por qué**: MySQL 8 con `utf8mb4_0900_ai_ci` compara sin distinguir mayúsculas ni acentos, y H2 sí las distingue. La verificación previa hace que los dos motores se comporten igual. La constraint `UNIQUE` sigue siendo la garantía final ante dos altas concurrentes.
- **Respaldo**: el servicio guarda con `flush` y atrapa `DataIntegrityViolationException`. Si el mensaje contiene `uk_sillon_nombre`, `uk_prestacion_nombre`, `uk_profesional_email` o `uk_profesional_matricula`, la traduce a `ConflictoException`. Es el mismo patrón de `ServicioDeUsuarios`. Un test `@Tag("mysql")` (perfil `it-mysql`) confirma que MySQL incluye el nombre de la constraint en el mensaje.
- *Riesgo aceptado*: MySQL además ignora los acentos ("Sillon 1" choca con "Sillón 1") y H2 no. En MySQL la verificación previa deja pasar ese caso, pero la constraint lo rechaza y el respaldo devuelve 409, así que el usuario igual ve un conflicto. En H2 se aceptaría; no se testea ese caso.

### D5. Asignación de prestaciones: reemplazo del conjunto, todo o nada
`ServicioDeProfesionales.asignarPrestaciones(id, ids)`:
1. Elimina los ids repetidos.
2. Para cada id busca la prestación. Si no existe → `ValidacionDeDominioException("La prestación 99 no existe")` → 400.
3. Calcula `agregadas = ids − actuales`. Si alguna agregada está inactiva → 400, usando la validación que ya hace `habilitarPrestacion`.
4. Recién después aplica los cambios: deshabilita las que salen y habilita las agregadas (las inactivas que ya tenía se conservan sin pasar por `habilitarPrestacion`).

Como toda la validación ocurre antes de modificar, un error no deja cambios a medias aunque la transacción no se revierta. El `@Transactional` revierte igual.
- *Alternativa descartada*: endpoints para agregar o quitar de a una prestación. Implican más llamadas desde el diálogo con casillas y estados intermedios. El reemplazo coincide con el formulario.

### D6. Frontend: `features/recursos/` con sub-rutas por pestaña
- **Rutas** (dentro de `RutaProtegida` para los tres roles internos):
  - `/panel/recursos` redirige a `/panel/recursos/profesionales`.
  - Hay sub-rutas `profesionales`, `sillones` y `prestaciones`, renderizadas por `RecursosPage`, que tiene el encabezado y la navegación entre pestañas.
  - Las pestañas son enlaces (`NavLink` con `aria-current="page"`), no un `tablist` con estado. Así cada pestaña tiene URL propia, el botón atrás funciona y la accesibilidad es simple.
  - La entrada de menú "Recursos" va sin `end`, así queda marcada en cualquier sub-ruta.
- **Componentes**:
  - `ProfesionalesTab`, `SillonesTab` y `PrestacionesTab`: tabla responsiva con el patrón de `UsuariosPage`.
  - `FormularioProfesional`, `FormularioSillon` y `FormularioPrestacion`: RHF + `zodResolver`, el mismo componente para alta y edición, con valores iniciales.
  - `DialogoPrestaciones`: casillas de las prestaciones activas, más las inactivas que ya tenía el profesional, rotuladas "(inactiva)".
  - `ConfirmarDesactivacion`.
- **Datos**: `features/recursos/api.ts` con un hook por consulta (`useProfesionales`, `useSillones`, `usePrestaciones`) y mutaciones que invalidan solo la clave del recurso afectado.
  - La asignación de prestaciones invalida profesionales.
  - Desactivar una prestación invalida prestaciones y profesionales.
- **Esquemas Zod** en `features/recursos/esquemas.ts`: largos máximos (100/100/30/254 para el profesional, 50 para el sillón, 100 para la prestación), email válido y `duracionMinutos` entero > 0 con el mensaje "La duración debe ser mayor que cero", igual al del dominio.
- **Permisos en la UI**: `const esAdmin = usuario.rol === 'administrador'` decide si se renderizan las acciones. No hay otra lógica de permisos en el cliente.
- **Componentes compartidos**: `Campo`, `Dialogo` y `estilosFormulario` pasan de `features/usuarios/` a `components/formulario/`, y `leerErrorDeApi` pasa a `lib/erroresApi.ts`. Es un refactor mecánico: se actualizan los imports de usuarios y sus tests siguen en verde sin cambios de comportamiento.
  - *Alternativa descartada*: importar desde `features/usuarios/`, que acopla dos features.
- La zona de `CHANGES.md` menciona `features/agenda/`, pero esa carpeta queda para la agenda de C-11. Los recursos van en su propia feature.

### D7. Mensajes de estado y accesibilidad
- Región `role="status"` (como en usuarios) para "Prestación creada", "Sillón desactivado", etc.
- Las tablas llevan `<caption>` o `aria-label` y encabezados `<th scope="col">`.
- Los diálogos con foco atrapado y Escape ya resueltos en `Dialogo`.
- La duración se muestra como "40 min".

## Risks / Trade-offs

- [Dos administradores editan a la vez el mismo recurso] → gana la última escritura. Es aceptable por el tamaño del consultorio y queda documentado como no-objetivo.
- [Diferencia de colación entre MySQL y H2 para acentos] → la verificación previa ignora mayúsculas en ambos motores y la constraint con el respaldo de D4 cubre MySQL. Hay un test `@Tag("mysql")` de duplicado → 409.
- [Desactivar un profesional que tiene usuario vinculado] → el usuario sigue pudiendo ingresar. No se cambia ahora; si molesta en la demo, se resuelve en otro change.
- [Mover componentes de `features/usuarios/`] → puede romper imports. Los tests de usuarios de C-04 deben seguir en verde sin modificar las aserciones, y se mueve en una tarea separada antes de construir la UI nueva.
- [Plazo: entrega el 26/11] → el backend se puede mergear completo aunque falten pantallas. Las tareas están ordenadas para que la API quede terminada y verificada primero.

## Migration Plan

- No hay migración de base ni variables de entorno nuevas. El deploy es el normal de backend y frontend.
- Rollback: revertir el merge. No quedan datos incompatibles porque el esquema no cambia.

## UI Mockups

- Design system revision: no aplica (`Mode: off`, `Approval: skipped`)
- Decision: no (project policy) — 2026-11-25
- Mockups: —
- Approval: —
- Preview: —
- Visual verify: —
