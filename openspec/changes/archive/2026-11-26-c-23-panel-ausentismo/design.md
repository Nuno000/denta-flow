# Design: Panel de ausentismo (`C-23`)

## Context

`C-13` (US-024) ya permite registrar los estados de asistencia (`asistio`, `ausente`, `ausente_con_aviso`) sobre la tabla `turno`, la cual cuenta con índices compuestos `(profesional_id, estado, inicio, fin)` y `(estado, inicio)` creados en `V009_1`. En `C-23` (US-031) construimos un reporte de solo lectura sobre esos mismos datos, sin alterar el esquema de base de datos ni agregar dependencias externas.

## Goals / Non-Goals

**Goals:**
- Calcular tasas de ausentismo exactas en dominio Java puro (`ar.dentaflow.dominio.reportes`) usando `America/Argentina/Buenos_Aires` para límites diarios, franjas horarias y días de la semana.
- Exponer `GET /api/reportes/ausentismo` protegido por `@PersonalInterno`, aplicando la regla de "propios" al rol `profesional` y minimización de datos personales (Ley 25.326).
- Verificar la consulta JPQL tanto en H2 `MODE=MySQL` como en MySQL 8 real (`@Tag("mysql")`), sin Docker ni Testcontainers.
- Incorporar la vista `/panel/ausentismo` responsive (390 px y 1440 px) y accesible (WCAG 2.1 AA) con enlace en `BarraDelPanel`.

**Non-Goals:**
- Exportación PDF/Excel, gráficos con librerías pesadas de terceros o comparativas multi-sede.
- Alertas automáticas o sanciones a pacientes reincidentes.
- Modificación de turnos o estados desde el panel de ausentismo (es estrictamente de solo lectura).

## Decisions

- **D1 — Definición de tasa y universo evaluado:** Solo se consideran los estados `ASISTIO`, `AUSENTE` y `AUSENTE_CON_AVISO`. `CANCELADO` liberó el turno con anticipación (`RN-TU-06`) y `PENDIENTE`/`CONFIRMADO` aún no fueron evaluados. `totalAusentes = ausentesSinAviso + ausentesConAviso`, y `tasaAusentismo = (totalAusentes * 100.0) / totalEvaluados` con redondeo `RoundingMode.HALF_UP` a 1 decimal (`0.0` si `totalEvaluados == 0`).
- **D2 — Agregación en dominio Java puro en vez de SQL dependiente de zona horaria:** Como MySQL 8 y H2 difieren en funciones de zona horaria (`CONVERT_TZ` requiere tablas de zonas cargadas en MySQL y no existe igual en H2), la consulta JPQL (`LecturaDeAusentismoJpa`) recupera en un solo `SELECT` con proyección constructor (`RegistroAsistenciaParaReporte`) los campos mínimos (`inicio`, `estado`, profesional id/nombre/apellido, paciente id/nombre/apellido) filtrados por `[desde, hasta)` en UTC y estado en los 3 estados de asistencia. El cálculo de `FranjaHoraria`, `DayOfWeek` en `America/Argentina/Buenos_Aires` y las agregaciones ocurren en `CalculadorDeAusentismo` (Java puro, sin Spring ni JPA). Para un consultorio chico (1–5 profesionales) y un rango acotado a 366 días, el volumen es de unos pocos miles de filas y el comportamiento es 100 % idéntico entre H2 y MySQL 8.
- **D3 — Regla de "propios" idéntica a `ConsultaDeAgenda`:** `ConsultaDeAusentismo` reutiliza la misma política de `C-11`: `ADMINISTRADOR` y `RECEPCION` ven todos los profesionales o el `profesionalId` pedido; `PROFESIONAL` sin `profesionalId` recibe su propio profesional y ante `profesionalId` ajeno o sin vínculo recibe `AccessDeniedException` (403).
- **D4 — Franjas horarias fijas en hora del consultorio:** `manana` (`00:00–11:59`, "Mañana (antes de las 12:00)"), `tarde` (`12:00–17:59`, "Tarde (12:00 a 17:59)") y `noche` (`18:00–23:59`, "Noche (desde las 18:00)"). Se devuelven siempre en orden cronológico para mantener consistencia visual.
- **D5 — Pacientes con ausencias reiteradas y privacidad (Ley 25.326):** Umbral configurable vía `minimoAusencias` (por defecto `2`, mínimo `1`). La proyección nunca lee DNI, teléfono, email ni token de acceso; los pacientes anonimizados (`C-15`) conservan su conteo histórico mostrando `"Paciente Anonimizado"`.
- **D6 — Rango máximo de consulta:** Máximo `366` días inclusivos para acotar memoria y latencia del endpoint.
- **D7 — Frontend desacoplado y sincronizado en URL:** Nueva feature `frontend/src/features/ausentismo/` con `api.ts` (TanStack Query), `parametros.ts` (validación y normalización de rango/filtros en URL con defecto de últimos 30 días en hora del consultorio), componentes reutilizables (`TarjetasResumen`, `BarraDeTasa`, `TablaAusentismoProfesionales`, `TablaPacientesReiterados`) y `AusentismoPage`.

## UI Mockups

- `Decision: no (project policy)` — `.active-orchestrator-state.json` tiene `visual_design.mode: "off"`. Se siguen las clases Tailwind, componentes (`BarraDelPanel`, `Celda`, `estilos.ts`) y estándares WCAG 2.1 AA ya establecidos en el panel interno.

## Risks / Trade-offs

- **Volumen en rangos largos:** Para un consultorio chico (1–5 profesionales) un año representa ~5.000–15.000 turnos como máximo, manejable en milisegundos en memoria; el tope de 366 días y los índices `ix_turno_estado_inicio` / `ix_turno_profesional_solapamiento` previenen consultas descontroladas.
- **Impacto en menú del panel:** Agregar `Ausentismo` a `BarraDelPanel` actualiza el contrato del menú en `autorizacion-por-rol` y requiere actualizar las aserciones de `BarraDelPanel.test.tsx`.
