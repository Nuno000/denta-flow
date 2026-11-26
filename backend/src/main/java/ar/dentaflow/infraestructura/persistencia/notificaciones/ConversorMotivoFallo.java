package ar.dentaflow.infraestructura.persistencia.notificaciones;

import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el motivo de falla (conexion, autenticacion, destinatario, desconocido) como su código. */
@Converter
public class ConversorMotivoFallo implements AttributeConverter<MotivoFallo, String> {

    @Override
    public String convertToDatabaseColumn(MotivoFallo valor) {
        return valor == null ? null : valor.codigo();
    }

    @Override
    public MotivoFallo convertToEntityAttribute(String codigo) {
        return codigo == null ? null : MotivoFallo.desdeCodigo(codigo);
    }
}
