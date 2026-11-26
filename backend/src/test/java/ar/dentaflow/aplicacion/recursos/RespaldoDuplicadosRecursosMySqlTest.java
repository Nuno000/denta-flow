package ar.dentaflow.aplicacion.recursos;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec gestion-recursos — "Duplicado que llega a la base" sobre MySQL 8 real (perfil {@code it-mysql}):
 * confirma que el mensaje de la violación de MySQL nombra la constraint y el servicio la traduce a 409
 * (design D4).
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RespaldoDuplicadosRecursosMySqlTest extends RespaldoDuplicadosRecursosBase {
}
