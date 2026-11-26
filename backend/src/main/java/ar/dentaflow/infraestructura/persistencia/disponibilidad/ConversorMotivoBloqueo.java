package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link MotivoBloqueo} por su nombre, el que valida {@code ck_bloqueo_motivo}. */
@Converter
public class ConversorMotivoBloqueo implements AttributeConverter<MotivoBloqueo, String> {

    @Override
    public String convertToDatabaseColumn(MotivoBloqueo motivo) {
        return motivo == null ? null : motivo.name();
    }

    @Override
    public MotivoBloqueo convertToEntityAttribute(String nombre) {
        return nombre == null ? null : MotivoBloqueo.valueOf(nombre);
    }
}
