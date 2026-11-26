package ar.dentaflow.infraestructura.persistencia.agenda;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Lectura de la agenda sobre MySQL 8 real (perfil {@code it-mysql}): misma consulta en el motor de producción. */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class LecturaDeAgendaMySqlTest extends LecturaDeAgendaBase {
}
