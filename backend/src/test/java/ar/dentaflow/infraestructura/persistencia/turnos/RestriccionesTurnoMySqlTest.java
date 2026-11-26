package ar.dentaflow.infraestructura.persistencia.turnos;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones de la tabla turno sobre MySQL 8 real (perfil {@code it-mysql}, MySQL 8.0.16 o superior). */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class RestriccionesTurnoMySqlTest extends RestriccionesTurnoBase {
}
