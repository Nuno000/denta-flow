# Tasks — C-16 `reserva-online-publica`

> **Gobernanza ALTA:** no empezar hasta que Florencia apruebe `design.md` (Q1..Q6).
>
> TDD estricto en cada tarea: **RED** (test que falla y se ve fallar), **GREEN** (código mínimo), **REFACTOR** (con tests en verde). Nada requiere Docker ni Testcontainers. Backend desde `backend/` con `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local); frontend desde `frontend/` con `npm run lint`, `npm test` y `npm run build`. Evidencia en `openspec/changes/c-16-reserva-online-publica/evidencia/`.
>
> **Bloque A** se puede hacer ya (C-14 y C-15 en `main`). **Bloque B** espera a C-10 en `main` y el rebase de la rama.
>
> Rama: `feat/C-16-reserva-online-publica`. Commits en Conventional Commits en castellano, sin `Co-Authored-By`.

## 1. Línea base

- [ ] 1.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm run lint`, `npm test` y `npm run build` antes de tocar código y anotar los resultados en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa

## 2. Bloque A — Contacto del consultorio (spec `parametros-consultorio`, "Contacto del consultorio público")

- [ ] 2.1 RED: `CatalogoDeParametrosConsistenciaTest` y "Base recién migrada" esperan cuatro claves y cuatro filas provisorias; `ParametrosControllerTest`: `contacto-consultorio` vacío → 400 sin cambiar el valor, válido → 200 y deja de ser provisorio, 301 caracteres → 400. GREEN: `ClaveParametro.CONTACTO_CONSULTORIO` (texto, máx. 300; commit aparte, archivo compartido de C-07), `V016_1__parametro_contacto_consultorio.sql` y `db/migration/README.md`
- [ ] 2.2 RED: `ContactoPublicoControllerTest`: sin token, `GET /api/publico/contacto` → 200 con exactamente `contacto` y `Cache-Control: no-store`; tras editarlo el administrador, devuelve el nuevo. GREEN: `ContactoPublicoController` + `ContactoRespuesta` sobre la lectura tipada de C-07

## 3. Bloque A — Catálogo público (spec "Catálogo público para reservar")

- [ ] 3.1 RED: `CatalogoPublicoControllerTest`: `GET /api/publico/prestaciones` sin token → solo activas con id, nombre y duración, ordenadas por nombre; `GET /api/publico/profesionales?prestacionId=` → solo activos que la realizan, con id, nombre y apellido (sin email ni matrícula); prestación inexistente o inactiva → 404; sin `prestacionId` → 400. GREEN: `CatalogoPublicoController`, `PrestacionPublica`, `ProfesionalPublico`
- [ ] 3.2 Confirmar que `GuardasEnControladoresTest` sigue en verde (exime `presentacion.publico`) y que OpenAPI lista los endpoints bajo "Reserva pública"

## 4. Bloque A — Límite anti-abuso (spec "Límite de reservas por origen")

- [ ] 4.1 RED: `LimitadorDeReservasPorOrigenTest` (reloj fijo): 5 pedidos de la misma IP pasan, el 6º lanza `DemasiadasReservasException` con los segundos restantes; otra IP pasa; al vencer la ventana de 10 min vuelve a pasar; las entradas vencidas se limpian. GREEN: `LimitadorDeReservasPorOrigen` (design D5)
- [ ] 4.2 RED: `ManejadorDeErroresTest`: `DemasiadasReservasException` → 429 `ProblemDetail` en español con `Retry-After`. GREEN: handler aditivo en `ManejadorDeErrores` (commit aparte, archivo compartido)

## 5. Bloque B — Reserva (requiere C-10 en `main`)

- [ ] 5.1 Rebasar sobre `main` con C-10 y verificar su contrato contra design D2/D3 (`ServicioDeReservaDeHuecos.reservar`, campos de `SolicitudDeReservaDeHueco`, `HuecoNoDisponibleException`, `GET /api/publico/huecos`). Si algo cambió, ajustar design, specs y estas tareas antes de seguir
- [ ] 5.2 RED: `ServicioDeReservaOnlineTest` (Mockito): el limitador corre primero; aviso no aceptado → 400 sin llamar a pacientes, reservas ni notificaciones; orden pacientes → reserva → `enviarConfirmacion(turnoId)`; el consentimiento lleva el instante del reloj; `HuecoNoDisponibleException` se propaga y no se pide email. GREEN: `ServicioDeReservaOnline` sin `@Transactional` (design D2)
- [ ] 5.3 RED: `ReservasPublicasControllerTest` (`@SpringBootTest` + MockMvc, H2, reloj fijo, profesional con horario cargado): reserva "cualquiera" válida → 201 con inicio `-03:00`, profesional, estado `pendiente` y sin token, sillón ni datos del paciente; el turno queda `online`, no sobreturno, con sillón e historial; el canal simulado de C-14 tiene un email con los enlaces del turno. GREEN: `ReservasPublicasController`, `ReservaSolicitud`, `PacienteSolicitud`, `ReservaRespuesta`
- [ ] 5.4 RED: mismo test: email `ana@` → 400 por campo sin repetir el valor; aviso `false`/ausente → 400 con el mensaje de C-15 y sin paciente ni turno; `sillonId`/`esSobreturno` en el cuerpo se ignoran; horario ya ocupado → 409 "El horario elegido ya no está disponible; elegí otro" sin email; paciente con DNI existente y otro email → reserva creada, email registrado sin cambios. GREEN: ajustes de validación y mapeo
- [ ] 5.5 RED: el 6º `POST` de la misma IP en 10 minutos → 429 con `Retry-After` y nada creado. GREEN: limitador conectado al controlador (`request.getRemoteAddr()`)
- [ ] 5.6 RED: `LogsDeReservaPublicaTest` (`OutputCaptureExtension`): una reserva exitosa y una rechazada (409) no dejan en el log ni en el cuerpo el DNI, el nombre, el teléfono, el email ni la IP. GREEN: ajustar logs
- [ ] 5.7 RED: `ReservaPublicaConcurrenciaMySqlTest` (`@Tag("mysql")`, `it-mysql`, `@RepeatedTest(5)`, `CountDownLatch`): dos `POST` simultáneos al mismo hueco con un profesional y un sillón → un 201, un 409 y un solo turno activo. GREEN: ajustar si falla. Guardar la salida en `evidencia/it-mysql.txt`

## 6. Pantalla `/reservar` (spec "Pantalla de reserva mobile-first y accesible")

> 6.1 a 6.5 se pueden hacer en el bloque A contra la API simulada (`test/apiSimulada`) con el contrato de design D3 y de C-10 D7; 6.6 y 6.7 necesitan el bloque B.

- [ ] 6.1 RED: `esquemasReserva.test.ts`: DNI de 7 u 8 dígitos, nombre y apellido obligatorios, teléfono y email válidos, aviso obligatorio (reusa `esquemaAvisoPrivacidad`), mensajes en español. GREEN: `esquemasReserva.ts`
- [ ] 6.2 RED: `reservaApi.test.tsx`: `usePrestacionesPublicas`, `useProfesionalesDe`, `useHuecosDelDia`, `useContacto` y `useReservar` llaman a las rutas de design D3 por `apiClient` y exponen los errores 409/429 con su estado. GREEN: `reservaApi.ts`
- [ ] 6.3 RED: `ReservaPage.test.tsx`: flujo prestación → "Cualquier profesional" → día → horario → datos + aviso → "Reservar turno" → "¡Listo! Reservaste tu turno" con fecha, hora, prestación, profesional y estado pendiente; cada paso mueve el foco a su `h2`; "Volver" conserva lo elegido. GREEN: `ReservaPage` y pasos
- [ ] 6.4 RED: errores: DNI vacío → mensaje asociado por `aria-describedby` y foco en el campo; doble toque → un solo pedido y botón deshabilitado; 409 → alerta, nueva consulta de horarios del día, datos conservados; 429 → mensaje de espera; sin huecos → "No hay horarios disponibles" + contacto. GREEN: manejo de estados
- [ ] 6.5 RED: `rutas.test.tsx`: `/reservar` es pública (sin sesión) e `InicioPage` enlaza "Reservar un turno". GREEN: ruta en `routes/rutas.tsx` (aditivo) y enlace
- [ ] 6.6 Prueba manual con el backend real en `dev` (seed de C-06 y C-10): reservar con "cualquiera" y con profesional, ver el email en el canal simulado, y forzar un 409 reservando el mismo horario desde otra pestaña. Anotar en `evidencia/verificacion-dev.md`
- [ ] 6.7 Revisión a 390×844 y con teclado (orden de foco, foco visible, contraste, objetivos de 44 px), incluida la del componente `AvisoPrivacidad` trasladada de C-15 4.4; capturas en `evidencia/` si hay navegador (Playwright MCP) o checklist manual si no. Anotar en `evidencia/accesibilidad.md`

## 7. Verificación y cierre

- [ ] 7.1 `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm run lint`, `npm test` y `npm run build` en verde; salidas en `evidencia/`
- [ ] 7.2 `evidencia/trazabilidad.md`: cada requisito y escenario de las specs → test que lo cubre
- [ ] 7.3 Avisar a Bruno (C-19) los textos accesibles de design D6 y que `ReservaPublicaConcurrenciaMySqlTest` ya existe; avisar a Bruno (C-18) que `V016_1` agrega una clave al catálogo de parámetros
