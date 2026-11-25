package ar.dentaflow.infraestructura.persistencia.usuarios;

import ar.dentaflow.dominio.usuarios.CatalogoDeRoles;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.RolDisponible;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lee el catálogo {@code rol} (solo lectura) y lo ordena como el enum: administrador, recepción, profesional. */
@Repository
public class CatalogoDeRolesJdbc implements CatalogoDeRoles {

    private final JdbcTemplate jdbc;

    public CatalogoDeRolesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RolDisponible> listar() {
        return jdbc.query("SELECT codigo, nombre FROM rol",
                        (fila, n) -> new RolDisponible(Rol.desdeCodigo(fila.getString("codigo")), fila.getString("nombre")))
                .stream()
                .sorted(Comparator.comparing(RolDisponible::rol))
                .toList();
    }
}
