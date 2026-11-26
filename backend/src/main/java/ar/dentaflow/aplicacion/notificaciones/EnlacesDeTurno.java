package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.turnos.Turno;
import org.springframework.stereotype.Component;

/**
 * Arma los enlaces del paciente ({@code <APP_BASE_URL>/turno/<token>/<accion>}) con el token de acceso que el turno
 * ya tiene (C-09, design D6): no genera ni guarda otro token, así que los enlaces de un turno son siempre los
 * mismos (confirmación, recordatorio de C-18). El token es Base64 URL y va en la ruta, sin codificar.
 *
 * <p><b>Contrato con C-17:</b> el frontend atiende esas tres rutas y sirve la página con
 * {@code Referrer-Policy: no-referrer} para que el token no salga a terceros.
 */
@Component
public class EnlacesDeTurno {

    private final String base;

    public EnlacesDeTurno(PropiedadesEnlaces propiedades) {
        this.base = propiedades.baseUrl();
    }

    public String confirmar(Turno turno) {
        return enlace(turno, "confirmar");
    }

    public String cancelar(Turno turno) {
        return enlace(turno, "cancelar");
    }

    public String reprogramar(Turno turno) {
        return enlace(turno, "reprogramar");
    }

    public EnlacesDelTurno todos(Turno turno) {
        return new EnlacesDelTurno(confirmar(turno), cancelar(turno), reprogramar(turno));
    }

    private String enlace(Turno turno, String accion) {
        return base + "/turno/" + turno.tokenAcceso() + "/" + accion;
    }
}
