# Design system — DentaFlow

Mode: off
Approval: skipped

## Decisión

- **Política**: `off`. DentaFlow no tiene design system formal ni mockups aprobados por change.
- **Fuente**: decisión de Bruno al llegar al primer change visual (C-04, pantalla de login), en la sesión del 2026-11-25.
- **Motivo**: entrega del MVP el jueves 2026-11-26 a las 12:00 (DD-10); no hay tiempo para una foundation visual y su aprobación.

## Qué implica para cada change

- Las pantallas se implementan directamente con Tailwind CSS, mobile-first y WCAG 2.1 AA (reglas de `CLAUDE.md`).
- Los agentes no piden mockups ni un design system en cada propuesta; si un change tiene `design.md`, registra `Decision: no (project policy)` en `## UI Mockups`.
- No hay marca, logo ni tokens definidos: no se inventan como si fueran finales.

## Cómo cambiar la política

Solo con un pedido explícito del equipo: invocar la skill `design-system` y elegir `system-only` o `system-and-mockups`. Actualizar `Mode:` acá y `visual_design.mode` en `.active-orchestrator-state.json` (deben coincidir).
