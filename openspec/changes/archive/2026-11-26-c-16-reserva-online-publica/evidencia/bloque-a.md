# Evidencia Bloque A — C-16 `reserva-online-publica`

Fecha: 2026-11-26  
Rama: `feat/C-16-reserva-online-publica`  

---

## 1. Resumen del Bloque A implementado

Se completaron todas las tareas del Bloque A definidas en `tasks.md`:

### 2. Contacto del consultorio público (spec `parametros-consultorio`)
- **ClaveParametro**: se agregó `CONTACTO_CONSULTORIO("contacto-consultorio", ...)` con validación de tipo texto y longitud máxima 300.
- **Migración Flyway**: `V016_1__parametro_contacto_consultorio.sql` inserta el parámetro provisorio por defecto compatible con H2 y MySQL 8.
- **Lectura tipada**: método `contactoConsultorio()` en `ParametrosDelConsultorio` y `ServicioDeParametros`.
- **Endpoint público**: `GET /api/publico/contacto` en `ContactoPublicoController` retornando `{ "contacto": "..." }` con `Cache-Control: no-store` sin requerir autenticación.
- **Tests**: `CatalogoDeParametrosConsistenciaTest`, `RestriccionesParametrosBase`, `ParametrosControllerTest`, `ContactoPublicoControllerTest`.

### 3. Catálogo público para reservar (spec `reserva-online`)
- **Endpoints públicos**:
  - `GET /api/publico/prestaciones`: lista solo prestaciones activas con id, nombre y duración en minutos, ordenadas alfabéticamente.
  - `GET /api/publico/profesionales?prestacionId=`: lista solo profesionales activos vinculados a la prestación (id, nombre, apellido, sin exponer email ni matrícula). Si la prestación no existe o está inactiva devuelve 404; si falta `prestacionId` devuelve 400.
- **Encabezados y OpenAPI**: ambos con `Cache-Control: no-store` y etiqueta `@Tag(name = "Reserva pública")`.
- **Seguridad**: `GuardasEnControladoresTest` verificado en verde (exime el paquete `presentacion.publico`).
- **Tests**: `CatalogoPublicoControllerTest`.

### 4. Límite anti-abuso por origen (spec `reserva-online`)
- **LimitadorDeReservasPorOrigen**: ventana deslizante fija de 10 minutos en memoria (`ConcurrentHashMap`), máx. 5 pedidos por dirección IP, con reloj inyectable (`Clock`) y limpieza de ventanas vencidas.
- **Excepción y manejo de error**: `DemasiadasReservasException` mapeada en `ManejadorDeErrores` a HTTP 429 Too Many Requests con encabezado `Retry-After` (segundos restantes) y `ProblemDetail` en español ("Demasiados pedidos: Hiciste demasiados pedidos de reserva. Probá de nuevo en unos minutos.").
- **Privacidad**: no loguea ni expone la dirección IP.
- **Tests**: `LimitadorDeReservasPorOrigenTest`, `ManejadorDeErroresTest`.

### 6. Pantalla `/reservar` mobile-first contra API simulada (tareas 6.1 a 6.5)
- **Validaciones**: `esquemasReserva.ts` con Zod (DNI 7-8 dígitos numéricos, nombre y apellido obligatorios, teléfono y email válidos, aviso de privacidad obligatorio reutilizando `esquemaAvisoPrivacidad`).
- **Hooks de API**: `reservaApi.ts` con React Query (`usePrestacionesPublicas`, `useProfesionalesDe`, `useHuecosDelDia`, `useContacto`, `useReservar`).
- **Página y pasos**: `ReservaPage.tsx` con navegación por pasos (`PasoPrestacion`, `PasoProfesional`, `PasoHorario`, `PasoDatos`, `PasoConfirmacion`), foco accesible en `h2` al cambiar de paso, opción "Cualquier profesional", botón "Volver" con estado conservado, y manejo de errores 400 por campo, 409 horario tomado (reintentar manteniendo datos), 429 límite por IP y sin huecos con contacto.
- **Ruta pública**: `/reservar` agregada en `routes/rutas.tsx` fuera de rutas protegidas y enlace "Reservar un turno" en `InicioPage.tsx`.
- **Tests**: `esquemasReserva.test.ts` (18 tests), `reservaApi.test.tsx` (6 tests), `ReservaPage.test.tsx` (9 tests), `rutas.test.tsx` (9 tests).

---

## 2. Resultados de las verificaciones técnicas

### Backend
1. `./mvnw verify` (H2 `MODE=MySQL`):
   - **939 tests ejecutados, 0 fallas, 0 errores, BUILD SUCCESS**.
2. `./mvnw verify -Pit-mysql` (MySQL 8 local):
   - **187 tests ejecutados, 0 fallas, 0 errores, BUILD SUCCESS**.

### Frontend
1. `npm run lint` (`oxlint --deny-warnings`):
   - **0 advertencias, 0 errores** en 102 archivos con 117 reglas.
2. `npm test` (`vitest run`):
   - **31 archivos pasados, 202 tests pasados, 0 fallas**.
3. `npm run build` (`tsc -b && vite build`):
   - **Compilación exitosa** sin errores de TypeScript ni de empaquetado.

### OpenSpec
- `npx openspec validate c-16-reserva-online-publica --strict`: **Change 'c-16-reserva-online-publica' is valid**.
