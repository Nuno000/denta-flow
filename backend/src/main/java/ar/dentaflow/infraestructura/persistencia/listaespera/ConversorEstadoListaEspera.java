package ar.dentaflow.infraestructura.persistencia.listaespera;

import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConversorEstadoListaEspera implements AttributeConverter<EstadoListaEspera, String> {

    @Override
    public String convertToDatabaseColumn(EstadoListaEspera estado) {
        return estado == null ? null : estado.codigo();
    }

    @Override
    public EstadoListaEspera convertToEntityAttribute(String codigo) {
        return codigo == null ? null : EstadoListaEspera.desdeCodigo(codigo);
    }
}
