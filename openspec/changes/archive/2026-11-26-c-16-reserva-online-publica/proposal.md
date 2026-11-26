# Proposal — C-16 `reserva-online-publica`

## Why

US-010 y el Flujo 1 son el centro del MVP: el paciente reserva desde un link, en el celular, sin registrarse, y nunca se le ofrece ni se le asigna un horario que choque con otro turno (RN-DI-01, RN-DI-02, RN-TU-01/02). C-16 (`CHANGES.md`, oleada 6, camino crítico, governance **ALTO**, en el corte del 26/11) junta lo que dejaron listo los changes anteriores: el motor de disponibilidad (C-10), el aviso y el consentimiento (C-15) y el email con los enlaces (C-14, ya en `main`).

**Plan de entrega**: C-10 tiene la propuesta en `main` pero **todavía no tiene código**. Esta propuesta se mergea sola (PR de documentación, como hicieron C-10, C-15, C-18 y C-19) y el trabajo se divide en dos bloques (ver `tasks.md`): el **bloque A** no necesita a C-10 (catálogo público, contacto del consultorio, límite anti-abuso y la pantalla contra una API simulada); el **bloque B** espera a C-10 en `main` (endpoint de reserva y tests de punta a punta).

## What Changes

- **Endpoint de reserva** `POST /api/publico/reservas` (público): valida el input en el backend, exige el aviso aceptado, registra al paciente (`ServicioDePacientes.registrarDesdeReservaOnline`, con el instante de consentimiento), reserva con la estrategia por candidato de C-10 (`ServicioDeReservaDeHuecos.reservar`, que crea el turno `pendiente` y su historial por `ServicioDeTurnos`), y pide el email de confirmación (`ServicioDeNotificaciones.enviarConfirmacion`, C-14). Responde **201** con el resumen del turno (sin token ni datos personales) o **409** si el horario ya no está disponible.
- **Catálogo público de lectura**: `GET /api/publico/prestaciones` (activas, con duración) y `GET /api/publico/profesionales?prestacionId=` (activos que realizan la prestación, solo id y nombre). Los huecos los da `GET /api/publico/huecos` de C-10.
- **Contacto del consultorio**: nuevo parámetro `contacto-consultorio` (texto editable por el administrador, C-07) expuesto en `GET /api/publico/contacto`, para el mensaje "no hay huecos" y para el horario tomado (Flujo 1, casos de error). Migración `V016_1`.
- **Anti-abuso**: límite de reservas por dirección IP (5 cada 10 minutos, 429 con `Retry-After`), con el mismo patrón que el límite de ingresos de C-04. Sin captcha en el MVP.
- **Pantalla pública mobile-first** `/reservar` en `frontend/src/features/reserva/`: prestación → profesional o "cualquiera" (PA-05) → día y horario → datos (DNI, nombre, apellido, teléfono, email) + aviso de privacidad (componente de C-15) → confirmación. React Hook Form + Zod, TanStack Query sobre `lib/apiClient`, WCAG 2.1 AA, revisada a 390 px y con teclado (tarea trasladada desde C-15).
- **Horario tomado entre la selección y la confirmación**: la pantalla informa, vuelve a pedir los huecos del mismo día y conserva los datos cargados.
- **Nunca solapamientos ni sobreturno online** (RN-DI-02): la reserva solo pasa por C-10 → C-09; el `POST` no acepta sillón, origen ni sobreturno.
- **Tests**: reserva exitosa, aviso no aceptado, datos inválidos, horario tomado (409), sin huecos, límite por IP, y dos reservas simultáneas del mismo hueco contra MySQL 8 real (`@Tag("mysql")`, un 201 y un 409; es el `ReservaPublicaConcurrenciaMySqlTest` que espera C-19).

## Capabilities

### New Capabilities
- `reserva-online`: catálogo público para reservar, creación de la reserva online con validación, consentimiento, asignación por el motor y email de confirmación, manejo del horario tomado y de la falta de huecos, límite anti-abuso y la pantalla pública mobile-first.

### Modified Capabilities
- `parametros-consultorio`: el catálogo cerrado suma `contacto-consultorio` (texto) y su valor inicial provisorio.

## Impact

- **Backend**: `aplicacion/reservas/` (caso de uso `ServicioDeReservaOnline` y limitador), `presentacion/publico/` (controladores de catálogo, contacto y reserva, DTOs), `dominio/configuracion/ClaveParametro` (aditivo), migración `V016_1__parametro_contacto_consultorio.sql`. Sin cambios en `ConfiguracionSeguridad` (`/api/publico/**` ya es público) ni en `ServicioDeTurnos`.
- **Frontend**: `features/reserva/` (página, pasos, API y esquemas), ruta `/reservar` en `routes/rutas.tsx` (aditivo) y enlace desde `InicioPage`.
- **Depende de**: C-10 (bloqueante para el bloque B: `ServicioDeReservaDeHuecos`, `HuecoNoDisponibleException`, `GET /api/publico/huecos`), C-14 (en `main`), C-15 (en `main`), C-07 (parámetros).
- **Contratos**: C-17 reutiliza la pantalla de huecos para reprogramar; C-19 escribe `reserva.spec.ts` con los textos accesibles de los botones definidos acá y corre la concurrencia de la API pública.
- **Historias y reglas**: US-010, US-040 (UI); RN-DI-01..03, RN-TU-01/02, RN-PR-01..03; PA-05, PA-06, PA-13 (resueltas); Flujo 1 completo.
- **Fuera de alcance**: confirmar, cancelar y reprogramar desde los enlaces (C-17); captcha; reserva de varios turnos a la vez; pagos; recordatorios (C-18).
