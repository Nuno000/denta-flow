---
description: "Verifica que la implementación coincida con los artefactos del change (OpenSpec verify)"
---

Cargá la skill `openspec-verify-change` con la herramienta `skill` y seguí sus instrucciones al pie de la letra.

Change a verificar: $ARGUMENTS

Reglas:

- Si `$ARGUMENTS` viene vacío, corré `openspec list --json` y preguntá cuál de los changes activos se verifica.
- Contrastá specs, design y tasks del change contra el código real y los tests ejecutados; no des nada por cumplido sin evidencia.
- Reportá diferencias por severidad (bloqueante / mayor / menor) con archivo y línea. No archives el change en este paso.
