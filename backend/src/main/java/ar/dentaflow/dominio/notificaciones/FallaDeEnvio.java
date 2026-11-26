package ar.dentaflow.dominio.notificaciones;

/**
 * El canal no pudo entregar el email al servidor de correo. Lleva solo la categoría de la falla: el texto del
 * proveedor (que puede repetir la dirección) no viaja en el mensaje ni como causa.
 */
public class FallaDeEnvio extends RuntimeException {

    private final MotivoFallo motivo;

    public FallaDeEnvio(MotivoFallo motivo) {
        super("No se pudo enviar el email: " + motivo.codigo(), null, false, false);
        this.motivo = motivo;
    }

    public MotivoFallo motivo() {
        return motivo;
    }
}
