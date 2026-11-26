package ar.dentaflow.infraestructura.persistencia.reportes;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Lectura de ausentismo sobre MySQL 8 real (perfil {@code it-mysql}). */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class LecturaDeAusentismoMySqlTest extends LecturaDeAusentismoBase {
}
