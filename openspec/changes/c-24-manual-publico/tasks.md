# Tasks — C-24 `manual-publico`

> **Gobernanza BAJO** (`CHANGES.md`): propuesta visual y estática sin cambios en backend. Depende formalmente del cierre del MVP (`C-19`) para la toma final de capturas con el seed de demo consolidado.
> Rama: `feat/C-24-manual-publico`. Responsable: Jessica. Revisora: Florencia.
>
> Metodología TDD estricta en cada tarea frontend: **RED** (escribir el test unitario/componente y comprobar el fallo), **GREEN** (código mínimo para ponerlo en verde) y **REFACTOR** (limpieza manteniendo la suite en verde).
> Verificación de accesibilidad (WCAG 2.1 AA) y mobile-first (390 px). Registro de evidencias en `openspec/changes/c-24-manual-publico/evidencia/`.

## 1. Línea base

- [ ] 1.1 Ejecutar `npm run lint`, `npm test` y `npm run build` en `frontend/` y registrar la salida en `evidencia/linea-base.txt`.

## 2. Enlaces y navegación hacia el manual (specs `manual-publico` y `autorizacion-por-rol`)

- [ ] 2.1 RED→GREEN `rutas.test.tsx`: verificar que la ruta `/manual` existe, es pública y renderiza el componente `ManualPage` sin requerir sesión ni token.
- [ ] 2.2 RED→GREEN `InicioPage.test.tsx`: verificar que la portada pública (`/`) muestra un enlace accesible "Manual de uso" hacia `/manual`.
- [ ] 2.3 RED→GREEN `BarraDelPanel.test.tsx`: verificar que la barra del panel (`BarraDelPanel`) muestra el enlace "Manual" hacia `/manual` para los tres roles internos (`administrador`, `recepcion`, `profesional`).

## 3. Componentes y contenidos del manual por actor (spec `manual-publico`)

- [ ] 3.1 RED→GREEN `ManualPage.test.tsx`: verificar que `ManualPage` presenta el título principal `<h1>DentaFlow — Manual de Uso</h1>`, introducción al sistema y un índice de navegación accesible (`<nav aria-label="Índice del manual">`) con anclas hacia `#paciente`, `#recepcion`, `#profesional` y `#administrador`.
- [ ] 3.2 RED→GREEN `ManualPaciente.test.tsx`: verificar que la sección `#paciente` describe paso a paso:
  - Reserva online sin registro (prestación, profesional, horario, datos y consentimiento).
  - Confirmación con un toque desde el correo de recordatorio.
  - Cancelación y reprogramación de turnos mediante enlace con token.
  - Aviso de privacidad e información de la Ley 25.326.
- [ ] 3.3 RED→GREEN `ManualRecepcion.test.tsx`: verificar que la sección `#recepcion` describe:
  - Navegación de agenda en vistas diaria y semanal con filtros interactivos por profesional y sillón.
  - Carga manual de turnos respetando la no superposición de horarios ni bloqueos.
  - Carga de sobreturnos explícitos debidamente justificados y auditados.
  - Marcación de estados de asistencia (`asistio`, `ausente`, `ausente_con_aviso`).
  - Envío de recordatorio prellenado por WhatsApp (`wa.me`).
- [ ] 3.4 RED→GREEN `ManualProfesional.test.tsx`: verificar que la sección `#profesional` describe:
  - Consulta de "Mi agenda del día" filtrada para el profesional autenticado.
  - Registro de asistencia de pacientes asignados.
- [ ] 3.5 RED→GREEN `ManualAdministrador.test.tsx`: verificar que la sección `#administrador` describe:
  - Gestión integral de recursos (profesionales, sillones y prestaciones con duración).
  - Configuración de horarios habituales y creación de bloqueos.
  - Gestión de usuarios y asignación de roles RBAC.
  - Consulta de auditoría e historial inmutable de turnos.
  - Parámetros del consultorio (anticipación mínima, aviso de privacidad, plantilla de WhatsApp).

## 4. Captura de pantallas y verificación de privacidad (Ley 25.326)

- [ ] 4.1 Generar las capturas de pantalla de los flujos de paciente y panel interno utilizando los datos de prueba del seed de desarrollo o entorno de demo tras C-19.
- [ ] 4.2 Optimizar las imágenes a formato WebP en `frontend/public/manual/` con nombres descriptivos y dimensiones adaptadas.
- [ ] 4.3 Auditoría de datos ficticios: certificar que ninguna captura contenga datos reales de pacientes, teléfonos auténticos, correos verdaderos ni información sensible, registrando la verificación en `evidencia/privacidad-datos-ficticios.md`.

## 5. Accesibilidad WCAG 2.1 AA y verificación Mobile-First (390 px)

- [ ] 5.1 Verificar atributos `alt` descriptivos en todas las etiquetas `<img>` o ilustraciones de la guía.
- [ ] 5.2 Verificar navegación por teclado (Tab, Shift+Tab, Enter en anclas de salto) y enfoque de foco visible (`focus-visible:ring-2`) en todos los elementos interactivos.
- [ ] 5.3 Validar la presentación responsive en viewport de 390 px × 844 px garantizando ausencia total de scroll horizontal (`overflow-x`), registrando la evidencia en `evidencia/accesibilidad.md`.

## 6. Verificación técnica y cierre

- [ ] 6.1 Ejecutar suite completa de tests de frontend: `npm run lint`, `npm test` y `npm run build` en verde.
- [ ] 6.2 Completar matriz de trazabilidad escenario → test en `evidencia/trazabilidad.md`.
- [ ] 6.3 Ejecutar `npx openspec validate c-24-manual-publico --strict` y verificar 100% de cumplimiento sin advertencias.
