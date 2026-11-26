# Tasks — C-05 `gestion-recursos-admin`

> **Gobernanza BAJO:** autonomía con tests en verde. Rama: `feat/C-05-gestion-recursos-admin`. Revisor del PR: Bruno.
>
> TDD estricto en cada tarea: **RED** (escribir el test y verlo fallar), **GREEN** (código mínimo hasta verde) y **REFACTOR** (con los tests en verde después de cada paso). Nada requiere Docker ni Testcontainers.
> - Backend, desde `backend/`: `./mvnw verify` (H2 `MODE=MySQL`) y `./mvnw verify -Pit-mysql` (MySQL 8 local).
> - Frontend, desde `frontend/`: `npm test`, `npm run lint` y `npm run build`.
>
> El orden deja la API terminada y verificada antes de la UI (design, Risks: plazo).

## 1. Línea base

- [x] 1.1 Correr `./mvnw verify` en `backend/` y `npm test` en `frontend/` antes de tocar código, y anotar "N tests en verde" de cada uno en `openspec/changes/c-05-gestion-recursos-admin/evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa

## 2. Dominio: edición (`dominio/recursos`; spec "Edición de recursos")

- [x] 2.1 RED: en `ProfesionalTest`, `actualizarDatos` cambia nombre, apellido, matrícula y email (este último a minúsculas). Con matrícula vacía la deja nula. Con un dato inválido (apellido en blanco, email mal formado, nombre de 101 caracteres) lanza `ValidacionDeDominioException` y **no cambia ningún campo**. Conserva `activo` y `prestacionIds`. GREEN: `Profesional.actualizarDatos` (design D2). Verificar que pasa
- [x] 2.2 RED: en `PrestacionTest`, `renombrar` y `actualizar(nombre, duracion)` aplican los cambios. Una duración de 0 con un nombre válido no cambia el nombre (todo o nada). Un nombre en blanco se rechaza. GREEN: implementar. Verificar que pasa y que `DominioSinFrameworksTest` sigue en verde

## 3. Servicios de aplicación (`aplicacion/recursos`; spec "Alta", "Edición", "Duplicados", "Desactivación", "Asignación")

- [x] 3.1 RED: `ServicioDeSillonesTest` (Mockito, repositorio mockeado):
  - `crear` guarda un sillón activo.
  - `crear("sillón 1 ")` con "Sillón 1" existente lanza `ConflictoException` sin guardar.
  - `renombrar` excluye el propio id al comparar.
  - Un id inexistente da `RecursoNoEncontradoException`.
  - `desactivar` y `activar` son idempotentes.

  GREEN: `ServicioDeSillones` (design D1 y D4). Verificar que pasa
- [x] 3.2 RED: `ServicioDePrestacionesTest`, con los mismos casos que 3.1 más la edición de nombre y duración (conservar el propio nombre al cambiar solo la duración se acepta). GREEN: `ServicioDePrestaciones`. Verificar que pasa
- [x] 3.3 RED: `ServicioDeProfesionalesTest` (alta y edición):
  - Un email repetido de otro profesional da conflicto.
  - Una matrícula repetida da conflicto.
  - Dos profesionales sin matrícula se aceptan.
  - Editar el propio email sin cambiarlo se acepta.

  GREEN: alta, edición, obtener, listar, desactivar y activar en `ServicioDeProfesionales`. Verificar que pasa
- [x] 3.4 RED: `ServicioDeProfesionalesTest` (asignación, design D5):
  - Reemplazar {Limpieza} por {Consulta, Extracción}.
  - Agregar una inactiva da 400 y no cambia nada.
  - Conservar una inactiva ya asociada se acepta.
  - Un id inexistente da 400 con el id en el mensaje y no cambia nada.
  - Un conjunto vacío deja al profesional sin prestaciones.
  - Los ids repetidos se asocian una sola vez.

  GREEN: `asignarPrestaciones`. Verificar que pasa
- [x] 3.5 RED: `ServiciosDeRecursosIntegracionTest` (`@SpringBootTest`, perfil `test`, `@Transactional`):
  - Alta, edición y relectura contra H2 `MODE=MySQL`.
  - Después de asignar un conjunto vacío, `listarActivosQueRealizan` ya no devuelve al profesional.
  - Un duplicado que se saltea la verificación previa (insertando antes por `JdbcTemplate`) se traduce a `ConflictoException`, no a `DataIntegrityViolationException`.

  GREEN: el respaldo de D4 (`flush` y la traducción por nombre de constraint). Verificar `./mvnw verify` en verde

## 4. API REST y permisos (`presentacion/recursos`; spec "Consulta…", "Modificación solo por el administrador", "Alta", "Edición", "Duplicados", "Desactivación", "Asignación")

- [x] 4.1 RED: `SillonesControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`, perfil `test`, tokens como en `UsuariosControllerTest`):
  - Administrador: `POST` → 201 con `Location`; `PUT` → 200; `desactivar` dos veces → 200; `GET /{id}` del inactivo → 200 con `activo=false`.
  - Nombre en blanco → 400 con `errores.nombre`.
  - Duplicado → 409 `application/problem+json`.
  - Id inexistente → 404.
  - `DELETE` → 405 o 404 sin borrar.

  GREEN: `SillonesController` y los DTO `SillonSolicitud` y `SillonRespuesta` (design D3). Verificar que pasa
- [x] 4.2 RED: permisos de sillones:
  - `recepcion` y `profesional` con `GET` → 200.
  - `recepcion` con `POST` válido → 403 y no se crea.
  - `profesional` con `PUT` → 403 y no cambia.
  - Sin token → 401 con `WWW-Authenticate: Bearer`.

  GREEN: guardas `@PersonalInterno` y `@SoloAdministrador`. Verificar que pasa y que `GuardasEnControladoresTest` sigue en verde sin excepciones nuevas
- [x] 4.3 RED y GREEN: `PrestacionesControllerTest`, con los casos de 4.1 y 4.2 más duración `0`, `-15` y `null` → 400 con `errores.duracionMinutos`, y `"veinte"` → 400 en formato de problema (cuerpo con tipo incorrecto, sin error por campo). Implementar `PrestacionesController` y sus DTO. Verificar que pasa
- [x] 4.4 RED y GREEN: `ProfesionalesControllerTest`, con los casos de 4.1 y 4.2 adaptados (email mal formado → 400, email o matrícula repetidos → 409) más `PUT /{id}/prestaciones`:
  - Ids válidos → 200 con `prestacionIds` ordenados.
  - Una inactiva nueva → 400.
  - Un id inexistente → 400.
  - Profesional inexistente → 404.
  - `recepcion` → 403.

  Implementar `ProfesionalesController` y sus DTO. Verificar que pasa
- [x] 4.5 RED: `RespaldoDuplicadosRecursosMySqlTest` (`@Tag("mysql")`, perfil `it-mysql`, mismos casos que la base compartida `RespaldoDuplicadosRecursosBase` de 3.5): un duplicado de sillón (alta y edición), de prestación, de email y de matrícula que llega a la constraint se traduce a `ConflictoException` en MySQL 8. GREEN: ajustar el respaldo de D4 solo si falla. Verificar `./mvnw verify -Pit-mysql` en verde
- [x] 4.6 REFACTOR: anotaciones OpenAPI (`@Tag` "Profesionales", "Sillones" y "Prestaciones"; `@Operation` y `@ApiResponse` 200/201/400/403/404/409) y ayudantes de test compartidos. Verificar `./mvnw verify` en verde y que `/v3/api-docs` lista las rutas nuevas (aserción en uno de los tests de controller)

## 5. Frontend: componentes compartidos y cliente de API (design D6)

- [x] 5.1 Refactor sin cambio de comportamiento: mover `Campo`, `Dialogo` y `estilosFormulario` a `frontend/src/components/formulario/`, y `leerErrorDeApi` a `frontend/src/lib/erroresApi.ts`. Actualizar los imports de `features/usuarios/`. Verificar que `npm test`, `npm run lint` y `npm run build` siguen en verde sin modificar las aserciones de los tests de usuarios
- [x] 5.2 RED: `features/recursos/api.test.tsx` (con `simularApi`):
  - `useSillones` y `usePrestaciones` piden `GET /api/sillones` y `GET /api/prestaciones`.
  - `useCrearSillon` hace `POST` e invalida el listado.
  - `useAsignarPrestaciones` hace `PUT /api/profesionales/{id}/prestaciones` e invalida profesionales.
  - Desactivar una prestación invalida prestaciones y profesionales.

  GREEN: `features/recursos/api.ts` con tipos estrictos y sin `any`. Verificar que pasa
- [x] 5.3 RED: `features/recursos/esquemas.test.ts`:
  - Duración `0`, `-1` y `1.5` rechazadas con "La duración debe ser mayor que cero".
  - Nombre de sillón de 51 caracteres rechazado.
  - Email mal formado rechazado.
  - Matrícula vacía aceptada como `null`.

  GREEN: `esquemas.ts` con Zod. Verificar que pasa

## 6. Frontend: pantallas (spec "Sección Recursos del panel", "Acciones… solo para el administrador", "Formularios…", "Desactivación con confirmación", "Asignación de prestaciones desde el panel"; delta "Menú del panel según el rol")

- [x] 6.1 RED: en `rutas.test.tsx` y `BarraDelPanel.test.tsx`:
  - `/panel/recursos` redirige a `/panel/recursos/profesionales`.
  - Los tres roles internos entran.
  - Sin sesión, va al ingreso.
  - Menú: el administrador en usuarios ve "Panel", "Recursos" y "Usuarios", con "Usuarios" actual.
  - Recepción en `/panel` ve "Panel" y "Recursos", con "Panel" actual.
  - Profesional en `/panel/recursos/sillones` tiene "Recursos" con `aria-current="page"`.

  GREEN: rutas, `RecursosPage` con la navegación de pestañas y la entrada "Recursos" (sin `end`) en `SECCIONES`. Verificar que pasa
- [x] 6.2 RED: `SillonesTab.test.tsx`:
  - Listado con nombre y estado.
  - Recepción no ve "Nuevo sillón" ni las acciones de fila.
  - El administrador da de alta, la API responde 201, se cierra el formulario, se anuncia "Sillón creado" y el sillón aparece.
  - Un 409 deja el formulario abierto con el mensaje de la API.
  - Desactivar pide confirmación y, si se cancela, no llama a la API.
  - Reactivar se hace sin confirmación.
  - Error de carga → aviso.

  GREEN: `SillonesTab`, `FormularioSillon` y `ConfirmarDesactivacion`. Verificar que pasa
- [x] 6.3 RED y GREEN: `PrestacionesTab.test.tsx`, con los casos de 6.2 más la duración mostrada como "40 min", y duración 0 que muestra el error junto al campo sin llamar a la API. Implementar `PrestacionesTab` y `FormularioPrestacion`. Verificar que pasa
- [x] 6.4 RED y GREEN: `ProfesionalesTab.test.tsx`:
  - Listado con nombre completo, email, matrícula, estado y los nombres de sus prestaciones.
  - Alta y edición con matrícula opcional.
  - 409 por email repetido.
  - `DialogoPrestaciones` ofrece solo las activas más la inactiva ya asignada rotulada "(inactiva)".
  - Guardar envía el conjunto y el listado se actualiza.
  - Recepción no ve las acciones.

  Implementar `ProfesionalesTab`, `FormularioProfesional` y `DialogoPrestaciones`. Verificar que pasa
- [x] 6.5 REFACTOR y accesibilidad: tablas con `aria-label` y `th scope="col"`, botones con nombre accesible por fila ("Editar Sillón 1"), región `role="status"` y sin `any`. Verificar `npm test`, `npm run lint` y `npm run build` en verde

## 7. Verificación integral y evidencia

- [x] 7.1 Prueba manual con el backend en perfil `dev` (MySQL local, seed habilitado) y `npm run dev`:
  - Como administrador: alta, edición, desactivación y reactivación de un recurso de cada tipo, y asignación de prestaciones.
  - Como recepción: solo lectura.
  - En celular (390 px) y escritorio (1440 px).

  Guardar las capturas y notas en `openspec/changes/c-05-gestion-recursos-admin/evidencia/`. Verificar que no hay errores en la consola del navegador
- [x] 7.2 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm test`, `npm run lint` y `npm run build`, y anotar los totales ("H2 N/N, MySQL N/N, frontend N") en `evidencia/verificacion.txt`. Verificar que todo está en verde y que `openspec validate c-05-gestion-recursos-admin --strict` pasa

## Workflow follow-up

- Proponer el commit (Conventional Commits en castellano, sin `Co-Authored-By`) y esperar la aprobación antes de commitear o pushear.
- Abrir el PR a `main` con Bruno como revisor (`docs/plan-trabajo-equipo.md`).
- Después del merge: `/opsx:archive`, marcar C-05 `[x]` en `CHANGES.md` con los totales de tests y la ruta de la evidencia.
