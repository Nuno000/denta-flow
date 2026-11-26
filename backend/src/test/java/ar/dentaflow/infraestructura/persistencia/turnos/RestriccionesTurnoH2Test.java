package ar.dentaflow.infraestructura.persistencia.turnos;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de la tabla turno sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesTurnoH2Test extends RestriccionesTurnoBase {
}
