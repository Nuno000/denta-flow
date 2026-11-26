# Evidencia del bloque B de C-15 (con C-09 en `main`)

- **Fecha**: 2026-11-26 · **Base**: `main` en `1dccfbc` (C-09 mergeado), traído a la rama con merge · Windows 11, Temurin 21, MySQL 8.4

## Decisiones tomadas al ver C-09

1. **DNI nulo al anonimizar** (Bruno, 2026-11-26): C-09 dejó `dni VARCHAR(8)` con CHECK de 7 u 8 caracteres, así que el sustituto `ANON-<id>` no entraba. `V015_1` permite `dni NULL` solo con `anonimizado_en` (`ck_paciente_dni_o_anonimizado`).
2. **Consentimiento con el paciente, no con el turno**: en C-09 el paciente se registra en su propia transacción antes que el turno. `registrarDesdeReservaOnline` ahora exige `consentimientoEn`; `RegistroDeConsentimiento` (`MANDATORY`) se descartó. Se ajustaron 7 llamadas de tests de C-09 que registraban online sin aceptación y 2 expectativas (un paciente existente ahora guarda la nueva aceptación).
3. **Puerto `TurnosDelPaciente`**: el test de arquitectura de C-09 (`UnicaViaDeEscrituraDeTurnosTest`) marcó a `ServicioDeAnonimizacion` por usar `TurnoRepositorio` y llamar a `guardar` (de pacientes). Era un falso positivo, pero en vez de tocar el test se agregó un puerto de solo lectura implementado en `infraestructura/persistencia/turnos/`.

## Ciclo TDD

| Tarea | Test | RED | GREEN |
|---|---|---|---|
| 5.2 | `RestriccionesPrivacidadPaciente{H2,MySql}Test` (4) | Columnas inexistentes | `V015_1__privacidad_paciente.sql` + mapeo |
| 7.1 | `PacienteAnonimizarTest` (7) | No compilaba (sin `anonimizar`) | `Paciente.anonimizar`, `estaAnonimizado`, `puedeRecibirEmails` |
| 6.2 | `ServicioDePacientesTest.desdeLaReservaOnlineSinConsentimiento…` | No compilaba (sin `AVISO_NO_ACEPTADO`) | Guarda en `registrarDesdeReservaOnline` |
| 7.2 | `ServicioDeAnonimizacionTest` (4) | Test y servicio escritos en el mismo paso: el RED no se vio por separado | `ServicioDeAnonimizacion` |
| 7.3 | `PacienteAdminControllerTest` (6) | 404/403 esperados, el endpoint no existía | `PacienteAdminController` |
| 5.3, 7.4, 8.1 | `PrivacidadPacienteIntegracionTest` (3) | Pasaron de entrada: cubren comportamiento ya implementado | — |

## Resultados

| Verificación | Resultado |
|---|---|
| `./mvnw clean verify` (H2) | 664 tests, 0 fallas |
| `./mvnw clean verify -Pit-mysql` (MySQL 8.4) | 139 tests, 0 fallas (incluye `RestriccionesPrivacidadPacienteMySqlTest`); corrido antes del refactor del puerto `TurnosDelPaciente`, que no toca SQL |
| `npm test` / `lint` / `build` | 103 tests en verde / OK / OK |
| `openspec validate --strict` | válido |

## Pendiente

- 4.4: revisión del componente a 390 px y con teclado cuando C-16 lo use en la pantalla de reserva.
- C-16 tiene que pasar `consentimientoEn` y exigir `aceptaAvisoPrivacidad` en su DTO.
- C-14/C-18 tienen que filtrar destinatarios con `Paciente.puedeRecibirEmails()`.
