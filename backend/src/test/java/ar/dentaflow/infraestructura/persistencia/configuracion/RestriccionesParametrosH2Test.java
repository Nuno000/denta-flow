package ar.dentaflow.infraestructura.persistencia.configuracion;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones y valores iniciales de {@code V007_1} sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesParametrosH2Test extends RestriccionesParametrosBase {
}
