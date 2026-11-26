---
description: "Explora el problema y el codebase antes de proponer un change (OpenSpec explore)"
---

Cargá la skill `openspec-explore` con la herramienta `skill` y seguí sus instrucciones al pie de la letra.

Tema a explorar: $ARGUMENTS

Reglas:

- Este modo NO implementa: solo investiga, lee, piensa y (si el usuario lo confirma) captura el resultado como artefactos de un change.
- No inventes el estado de los artefactos: usá el CLI (`openspec list --json`, `openspec status --change "<name>" --json`).
- Si `$ARGUMENTS` viene vacío, pedí el tema antes de empezar.
