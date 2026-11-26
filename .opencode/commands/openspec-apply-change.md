---
description: "Implementa las tareas pendientes de un change de OpenSpec"
---

Cargá la skill `openspec-apply-change` con la herramienta `skill` y seguí sus instrucciones al pie de la letra.

Change a implementar: $ARGUMENTS

Reglas:

- Si `$ARGUMENTS` viene vacío, corré `openspec list --json` y preguntá cuál de los changes activos se implementa.
- Respetá el orden de `tasks.md`, marcá cada tarea al completarla y no cierres el change (eso es `openspec-archive-change`).
- Aplicá las reglas duras del `CLAUDE.md` del proyecto (TDD estricto, rama `feat/C-XX-...`, sin Docker, etc.).
