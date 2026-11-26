package ar.dentaflow.aplicacion.notificaciones;

/** Los tres enlaces del paciente para un turno. {@link #toString()} no los muestra: llevan el token. */
public record EnlacesDelTurno(String confirmar, String cancelar, String reprogramar) {

    @Override
    public String toString() {
        return "EnlacesDelTurno[***]";
    }
}
