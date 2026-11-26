package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.FranjaLocal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de disponibilidad de recursos para la reserva y la agenda (spec disponibilidad-recursos,
 * RN-TU-04): horario de atención menos bloqueos, con día y hora del consultorio e intervalos
 * {@code [inicio, fin)}. No toma bloqueos de fila: C-09 lo llama después de bloquear profesional y
 * sillón dentro de su transacción, y C-10 lo usa para calcular huecos sin duplicar reglas.
 * Recurso inexistente → {@code RecursoNoEncontradoException}; rango vacío o invertido →
 * {@code ValidacionDeDominioException}.
 */
public interface DisponibilidadDeRecursos {

    /** Activo, dentro de su horario de un único día del consultorio y sin bloqueos suyos ni del consultorio. */
    boolean profesionalDisponible(Long profesionalId, Instant inicio, Instant fin);

    /** Activo y sin bloqueos suyos ni del consultorio; los sillones no tienen horario propio. */
    boolean sillonDisponible(Long sillonId, Instant inicio, Instant fin);

    /** Franjas de atención del profesional en la fecha del consultorio, con los contiguos unidos. */
    List<FranjaLocal> franjasDeAtencion(Long profesionalId, LocalDate fecha);

    /** Bloqueos del profesional y del consultorio que tocan la fecha del consultorio, por inicio. */
    List<Bloqueo> bloqueosQueAfectan(Long profesionalId, LocalDate fecha);
}
