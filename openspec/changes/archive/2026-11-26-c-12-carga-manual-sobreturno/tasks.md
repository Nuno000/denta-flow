# Tasks — C-12 `carga-manual-sobreturno`

> **Gobernanza CRÍTICO:** no se escribe código sin la aprobación explícita de Bruno de estos artefactos. Rama: `feat/C-12-carga-manual-sobreturno`. Revisora del PR: Florencia.
>
> TDD estricto en cada tarea: **RED** (el test falla por la razón correcta), **GREEN** (código mínimo) y **REFACTOR** (con todo en verde). Nada requiere Docker ni Testcontainers. No se modifica `ServicioDeTurnos`, `ServicioDePacientes`, `MotorDeDisponibilidad` ni el esquema (sin migración Flyway: `es_sobreturno` ya existe en `V009_1`). Datos de prueba siempre ficticios.

## 1. Línea base

- [x] 1.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql` y, en `frontend/`, `npm test`. Anotar los totales (casos de los XML de surefire, que incluyen los `@Nested`) en `openspec/changes/c-12-carga-manual-sobreturno/evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo.

## 2. Caso de uso de carga manual (backend, aplicación)

- [x] 2.1 RED→GREEN `CargaManualDeTurnos.cargar`: registra al paciente con `registrarDesdeRecepcion` (sin consentimiento) **antes** de `ServicioDeTurnos.crear`, y arma la `SolicitudDeTurno` con origen `RECEPCION`, `esSobreturno=false` y `Actor.usuario(usuarioId)`. Verificar con `CargaManualDeTurnosTest` (Mockito, `InOrder`).
- [x] 2.2 RED→GREEN `CargaManualDeTurnos.cargarSobreturno`: igual que 2.1 con `esSobreturno=true`; ante `AgendaNoDisponibleException` la propaga sin consultar al motor. Verificar con `CargaManualDeTurnosTest`.
- [x] 2.3 RED→GREEN alternativas (design D3): con `Clock` fijo y `MotorDeDisponibilidad` simulado, ante `TurnoSolapadoException` o `AgendaNoDisponibleException` en la carga común lanza `ConflictoConAlternativasException` con el mensaje original y hasta 3 alternativas ordenadas por distancia al inicio pedido (desempate por la más temprana), prefiriendo el sillón pedido; ventana `ConsultaDeHuecos(prestacion, profesional, diaLocal, diaLocal+6)` con `min(7, diasMaximos)` días, incluido el caso `23:30-03:00`; lista vacía si el motor no devuelve huecos o falla. Verificar con `CargaManualDeTurnosTest` / `SugeridorDeAlternativasTest`.
- [x] 2.4 REFACTOR: Javadoc del contrato (sin transacción propia, paciente registrado aunque falle el turno) y sin datos personales en logs. Verificar `./mvnw -Dtest='CargaManual*,Sugeridor*' test` en verde.

## 3. Endpoints de carga y sobreturno (backend, presentación)

- [x] 3.1 RED→GREEN `POST /api/turnos` en `CargaManualController` con `@AdministradorORecepcion` y `SolicitudDeCargaManual` (records con Bean Validation, paciente anidado `@Valid`): 201 con `TurnoCargadoRespuesta` (`inicio`/`fin` con `-03:00`, `estado=pendiente`, `esSobreturno=false`), turno guardado con inicio UTC y creador correcto, una fila `creacion` en `turno_historial`; la respuesta no contiene token, DNI, teléfono ni email; un campo `esSobreturno: true` en el cuerpo se ignora. Verificar con `CargaManualControllerTest` (MockMvc, H2, perfil `test`).
- [x] 3.2 RED→GREEN conflicto rechazado (test de `CHANGES.md`): 409 por profesional ocupado, por sillón ocupado, fuera de horario y por bloqueo, sin turno nuevo ni historial, con `alternativas` en el `ProblemDetail`; un turno cancelado no bloquea (201). Implica el handler de `ConflictoConAlternativasException` en `ManejadorDeErrores` (design D4). Verificar con `CargaManualControllerTest`.
- [x] 3.3 RED→GREEN paciente por DNI: paciente nuevo creado con DNI normalizado; paciente existente reutilizado con su contacto reemplazado; email faltante → 400 con `errores.paciente.email` sin crear ni modificar paciente; conflicto después de un paciente nuevo → 409 y el paciente queda registrado. Verificar con `CargaManualControllerTest`.
- [x] 3.4 RED→GREEN validaciones: 400 por inicio sin desplazamiento, con segundos o en el pasado (`Clock` fijo) y por cuerpo vacío; 404 por prestación, profesional o sillón inexistente; ninguno trae `alternativas`. Verificar con `CargaManualControllerTest`.
- [x] 3.5 RED→GREEN `POST /api/turnos/sobreturnos` (test de `CHANGES.md`: sobreturno permitido solo con permiso y auditado): recepción y administrador cargan un sobreturno superpuesto → 201 con `esSobreturno=true`, y el historial registra `creacion` con el usuario, el instante y `"es_sobreturno":"true"`, sin datos personales ni token; sobreturno fuera de horario → 409 sin `alternativas`; un turno común posterior en ese horario sigue pudiendo crearse. Verificar con `CargaManualControllerTest`.
- [x] 3.6 RED→GREEN RBAC (test de `CHANGES.md`: profesional no puede cargar sobreturno): el rol `profesional` recibe 403 en `POST /api/turnos/sobreturnos` y en `POST /api/turnos`, sin crear paciente ni turno; sin token → 401 en ambos. Verificar con `CargaManualControllerTest`.
- [x] 3.7 REFACTOR: anotaciones OpenAPI (`@Tag("Turnos")`, `@Operation`, `@ApiResponse` 201, 400, 401, 403, 404, 409) y aserción de que `/v3/api-docs` lista ambas rutas y que ninguna ruta bajo `/api/publico/` crea sobreturnos. Verificar `./mvnw verify` en verde.

## 4. Búsqueda de paciente por DNI (backend, presentación)

- [x] 4.1 RED→GREEN `POST /api/pacientes/busqueda` en `PacienteRecepcionController` con `@AdministradorORecepcion`: 200 con `PacienteRespuesta` (DNI normalizado) y `Cache-Control: no-store`; 404 si no existe; 400 por DNI mal formado sin repetir el valor; 403 para `profesional`; 401 sin token. Verificar con `PacienteRecepcionControllerTest` (MockMvc, H2).
- [x] 4.2 REFACTOR: OpenAPI (`@Tag("Pacientes")`, respuestas 200, 400, 401, 403, 404) y que el DNI no aparece en los logs del test (captura de logs). Verificar `./mvnw verify` en verde.

## 5. Concurrencia contra MySQL 8 real

- [x] 5.1 RED→GREEN `CargaManualSimultaneaMySqlTest` (`@Tag("mysql")`, perfil `it-mysql`, `@RepeatedTest(5)`, dos hilos con `CountDownLatch`): dos `CargaManualDeTurnos.cargar` simultáneos del mismo profesional y horario en sillones distintos, con pacientes ficticios distintos → exactamente un éxito y un `ConflictoException`, un solo turno activo; limpia lo que crea. Justificación (design D7): el bloqueo ya lo cubre `CreacionSimultaneaTurnosMySqlTest`, pero se verifica el camino nuevo paciente-en-otra-transacción + turno. Verificar `./mvnw verify -Pit-mysql` en verde.

## 6. Pantalla "Nuevo turno" (frontend)

- [x] 6.1 RED→GREEN `inicioEnConsultorio(fecha, hora)` en `features/agenda/fechas.ts` → `2026-12-01T09:00:00-03:00`, independiente de la zona de la máquina. Verificar con `fechas.test.ts`.
- [x] 6.2 RED→GREEN hooks en `features/agenda/api.ts`: `useBuscarPacientePorDni` (`POST /api/pacientes/busqueda` con `{ dni }`), `useCargarTurno` (`POST /api/turnos`) y `useCargarSobreturno` (`POST /api/turnos/sobreturnos`), tipados sin `any`; los dos últimos invalidan `['agenda']`; tipo `Alternativa` leído del 409. Verificar con `api.test.tsx` (`simularApi`: método, ruta y cuerpo).
- [x] 6.3 RED→GREEN esquema Zod `esquemaCargaDeTurno` (DNI, teléfono, email, ids, fecha y hora obligatorios y con formato). Verificar con `esquemaCargaDeTurno.test.ts`.
- [x] 6.4 RED→GREEN `CargaDeTurnoPage` (React Hook Form + Zod, `Campo`): buscar DNI precarga los datos editables; DNI no registrado avisa "paciente nuevo"; profesionales filtrados por la prestación y solo recursos activos (`usePrestaciones`, `useProfesionales`, `useSillones`); campo obligatorio vacío marca y anuncia el error sin enviar; carga exitosa anuncia "Turno cargado" (`aria-live`) y ofrece volver a la agenda. Verificar con `CargaDeTurnoPage.test.tsx`.
- [x] 6.5 RED→GREEN errores en la pantalla: 400 con `errores` → error junto al campo (`paciente.email`); 409 con alternativas → motivo y botones en hora del consultorio, elegir uno cambia fecha, hora y sillón **sin** enviar pedido; 409 sin alternativas → aviso; 403 → "No tenés permiso para cargar turnos". Verificar con `CargaDeTurnoPage.test.tsx`.
- [x] 6.6 RED→GREEN sobreturno explícito: "Cargar sobreturno" valida el formulario y abre `Dialogo`; confirmar envía `POST /api/turnos/sobreturnos` y anuncia "Sobreturno cargado"; cancelar no envía nada. Verificar con `CargaDeTurnoPage.test.tsx`.
- [x] 6.7 RED→GREEN acceso por rol: ruta `/panel/agenda/nuevo-turno` con `RutaProtegida roles={['administrador','recepcion']}` en `routes/rutas.tsx`; `AgendaPage` muestra "Nuevo turno" solo a esos roles; el profesional ve el aviso de permiso en la ruta. Verificar con `rutas.test.tsx` y `AgendaPage.test.tsx`.
- [x] 6.8 REFACTOR y chequeo: sin `fetch` suelto ni `any`, etiquetas asociadas y uso con teclado. Correr `npm test`, `npm run lint` y `npm run build` en verde.

## 7. Verificación integrada y evidencia

- [x] 7.1 Prueba visual con Playwright MCP a 390x844 y 1440x900 con la API simulada: formulario vacío, paciente precargado, 409 con alternativas y diálogo de sobreturno. Guardar capturas en `openspec/changes/c-12-carga-manual-sobreturno/evidencia/capturas/`, borrar `.playwright-mcp/` y detener `vite`.
- [ ] 7.2 Punta a punta contra el backend real (perfil `test`, H2 en archivo en `target/`, usuario de recepción ficticio, sin `.env`): cargar un turno, provocar un conflicto y usar una alternativa, cargar un sobreturno y confirmar las filas en `turno` y `turno_historial`. Anotar en `evidencia/punta-a-punta.md` y limpiar procesos y archivos.
- [x] 7.3 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm test`, `npm run lint`, `npm run build` y `openspec validate c-12-carga-manual-sobreturno --strict`; anotar los totales frente a la línea base en `evidencia/verificacion.txt`.
- [x] 7.4 Actualizar `CHANGES.md` §C-12: estado, evidencia (`openspec/changes/c-12-carga-manual-sobreturno/evidencia/`) y corrección de "email opcional" a obligatorio según DD-12 (hallazgo H1 de `design.md`). Verificar con `git diff CHANGES.md`.

## Workflow follow-up

- Proponer el commit (Conventional Commits en castellano, sin `Co-Authored-By`) y esperar el pedido explícito antes de commitear o pushear.
- Abrir el PR a `main` con Florencia como revisora.
- Archivar con `/opsx:archive`, creando la spec `carga-manual-turnos`.
