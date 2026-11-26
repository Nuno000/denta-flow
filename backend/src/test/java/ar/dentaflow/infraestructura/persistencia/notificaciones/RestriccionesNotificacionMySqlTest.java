package ar.dentaflow.infraestructura.persistencia.notificaciones;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de la tabla notificacion sobre MySQL 8 real (perfil {@code it-mysql}). */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesNotificacionMySqlTest extends RestriccionesNotificacionBase {
}
