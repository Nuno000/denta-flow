package ar.dentaflow.infraestructura.persistencia.turnos;

import ar.dentaflow.dominio.turnos.OrigenTurno;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link OrigenTurno} como su código, el mismo que valida {@code ck_turno_origen}. */
@Converter
public class ConversorOrigenTurno implements AttributeConverter<OrigenTurno, String> {

    @Override
    public String convertToDatabaseColumn(OrigenTurno origen) {
        return origen == null ? null : origen.codigo();
    }

    @Override
    public OrigenTurno convertToEntityAttribute(String codigo) {
        return codigo == null ? null : OrigenTurno.desdeCodigo(codigo);
    }
}
