package ar.dentaflow.infraestructura.persistencia.notificaciones;

import ar.dentaflow.dominio.notificaciones.TipoNotificacion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el tipo de notificación (confirmacion, recordatorio) como su código. */
@Converter
public class ConversorTipoNotificacion implements AttributeConverter<TipoNotificacion, String> {

    @Override
    public String convertToDatabaseColumn(TipoNotificacion valor) {
        return valor == null ? null : valor.codigo();
    }

    @Override
    public TipoNotificacion convertToEntityAttribute(String codigo) {
        return codigo == null ? null : TipoNotificacion.desdeCodigo(codigo);
    }
}
