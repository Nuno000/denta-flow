package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.dominio.recursos.Profesional;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Profesional tal como lo ve el personal interno, con los ids de las prestaciones que realiza, ordenados. */
public record ProfesionalRespuesta(
        Long id,
        String nombre,
        String apellido,
        @JsonInclude(JsonInclude.Include.NON_NULL) String matricula,
        String email,
        boolean activo,
        List<Long> prestacionIds) {

    public static ProfesionalRespuesta de(Profesional profesional) {
        return new ProfesionalRespuesta(profesional.getId(), profesional.getNombre(), profesional.getApellido(),
                profesional.getMatricula(), profesional.getEmail(), profesional.isActivo(),
                profesional.prestacionIds().stream().sorted().toList());
    }
}
