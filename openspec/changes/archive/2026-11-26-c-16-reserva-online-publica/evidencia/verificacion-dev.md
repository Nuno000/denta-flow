# Verificación en Desarrollo — C-16 `reserva-online-publica`

Fecha: 2026-11-26  
Rama: `feat/C-16-reserva-online-publica`

## 1. Escenarios probados con backend real

### Escenario 1: Reserva con "Cualquier profesional"
- **Entrada**: Prestación activa (ej. Limpieza, 30 min), "Cualquier profesional", fecha y horario disponible, datos de paciente válidos y aviso de privacidad aceptado.
- **Resultado**:
  - `POST /api/publico/reservas` retorna `201 Created` con encabezado `Cache-Control: no-store`.
  - El cuerpo incluye `inicio`, `fin`, `prestacion`, `profesional` asignado por el sistema y `estado: "pendiente"`.
  - No expone `token`, `tokenAcceso`, `sillonId` ni datos personales del paciente.
  - El turno queda guardado en la base con origen `online`, no sobreturno, y sillón asignado.
  - En el canal de notificaciones se genera el email de confirmación con los enlaces `/turno/{token}/confirmar` y `/turno/{token}/cancelar`.

### Escenario 2: Reserva con profesional específico
- **Entrada**: Prestación activa, profesional elegido específicamente, horario dentro de su franja de atención sin solapamientos.
- **Resultado**:
  - Se asigna el profesional solicitado y el primer sillón disponible.
  - Estado `pendiente`, email transaccional enviado con enlaces no adivinables.

### Escenario 3: Horario tomado entre selección y confirmación (409 Conflict)
- **Entrada**: Dos reservas simultáneas sobre el mismo horario con 1 profesional y 1 sillón.
- **Resultado**:
  - La primera reserva obtiene `201 Created`.
  - La segunda recibe `409 Conflict` con el mensaje: *"El horario elegido ya no está disponible; elegí otro"*.
  - En la UI se muestra el aviso de conflicto, se invalidan y refrescan automáticamente los horarios del día en TanStack Query, y los datos personales cargados en el formulario se conservan para que el paciente no tenga que reescribirlos.
