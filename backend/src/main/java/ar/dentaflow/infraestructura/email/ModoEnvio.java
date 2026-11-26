package ar.dentaflow.infraestructura.email;

/** Por dónde salen los emails ({@code APP_MAIL_MODO}, design D4 y D7). */
public enum ModoEnvio {
    /** Servidor SMTP real (Brevo o Gmail, según {@code SPRING_MAIL_*}). */
    SMTP,
    /** En memoria, sin conexión: desarrollo local, pruebas y E2E (C-19). Es el valor por defecto. */
    SIMULADO
}
