package ar.dentaflow.infraestructura.persistencia.agenda;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Lectura de la agenda sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class LecturaDeAgendaH2Test extends LecturaDeAgendaBase {
}
