package ar.dentaflow.infraestructura.persistencia.pacientes;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de la tabla paciente sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesPacienteH2Test extends RestriccionesPacienteBase {
}
