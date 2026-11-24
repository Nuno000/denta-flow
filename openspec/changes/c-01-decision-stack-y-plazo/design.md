# Design — C-01 `decision-stack-y-plazo`

## Context

Ver `proposal.md` (Why). Estado actual:

- No existe código (`backend/` y `frontend/` los crea C-02), así que el cambio de motor no requiere migrar datos. Solo hay que corregir la documentación que guía a personas y agentes.
- Documentos con el stack viejo: `knowledge-base/02` (tabla de stack), `08` (patrones, árbol con `docker-compose.yml`, variables con URL `jdbc:postgresql`), `09` (DD-08), `10` (IN-03 y PA-02 citan PostgreSQL), `README.md` de la KB, `CLAUDE.md` (stack, skills de datos e infra, dos reglas duras) y `.atl/skill-registry.md` (H2 `MODE=PostgreSQL`, `btree_gist`, Neon). `04` no nombra el motor, pero sus constraints de Turno se apoyan en la restricción de exclusión.
- `openspec/config.yaml` y `docs/plan-trabajo-equipo.md` ya describen el stack nuevo: son la referencia.

## Goals / Non-Goals

**Goals:**
- Que todo documento que lea un agente describa un único stack: MySQL 8 + H2 `MODE=MySQL`, sin Docker.
- Dejar especificado el mecanismo anti-solapamiento con el detalle suficiente para que C-09 lo implemente sin volver a decidirlo.
- Registrar el plazo, el recorte y las PA resueltas de forma que `CHANGES.md` muestre qué entra en la entrega del 2026-11-26.

**Non-Goals:**
- Escribir DDL, entidades o configuración de Spring (eso es C-02 y C-09).
- Elegir el hosting de MySQL para producción (PA-14, se decide en C-19).
- Reasignar responsables: el reparto de `docs/plan-trabajo-equipo.md` se mantiene; solo se marca el corte.

## Decisions

### D1. Bloquear las filas de los recursos, no rangos de la tabla `turno`

La transacción que crea o mueve un turno hace `SELECT ... FOR UPDATE` sobre la fila del `profesional` y después sobre la del `sillon` (por PK). Con los dos bloqueos tomados, verifica el solapamiento y recién entonces inserta.

- **Por qué**: serializa a quienes compiten por el mismo recurso, con bloqueos por PK que son predecibles en InnoDB.
- **Alternativa descartada**: `SELECT ... FOR UPDATE` sobre el rango de turnos solapados. Depende de los gap/next-key locks de InnoDB, que solo funcionan en `REPEATABLE READ`, bloquean de más según el índice usado y no los reproduce H2.
- **Alternativa descartada**: bloqueo optimista con `@Version` en el recurso. Obliga a reintentar en cada colisión y no es más simple de probar.
- **Alternativa descartada**: tabla de "slots" con `UNIQUE` por recurso y franja. Fuerza una granularidad fija que choca con duraciones variables por prestación (RN-TU-03).

### D2. Orden de bloqueo global: profesional, después sillón

Siempre primero el profesional y después el sillón. Si una operación necesita varios del mismo tipo, van en orden ascendente de id. Un orden total evita interbloqueos (deadlocks) entre reservas que compiten por los mismos recursos.

### D3. La verificación de solapamiento lee el último estado confirmado

En InnoDB con `REPEATABLE READ`, una lectura no bloqueante puede usar una foto tomada antes de obtener el bloqueo y no ver un turno recién confirmado por otra transacción. Para evitarlo:

- el método transaccional de creación o cambio de turno usa `Isolation.READ_COMMITTED`, **o**
- la consulta de solapamiento es una lectura bloqueante (`FOR SHARE`).

C-09 elige una de las dos y la prueba en `it-mysql`. Así queda registrado en `08` (Patrones).

### D4. "Cualquier profesional" se resuelve con intentos sucesivos, no con bloqueos múltiples

Para PA-05 con "primero disponible" y PA-06 (sillón por sistema), el motor (C-10) arma la lista de candidatos (profesional, sillón) ordenada. La creación (C-09) intenta un candidato por transacción. Si al verificar bajo bloqueo hay conflicto, sigue con el siguiente; si se agotan, informa "hueco tomado" (Flujo 1). Así ninguna transacción retiene bloqueos de recursos que no usa.

### D5. Sobreturno

El turno con `es_sobreturno = true` toma los mismos bloqueos (orden D2), pero se excluye de la verificación de solapamiento y sus propios rangos tampoco cuentan como ocupación para los sobreturnos (RN-TU-07, RN-SO-01). Lo implementa C-12; C-09 deja la bandera y el filtro.

### D6. Tipos e índices compatibles con MySQL 8 y H2 `MODE=MySQL`

- PK `BIGINT AUTO_INCREMENT`; tiempos en `DATETIME(6)` guardados en UTC (`hibernate.jdbc.time_zone=UTC`), mapeados a `Instant`; la zona `America/Argentina/Buenos_Aires` se aplica solo al calcular horarios y mostrar datos (SU-09).
- Estados como `VARCHAR` + `CHECK` (MySQL ≥ 8.0.16 los aplica) en lugar de `ENUM`.
- Índices `(profesional_id, estado, inicio, fin)` y `(sillon_id, estado, inicio, fin)` sobre `turno`.
- Una sola ubicación de migraciones (`db/migration`) con SQL válido en ambos motores; versión `V0NN_<n>__desc.sql` por change.

### D7. Dos niveles de prueba sin Docker

- Perfil `test`: H2 en memoria, para unitarios, slices y la mayoría de la integración.
- Perfil `it-mysql` (tests marcados `@Tag("mysql")`, excluidos del build por defecto): MySQL 8 nativo local, para concurrencia y bloqueos. En GitHub Actions corre contra el MySQL que ya viene preinstalado en los runners `ubuntu-latest` (se inicia el servicio, sin contenedores). C-02 deja el job armado.

### D8. Cómo se actualiza cada documento

- `09`: se agrega **DD-09**. DD-08 queda con la nota "Reemplazada parcialmente por DD-09 (base de datos, tests, hosting de la base, desarrollo local)"; frontend, backend y email siguen vigentes. También se registra el riesgo de plazo.
- `10`: PA-01, PA-03, PA-05, PA-06 y PA-10 pasan a "Preguntas resueltas" con fecha 2026-11-24; IN-01 y IN-03 se marcan resueltas o actualizadas; se agrega PA-14 (hosting MySQL, decide C-19).
- `04`: las constraints de Turno pasan a "garantía por bloqueo pesimista (DD-09)"; se suman los índices de D6 y las reglas de asignación de PA-05/06 y el estado inicial de PA-10.
- `08`: se reemplaza la fila del patrón de exclusión por D1–D3; se quita `docker-compose.yml` del árbol; la URL de ejemplo pasa a `jdbc:mysql://localhost:3306/dentaflow`; el árbol de tests pasa a "JUnit 5, Mockito, H2 / it-mysql".
- `01`: se agrega la sección "Plazo y corte de entrega".
- `02`, `README.md` de la KB: tabla de stack y resumen.
- `CHANGES.md`: encabezado, marca de corte en la tabla, notas de stack de C-02, C-09, C-10, C-14 y C-19, y "Bloqueado por" de C-09, C-10 y C-16 sin las PA ya resueltas.
- `CLAUDE.md`: se regenera con `agent-instruction`. Las reglas "NUNCA confiar solo en el código… `EXCLUDE USING gist`" y "NUNCA usar H2…" se reemplazan por:
  - anti-solapamiento con bloqueo pesimista profesional → sillón + test de concurrencia en `it-mysql`;
  - nunca Docker ni Testcontainers; persistencia probada con H2 `MODE=MySQL` y la concurrencia también en MySQL real.
  - También cambia la convención de ramas a `feat/C-XX-nombre-corto`.
- `.atl/skill-registry.md`: se regenera con `skill-registry`, conservando el formato en español. Las skills de PostgreSQL pasan a "NO RELEVANTE" y el patrón de datos describe MySQL + H2.

## Risks / Trade-offs

- [Sin garantía declarativa en la base: si un código futuro inserta turnos sin pasar por el servicio de C-09, puede haber solapamientos] → Una única vía de escritura de turnos (servicio de C-09) + revisión en PR + test de concurrencia obligatorio en `it-mysql` para cada change que cree o mueva turnos (C-12, C-16, C-17, C-20).
- [H2 da falsos verdes en concurrencia] → Concurrencia solo se acepta como verificada en `it-mysql` (local o CI).
- [Bloqueo por recurso serializa reservas del mismo profesional] → Aceptable: consultorio de 1 a 5 profesionales, transacciones cortas.
- [Plazo de < 48 h para 14 changes con 10 en serie] → El corte prioriza C-09 + C-10 (diferencial DD-02); se registra en `09` como riesgo con plan de degradación: demo local si no hay despliegue.
- [Agentes con contexto viejo siguen aplicando las reglas PostgreSQL] → `CLAUDE.md` regenerado en el mismo PR; aviso al equipo al mergear (paso 8 del protocolo).

## Migration Plan

1. Aplicar las actualizaciones de documentos en la rama `feat/C-01-decision-stack-y-plazo`.
2. Verificar con una búsqueda de `PostgreSQL|Postgres|Neon|Testcontainers|docker|gist|tstzrange|MODE=PostgreSQL` en KB, `CLAUDE.md`, `.atl/` y `CHANGES.md`. Solo debe quedar en el texto histórico de DD-08 y en el registro del reemplazo.
3. PR → aprobación de Bruno → archivar → squash merge. Avisar al equipo que C-02 queda desbloqueado.
4. Rollback: revertir el commit de merge. No hay datos ni código afectados.

## Open Questions

- PA-14: proveedor gratuito de MySQL 8 para producción (se decide en C-19; no afecta C-02 a C-17).
