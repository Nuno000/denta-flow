# Ciclo TDD — C-14

1. **RED** (2026-11-26): se escribieron primero los tests de los bloques 2 a 8 y `./mvnw test-compile` falló con
   198 errores de compilación en 12 clases nuevas (`PropiedadesEmailTest`, `PropiedadesEnlacesTest`,
   `NotificacionTest`, `PoliticaDeReintentoTest`, `NotificacionRepositorioTest`, `EnlacesDeTurnoTest`,
   `ServicioDeEnlacesTest`, `PlantillaConfirmacionReservaTest`, `EnviadorDeEmailSimuladoTest`,
   `EnviadorDeEmailSmtpTest`, `ServicioDeNotificacionesTest`, `LogsDeNotificacionesTest`): no existía el código.
   `RestriccionesNotificacionBase` compilaba pero no tenía tabla (`V014_1` todavía no existía).
2. **GREEN**: con el código de dominio, migración, persistencia, aplicación e infraestructura, la primera corrida
   de las 15 clases dio 86/87. La falla (`rechazaUnMotivoFueraDelCatalogo`) era del test: el valor inválido
   `"timeout del proveedor"` (21 caracteres) chocaba con el largo de `VARCHAR(20)` antes que con el `CHECK`. Se
   cambió por `"timeout"` y quedó 87/87.
3. **REFACTOR**: `NombresParaElPaciente` compartido entre la plantilla y la resolución de enlaces; la
   documentación de `design.md` se ajustó a lo implementado (D4, D5 y D7).
4. **Guarda de arquitectura**: la primera corrida completa marcó `UnicaViaDeEscrituraDeTurnosTest` (C-09): la
   heurística ve `TurnoRepositorio` y `.guardar(` en el mismo archivo (`ServicioDeNotificaciones` guarda
   notificaciones, no turnos). En vez de tocar la guarda, los servicios de C-14 leen turnos con
   `ServicioDeTurnos.buscarPorId` / `buscarPorToken`, que C-09 ya expone: reutilizan C-09 y no dependen de su
   persistencia.
