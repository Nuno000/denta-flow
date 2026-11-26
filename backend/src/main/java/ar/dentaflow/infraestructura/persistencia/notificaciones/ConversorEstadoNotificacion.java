package ar.dentaflow.infraestructura.persistencia.notificaciones;

import ar.dentaflow.dominio.notificaciones.EstadoNotificacion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el estado de notificación (en_curso, enviada, fallida, omitida) como su código. */
@Converter
public class ConversorEstadoNotificacion implements AttributeConverter<EstadoNotificacion, String> {

    @Override
    public String convertToDatabaseColumn(EstadoNotificacion valor) {
        return valor == null ? null : valor.codigo();
    }

    @Override
    public EstadoNotificacion convertToEntityAttribute(String codigo) {
        return codigo == null ? null : EstadoNotificacion.desdeCodigo(codigo);
    }
}
