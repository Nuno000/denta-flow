package ar.dentaflow.dominio.configuracion;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de parámetros. Sin borrado ni alta: las filas las crea la migración. */
public interface ParametroRepositorio {

    Optional<Parametro> buscar(ClaveParametro clave);

    /** Todas las filas existentes, sin un orden garantizado. */
    List<Parametro> listarTodos();

    Parametro guardar(Parametro parametro);
}
