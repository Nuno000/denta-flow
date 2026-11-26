# Proposal — C-20 `lista-de-espera`

## Why

Cuando un paciente cancela un turno, el horario queda libre y puede perderse si nadie vuelve a consultar la reserva online a tiempo. `C-20` (`lista-de-espera` [DIF], `US-030`, `RN-CA-04`, Flujo 6) permite registrar pacientes en una lista de espera y ofrecerles automáticamente por email los huecos liberados compatibles para no perder turnos en el consultorio.

## What Changes

- **Nuevo modelo y persistencia `ListaEspera` (Migración `V020_1`)**: entidad con paciente, prestación, profesional opcional (`null` = cualquier profesional), rango deseado (`fecha_desde`, `fecha_hasta`, `hora_desde`, `hora_hasta`), estado (`activa`, `ofrecida`, `atendida`, `vencida`), referencia al turno/hueco ofrecido, token de aceptación no adivinable y fecha de vencimiento de la oferta.
- **Parámetro configurable del consultorio**: agregar `lista-espera-plazo-oferta-horas` en `ClaveParametro` (entero en horas, por defecto `4`, rango `1..72`) para resolver el plazo de oferta sin valor fijo (bloqueo documentado en `CHANGES.md`).
- **Gestión interna por recepción y administrador**: endpoints y vista en el panel (`/panel/lista-de-espera`) para dar de alta pacientes en la lista de espera (reutilizando búsqueda/creación de paciente por DNI), listar solicitudes con filtros por estado y cancelar/dar de baja entradas activas.
- **Oferta automática al cancelarse un turno (`RN-CA-04`)**: al pasar un turno a `cancelado`, buscar entradas `activa` compatibles (misma prestación, profesional coincidente o sin preferencia, y horario del turno liberado dentro del rango deseado, verificando disponibilidad real con `MotorDeDisponibilidad`), pasarlas a `ofrecida` con un token único y plazo de vencimiento, y enviar el email de oferta de forma asíncrona tras el commit.
- **Aceptación pública por enlace con garantía anti-solapamiento**: endpoint y pantalla pública (`/lista-de-espera/aceptar?token=...`) donde el primer paciente que acepta obtiene el turno mediante `ServicioDeTurnos.crear` (bloqueo pesimista profesional → sillón de `C-09`), marcando su entrada como `atendida` y las demás ofertas para ese mismo hueco como `vencida` (o devolviendo conflicto claro si el hueco ya fue tomado).
- **Vencimiento de ofertas y liberación natural del hueco**: si nadie acepta antes de `ofrecida_hasta` (verificado al intentar aceptar y mediante limpieza/expiración al consultar o ejecutar tarea de vencimiento), las ofertas vencidas pasan a `vencida` y el hueco sigue libre en la reserva online (`C-10`/`C-16`), ya que las ofertas no bloquean la agenda.

## Capabilities

### New Capabilities
- `lista-de-espera`: Gestión de lista de espera de pacientes, oferta automática por email ante cancelación de turnos, aceptación transaccional sin solapamientos y vencimiento de ofertas (`US-030`, `RN-CA-04`, Flujo 6).

### Modified Capabilities
- `parametros-consultorio`: Incorporación del parámetro configurable `lista-espera-plazo-oferta-horas` en el catálogo cerrado de parámetros (`ClaveParametro`).

## Impact

- **Backend**:
  - `dominio/listaespera/` y `aplicacion/listaespera/` (modelo, máquina de estados, matching, oferta y aceptación transaccional).
  - `dominio/configuracion/ClaveParametro.java` y migración `V020_1__crear_lista_de_espera.sql`.
  - `aplicacion/turnos/ServicioDeTurnos.java` (publicación de evento de cancelación tras transición a `CANCELADO`).
  - `aplicacion/notificaciones/` e `infraestructura/email/` (plantilla y envío asíncrono de email de oferta).
  - `presentacion/listaespera/` y `presentacion/publico/` (endpoints protegidos para recepción/admin y endpoint público de aceptación por token).
- **Frontend**:
  - `frontend/src/features/lista-espera/` (panel interno `/panel/lista-de-espera` y pantalla pública `/lista-de-espera/aceptar`).
  - Navegación en `BarraDelPanel.tsx` y rutas en `App.tsx`.
- **Restricciones**: Sin Docker ni Testcontainers (`DD-09`); tests en H2 `MODE=MySQL` y concurrencia de aceptación simultánea con `@Tag("mysql")` en perfil `it-mysql`.
