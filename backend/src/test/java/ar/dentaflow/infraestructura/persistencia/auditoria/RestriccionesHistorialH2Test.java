package ar.dentaflow.infraestructura.persistencia.auditoria;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones del historial de turnos sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesHistorialH2Test extends RestriccionesHistorialBase {
}
