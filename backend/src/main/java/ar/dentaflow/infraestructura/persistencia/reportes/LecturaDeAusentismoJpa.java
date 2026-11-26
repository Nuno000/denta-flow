package ar.dentaflow.infraestructura.persistencia.reportes;

import ar.dentaflow.aplicacion.reportes.LecturaDeAusentismo;
import ar.dentaflow.dominio.reportes.RegistroAsistenciaParaReporte;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Lectura JPQL única de turnos con asistencia registrada (C-23, design D2). Compatible con MySQL 8 y H2
 * {@code MODE=MySQL}, aprovecha {@code ix_turno_estado_inicio} e {@code ix_turno_profesional_solapamiento} de
 * {@code V009_1} y nunca selecciona DNI, teléfono, email ni token del paciente (Ley 25.326).
 */
@Repository
class LecturaDeAusentismoJpa implements LecturaDeAusentismo {

    private static final List<EstadoTurno> ESTADOS_EVALUADOS = List.of(
            EstadoTurno.ASISTIO,
            EstadoTurno.AUSENTE,
            EstadoTurno.AUSENTE_CON_AVISO);

    private static final String CONSULTA_BASE =
            "select new ar.dentaflow.dominio.reportes.RegistroAsistenciaParaReporte("
                    + "t.inicio, t.estado, pr.id, pr.nombre, pr.apellido, pa.id, pa.nombre, pa.apellido) "
                    + "from Turno t "
                    + "join Profesional pr on pr.id = t.profesionalId "
                    + "join Paciente pa on pa.id = t.pacienteId "
                    + "where t.estado in :estados and t.inicio >= :desde and t.inicio < :hasta";

    private final EntityManager em;

    LecturaDeAusentismoJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<RegistroAsistenciaParaReporte> buscarEvaluados(Instant desde, Instant hasta, Long profesionalId) {
        StringBuilder jpql = new StringBuilder(CONSULTA_BASE);
        if (profesionalId != null) {
            jpql.append(" and t.profesionalId = :profesionalId");
        }
        jpql.append(" order by t.inicio, t.id");

        TypedQuery<RegistroAsistenciaParaReporte> consulta = em
                .createQuery(jpql.toString(), RegistroAsistenciaParaReporte.class)
                .setParameter("estados", ESTADOS_EVALUADOS)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta);
        if (profesionalId != null) {
            consulta.setParameter("profesionalId", profesionalId);
        }
        return consulta.getResultList();
    }
}
