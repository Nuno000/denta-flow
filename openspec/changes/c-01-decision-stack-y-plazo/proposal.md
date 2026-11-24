# Proposal — C-01 `decision-stack-y-plazo`

## Why

El equipo cambió el stack a **MySQL 8 nativo + Flyway** con tests sobre **H2 `MODE=MySQL`**, sin Docker ni Testcontainers, por limitación de RAM en las máquinas locales. Hoy ese cambio solo figura en `openspec/config.yaml` y en `docs/plan-trabajo-equipo.md`. La KB (DD-08), `CLAUDE.md` y `.atl/skill-registry.md` siguen exigiendo PostgreSQL 16, `EXCLUDE USING gist` y Testcontainers, y `CLAUDE.md` además prohíbe H2. Así, cualquier agente que implemente C-02 en adelante recibe instrucciones contradictorias. Además, ya se conoce el plazo de entrega (PA-01: **jueves 2026-11-26, 12:00**) y el equipo resolvió PA-03, PA-05, PA-06 y PA-10. C-01 es la compuerta del roadmap: ningún otro change debe empezar hasta registrarlo.

## What Changes

- **Nueva decisión DD-09 — Stack MySQL 8 sin Docker** (reemplaza parcialmente DD-08, que queda como histórica):
  - MySQL 8 (InnoDB) + Flyway (`flyway-mysql`, `mysql-connector-j`) en desarrollo nativo y en despliegue; H2 en memoria `MODE=MySQL;DATABASE_TO_LOWER=TRUE` para el perfil `test`.
  - Sin Docker, `docker-compose` ni Testcontainers; sin Mailpit en contenedor.
  - Anti-solapamiento (RN-TU-01/02/07, DD-02): verificación dentro de la transacción de creación/cambio de turno con **bloqueo pesimista** (`SELECT ... FOR UPDATE`) sobre el **profesional y luego el sillón**, en ese orden fijo, más índices compuestos por recurso. Reemplaza la restricción `EXCLUDE USING gist`.
  - Tests de concurrencia también contra un MySQL 8 local (perfil `it-mysql`), porque H2 no reproduce el bloqueo de filas de InnoDB.
  - Neon deja de aplicar: el proveedor gratuito de MySQL para producción se decide en C-19 (nueva PA-14).
- **PA-01 resuelta**: entrega el 2026-11-26 a las 12:00. **Recorte del alcance** registrado en `01_vision_y_objetivos.md` y `CHANGES.md`:
  - **Corte de entrega**: C-01, C-02, C-03, C-04, C-06, C-07, C-08, C-09, C-10, C-14, C-15, C-16, C-17 y C-19. Es el camino crítico más sus dependencias reales; C-06, C-07, C-14 y C-15 en versión mínima.
  - **Fuera del corte, sin borrarlos del roadmap**: C-05 (alcanza el seed), C-11, C-12, C-13, C-18 y los diferenciadores C-20 a C-24. Durante el corte, las dependencias de C-19 se limitan a los changes que están dentro.
- **PA resueltas** en `10_preguntas_abiertas.md` y reflejadas en `09`, `04` y `CHANGES.md`:
  - **PA-03**: WhatsApp en el MVP solo por enlace `wa.me` prellenado, sin API (confirma DD-05; IN-01 resuelta).
  - **PA-05**: el paciente elige un profesional o "cualquiera / primero disponible" (parámetro opcional del motor, RN-DI-04, US-010).
  - **PA-06**: primero se fija el profesional; luego el sistema asigna el primer sillón libre en ese rango. El paciente nunca elige sillón.
  - **PA-10**: el turno online nace `pendiente` y pasa a `confirmado` con el enlace del email (RN-TU-05, US-011).
- **Documentación alineada**: `knowledge-base/02`, `04`, `08`, `09`, `10` y `README.md`; las notas de stack y el campo "Bloqueado por" de las fichas de `CHANGES.md`; `docs/plan-trabajo-equipo.md` (corte y oleadas). `CLAUDE.md` se regenera con `agent-instruction` y `.atl/skill-registry.md` con `skill-registry`. `02` y `README.md` de la KB no estaban en el pedido original, pero también mencionan PostgreSQL y Neon.
- **BREAKING (reglas de agentes)**: se reemplazan las reglas duras de `CLAUDE.md` "NUNCA usar H2…" y "restricción `EXCLUDE USING gist`…" por su equivalente MySQL. Los agentes con contexto viejo deben releer `CLAUDE.md`.

## Capabilities

### New Capabilities
- Ninguna. C-01 es un change de decisión y documentación (`skip_specs: true`): no agrega comportamiento ejecutable.

### Modified Capabilities
- Ninguna (`openspec/specs/` está vacío). Las decisiones se especifican como requisitos en los changes que las implementan: anti-solapamiento con bloqueo (C-09), profesional opcional y asignación de sillón (C-10, C-16), estado inicial `pendiente` (C-09, C-17).

## Impact

- **Archivos**: `knowledge-base/01, 02, 04, 08, 09, 10, README.md`, `CHANGES.md`, `docs/plan-trabajo-equipo.md`, `CLAUDE.md`, `.atl/skill-registry.md`. No se toca código: todavía no existen `backend/` ni `frontend/`.
- **Changes posteriores**: C-02 (scaffolding MySQL + H2, sin `docker-compose.yml`), C-09 (bloqueo pesimista en lugar de la restricción de exclusión), C-10/C-16 (políticas de PA-05/06 ya decididas), C-14 (sin Mailpit en contenedor), C-19 (hosting MySQL, PA-14).
- **Riesgo de plazo**: el corte tiene 14 changes, con una cadena serial de 10 (C-01 → C-02 → C-03 → C-04 → C-08 → C-09 → C-10 → C-16 → C-17 → C-19), en menos de 48 h. Queda registrado como riesgo en `09` y `CHANGES.md`; si no alcanza, la prioridad es C-09 + C-10 (diferencial DD-02) con demo local.
- **Gobernanza**: MEDIO (documentación); el reemplazo de reglas duras de `CLAUDE.md` lo revisa Bruno en el PR.
