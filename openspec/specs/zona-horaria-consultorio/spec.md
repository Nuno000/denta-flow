# zona-horaria-consultorio Specification

## Purpose

Garantiza que todas las fechas y horas de la agenda se interpreten en la zona horaria del consultorio (`America/Argentina/Buenos_Aires`, SU-09) y se guarden en UTC, sin depender de la zona del servidor ni de la máquina del usuario.

## Requirements

### Requirement: Zona horaria única del consultorio
El sistema SHALL interpretar toda fecha y hora de agenda en la zona `America/Argentina/Buenos_Aires`, independientemente de la zona horaria configurada en el servidor.

#### Scenario: Servidor en otra zona
- **WHEN** el servidor corre con zona por defecto `UTC` y se interpreta el horario local "2026-12-01 09:00" del consultorio
- **THEN** el instante resultante es `2026-12-01T12:00:00Z`

#### Scenario: Instante mostrado en hora del consultorio
- **WHEN** se convierte el instante `2026-12-01T02:30:00Z` a hora del consultorio
- **THEN** el resultado es "2026-11-30 23:30", del día anterior

### Requirement: Almacenamiento en UTC
El sistema SHALL guardar los instantes en la base en UTC y SHALL devolverlos sin corrimiento al leerlos, en cualquier perfil.

#### Scenario: Ida y vuelta por la base
- **WHEN** se guarda el instante `2026-12-01T12:00:00Z` y se vuelve a leer
- **THEN** el valor leído es exactamente `2026-12-01T12:00:00Z`

### Requirement: Hora actual controlable
El sistema SHALL obtener la hora actual de una única fuente reemplazable, para que las reglas que dependen de "ahora" puedan probarse con una hora fija.

#### Scenario: Hora fija en pruebas
- **WHEN** una prueba fija la hora actual en `2026-11-26T15:00:00Z`
- **THEN** el sistema informa como fecha de hoy en el consultorio "2026-11-26" y como hora "12:00"

#### Scenario: Cambio de día por zona
- **WHEN** la hora actual fijada es `2026-11-27T02:00:00Z`
- **THEN** la fecha de hoy en el consultorio es "2026-11-26"
