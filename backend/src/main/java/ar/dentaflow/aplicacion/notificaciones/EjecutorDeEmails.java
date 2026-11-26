package ar.dentaflow.aplicacion.notificaciones;

import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Hilos propios para hablar con el servidor de correo (design D5): el envío nunca corre en el hilo de la request
 * ni dentro de su transacción. Es un tipo propio (no un {@code Executor}) para no reemplazar el ejecutor por
 * defecto de Spring Boot.
 */
public final class EjecutorDeEmails implements DisposableBean {

    private final ThreadPoolTaskExecutor hilos;

    private EjecutorDeEmails(ThreadPoolTaskExecutor hilos) {
        this.hilos = hilos;
    }

    /** {@code cantidad} hilos y una cola acotada: si se llena, {@link #ejecutar} lanza {@link RejectedExecutionException}. */
    public static EjecutorDeEmails conHilos(int cantidad, int capacidadDeCola) {
        ThreadPoolTaskExecutor hilos = new ThreadPoolTaskExecutor();
        hilos.setCorePoolSize(cantidad);
        hilos.setMaxPoolSize(cantidad);
        hilos.setQueueCapacity(capacidadDeCola);
        hilos.setThreadNamePrefix("email-");
        hilos.setWaitForTasksToCompleteOnShutdown(true);
        hilos.setAwaitTerminationSeconds(15);
        hilos.initialize();
        return new EjecutorDeEmails(hilos);
    }

    /** Corre cada tarea en un hilo nuevo y espera a que termine (perfil {@code test}). */
    public static EjecutorDeEmails sincronico() {
        return new EjecutorDeEmails(null);
    }

    public void ejecutar(Runnable tarea) {
        if (hilos != null) {
            hilos.execute(tarea);
            return;
        }
        Thread hilo = new Thread(tarea, "email-sincronico");
        hilo.start();
        try {
            hilo.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() {
        if (hilos != null) {
            hilos.shutdown();
        }
    }
}
