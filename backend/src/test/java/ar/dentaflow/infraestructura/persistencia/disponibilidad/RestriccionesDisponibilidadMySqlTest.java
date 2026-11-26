package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Restricciones del esquema de horarios y bloqueos sobre MySQL 8 real (perfil {@code it-mysql}). El
 * arranque del contexto también valida con {@code ddl-auto=validate} la migración {@code V006_1} y el
 * mapeo XML. Requiere MySQL 8.0.16 o superior para que los CHECK se apliquen; un CHECK violado llega
 * como {@code UncategorizedSQLException}, por eso la base exige {@code DataAccessException} que nombre
 * la restricción.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesDisponibilidadMySqlTest extends RestriccionesDisponibilidadBase {
}
