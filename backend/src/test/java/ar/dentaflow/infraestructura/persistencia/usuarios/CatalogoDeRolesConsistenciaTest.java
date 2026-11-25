package ar.dentaflow.infraestructura.persistencia.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.usuarios.CatalogoDeRoles;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.RolDisponible;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec gestion-usuarios — "Catálogo de roles". El enum {@link Rol} y la tabla {@code rol} deben
 * coincidir: si alguien agrega un rol en un solo lado, este test falla en CI (design D4).
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogoDeRolesConsistenciaTest {

    @Autowired
    private CatalogoDeRoles catalogo;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listaLosTresRolesConSuNombreVisibleEnOrdenFijo() {
        assertThat(catalogo.listar()).containsExactly(
                new RolDisponible(Rol.ADMINISTRADOR, "Administrador"),
                new RolDisponible(Rol.RECEPCION, "Recepción"),
                new RolDisponible(Rol.PROFESIONAL, "Profesional"));
    }

    @Test
    void lasFilasDeLaTablaSonExactamenteLosCodigosDelEnum() {
        assertThat(jdbc.queryForList("SELECT codigo FROM rol", String.class))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(Rol.values()).map(Rol::codigo).toList());
    }
}
