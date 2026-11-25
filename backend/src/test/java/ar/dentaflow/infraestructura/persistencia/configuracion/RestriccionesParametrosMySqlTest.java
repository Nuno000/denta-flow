package ar.dentaflow.infraestructura.persistencia.configuracion;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Restricciones de {@code V007_1} sobre MySQL 8 real (perfil {@code it-mysql}). El arranque del
 * contexto también valida con {@code ddl-auto=validate} la migración y el mapeo XML. Requiere
 * MySQL 8.0.16 o superior para que el CHECK de autor y fecha se aplique.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesParametrosMySqlTest extends RestriccionesParametrosBase {
}
