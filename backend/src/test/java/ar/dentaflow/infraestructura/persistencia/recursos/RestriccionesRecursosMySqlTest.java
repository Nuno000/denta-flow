package ar.dentaflow.infraestructura.persistencia.recursos;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Restricciones del esquema de recursos sobre MySQL 8 real (perfil {@code it-mysql}). El arranque
 * del contexto también valida con {@code ddl-auto=validate} la migración y el mapeo XML.
 * Requiere MySQL 8.0.16 o superior para que el CHECK de duración se aplique.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesRecursosMySqlTest extends RestriccionesRecursosBase {
}
