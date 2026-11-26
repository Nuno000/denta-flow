package ar.dentaflow.infraestructura.persistencia.turnos;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link EstadoTurno} como su código, el mismo que valida {@code ck_turno_estado}. */
@Converter
public class ConversorEstadoTurno implements AttributeConverter<EstadoTurno, String> {

    @Override
    public String convertToDatabaseColumn(EstadoTurno estado) {
        return estado == null ? null : estado.codigo();
    }

    @Override
    public EstadoTurno convertToEntityAttribute(String codigo) {
        return codigo == null ? null : EstadoTurno.desdeCodigo(codigo);
    }
}
