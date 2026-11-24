# Tasks — C-01 `decision-stack-y-plazo`

> Change de documentación (sin código). El "test que falla primero" es la búsqueda de términos del stack viejo: se registra en rojo al inicio y debe quedar en verde al final (grupo 6).
>
> Búsqueda de referencia (desde la raíz del repo):
> `grep -rniE "postgres|neon|testcontainers|docker|gist|tstzrange|MODE=PostgreSQL" knowledge-base CLAUDE.md .atl CHANGES.md docs/plan-trabajo-equipo.md`

## 1. Línea base

- [x] 1.1 Ejecutar la búsqueda de referencia y guardar la salida en `openspec/changes/c-01-decision-stack-y-plazo/evidencia/grep-antes.txt`; verificar que lista coincidencias en `02`, `08`, `09`, `10`, `README.md`, `CLAUDE.md`, `.atl/skill-registry.md` y `CHANGES.md` (la prueba arranca en rojo)

## 2. Decisiones y preguntas abiertas (`09`, `10`)

- [x] 2.1 Agregar **DD-09 — Stack MySQL 8 sin Docker** en `knowledge-base/09_decisiones_y_supuestos.md` con decisión, contexto (RAM local), alternativas (mantener PostgreSQL, PostgreSQL nativo + H2 `MODE=PostgreSQL`), justificación, trade-offs y el mecanismo D1–D3 y D7 de `design.md`. Marcar DD-08 "Reemplazada parcialmente por DD-09" sin borrar su texto. Verificar que DD-09 nombre el orden profesional → sillón y el perfil `it-mysql`
- [x] 2.2 En `09`, registrar el plazo (2026-11-26 12:00), el corte de entrega y el riesgo de plazo con plan de degradación (prioridad C-09 + C-10; demo local si no hay despliegue). Actualizar DD-05 quitando "Ver PA-03: sigue abierta". Verificar que no quede ninguna referencia a PA-03 como abierta
- [x] 2.3 En `knowledge-base/10_preguntas_abiertas.md`: mover PA-01, PA-03, PA-05, PA-06 y PA-10 a "Preguntas resueltas" con fecha 2026-11-24 y su resolución; marcar IN-01 resuelta (wa.me); actualizar IN-03 y la fila de PA-02 con DD-09; agregar PA-14 (hosting MySQL 8 gratuito, decide C-19, prioridad Media). Verificar que la tabla de abiertas ya no contenga esas cinco PA

## 3. Modelo, arquitectura y visión (`01`, `02`, `04`, `08`, `README`)

- [x] 3.1 En `knowledge-base/04_modelo_de_datos.md` (Turno): reemplazar la garantía genérica por "bloqueo pesimista profesional → sillón dentro de la transacción (DD-09)"; índices `(profesional_id, estado, inicio, fin)` y `(sillon_id, estado, inicio, fin)`; estado inicial online `pendiente` (PA-10); profesional opcional en la reserva y sillón asignado por sistema (PA-05/06). Verificar que la sección Turno cite DD-09 y PA-10
- [x] 3.2 En `knowledge-base/08_arquitectura_propuesta.md`: actualizar el encabezado del stack; reemplazar la fila del patrón de exclusión por bloqueo pesimista + orden + lectura del último estado confirmado (D1–D3) + intentos sucesivos (D4); quitar `docker-compose.yml` del árbol; tests "JUnit 5, Mockito, H2 (`test`) / MySQL (`it-mysql`)"; URL de ejemplo `jdbc:mysql://localhost:3306/dentaflow`; agregar la convención de migraciones `V0NN_<n>__desc.sql`. Verificar con la búsqueda de referencia acotada a `08` (0 coincidencias)
- [x] 3.3 Actualizar la tabla de stack de `knowledge-base/02_descripcion_general.md` y el resumen de `knowledge-base/README.md` (stack DD-09, plazo resuelto, PA que siguen abiertas). Verificar con la búsqueda acotada a esos dos archivos (0 coincidencias)
- [x] 3.4 Agregar en `knowledge-base/01_vision_y_objetivos.md` la sección "Plazo y corte de entrega" (fecha, changes dentro y fuera del corte, criterio de prioridad). Verificar que la lista coincida con `proposal.md`

## 4. Roadmap y plan del equipo (`CHANGES.md`, `docs/plan-trabajo-equipo.md`)

- [x] 4.1 En `CHANGES.md`: reemplazar el aviso "Cambio de stack en curso" por el stack DD-09 vigente y el plazo; agregar la columna `Corte 26/11` (sí/no) a la tabla de changes; anotar en C-19 que, durante el corte, depende solo de los changes incluidos. Verificar que los 14 changes del corte estén marcados "sí"
- [x] 4.2 En las fichas de `CHANGES.md`, actualizar las notas de stack de C-02 (sin `docker-compose.yml`; H2 + `it-mysql`; job de MySQL en CI), C-09 (bloqueo pesimista en lugar de `EXCLUDE USING gist`; tests en `it-mysql`), C-10 (red de seguridad = bloqueo de C-09), C-14 (sin Mailpit en contenedor) y C-19 (hosting MySQL, PA-14). Quitar PA-03/05/06/10 de "Bloqueado por" en C-09, C-10, C-16 y C-21, indicando la resolución. Verificar con la búsqueda de referencia acotada a `CHANGES.md` (0 coincidencias)
- [x] 4.3 En `docs/plan-trabajo-equipo.md`: quitar el aviso de "KB todavía describe PostgreSQL"; agregar el plazo y marcar en la matriz de oleadas qué changes quedan fuera del corte. Verificar que la matriz y `CHANGES.md` coincidan change por change

## 5. Instrucciones de agentes (`CLAUDE.md`, `.atl/skill-registry.md`)

- [x] 5.1 Regenerar `CLAUDE.md` con la skill `agent-instruction`: stack DD-09; skills de datos sin `postgresql-*` y sin skills de infra Docker; reglas duras con el reemplazo de D8 (anti-solapamiento con bloqueo + `it-mysql`; nunca Docker ni Testcontainers; H2 `MODE=MySQL` permitido); ramas `feat/C-XX-nombre-corto`; primer change elegible C-02. Verificar con la búsqueda acotada a `CLAUDE.md` (0 coincidencias) y que las demás reglas duras sigan presentes
- [x] 5.2 Regenerar `.atl/skill-registry.md` con la skill `skill-registry` y reaplicar el formato en español: stack MySQL 8 + H2 `MODE=MySQL`; skills de PostgreSQL y Docker como "NO RELEVANTE"; reglas compactas de datos y tests con MySQL / `it-mysql`. Verificar con la búsqueda acotada a `.atl/` (solo coincidencias en filas "NO RELEVANTE")

## 6. Verificación integral

- [x] 6.1 Ejecutar de nuevo la búsqueda de referencia y guardar la salida en `evidencia/grep-despues.txt`; verificar que solo queden coincidencias en el texto histórico de DD-08, en la nota de reemplazo de DD-09 y en las filas "NO RELEVANTE" del registro de skills
- [x] 6.2 Verificar a mano que los cuatro documentos de referencia (`openspec/config.yaml`, `docs/plan-trabajo-equipo.md`, `CLAUDE.md`, `knowledge-base/08`) describan igual el orden de bloqueo, el perfil `it-mysql` y la convención de migraciones
- [x] 6.3 Marcar C-01 `[x]` en la ficha y en la fila de `CHANGES.md`, y actualizar "Primer item elegible" a C-02. Verificar que el resto de las filas no cambió

## Workflow follow-up

- Commit en `feat/C-01-decision-stack-y-plazo` (Conventional Commits en castellano), push y PR a `main`; lo aprueba Bruno (PR de Florencia).
- Con el PR aprobado: `/openspec-archive-change c-01-decision-stack-y-plazo`, squash merge y avisar al equipo que C-02 queda desbloqueado.
