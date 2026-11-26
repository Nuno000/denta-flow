package ar.dentaflow.infraestructura.persistencia.turnos;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.turnos.CerrojoDeRecursos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloquea las filas de profesional y sillón con {@code SELECT id ... FOR UPDATE} nativo (design D5): SQL válido en
 * MySQL 8 y H2, sin arrastrar la colección de prestaciones del profesional. Exige una transacción abierta: el
 * bloqueo dura hasta que ella termina.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class CerrojoDeRecursosJpa implements CerrojoDeRecursos {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void bloquearProfesional(Long profesionalId) {
        bloquear("profesional", profesionalId, "El profesional no existe");
    }

    @Override
    public void bloquearSillon(Long sillonId) {
        bloquear("sillon", sillonId, "El sillón no existe");
    }

    private void bloquear(String tabla, Long id, String mensajeSiNoExiste) {
        boolean existe = !em.createNativeQuery("SELECT id FROM " + tabla + " WHERE id = :id FOR UPDATE")
                .setParameter("id", id)
                .getResultList()
                .isEmpty();
        if (!existe) {
            throw new RecursoNoEncontradoException(mensajeSiNoExiste);
        }
    }
}
