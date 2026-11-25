package ar.dentaflow.infraestructura.persistencia.usuarios;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones del esquema de usuarios sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesUsuarioH2Test extends RestriccionesUsuarioBase {
}
