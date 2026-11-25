package ar.dentaflow.infraestructura.persistencia.usuarios;

import ar.dentaflow.dominio.usuarios.Rol;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link Rol} como su código en minúsculas, que es la clave de la tabla {@code rol}. */
@Converter
public class ConversorRol implements AttributeConverter<Rol, String> {

    @Override
    public String convertToDatabaseColumn(Rol rol) {
        return rol == null ? null : rol.codigo();
    }

    @Override
    public Rol convertToEntityAttribute(String codigo) {
        return codigo == null ? null : Rol.desdeCodigo(codigo);
    }
}
