package ar.dentaflow.infraestructura.persistencia.notificaciones;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de la tabla notificacion sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesNotificacionH2Test extends RestriccionesNotificacionBase {
}
