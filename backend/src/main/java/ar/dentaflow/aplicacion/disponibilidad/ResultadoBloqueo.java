package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import java.util.List;

/** Bloqueo guardado y los turnos que quedan dentro de su franja (aviso, sin resolución: PA-11). */
public record ResultadoBloqueo(Bloqueo bloqueo, List<TurnoAfectado> turnosAfectados) {

    public ResultadoBloqueo {
        turnosAfectados = List.copyOf(turnosAfectados);
    }
}
