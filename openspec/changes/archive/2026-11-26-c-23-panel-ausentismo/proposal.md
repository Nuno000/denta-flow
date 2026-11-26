# Proposal: Panel de ausentismo (`C-23`)

## Why

Hoy el personal interno registra la asistencia de cada turno (`asistio`, `ausente`, `ausente_con_aviso`) desde la agenda (`C-13`, `US-024`), pero el consultorio no dispone de indicadores consolidados para medir cuánto ausentismo tiene, en qué profesionales, días o franjas horarias se concentra ni qué pacientes acumulan ausencias reiteradas. Implementar el diferenciador **`C-23` (`panel-ausentismo`, `US-031`)** convierte los registros de asistencia en métricas accionables respetando los permisos por rol (`RN-AU-02`, `03_actores_y_roles.md`) y la minimización de datos personales (`RN-PR-03`, Ley 25.326).

## What Changes

- **Cálculo de indicadores de ausentismo en dominio puro (`ar.dentaflow.dominio.reportes`)**:
  - Considera únicamente turnos con asistencia registrada (`asistio`, `ausente`, `ausente_con_aviso`); excluye `pendiente`, `confirmado` y `cancelado` (`RN-TU-05`, `RN-TU-06`).
  - Calcula tasa general de ausentismo (`(ausente + ausente_con_aviso) / evaluados * 100`, redondeada a 1 decimal; `0.0` si no hay evaluados) y separa `ausentesSinAviso` de `ausentesConAviso`.
  - Desglosa por **profesional**, por **franja horaria** (`Mañana`, `Tarde`, `Noche`, interpretadas en `America/Argentina/Buenos_Aires`), por **día de la semana** (`Lunes` a `Domingo`) y por **pacientes con ausencias reiteradas** (umbral configurable, por defecto `2` o más ausencias en el período).
- **Endpoint REST de solo lectura (`GET /api/reportes/ausentismo`)**:
  - Filtra por rango obligatorio `desde`..`hasta` (máximo 366 días en hora del consultorio), filtro opcional `profesionalId` y umbral opcional `minimoAusencias` (por defecto `2`, mínimo `1`).
  - Protegido con `@PersonalInterno` (`administrador`, `recepcion`, `profesional`): administrador y recepción ven todo el consultorio (o filtran por profesional); el profesional solo ve sus propios indicadores (`403` si solicita otro profesional o no está vinculado).
  - Respuesta mediante DTOs (`record`) sin DNI, teléfono, email ni token del paciente (Ley 25.326, `RN-PR-03`); documentado en OpenAPI.
  - Consulta JPQL única compatible con H2 `MODE=MySQL` y MySQL 8 real, sin migraciones nuevas (reutiliza los índices existentes de `V009_1`).
- **Pantalla "Ausentismo" en el panel interno (`/panel/ausentismo`)**:
  - Enlace `Ausentismo` en `BarraDelPanel` para los tres roles internos.
  - Filtros de rango (atajos rápidos 7/30/90 días y fechas personalizadas), filtro por profesional (oculto para rol `profesional`) y umbral de ausencias reiteradas.
  - Tarjetas resumen, barras visuales y tablas responsive (390 px y 1440 px), con estados de carga, vacío, error y anuncios accesibles WCAG 2.1 AA.

## Capabilities

### New Capabilities
- `panel-ausentismo`: Consulta agregada de métricas de ausentismo por período, profesional, franja horaria, día de la semana y pacientes con ausencias reiteradas, con control de permisos por rol y vista responsive en el panel interno (`C-23`, `US-031`).

### Modified Capabilities
- `autorizacion-por-rol`: Incorporar la sección `Ausentismo` en el menú del panel interno (`BarraDelPanel`) para los tres roles internos (`administrador`, `recepcion`, `profesional`).

## Impact

- **Backend**: nuevos paquetes `ar.dentaflow.dominio.reportes`, `ar.dentaflow.aplicacion.reportes`, `ar.dentaflow.infraestructura.persistencia.reportes` y `ar.dentaflow.presentacion.reportes`. Sin cambios de esquema ni migraciones Flyway nuevas; sin Docker ni Testcontainers.
- **Frontend**: nueva feature `frontend/src/features/ausentismo/`, nueva ruta `/panel/ausentismo` en `frontend/src/routes/rutas.tsx`, actualización de `BarraDelPanel.tsx` y su test `BarraDelPanel.test.tsx`.
- **Privacidad y seguridad**: cumple `RN-AU-02` (RBAC en backend), `RN-PR-01`/`RN-PR-03` (solo nombre y apellido del paciente; pacientes anonimizados muestran sus valores sustitutos) y zona horaria `America/Argentina/Buenos_Aires`.
