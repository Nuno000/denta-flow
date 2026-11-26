# Verificación de supuestos de C-10 sobre C-06 (`upstream/main` `6daa14b`)

Fecha: 2026-11-26  
Rama: `feat/C-10-motor-disponibilidad` rebasada sobre `upstream/main` (`6daa14b`, incluye PR #11 `C-06`).

| # | Supuesto en `proposal.md` | Estado | Evidencia en `main` |
|---|---|---|---|
| 1 | Paquetes `ar.dentaflow.dominio.disponibilidad` y `ar.dentaflow.aplicacion.disponibilidad` | ✔ | Existen con las clases de C-06; C-10 agrega clases nuevas sin colisión |
| 2 | Interfaz `DisponibilidadDeRecursos` (`profesionalDisponible`, `sillonDisponible`) implementada por `ServicioDeDisponibilidad` (`@Transactional(readOnly = true)`) | ✔ | `DisponibilidadDeRecursos.java` y `ServicioDeDisponibilidad.java` coinciden exactamente |
| 3 | Regla pura `CalendarioDeDisponibilidad` (`profesionalDisponible`, `sillonDisponible`, `franjasDelDia`) | ✔ | `CalendarioDeDisponibilidad.java` coincide exactamente |
| 4 | Record `FranjaLocal(LocalDate, LocalTime, LocalTime)` con `inicioInstante()` y `finInstante()` | ✔ | `FranjaLocal.java` coincide exactamente |
| 5 | Puertos `HorarioAtencionRepositorio.listarDelProfesional(Long)` y `BloqueoRepositorio.listarQueAfectanAProfesional/ASillon` | ✔ | `HorarioAtencionRepositorio.java` y `BloqueoRepositorio.java` coinciden |
| 6 | `Bloqueo` con `afectaAProfesional`, `afectaASillon`, `seSuperponeCon` y `AmbitoBloqueo` | ✔ | `Bloqueo.java` y `AmbitoBloqueo.java` coinciden |
| 7 | Horario semanal en hora local sin cruzar medianoche; bloqueos en UTC; bloqueo no cancela turnos | ✔ | Verificado en `CalendarioDeDisponibilidad` y `Bloqueo` |
| 8 | Migración `V006_1__crear_horarios_y_bloqueos.sql` | ✔ | Existe en `backend/src/main/resources/db/migration/V006_1__crear_horarios_y_bloqueos.sql` |
| 9 | Capabilities de C-06: `horarios-atencion`, `bloqueos-agenda`, `disponibilidad-recursos` | ℹ | C-06 se mergeó sin archivar sus specs en `openspec/specs/`; el código y la migración están completos en `main`. Sin impacto para C-10 |
| 10 | C-06 no registra un bean `RestriccionDeAgenda` | ✔ | Búsqueda en `backend/src/main/java`: solo `ServicioDeTurnos` y `TurnoPropuesto` referencian `RestriccionDeAgenda`. Se mantiene el Grupo 3 |
| 11 | Seed `dev` de C-06 con horarios L-V 09:00–13:00 y 14:00–18:00 | ✔ | Pendiente de usar solo en verificación manual (tarea 9.3) |

`openspec validate c-10-motor-disponibilidad --strict` → `Change 'c-10-motor-disponibilidad' is valid`.
