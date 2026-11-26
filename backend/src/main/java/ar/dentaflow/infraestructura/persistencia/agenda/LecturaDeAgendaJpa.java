package ar.dentaflow.infraestructura.persistencia.agenda;

import ar.dentaflow.aplicacion.agenda.CriterioDeAgenda;
import ar.dentaflow.aplicacion.agenda.FilaDeAgenda;
import ar.dentaflow.aplicacion.agenda.LecturaDeAgenda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Lectura de la agenda con JPQL (C-11, design D7). {@code Turno} (C-09) referencia a paciente, profesional, sillón
 * y prestación por id, así que se unen con joins de entidad por id en una sola consulta (sin N+1) que proyecta
 * directo a {@link FilaDeAgenda}: nunca se leen DNI, teléfono, email ni token. El cruce de intervalos es
 * semiabierto ({@code inicio < hasta AND fin > desde}) y aprovecha los índices {@code (profesional_id, estado,
 * inicio, fin)}, {@code (sillon_id, estado, inicio, fin)} y {@code (estado, inicio)} de {@code V009_1}.
 */
@Repository
class LecturaDeAgendaJpa implements LecturaDeAgenda {

    private static final String PROYECCION = "select new ar.dentaflow.aplicacion.agenda.FilaDeAgenda("
            + "t.id, t.inicio, t.fin, t.estado, t.origen, t.esSobreturno, "
            + "pa.nombre, pa.apellido, pe.id, pe.nombre, pr.id, pr.nombre, pr.apellido, s.id, s.nombre) "
            + "from Turno t "
            + "join Paciente pa on pa.id = t.pacienteId "
            + "join Profesional pr on pr.id = t.profesionalId "
            + "join Sillon s on s.id = t.sillonId "
            + "join Prestacion pe on pe.id = t.prestacionId "
            + "where t.inicio < :hasta and t.fin > :desde and t.estado in :estados";

    private static final String ORDEN = " order by t.inicio, pr.apellido, pr.nombre, s.nombre, t.id";

    private final EntityManager em;

    LecturaDeAgendaJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<FilaDeAgenda> buscar(CriterioDeAgenda criterio) {
        StringBuilder jpql = new StringBuilder(PROYECCION);
        if (criterio.profesionalId() != null) {
            jpql.append(" and t.profesionalId = :profesionalId");
        }
        if (criterio.sillonId() != null) {
            jpql.append(" and t.sillonId = :sillonId");
        }
        jpql.append(ORDEN);

        TypedQuery<FilaDeAgenda> consulta = em.createQuery(jpql.toString(), FilaDeAgenda.class)
                .setParameter("desde", criterio.desde())
                .setParameter("hasta", criterio.hasta())
                .setParameter("estados", criterio.estados());
        if (criterio.profesionalId() != null) {
            consulta.setParameter("profesionalId", criterio.profesionalId());
        }
        if (criterio.sillonId() != null) {
            consulta.setParameter("sillonId", criterio.sillonId());
        }
        return consulta.getResultList();
    }
}
