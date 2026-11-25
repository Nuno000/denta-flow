package ar.dentaflow.infraestructura.persistencia;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Ida y vuelta UTC contra MySQL 8 real. Solo corre con -Pit-mysql. */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class IdaYVueltaUtcMySqlTest extends IdaYVueltaUtcBase {
}
