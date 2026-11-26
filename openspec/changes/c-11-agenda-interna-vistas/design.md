# Design — C-11 `agenda-interna-vistas`

## Context

Ver `proposal.md` (Why). Estado actual relevante:

- **Frontend**: React 19 + React Router 7 + TanStack Query 5, sin librerías de fechas. Un único `apiClient` en `frontend/src/lib/apiClient.ts`. Cada feature tiene su `api.ts` con tipos y hooks (patrón de `features/recursos/api.ts`). Las pantallas del panel van dentro de `RutaProtegida` y usan `BarraDelPanel`, que tiene la lista `SECCIONES`.
- **Tests de UI**: `frontend/src/test/apiSimulada.tsx` simula la API en el borde HTTP (`fetch`) con claves `"MÉTODO /ruta"`. La clave **ignora la query string**: para verificar filtros y rango, los tests leen la URL de cada llamada en `fetchSimulado.mock.calls`.
- **Backend**: guardas `@PersonalInterno`, `@AdministradorORecepcion` y `@SoloAdministrador`. `UsuarioActual.obtener()` devuelve `UsuarioAutenticado(id, email, rol, profesionalId)`. `ZonaHorariaConsultorio` convierte entre hora local e `Instant`, y el `Clock` es inyectable (`RelojConfig`). `ManejadorDeErrores` responde en formato de problema (`ValidacionDeDominioException` → 400, `AccessDeniedException` → 403).
- **C-09 todavía no está en `main`**: no existen `Turno`, `Paciente` ni su repositorio. Renzo lo trabaja en paralelo y no se toca.
- Política visual del proyecto: `off` (`design-system/README.md` y `.active-orchestrator-state.json`), así que no hay gate de mockups.

## Goals / Non-Goals

**Goals:**
- Que toda la UI de `/panel/agenda` se pueda implementar y probar hoy, sin C-09, contra `simularApi`.
- Que conectar el backend real cambie **solo** el servidor: ni `api.ts` ni los componentes.
- Una única consulta por vista: un día o una semana.

**Non-Goals:**
- Grilla horaria por franjas, arrastrar y soltar, o vista mensual.
- Acciones sobre turnos (estado, asistencia, cancelar): C-13 y C-17.
- Paginación: el rango tope de 7 días acota la respuesta. Un consultorio de 1 a 5 profesionales tiene del orden de cientos de turnos por semana.
- Mostrar horarios de atención o bloqueos (C-06).

## Decisions

### D1. Contrato `GET /api/agenda/turnos`, definido por C-11

Parámetros: `desde` y `hasta` (`YYYY-MM-DD`, días del consultorio, ambos inclusivos), y opcionales `profesionalId`, `sillonId` y `estado` (uno de los seis de RN-TU-05).

Respuesta `200`: arreglo de `TurnoDeAgenda`:

```json
{
  "id": 41,
  "inicio": "2026-12-01T12:00:00Z",
  "fin": "2026-12-01T12:30:00Z",
  "estado": "confirmado",
  "origen": "online",
  "esSobreturno": false,
  "paciente": { "nombre": "Ana", "apellido": "Pérez" },
  "prestacion": { "id": 3, "nombre": "Limpieza" },
  "profesional": { "id": 10, "nombre": "Juan", "apellido": "Gómez" },
  "sillon": { "id": 1, "nombre": "Sillón 1" }
}
```

- **Por qué un endpoint propio en `/api/agenda` y no `GET /api/turnos`:** C-09 es CRÍTICO y su alcance es el modelo y el servicio de escritura. Un recurso de lectura de C-11 no obliga a negociar el contrato con Renzo bajo presión. Además, permite devolver una proyección mínima, con lo que se cumple la Ley 25.326 por diseño (sin DNI, teléfono, email ni token).
- **Alternativas descartadas:**
  - Que C-09 exponga el listado: lo bloquea y mezcla responsabilidades.
  - Paginación con `page/size`: la UI de una semana necesita todo junto.
- **Por qué fechas locales y no instantes en el pedido:** quien sabe la zona es el backend (`ZonaHorariaConsultorio`). La UI solo maneja "qué día del consultorio". El backend calcula `[desde 00:00, hasta+1 00:00)` locales → instantes UTC y filtra `inicio < finRango AND fin > inicioRango`.

### D2. Aislamiento por rol en el backend

`@PersonalInterno` en el controller. En la capa de aplicación, `ConsultaDeAgenda` recibe el usuario actual:

- `profesional` sin `profesionalId` vinculado → 403.
- `profesional` con filtro `profesionalId` distinto del propio → 403.
- `profesional` sin filtro → se fuerza su `profesionalId`.
- `administrador` y `recepcion` → filtros libres.

El 403 se lanza como `AccessDeniedException`, así el `ManejadorDeErrores` existente responde en formato de problema. Se responde 403 y no se ignora el filtro, porque ignorarlo en silencio ocultaría un error de la UI.

### D3. Cliente aislado en `features/agenda/api.ts`

Es el **único punto de contacto con la API**. Exporta:

- Tipos `EstadoTurno`, `TurnoDeAgenda`, `FiltrosAgenda` y `RangoAgenda`.
- `RUTA_AGENDA = '/api/agenda/turnos'`.
- `construirConsultaAgenda(rango, filtros): string`, una función pura que arma la query string y omite los filtros vacíos.
- `obtenerTurnosDeAgenda(rango, filtros)`, que llama a `apiClient.get`.
- `useTurnosDeAgenda(rango, filtros)`, un hook de TanStack Query con clave `['agenda', rango, filtros]` y `placeholderData: keepPreviousData` para no parpadear al navegar.

Los componentes consumen solo el hook. Los tests de pantalla simulan `GET /api/agenda/turnos` con `simularApi` y verifican la query string leyendo las llamadas. Cuando C-09 se mergee y el endpoint exista, la UI funciona sin cambios. Si el backend tuviera que cambiar un nombre de campo, el ajuste queda en este archivo.

- **Alternativa descartada:** un modo "demo" con datos falsos dentro de la app. Mezcla datos inventados con el producto real y viola el espíritu de "nada de datos falsos fuera de tests". La simulación vive solo en `src/test/`.

### D4. Fechas y zona horaria en el frontend sin dependencias

`features/agenda/fechas.ts`, con funciones puras sobre cadenas `YYYY-MM-DD`:

- `hoyEnConsultorio(ahora = new Date())` usa `Intl.DateTimeFormat('en-CA', { timeZone: 'America/Argentina/Buenos_Aires' })`.
- `sumarDias`, `lunesDeLaSemana` y `diasDeLaSemana` operan con aritmética de fechas UTC sobre la fecha civil. Así nunca dependen de la zona de la máquina.
- `horaEnConsultorio(instanteIso)` devuelve "09:00".
- `fechaLarga(fecha)` devuelve "jueves 26 de noviembre de 2026" con `Intl` `es-AR`.

Los tests fijan la hora con `vi.setSystemTime` y prueban los casos borde de cambio de día por zona (02:00Z → día anterior).

- **Alternativa descartada:** `date-fns-tz` o `dayjs`. Suman una dependencia el día de la entrega para algo que `Intl` resuelve, y Argentina no tiene horario de verano hoy (UTC-3 fijo). Igual usamos `Intl` con `timeZone` para no asumir el offset.

### D5. Estado de la pantalla en la URL

`useSearchParams` guarda `vista` (`dia` | `semana`, por defecto `dia`), `fecha` (por defecto, hoy del consultorio), `profesionalId`, `sillonId` y `estado`. Un valor inválido en la URL (fecha mal formada, vista desconocida) se normaliza al valor por defecto, sin llamar a la API con basura. El rango se deriva así:

- `dia` → `desde = hasta = fecha`.
- `semana` → lunes a domingo de `fecha`.

### D6. Componentes

En `features/agenda/`:

- **`AgendaPage`**: barra, título ("Agenda" o "Mi agenda" según el rol), `NavegacionDeFecha`, `FiltrosDeAgenda`, región `aria-live` con "N turnos el <fecha>" y la vista.
- **`NavegacionDeFecha`**: botones anterior, Hoy y siguiente; el selector Día/Semana como dos botones con `aria-pressed`; la fecha como encabezado `h2`.
- **`FiltrosDeAgenda`**: `<select>` rotulados. Los profesionales y sillones salen de `useProfesionales()` y `useSillones()` de `features/recursos/api.ts`, que ya leen los tres roles. El selector de profesional se oculta al rol `profesional`. Incluye "Limpiar filtros".
- **`VistaDiaria`**: lista (`<ol>`) de `TarjetaDeTurno`, que en desktop se ve en dos columnas de datos y en celular apila. Se usa lista y no tabla para que en 390 px no haya scroll horizontal.
- **`VistaSemanal`**: siete secciones (`<section aria-labelledby>`), una por día, con la grilla `grid-cols-1` en celular y `lg:grid-cols-7` en desktop. El día de hoy se marca con `aria-current="date"` y el texto "Hoy".
- **`EtiquetaDeEstado`**: texto más color de refuerzo con contraste AA, y "Sobreturno" como etiqueta aparte.

Ruta `/panel/agenda` dentro de `RutaProtegida` sin `roles`: los tres roles internos. Se agrega `{ ruta: '/panel/agenda', nombre: 'Agenda' }` a `SECCIONES`, antes de "Recursos". No necesita `conSubrutas`, porque la query string no cambia el `pathname` y `NavLink` la sigue marcando como actual.

### D7. Backend, después del merge de C-09

Capas (KB 08):

- **`presentacion/agenda/AgendaController`** (`@PersonalInterno`): `@RequestParam LocalDate desde, hasta` y los filtros opcionales. El estado se parsea al enum de C-09 y un valor desconocido da 400. El controller devuelve `List<TurnoDeAgendaRespuesta>` (records) y está documentado con `@Tag("Agenda")`, `@Operation` y `@ApiResponse` 200/400/401/403.
- **`aplicacion/agenda/ConsultaDeAgenda`** (`@Transactional(readOnly = true)`): valida el rango (`ValidacionDeDominioException`), aplica D2 y convierte días locales a instantes con `ZonaHorariaConsultorio`.
- **Repositorio de turnos de C-09**: se agrega un método de lectura con JPQL y `JOIN FETCH` de paciente, profesional, sillón y prestación, para evitar N+1, y con `ORDER BY` según la spec. Es solo lectura: no toca el servicio transaccional de escritura ni los bloqueos (regla dura de C-09). Si Renzo prefiere que el método viva en su repositorio, se agrega ahí en el PR de C-11. No hay migración: alcanzan los índices de C-09.

Tests:
- `ConsultaDeAgendaTest` con Mockito, para el rango y el rol.
- `AgendaControllerTest` con MockMvc, para 400, 401, 403 y el JSON sin datos sensibles.
- `@DataJpaTest` en H2 `MODE=MySQL` para el cruce de intervalos, el borde de medianoche local y el orden.
- No hace falta un test `@Tag("mysql")` de concurrencia: es lectura pura. Igual se corre `-Pit-mysql` para confirmar que la consulta funciona en MySQL 8.

### D8. Datos de prueba

`frontend/src/test/datosAgenda.ts` con turnos ficticios: nombres genéricos, sin DNI ni contacto, y fechas de diciembre de 2026. Incluye una función `apiDeAgenda(turnos, usuario)` que combina `GET /api/auth/yo`, profesionales, sillones y la agenda. Sigue el patrón de `datosRecursos.ts`.

## Risks / Trade-offs

- **[C-09 llega tarde o no llega antes de las 12:00]** → La UI queda terminada y probada, pero en la app real `/panel/agenda` mostraría "No se pudo cargar la agenda" (404 del backend). *Mitigación:* el PR puede mergearse con los grupos 1 a 5 (solo frontend) si el equipo lo acepta para la demo, y el change se archiva recién cuando esté el grupo 6. La pantalla de error es correcta y no rompe nada.
- **[Los nombres del modelo de C-09 difieren del contrato]** (p. ej., `horaInicio`, estado en mayúsculas) → El contrato es del endpoint de C-11, no de la entidad. El mapeo a `TurnoDeAgendaRespuesta` absorbe la diferencia y la UI no cambia.
- **[Conflicto de merge con C-09 en el repositorio de turnos]** → El método de lectura se agrega después de que C-09 esté en `main` (rama actualizada con `origin/main`), nunca antes.
- **[`simularApi` ignora la query string]** → Un test podría pasar con filtros mal enviados. *Mitigación:* hay tests explícitos que leen la URL llamada (`desde`, `hasta` y filtros) y tests unitarios de `construirConsultaAgenda`.
- **[Un profesional manipula la URL para ver otra agenda]** → El backend responde 403 (D2), y la UI muestra el aviso de falta de permiso. La UI nunca es la barrera.
- **[Semana con muchos turnos en celular]** → Lista apilada por día. Es aceptable para un consultorio chico.

## Migration Plan

Sin migraciones de base ni cambios de configuración. Rollback: revertir el PR. La ruta y el enlace de menú desaparecen y el resto del panel no cambia.
