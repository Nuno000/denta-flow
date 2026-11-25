package ar.dentaflow.infraestructura.persistencia.auditoria;

import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.HistorialDeTurnoRepositorio;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Implementación Spring Data del puerto {@link HistorialDeTurnoRepositorio}. Extiende {@link Repository}
 * y no {@code CrudRepository}: no hereda {@code delete*} ni {@code saveAll} (design D4).
 */
public interface HistorialDeTurnoRepositorioJpa extends Repository<CambioDeTurno, Long>, HistorialDeTurnoRepositorio {

    CambioDeTurno save(CambioDeTurno cambio);

    @Query("select c from CambioDeTurno c where c.turnoId = :turnoId order by c.ocurridoEn, c.id")
    List<CambioDeTurno> findHistorial(@Param("turnoId") long turnoId);

    @Override
    default CambioDeTurno agregar(CambioDeTurno cambio) {
        if (cambio.id() != null) {
            throw new IllegalArgumentException("El historial solo agrega registros nuevos; este ya tiene id "
                    + cambio.id());
        }
        // Valida el largo antes del INSERT: si lo detectara Hibernate al convertir, lo envolvería en una
        // JpaSystemException; así quien llama recibe la ValidacionDeDominioException (spec "Valores demasiado largos").
        ConversorValoresJson conversor = new ConversorValoresJson();
        conversor.convertToDatabaseColumn(cambio.valorAnterior());
        conversor.convertToDatabaseColumn(cambio.valorNuevo());
        return save(cambio);
    }

    @Override
    default List<CambioDeTurno> historialDe(long turnoId) {
        return findHistorial(turnoId);
    }
}
