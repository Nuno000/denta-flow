package ar.dentaflow.presentacion.pacientes;

import ar.dentaflow.dominio.pacientes.Paciente;
import java.time.Instant;

/** Resultado de la baja: solo el id y el instante (UTC), nunca datos personales (C-15). */
public record PacienteAnonimizadoRespuesta(Long id, Instant anonimizadoEn) {

    static PacienteAnonimizadoRespuesta de(Paciente paciente) {
        return new PacienteAnonimizadoRespuesta(paciente.getId(), paciente.getAnonimizadoEn());
    }
}
