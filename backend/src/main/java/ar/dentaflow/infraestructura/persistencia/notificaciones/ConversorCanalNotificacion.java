package ar.dentaflow.infraestructura.persistencia.notificaciones;

import ar.dentaflow.dominio.notificaciones.CanalNotificacion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el canal de notificación (email) como su código. */
@Converter
public class ConversorCanalNotificacion implements AttributeConverter<CanalNotificacion, String> {

    @Override
    public String convertToDatabaseColumn(CanalNotificacion valor) {
        return valor == null ? null : valor.codigo();
    }

    @Override
    public CanalNotificacion convertToEntityAttribute(String codigo) {
        return codigo == null ? null : CanalNotificacion.desdeCodigo(codigo);
    }
}
