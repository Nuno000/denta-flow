package ar.dentaflow.dominio.notificaciones;

/**
 * Puerto de salida de los emails (spec "Canal desacoplado del proveedor", design D3). Lo implementan el adaptador
 * SMTP y el simulado en {@code infraestructura/email}; C-18 lo reutiliza para los recordatorios.
 */
public interface EnviadorDeEmail {

    /**
     * Entrega el mensaje al servidor de correo.
     *
     * @throws FallaDeEnvio si no se pudo entregar, con la categoría de la falla
     */
    void enviar(MensajeEmail mensaje);
}
