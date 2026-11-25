package ar.dentaflow.infraestructura.persistencia.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Spec gestion-usuarios — persistencia de usuarios (design D4 y D5). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositorioTest {

    private static final String HASH = "$2a$10$hashDePrueba";

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private EntityManager em;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    private String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    private Long profesionalNuevo(String base) {
        return profesionales.guardar(Profesional.nuevo("Nombre", "Apellido", null, email(base))).getId();
    }

    @Test
    void guardaYRecuperaPorIdConElRolComoEnum() {
        Long profesionalId = profesionalNuevo("prof");
        Usuario guardado = usuarios.guardar(Usuario.nuevo(email("p"), HASH, Rol.PROFESIONAL, profesionalId));
        sincronizar();

        Usuario leido = usuarios.buscarPorId(guardado.getId()).orElseThrow();

        assertThat(leido.getEmail()).isEqualTo(email("p"));
        assertThat(leido.getCredencialHash()).isEqualTo(HASH);
        assertThat(leido.getRol()).isEqualTo(Rol.PROFESIONAL);
        assertThat(leido.getProfesionalId()).isEqualTo(profesionalId);
        assertThat(leido.isActivo()).isTrue();
        assertThat(usuarios.buscarPorId(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void buscaPorEmailNormalizado() {
        usuarios.guardar(Usuario.nuevo("  " + email("Recepcion").toUpperCase() + " ", HASH, Rol.RECEPCION, null));
        sincronizar();

        assertThat(usuarios.buscarPorEmail(email("recepcion"))).isPresent();
        assertThat(usuarios.buscarPorEmail(email("otro"))).isEmpty();
    }

    @Test
    void listarTodosIncluyeLosInactivosPorIdAscendente() {
        Usuario uno = usuarios.guardar(Usuario.nuevo(email("uno"), HASH, Rol.RECEPCION, null));
        Usuario dos = Usuario.nuevo(email("dos"), HASH, Rol.RECEPCION, null);
        dos.desactivar();
        dos = usuarios.guardar(dos);
        sincronizar();

        assertThat(usuarios.listarTodos()).extracting(Usuario::getId)
                .containsSubsequence(uno.getId(), dos.getId());
        assertThat(usuarios.buscarPorId(dos.getId()).orElseThrow().isActivo()).isFalse();
    }

    @Test
    void existeAdministradorActivoIgnoraALosInactivos() {
        boolean habiaAntes = usuarios.existeAdministradorActivo();
        Usuario inactivo = Usuario.nuevo(email("admin-inactivo"), HASH, Rol.ADMINISTRADOR, null);
        inactivo.desactivar();
        usuarios.guardar(inactivo);
        sincronizar();
        assertThat(usuarios.existeAdministradorActivo()).isEqualTo(habiaAntes);

        usuarios.guardar(Usuario.nuevo(email("admin"), HASH, Rol.ADMINISTRADOR, null));
        sincronizar();
        assertThat(usuarios.existeAdministradorActivo()).isTrue();
    }

    @Test
    void sabeQueProfesionalesYaTienenUsuario() {
        Long conUsuario = profesionalNuevo("con");
        Long sinUsuario = profesionalNuevo("sin");
        usuarios.guardar(Usuario.nuevo(email("vinculado"), HASH, Rol.PROFESIONAL, conUsuario));
        sincronizar();

        assertThat(usuarios.existeUsuarioDelProfesional(conUsuario)).isTrue();
        assertThat(usuarios.existeUsuarioDelProfesional(sinUsuario)).isFalse();
        assertThat(usuarios.idsDeProfesionalesConUsuario()).contains(conUsuario).doesNotContain(sinUsuario);
    }
}
