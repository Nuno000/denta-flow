package ar.dentaflow.infraestructura.persistencia.reportes;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Lectura de ausentismo sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class LecturaDeAusentismoH2Test extends LecturaDeAusentismoBase {
}
