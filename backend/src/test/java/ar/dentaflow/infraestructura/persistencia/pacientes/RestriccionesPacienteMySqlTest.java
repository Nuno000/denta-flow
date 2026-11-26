package ar.dentaflow.infraestructura.persistencia.pacientes;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de la tabla paciente sobre MySQL 8 real (perfil {@code it-mysql}, MySQL 8.0.16 o superior). */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesPacienteMySqlTest extends RestriccionesPacienteBase {
}
