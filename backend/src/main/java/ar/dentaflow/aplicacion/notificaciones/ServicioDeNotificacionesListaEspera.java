package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import ar.dentaflow.dominio.notificaciones.FallaDeEnvio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ServicioDeNotificacionesListaEspera {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeNotificacionesListaEspera.class);

    private final EnviadorDeEmail enviador;
    private final PlantillaOfertaListaEspera plantilla;
    private final EjecutorDeEmails ejecutor;
    private final PropiedadesEnlaces enlaces;

    public ServicioDeNotificacionesListaEspera(EnviadorDeEmail enviador, PlantillaOfertaListaEspera plantilla,
            EjecutorDeEmails ejecutor, PropiedadesEnlaces enlaces) {
        this.enviador = enviador;
        this.plantilla = plantilla;
        this.ejecutor = ejecutor;
        this.enlaces = enlaces;
    }

    public void enviarOferta(long listaEsperaId, String emailDestino, String nombrePaciente, String prestacion,
            String profesional, Instant inicioHueco, Instant ofrecidaHasta, String tokenAceptacion) {
        String enlace = enlaces.baseUrl() + "/lista-espera/aceptar?token=" + tokenAceptacion;
        MensajeEmail mensaje = plantilla.armar(emailDestino, nombrePaciente, prestacion, profesional,
                inicioHueco, ofrecidaHasta, enlace);
        Runnable tarea = () -> entregar(listaEsperaId, mensaje);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    programar(listaEsperaId, tarea);
                }
            });
            return;
        }
        programar(listaEsperaId, tarea);
    }

    private void programar(long listaEsperaId, Runnable tarea) {
        try {
            ejecutor.ejecutar(tarea);
        } catch (RejectedExecutionException e) {
            LOG.warn("listaEspera={}: cola de envío llena, no se pudo enviar la oferta", listaEsperaId);
        }
    }

    private void entregar(long listaEsperaId, MensajeEmail mensaje) {
        try {
            enviador.enviar(mensaje);
            LOG.info("listaEspera={}: oferta enviada", listaEsperaId);
        } catch (RuntimeException e) {
            LOG.warn("listaEspera={}: falló el envío de oferta ({})", listaEsperaId, e.getClass().getSimpleName());
        }
    }
}
