package ar.dentaflow.infraestructura.persistencia.pacientes;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de privacidad de la tabla paciente sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesPrivacidadPacienteH2Test extends RestriccionesPrivacidadPacienteBase {
}
