# Proposal

## Why

RN-PR-01 y DD-07 exigen cumplir la Ley 25.326 desde el MVP: aviso de privacidad visible y aceptado en la reserva (US-040), minimización de datos (RN-PR-03, SU-07) y una vía de baja de los datos personales. PA-12 quedó resuelta el 2026-11-25 (DD-12): aceptación obligatoria con instante registrado, endpoint de anonimización/baja y retención de 5 años. C-15 está en el corte de entrega del 26/11 (DD-10) y C-16 (`reserva-online-publica`) depende de él, así que conviene tener el contrato listo aunque C-09 todavía no esté mergeado.

## What Changes

- **Aviso de privacidad público**: `GET /api/publico/aviso-privacidad` devuelve el texto y la leyenda de aceptación que el administrador edita en los parámetros de C-07, sin datos personales.
- **Consentimiento obligatorio**: servicio `RegistroDeConsentimiento` que C-16 invoca dentro de la transacción de la reserva: rechaza la reserva si no se aceptó el aviso y, si se aceptó, guarda en el paciente el instante (UTC, del `Clock`) de la última aceptación.
- **Componente de frontend `AvisoPrivacidad`** en `frontend/src/features/reserva/`: muestra el aviso, una casilla obligatoria con la leyenda y el error accesible (WCAG 2.1 AA, mobile-first), validada con Zod (`z.literal(true)`), para que C-16 lo incruste en su formulario.
- **Anonimización/baja**: `POST /api/pacientes/{id}/anonimizar`, solo para `administrador`. Reemplaza nombre, apellido, DNI, teléfono y email por valores sustitutos no identificables, registra quién y cuándo, conserva los turnos (retención de 5 años) y se rechaza con 409 si el paciente tiene turnos futuros `pendiente` o `confirmado`. Es idempotente.
- **Minimización**: el paciente solo tiene DNI, nombre, apellido, teléfono y email (más las marcas de consentimiento y anonimización); un test de arquitectura impide agregar campos clínicos.
- **Sin datos personales en logs ni en errores**: test que captura los logs de una reserva rechazada y de una anonimización y verifica que no aparecen DNI, email ni teléfono; los errores 4xx no repiten el input.
- **HTTPS detrás del proxy**: `server.forward-headers-strategy=framework` y HSTS en el perfil de despliegue; la verificación en el entorno público queda para C-19.
- Migración `V015_1__privacidad_paciente.sql`: columnas `anonimizado_en` y `anonimizado_por` en `paciente` (y `consentimiento_privacidad_en` solo si C-09 no la crea).

## Capabilities

### New Capabilities
- `privacidad-datos-personales`: aviso de privacidad público, consentimiento obligatorio con instante registrado, minimización de los datos del paciente, anonimización/baja con retención de 5 años y ausencia de datos personales en logs y errores.

### Modified Capabilities
<!-- Ninguna: el aviso usa el prefijo público ya previsto en `autorizacion-por-rol` y la lectura tipada ya definida en `parametros-consultorio`. -->

## Impact

- **Backend**: `dominio/pacientes/` (reglas de consentimiento y anonimización, Java puro), `aplicacion/pacientes/`, `presentacion/publico/` y `presentacion/pacientes/`, migración `V015_1`, `application.yml` (forward headers) y la configuración de seguridad (HSTS, regla del endpoint de anonimización).
- **Frontend**: `features/reserva/AvisoPrivacidad.tsx`, hook `useAvisoPrivacidad` sobre `lib/apiClient.ts` y TanStack Query.
- **Depende de C-09** (entidad y tabla `paciente`, servicio de turnos): el bloque A de `tasks.md` (aviso público, componente, logs, HTTPS) se puede hacer ya; el bloque B (consentimiento y anonimización) espera el merge de C-09.
- **Contrato con C-16**: el request de reserva incluye `aceptaAvisoPrivacidad` y la reserva llama a `RegistroDeConsentimiento.registrar(...)` en su transacción.
- **Contrato con C-14 y C-18**: no se envían emails a pacientes anonimizados.
- **Fuera de alcance**: purga automática al vencer los 5 años (no hay datos tan viejos en el MVP; queda documentado el punto de extensión), revisión legal del texto (editable en parámetros) y la vista de pacientes por rol (C-11/C-12).
