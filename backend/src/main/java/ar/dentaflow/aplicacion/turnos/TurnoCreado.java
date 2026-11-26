package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.dominio.turnos.Turno;

/** Turno recién creado junto con su token de acceso, para que C-14 arme el enlace sin volver a leerlo. */
public record TurnoCreado(Turno turno, String tokenAcceso) {

    /** El token no se imprime. */
    @Override
    public String toString() {
        return "TurnoCreado[turno=" + turno + "]";
    }
}
