## Purpose

Dar al paciente, sin cuenta, acceso a su turno mediante enlaces con un token no adivinable (SU-03), y resolver ese token a la situación del turno para que la confirmación, la cancelación y la reprogramación (C-17) respondan con el mensaje correcto.

## ADDED Requirements

### Requirement: Enlaces armados con la URL pública y el token del turno
El sistema SHALL armar los enlaces de confirmar, cancelar y reprogramar como `<APP_BASE_URL>/turno/<token>/confirmar`, `/cancelar` y `/reprogramar`, usando el token de acceso que el turno ya tiene. MUST NOT generar ni guardar un token adicional, y los tres enlaces de un turno MUST seguir siendo los mismos en emails sucesivos.

#### Scenario: Enlaces de un turno
- **GIVEN** `APP_BASE_URL` es `https://turnos.example` y el token del turno es `AbC...xyz` (43 caracteres)
- **WHEN** se arman sus enlaces
- **THEN** son `https://turnos.example/turno/AbC...xyz/confirmar`, `.../cancelar` y `.../reprogramar`

#### Scenario: URL base con barra final
- **GIVEN** `APP_BASE_URL` es `https://turnos.example/`
- **WHEN** se arman los enlaces
- **THEN** no quedan barras dobles en la ruta

#### Scenario: Mismos enlaces en confirmación y recordatorio
- **WHEN** se arman los enlaces del mismo turno dos veces
- **THEN** son idénticos

### Requirement: Resolución de un token
El sistema SHALL resolver un token a exactamente una situación: `VIGENTE` (turno `pendiente` o `confirmado` cuyo inicio todavía no llegó), `CANCELADO` (turno cancelado), `VENCIDO` (el inicio ya llegó o el turno ya tiene asistencia registrada) o `INEXISTENTE` (token mal formado o sin turno). La comparación con el inicio MUST usar el reloj del sistema en UTC.

#### Scenario: Turno pendiente futuro
- **GIVEN** un turno `pendiente` del `2026-12-02T13:00:00Z` y el reloj en `2026-12-01T13:00:00Z`
- **WHEN** se resuelve su token
- **THEN** la situación es `VIGENTE`

#### Scenario: Turno ya cancelado
- **GIVEN** un turno `cancelado` del día siguiente
- **WHEN** se resuelve su token
- **THEN** la situación es `CANCELADO`

#### Scenario: Turno cuyo inicio ya pasó
- **GIVEN** un turno `pendiente` del `2026-12-02T13:00:00Z` y el reloj en `2026-12-02T13:00:00Z`
- **WHEN** se resuelve su token
- **THEN** la situación es `VENCIDO`

#### Scenario: Turno con asistencia registrada
- **GIVEN** un turno en estado `asistio`
- **WHEN** se resuelve su token
- **THEN** la situación es `VENCIDO`

#### Scenario: Token inexistente o mal formado
- **WHEN** se resuelve un token con formato válido que no corresponde a ningún turno, o un texto como `../x` o vacío
- **THEN** la situación es `INEXISTENTE`, sin indicar cuál de los dos casos ocurrió

### Requirement: La resolución no expone ni modifica el turno
Resolver un token MUST NOT cambiar el estado del turno ni registrar historial, y para un token `INEXISTENTE` MUST NOT devolver datos de ningún turno. Para los demás casos devuelve solo lo que el paciente necesita ver: inicio, fin, prestación, profesional y estado.

#### Scenario: Resolver no confirma
- **GIVEN** un turno `pendiente`
- **WHEN** se resuelve su token dos veces
- **THEN** el turno sigue `pendiente` y su historial no tiene entradas nuevas

#### Scenario: Sin datos personales en la resolución
- **WHEN** se resuelve el token de un turno vigente
- **THEN** el resultado no contiene DNI, teléfono, email ni el id del paciente
