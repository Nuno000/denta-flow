---
description: "Crea un change de OpenSpec con proposal.md, specs/, design.md y tasks.md en un paso"
---

Cargá la skill `openspec-propose` con la herramienta `skill` y seguí sus instrucciones al pie de la letra.

Change a proponer (nombre o descripción de lo que se quiere construir): $ARGUMENTS

Reglas:

- Frontera de planificación: este comando solo genera artefactos de planificación; no toca código de producción ni arranca el apply en la misma respuesta.
- Respetá `openspec/config.yaml` (contexto y reglas del proyecto) y la Knowledge Base en `knowledge-base/`.
- Si `$ARGUMENTS` viene vacío, pedí el alcance antes de crear el change.
