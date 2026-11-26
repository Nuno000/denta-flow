package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el {@link AmbitoBloqueo} por su nombre, el que valida {@code ck_bloqueo_ambito}. */
@Converter
public class ConversorAmbitoBloqueo implements AttributeConverter<AmbitoBloqueo, String> {

    @Override
    public String convertToDatabaseColumn(AmbitoBloqueo ambito) {
        return ambito == null ? null : ambito.name();
    }

    @Override
    public AmbitoBloqueo convertToEntityAttribute(String nombre) {
        return nombre == null ? null : AmbitoBloqueo.valueOf(nombre);
    }
}
