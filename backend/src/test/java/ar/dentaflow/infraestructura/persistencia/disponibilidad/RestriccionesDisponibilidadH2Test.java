package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Restricciones del esquema de horarios y bloqueos sobre H2 en modo MySQL. */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionesDisponibilidadH2Test extends RestriccionesDisponibilidadBase {
}
