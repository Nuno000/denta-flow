package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Persiste {@link LocalTime} como hora de reloj sin desplazamiento de zona, neutralizando el ajuste
 * por la zona horaria de la JVM cuando {@code hibernate.jdbc.time_zone=UTC} está activo.
 */
@Converter
public class ConversorHoraLocal implements AttributeConverter<LocalTime, Time> {

    @Override
    public Time convertToDatabaseColumn(LocalTime hora) {
        if (hora == null) {
            return null;
        }
        long millisUtc = hora.toSecondOfDay() * 1000L + hora.getNano() / 1_000_000L;
        return new Time(millisUtc);
    }

    @Override
    public LocalTime convertToEntityAttribute(Time valor) {
        if (valor == null) {
            return null;
        }
        return Instant.ofEpochMilli(valor.getTime()).atOffset(ZoneOffset.UTC).toLocalTime();
    }
}
