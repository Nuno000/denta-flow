package ar.dentaflow.infraestructura.persistencia.recursos;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones del esquema de recursos sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesRecursosH2Test extends RestriccionesRecursosBase {
}
