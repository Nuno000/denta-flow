package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

/** Rango de atención semanal; horas {@code HH:mm} en la zona del consultorio. */
public record HorarioRespuesta(
        Long id,
        Long profesionalId,
        DiaSemana dia,
        @Schema(type = "string", example = "09:00") @JsonFormat(pattern = "HH:mm") LocalTime inicio,
        @Schema(type = "string", example = "13:00") @JsonFormat(pattern = "HH:mm") LocalTime fin) {

    static HorarioRespuesta de(HorarioAtencion horario) {
        return new HorarioRespuesta(horario.getId(), horario.getProfesionalId(), DiaSemana.de(horario.getDia()),
                horario.getInicio(), horario.getFin());
    }
}
