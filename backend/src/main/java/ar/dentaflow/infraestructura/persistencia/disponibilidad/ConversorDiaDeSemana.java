package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;

/** Guarda el {@link DayOfWeek} como su número ISO-8601 (1 = lunes ... 7 = domingo), el que valida {@code ck_horario_atencion_dia}. */
@Converter
public class ConversorDiaDeSemana implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek dia) {
        return dia == null ? null : dia.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer numero) {
        return numero == null ? null : DayOfWeek.of(numero);
    }
}
