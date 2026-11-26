---
description: "Archiva un change completado y sincroniza sus delta specs con las specs principales"
---

Cargá la skill `openspec-archive-change` con la herramienta `skill` y seguí sus instrucciones al pie de la letra.

Change a archivar: $ARGUMENTS

Reglas:

- Si `$ARGUMENTS` viene vacío, corré `openspec list --json` y preguntá cuál de los changes activos se archiva.
- Verificá antes que todas las tareas de `tasks.md` estén completas y que la evidencia esté registrada; si no, frená y mostrá qué falta.
- El archivado mueve el change a `openspec/changes/archive/YYYY-MM-DD-<name>/` y actualiza `openspec/specs/`.
