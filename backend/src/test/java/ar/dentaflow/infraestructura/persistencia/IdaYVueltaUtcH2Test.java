package ar.dentaflow.infraestructura.persistencia;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Ida y vuelta UTC sobre H2 en modo MySQL (perfil test). */
@SpringBootTest
@ActiveProfiles("test")
class IdaYVueltaUtcH2Test extends IdaYVueltaUtcBase {
}
