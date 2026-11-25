package ar.dentaflow.dominio.comun;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Única interpretación de fechas y horas de la agenda (SU-09): en la zona del consultorio y
 * guardadas como {@link Instant} en UTC. No depende de la zona del servidor ({@code TZ} ni
 * {@code user.timezone}). Lo que necesita "ahora" recibe un {@link Clock} para poder fijarlo en
 * pruebas.
 */
public final class ZonaHorariaConsultorio {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private ZonaHorariaConsultorio() {
    }

    /** Fecha y hora de reloj del consultorio → instante absoluto. */
    public static Instant aInstante(LocalDateTime horaLocal) {
        return horaLocal.atZone(ZONA).toInstant();
    }

    /** Instante absoluto → fecha y hora de reloj del consultorio. */
    public static LocalDateTime aHoraLocal(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZONA);
    }

    /** Fecha de hoy en el consultorio según el reloj dado. */
    public static LocalDate hoy(Clock reloj) {
        return LocalDate.now(reloj.withZone(ZONA));
    }

    /** Fecha y hora actuales en el consultorio según el reloj dado. */
    public static LocalDateTime ahora(Clock reloj) {
        return LocalDateTime.now(reloj.withZone(ZONA));
    }
}
