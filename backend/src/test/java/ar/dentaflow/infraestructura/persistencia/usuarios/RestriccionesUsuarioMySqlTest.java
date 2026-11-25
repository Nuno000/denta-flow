package ar.dentaflow.infraestructura.persistencia.usuarios;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Restricciones del esquema de usuarios sobre MySQL 8 real (perfil {@code it-mysql}). Requiere MySQL
 * 8.0.16 o superior para que el CHECK de coherencia rol–profesional se aplique.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesUsuarioMySqlTest extends RestriccionesUsuarioBase {
}
