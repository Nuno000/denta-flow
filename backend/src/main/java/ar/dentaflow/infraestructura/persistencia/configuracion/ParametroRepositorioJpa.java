package ar.dentaflow.infraestructura.persistencia.configuracion;

import ar.dentaflow.dominio.configuracion.ClaveParametro;
import ar.dentaflow.dominio.configuracion.Parametro;
import ar.dentaflow.dominio.configuracion.ParametroRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Implementación Spring Data del puerto {@link ParametroRepositorio}; sin {@code delete*} heredados. */
public interface ParametroRepositorioJpa extends Repository<Parametro, String>, ParametroRepositorio {

    Parametro save(Parametro parametro);

    Optional<Parametro> findById(String clave);

    List<Parametro> findAll();

    @Override
    default Optional<Parametro> buscar(ClaveParametro clave) {
        return findById(clave.codigo());
    }

    @Override
    default List<Parametro> listarTodos() {
        return findAll();
    }

    @Override
    default Parametro guardar(Parametro parametro) {
        return save(parametro);
    }
}
