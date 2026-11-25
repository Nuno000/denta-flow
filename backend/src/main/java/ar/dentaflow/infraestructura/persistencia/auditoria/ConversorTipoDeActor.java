package ar.dentaflow.infraestructura.persistencia.auditoria;

import ar.dentaflow.dominio.auditoria.TipoDeActor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link TipoDeActor} como su código ({@code usuario}, {@code paciente}, {@code sistema}). */
@Converter
public class ConversorTipoDeActor implements AttributeConverter<TipoDeActor, String> {

    @Override
    public String convertToDatabaseColumn(TipoDeActor tipo) {
        return tipo == null ? null : tipo.codigo();
    }

    @Override
    public TipoDeActor convertToEntityAttribute(String codigo) {
        return codigo == null ? null : TipoDeActor.desdeCodigo(codigo);
    }
}
