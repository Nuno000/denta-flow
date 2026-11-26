# Proposal — C-05 `gestion-recursos-admin`

## Why

C-03 dejó el modelo de profesionales, sillones y prestaciones con su esquema (`V003_1`) y datos de demostración, pero no hay forma de administrarlos: el único camino para cargarlos es el seed. US-001 pide que el administrador dé de alta, edite y desactive los recursos reales del consultorio y diga qué prestaciones hace cada profesional (Flujo 7, primer paso de la configuración). Además, la matriz RBAC de `03_actores_y_roles.md` da lectura de estos recursos a recepción y profesional, que hoy no la tienen. Sus dependencias (C-03 y C-04) ya están en `main`, y el change suma pantallas visibles para la demo del 26/11.

## What Changes

- **API REST de recursos** (backend, capas `presentacion` → `aplicacion` → `dominio`):
  - `/api/profesionales`, `/api/sillones` y `/api/prestaciones`: listar (activos e inactivos, con su estado) y consultar por id para todo el personal interno (`administrador`, `recepcion`, `profesional`).
  - Alta, edición, desactivación y reactivación, **solo para `administrador`**. Ningún endpoint borra (se mantiene "Desactivar sin borrar" de C-03).
  - `PUT /api/profesionales/{id}/prestaciones`: reemplaza el conjunto de prestaciones que realiza un profesional (US-001, criterio 3).
  - Validación con Bean Validation en los DTO (records) y en el dominio. Respuestas de error en formato de problema (RFC 9457) con 400, 404 o 409 (duplicados).
- **Dominio** (`dominio/recursos`, Java puro): edición de los datos del profesional y del nombre y la duración de la prestación, con las mismas validaciones del alta. Cambiar la duración no afecta a los turnos ya creados (RN-TU-03).
- **Frontend**: sección "Recursos" del panel interno (`/panel/recursos`) con pestañas Profesionales, Sillones y Prestaciones; tablas responsivas y formularios con React Hook Form + Zod; acciones de edición solo visibles para el administrador; consultas y mutaciones con TanStack Query desde un cliente en `features/recursos/api.ts`.
- **Menú del panel**: se agrega la entrada "Recursos", visible para los tres roles internos.
- **Sin migración nueva**: el esquema de C-03 ya cubre todo (tablas, `UNIQUE`, `CHECK` de duración y la relación profesional-prestación).
- Fuera de alcance: horarios y bloqueos (C-06), qué pasa con los turnos de un profesional o sillón desactivado (C-09/C-10) y la auditoría de cambios de recursos (RN-AU-01 solo exige auditar turnos).

## Capabilities

### New Capabilities
- `gestion-recursos`: administración de profesionales, sillones y prestaciones por API y desde el panel. Cubre quién puede leer y quién puede modificar cada recurso, la edición, la activación y desactivación, la asignación de prestaciones a un profesional, el manejo de duplicados y las pantallas del panel.

### Modified Capabilities
- `autorizacion-por-rol`: el requisito "Menú del panel según el rol" suma la sección "Recursos" para los tres roles internos, así que cambian sus escenarios.

## Impact

- **Backend**: nuevo `aplicacion/recursos/` (servicios de profesionales, sillones y prestaciones) y `presentacion/recursos/` (3 controllers + DTO records). Métodos nuevos de edición en `dominio/recursos/Profesional` y `Prestacion`. Se reutilizan las guardas `@SoloAdministrador` y `@PersonalInterno` y el `ManejadorDeErrores` de C-04. Los puertos de repositorio no cambian.
- **API**: 19 endpoints nuevos (6 por recurso más la asignación de prestaciones) bajo `/api/profesionales`, `/api/sillones` y `/api/prestaciones`, documentados en OpenAPI.
- **Frontend**: nueva `features/recursos/`, ruta `/panel/recursos/*` en `routes/rutas.tsx` y la entrada nueva en `BarraDelPanel`. Los componentes de formulario de `features/usuarios/` (`Campo`, `Dialogo`, `estilosFormulario`) y `leerErrorDeApi` pasan a `components/` y `lib/` para compartirlos, sin cambiar su comportamiento.
- **Base de datos**: sin cambios. Tests sobre H2 `MODE=MySQL`, más un test `@Tag("mysql")` que mapea los duplicados a 409 en MySQL 8 real (perfil `it-mysql`). Sin Docker ni Testcontainers.
- **Referencias**: `CHANGES.md` §C-05, US-001, RN-TU-03, RN-AU-02, matriz RBAC (`03_actores_y_roles.md`), Flujo 7 (`07_flujos_principales.md`).
