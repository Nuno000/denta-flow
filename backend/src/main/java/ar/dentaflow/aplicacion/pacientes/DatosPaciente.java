package ar.dentaflow.aplicacion.pacientes;

import java.time.Instant;

/**
 * Datos de contacto con los que se registra un paciente. {@code consentimientoEn} es opcional: si viene, reemplaza
 * la fecha de aceptación del aviso de privacidad (lo envía C-15).
 */
public record DatosPaciente(String dni, String nombre, String apellido, String telefono, String email,
        Instant consentimientoEn) {

    /** Evita imprimir datos personales en logs o mensajes de error. */
    @Override
    public String toString() {
        return "DatosPaciente[...]";
    }
}
