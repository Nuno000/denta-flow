package ar.dentaflow.infraestructura.persistencia.usuarios;

import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Implementación Spring Data del puerto {@link UsuarioRepositorio}; sin {@code delete*} heredados. */
public interface UsuarioRepositorioJpa extends Repository<Usuario, Long>, UsuarioRepositorio {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByEmail(String email);

    @Query("select u from Usuario u order by u.id")
    List<Usuario> findTodosPorId();

    @Query("select count(u) > 0 from Usuario u where u.rol = :rol and u.activo = true")
    boolean existeActivoConRol(@Param("rol") Rol rol);

    boolean existsByProfesionalId(Long profesionalId);

    @Query("select u.profesionalId from Usuario u where u.profesionalId is not null")
    List<Long> findProfesionalIdsVinculados();

    @Override
    default Usuario guardar(Usuario usuario) {
        return save(usuario);
    }

    @Override
    default Optional<Usuario> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default Optional<Usuario> buscarPorEmail(String emailNormalizado) {
        return findByEmail(emailNormalizado);
    }

    @Override
    default List<Usuario> listarTodos() {
        return findTodosPorId();
    }

    @Override
    default boolean existeAdministradorActivo() {
        return existeActivoConRol(Rol.ADMINISTRADOR);
    }

    @Override
    default boolean existeUsuarioDelProfesional(Long profesionalId) {
        return existsByProfesionalId(profesionalId);
    }

    @Override
    default Set<Long> idsDeProfesionalesConUsuario() {
        return new HashSet<>(findProfesionalIdsVinculados());
    }
}
