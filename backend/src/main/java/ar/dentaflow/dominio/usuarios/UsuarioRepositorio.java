package ar.dentaflow.dominio.usuarios;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Puerto de persistencia de usuarios. Sin borrado: los usuarios se desactivan. */
public interface UsuarioRepositorio {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    /** Busca por el email ya normalizado (ver {@link Usuario#normalizarEmail(String)}). */
    Optional<Usuario> buscarPorEmail(String emailNormalizado);

    /** Todos, activos e inactivos, por id ascendente. */
    List<Usuario> listarTodos();

    boolean existeAdministradorActivo();

    boolean existeUsuarioDelProfesional(Long profesionalId);

    /** Ids de los profesionales que ya tienen un usuario, activo o inactivo. */
    Set<Long> idsDeProfesionalesConUsuario();
}
