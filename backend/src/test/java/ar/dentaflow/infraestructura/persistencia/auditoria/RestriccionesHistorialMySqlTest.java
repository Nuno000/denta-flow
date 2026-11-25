package ar.dentaflow.infraestructura.persistencia.auditoria;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Restricciones del historial de turnos sobre MySQL 8 real (perfil {@code it-mysql}). Requiere MySQL
 * 8.0.16 o superior para que los CHECK se apliquen.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesHistorialMySqlTest extends RestriccionesHistorialBase {
}
