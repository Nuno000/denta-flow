package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Implementación provisoria de {@link TurnosAfectadosPorBloqueo} mientras no existe la tabla de
 * turnos: siempre devuelve una lista vacía. <strong>Reemplazar en C-09</strong> por la consulta real
 * y borrar esta clase (dos implementaciones del puerto hacen fallar el arranque).
 */
@Component
public class SinTurnosAfectados implements TurnosAfectadosPorBloqueo {

    @Override
    public List<TurnoAfectado> listar(Bloqueo bloqueo) {
        return List.of();
    }
}
