package ar.dentaflow.presentacion.pacientes;

import ar.dentaflow.dominio.pacientes.Paciente;

/**
 * Datos de contacto de un paciente para precargar la carga manual (C-12). Solo para administrador y recepción; el
 * DNI sale normalizado (sin puntos).
 */
public record PacienteRespuesta(Long id, String dni, String nombre, String apellido, String telefono, String email) {

    static PacienteRespuesta de(Paciente paciente) {
        return new PacienteRespuesta(paciente.getId(), paciente.getDni(), paciente.getNombre(), paciente.getApellido(),
                paciente.getTelefono(), paciente.getEmail());
    }

    /** Evita imprimir datos personales en logs o mensajes de error. */
    @Override
    public String toString() {
        return "PacienteRespuesta[...]";
    }
}
