package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.aplicacion.turnos.AlternativaDeTurno;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import java.time.OffsetDateTime;

/** Horario libre sugerido en el 409 de la carga común (C-12, design D4), con el desplazamiento del consultorio. */
public record AlternativaRespuesta(OffsetDateTime inicio, OffsetDateTime fin, long profesionalId, long sillonId) {

    public static AlternativaRespuesta de(AlternativaDeTurno alternativa) {
        return new AlternativaRespuesta(
                alternativa.inicio().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                alternativa.fin().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                alternativa.profesionalId(), alternativa.sillonId());
    }
}
