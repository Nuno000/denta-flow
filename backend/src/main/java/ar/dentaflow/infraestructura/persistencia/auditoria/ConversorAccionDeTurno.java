package ar.dentaflow.infraestructura.persistencia.auditoria;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda la {@link AccionDeTurno} como su código, el mismo que valida {@code ck_turno_historial_accion}. */
@Converter
public class ConversorAccionDeTurno implements AttributeConverter<AccionDeTurno, String> {

    @Override
    public String convertToDatabaseColumn(AccionDeTurno accion) {
        return accion == null ? null : accion.codigo();
    }

    @Override
    public AccionDeTurno convertToEntityAttribute(String codigo) {
        return codigo == null ? null : AccionDeTurno.desdeCodigo(codigo);
    }
}
