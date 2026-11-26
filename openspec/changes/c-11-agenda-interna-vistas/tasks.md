# Tasks — C-11 `agenda-interna-vistas`

> **Gobernanza MEDIO:** implementar con checkpoints y avisar las decisiones. Rama: `feat/C-11-agenda-interna-vistas`. Revisor del PR: Bruno.
>
> TDD estricto en cada tarea: **RED** (escribir el test y verlo fallar), **GREEN** (el código mínimo hasta verde) y **REFACTOR** (con los tests en verde). Nada requiere Docker ni Testcontainers.
>
> **Bloques:** los grupos 1 a 5 son solo frontend y se hacen **ya**, contra `simularApi`. El grupo 6 (backend) empieza **recién cuando C-09 esté mergeado en `main`**. No se toca código de C-09, C-06, C-15 ni C-19.

## 1. Línea base

- [x] 1.1 Correr `npm test`, `npm run lint` y `npm run build` en `frontend/`, y anotar "N tests en verde" en `openspec/changes/c-11-agenda-interna-vistas/evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa.

## 2. Fechas del consultorio (`features/agenda/fechas.ts`)

- [x] 2.1 RED→GREEN `hoyEnConsultorio(ahora)`: `2026-11-26T15:00:00Z` → "2026-11-26" y `2026-11-27T02:00:00Z` → "2026-11-26" (spec "Hoy en otra zona horaria"). Verificar con `fechas.test.ts`.
- [x] 2.2 RED→GREEN `sumarDias`, `lunesDeLaSemana` y `diasDeLaSemana`: el miércoles "2026-12-02" da el lunes "2026-11-30" y los 7 días hasta "2026-12-06"; el domingo pertenece a la semana que empezó el lunes anterior; el cruce de mes y de año funciona (31/12 → 1/1). Verificar con `fechas.test.ts`, también con `TZ` distinta de Argentina simulada vía `vi.setSystemTime`.
- [x] 2.3 RED→GREEN `horaEnConsultorio` (`2026-12-01T12:00:00Z` → "09:00"; `2026-12-01T02:30:00Z` → "23:30") y `fechaLarga` ("jueves 26 de noviembre de 2026"). Verificar con `fechas.test.ts`.
- [x] 2.4 RED→GREEN `rangoDeVista(vista, fecha)` y `normalizarParametros(searchParams, hoy)`, que tiran a valores por defecto una fecha mal formada, una vista desconocida o un id no numérico (design D5). Verificar con `fechas.test.ts` y `parametros.test.ts`.

## 3. Cliente de la agenda aislado (`features/agenda/api.ts`)

- [x] 3.1 Tipos `EstadoTurno`, `TurnoDeAgenda`, `FiltrosAgenda` y `RangoAgenda` según el contrato del design D1, y `RUTA_AGENDA`. Verificar con `npm run build` (tsc estricto, sin `any`).
- [x] 3.2 RED→GREEN `construirConsultaAgenda(rango, filtros)`: incluye `desde` y `hasta`, y omite los filtros vacíos o indefinidos. Verificar con `api.test.ts`.
- [x] 3.3 RED→GREEN `useTurnosDeAgenda`: con `simularApi` responde la lista; la URL llamada contiene `desde`, `hasta` y los filtros, leída de `fetchSimulado.mock.calls`; un 500 deja el hook en error. Verificar con `api.test.tsx`.
- [x] 3.4 Crear `frontend/src/test/datosAgenda.ts` con turnos **ficticios** (sin DNI ni contacto) en los seis estados, con un sobreturno y un turno de 23:30 local, más `apiDeAgenda(turnos, usuario)` que combina `auth/yo`, profesionales, sillones y la agenda. Verificar que lo usan los tests de 3.3.

## 4. Pantalla `/panel/agenda`

- [x] 4.1 RED→GREEN Ruta `/panel/agenda` dentro de `RutaProtegida` para los tres roles, y `AgendaPage` que abre en la vista diaria de hoy del consultorio, con la fecha larga como encabezado (spec "Abrir la agenda"). Verificar con `AgendaPage.test.tsx` (`vi.setSystemTime`) y que `rutas.test.tsx` siga en verde.
- [x] 4.2 RED→GREEN `VistaDiaria` y `TarjetaDeTurno`: turnos en orden con "09:00–09:30", paciente, prestación, profesional, sillón y estado; mensaje "No hay turnos para este día" (spec "Vista diaria"). Verificar con `VistaDiaria.test.tsx`.
- [x] 4.3 RED→GREEN `EtiquetaDeEstado`: los seis estados en texto castellano y la etiqueta "Sobreturno" (spec "Estado y sobreturno visibles sin depender del color"). Verificar con `EtiquetaDeEstado.test.tsx`.
- [x] 4.4 RED→GREEN `NavegacionDeFecha`: anterior, Hoy y siguiente actualizan la URL y piden el nuevo rango; Día/Semana con `aria-pressed`; región `aria-live` con "N turnos el <fecha>". Verificar con `AgendaPage.test.tsx`.
- [x] 4.5 RED→GREEN `VistaSemanal`: lunes a domingo, una sola llamada con `desde` el lunes y `hasta` el domingo, "Sin turnos" en los días vacíos y hoy marcado con `aria-current="date"` (spec "Vista semanal"). Verificar con `VistaSemanal.test.tsx`.
- [x] 4.6 RED→GREEN `FiltrosDeAgenda`: selectores rotulados de profesional, sillón y estado cargados desde `useProfesionales`/`useSillones`; cambian la URL y la consulta; "Limpiar filtros"; abrir con `?vista=semana&fecha=2026-12-02&sillonId=2` respeta la URL (spec "Filtros en la pantalla", "Agenda desde un enlace con filtros"). Verificar con `FiltrosDeAgenda.test.tsx`.
- [x] 4.7 RED→GREEN Rol `profesional`: título "Mi agenda", sin filtro de profesional, y la consulta no envía `profesionalId` (el backend lo fuerza). Verificar con `AgendaPage.test.tsx`.
- [x] 4.8 RED→GREEN Carga y errores: indicador de carga; 500 → "No se pudo cargar la agenda" con "Reintentar", que vuelve a llamar; 403 → aviso de falta de permiso (spec "Carga y errores en la pantalla"). Verificar con `AgendaPage.test.tsx`.
- [x] 4.9 REFACTOR: extraer lo repetido, revisar que no haya `fetch` suelto ni `any`, y correr `npm test`, `npm run lint` y `npm run build` en verde.

## 5. Menú del panel y verificación visual del frontend

- [x] 5.1 RED→GREEN Agregar "Agenda" a `SECCIONES` antes de "Recursos", para los tres roles, y actualizar los escenarios de `BarraDelPanel.test.tsx` según el delta de `autorizacion-por-rol` (incluido "Profesional en la agenda" con query string). Verificar con `BarraDelPanel.test.tsx`.
- [x] 5.2 Prueba visual con Playwright MCP a 390x844 y 1440x900 (vistas diaria y semanal, como recepción y como profesional, estado vacío y error), con la API simulada en el navegador por intercepción de red (`browser_run_code_unsafe` / `page.route`), sin datos falsos dentro de la app. Guardar las capturas en `evidencia/capturas/` y los hallazgos en `evidencia/prueba-visual.md`: sin scroll horizontal en 390 px y foco visible con teclado. Si el navegador no está disponible, dejar el checklist manual y anotarlo.
- [x] 5.3 Checkpoint con la usuaria: mostrar las capturas y decidir si el PR de la demo sale con el frontend solo o espera al grupo 6 (design, Risks). Resuelto: opción C (respaldo del frontend en c21a1b9 y backend en la misma rama).

## 6. Endpoint de lectura en el backend (recién después del merge de C-09)

- [x] 6.1 Actualizar la rama con `origin/main` que ya contenga C-09; leer su entidad `Turno`, el enum de estados y el repositorio, y anotar en `design.md` (D7) cualquier diferencia de nombres con el contrato. Verificar que `./mvnw verify` está en verde antes de tocar nada.
- [x] 6.2 RED→GREEN `ConsultaDeAgenda` (aplicación, Mockito): rango invertido o mayor a 7 días → `ValidacionDeDominioException`; profesional sin vínculo → 403; profesional con otro `profesionalId` → 403; profesional sin filtro → se fuerza el suyo; recepción sin filtros → todos; sin estado → excluye `cancelado`. Verificar con `ConsultaDeAgendaTest`.
- [x] 6.3 RED→GREEN Consulta de solo lectura en el repositorio de turnos (JPQL con `JOIN FETCH`, cruce `inicio < finRango AND fin > inicioRango`, orden por inicio, profesional y sillón), sin tocar el servicio de escritura ni sus bloqueos. Verificar con `@DataJpaTest` en H2 `MODE=MySQL`: borde de 23:30 local (02:30Z del día siguiente), turno que empieza a las 00:00 del día siguiente excluido, filtros combinados y orden.
- [x] 6.4 RED→GREEN `AgendaController` (`@PersonalInterno`) y record `TurnoDeAgendaRespuesta`: 200 con el JSON del contrato, 400 por fecha mal formada o estado desconocido, 401 sin token, 403 al profesional ajeno, y la respuesta no contiene `dni`, `telefono`, `email` ni `token`. Verificar con `AgendaControllerTest` (MockMvc).
- [x] 6.5 REFACTOR: anotaciones OpenAPI (`@Tag("Agenda")`, `@Operation`, `@ApiResponse` 200/400/401/403) y una aserción de que `/v3/api-docs` lista `/api/agenda/turnos`. Verificar `./mvnw verify` en verde.
- [x] 6.6 Correr `./mvnw verify -Pit-mysql` contra MySQL 8 local para confirmar la consulta en el motor real, y anotar el resultado.
- [x] 6.7 Prueba de punta a punta manual o con Playwright: backend local con seed y turnos creados por el servicio de C-09, y frontend contra el backend real, sin `simularApi`. Confirmar que la UI no necesitó cambios en `api.ts` (o anotar el ajuste). Guardar la captura en `evidencia/`.

## 7. Verificación final

- [x] 7.1 Correr `npm test`, `npm run lint`, `npm run build`, `./mvnw verify` y `./mvnw verify -Pit-mysql`, y `openspec validate c-11-agenda-interna-vistas --strict`; anotar los totales frente a la línea base en `evidencia/verificacion.txt`.

## Workflow follow-up

- Abrir el PR a `main` con Bruno como revisor, sin línea `Co-Authored-By`.
- Después del merge: `/opsx:archive`, marcar C-11 `[x]` en `CHANGES.md` con los totales de tests y la ruta de la evidencia.
