package ar.dentaflow.aplicacion.notificaciones;

import java.time.Instant;

/**
 * Lo que entra en un email del turno: solo lo necesario (minimización, Ley 25.326). Sin DNI ni teléfono; del
 * paciente, la dirección y el nombre de pila. {@link #toString()} no muestra nada.
 */
public record DatosDelEmail(String destinatario, String nombrePaciente, Instant inicio, String prestacion,
        String profesional, EnlacesDelTurno enlaces) {

    @Override
    public String toString() {
        return "DatosDelEmail[***]";
    }
}
