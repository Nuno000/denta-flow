package ar.dentaflow.dominio.turnos;

/** Qué recurso impidió la reserva por solapamiento. */
public enum RecursoOcupado {
    PROFESIONAL("profesional"),
    SILLON("sillón");

    private final String descripcion;

    RecursoOcupado(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
