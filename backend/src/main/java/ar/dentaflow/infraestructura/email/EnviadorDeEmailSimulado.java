package ar.dentaflow.infraestructura.email;

import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import ar.dentaflow.dominio.notificaciones.FallaDeEnvio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Canal simulado (design D4): guarda en memoria los últimos {@value #MAXIMO_GUARDADOS} emails, sin conectarse a
 * nada y sin escribir su contenido en el log. Lo usan las pruebas, el desarrollo local y los E2E de C-19, que leen
 * los enlaces de acá en vez de un buzón. Se le puede pedir que falle las próximas veces para probar reintentos.
 */
public class EnviadorDeEmailSimulado implements EnviadorDeEmail {

    public static final int MAXIMO_GUARDADOS = 50;

    private final Deque<MensajeEmail> mensajes = new ArrayDeque<>();
    private int fallasPendientes;
    private MotivoFallo motivoDeLasFallas;
    private int intentos;

    @Override
    public synchronized void enviar(MensajeEmail mensaje) {
        intentos++;
        if (fallasPendientes > 0) {
            fallasPendientes--;
            throw new FallaDeEnvio(motivoDeLasFallas);
        }
        if (mensajes.size() == MAXIMO_GUARDADOS) {
            mensajes.removeFirst();
        }
        mensajes.addLast(mensaje);
    }

    /** Hace fallar los próximos {@code cantidad} envíos con {@code motivo}. */
    public synchronized void fallarLasProximas(int cantidad, MotivoFallo motivo) {
        this.fallasPendientes = cantidad;
        this.motivoDeLasFallas = motivo;
    }

    /** Emails entregados, del más viejo al más nuevo. */
    public synchronized List<MensajeEmail> mensajes() {
        return List.copyOf(mensajes);
    }

    public synchronized List<MensajeEmail> mensajesPara(String destinatario) {
        return mensajes.stream().filter(m -> m.destinatario().equals(destinatario)).toList();
    }

    /** Llamadas a {@link #enviar}, exitosas o no. */
    public synchronized int intentos() {
        return intentos;
    }

    public synchronized void limpiar() {
        mensajes.clear();
        fallasPendientes = 0;
        motivoDeLasFallas = null;
        intentos = 0;
    }
}
