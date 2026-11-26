# Tasks — C-21 `whatsapp-prellenado`

> **Gobernanza BAJO** (`CHANGES.md`): autonomía si los tests pasan. Igual se avisa la decisión de privacidad (design D1) en la PR. Rama: `feat/C-21-whatsapp-prellenado`. Revisora del PR: Florencia.
>
> TDD estricto en cada tarea: **RED** (escribir el test y verlo fallar), **GREEN** (el código mínimo hasta verde) y **REFACTOR** (con los tests en verde). Nada requiere Docker ni Testcontainers. Evidencia en `openspec/changes/c-21-whatsapp-prellenado/evidencia/`. Commits en Conventional Commits en castellano, sin `Co-Authored-By`.

## 1. Línea base

- [ ] 1.1 Correr `./mvnw -B verify`, `npm run lint`, `npm test` y `npm run build` sobre `main` actualizado y anotar los resultados en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa.

## 2. Dominio: teléfono, plantilla y enlace (Java puro, `dominio/notificaciones/`)

- [ ] 2.1 RED→GREEN `TelefonoWhatsappTest`: normaliza los casos del design D2 y de la spec "Teléfono normalizado al formato argentino de WhatsApp": `011 15 5555-0000`, `0261 15 555-0000`, `2615550000`, `+542615550000`, `+5492954555000`, `0054 9 11 5555 0000`, `(0294) 15-455-5000`. Rechaza `+59899123456`, `00000000`, `12345` y `261155550001234`.
- [ ] 2.2 RED→GREEN `PlantillaWhatsappTest`: reemplaza los seis marcadores; fecha y hora en `America/Argentina/Buenos_Aires` (`2026-12-05T02:30:00Z` → "04/12/2026" y "23:30"); un marcador desconocido queda literal; un marcador repetido se reemplaza todas las veces.
- [ ] 2.3 RED→GREEN `EnlaceWhatsappTest`: `https://wa.me/<dígitos>?text=` con UTF-8 y `%20`; "¿Venís? Turno #2 & más" da el texto codificado de la spec; un `+` literal queda `%2B`.

## 3. Parámetro `whatsapp-plantilla` (spec `parametros-consultorio`)

- [ ] 3.1 RED→GREEN `ClaveParametroTest` y `CatalogoDeParametrosConsistenciaTest`: existe `whatsapp-plantilla` (texto, máximo 1000) y hay una fila por clave. Migración `V021_1__parametro_plantilla_whatsapp.sql` con el valor provisorio del design D3.
- [ ] 3.2 RED→GREEN `ParametrosControllerTest`: `GET /api/parametros` lista cinco parámetros; `PUT` de `whatsapp-plantilla` vacío o con 1001 caracteres responde 400 sin cambiar el valor; válido responde 200. `ServicioDeParametros.plantillaWhatsapp()` devuelve el valor vigente.

## 4. Caso de uso y endpoint (spec `mensajes-whatsapp`)

- [ ] 4.1 RED→GREEN `ServicioDeMensajesWhatsappTest` (Mockito): turno inexistente → `RecursoNoEncontradoException`; estados `cancelado`/`asistio`/`ausente`/`ausente_con_aviso` → `ConflictoException`; pendiente/confirmado con teléfono válido → mensaje y enlace; teléfono inválido o paciente anonimizado → enlace nulo con mensaje.
- [ ] 4.2 RED→GREEN `WhatsappControllerTest` (`@SpringBootTest` + MockMvc, H2): recepción y administrador reciben 200 con `mensaje` y `enlace` y `Cache-Control: no-store`; profesional 403; sin sesión 401; turno inexistente 404; turno cancelado 409; el historial del turno no cambia.
- [ ] 4.3 RED→GREEN `LogsDeWhatsappTest` (`OutputCaptureExtension`): pedir el mensaje no deja en el log teléfono, nombre del paciente ni token.

## 5. Frontend (`features/agenda/`)

- [ ] 5.1 RED→GREEN `api.test.tsx`: `useMensajeWhatsapp(turnoId)` llama a `GET /api/turnos/{id}/whatsapp` con la sesión y no consulta con `turnoId` nulo.
- [ ] 5.2 RED→GREEN `WhatsappDialogo.test.tsx`: muestra la vista previa y el enlace "Abrir WhatsApp" con `href`, `target="_blank"` y `rel="noopener noreferrer"`; con `enlace` nulo muestra el aviso `role="alert"` sin enlace; un error de la API muestra "Reintentar"; Escape y "Cerrar" cierran.
- [ ] 5.3 RED→GREEN `TarjetaDeTurno.test.tsx`: el botón "WhatsApp" aparece para administrador y recepción en turnos pendientes y confirmados, no aparece para el profesional ni en turnos cancelados o atendidos, y abre el diálogo.

## 6. Verificación y cierre

- [ ] 6.1 `./mvnw -B verify`, `npm run lint`, `npm test` y `npm run build` en verde; registrar las salidas en `evidencia/`.
- [ ] 6.2 Revisión del diálogo a 390×844 y con teclado (foco, Escape, orden de tabulación). Registrar en `evidencia/accesibilidad.md`.
- [ ] 6.3 `evidencia/trazabilidad.md`: matriz de cada escenario de las specs → tests que lo verifican.
- [ ] 6.4 `npx openspec validate c-21-whatsapp-prellenado --strict` en verde.
