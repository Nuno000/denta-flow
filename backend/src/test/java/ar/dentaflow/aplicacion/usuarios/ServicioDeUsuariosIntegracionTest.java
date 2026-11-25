package ar.dentaflow.aplicacion.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.usuarios.Rol;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Spec gestion-usuarios — "Email único" y "Blanqueo de contraseña", contra la base H2 real. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServicioDeUsuariosIntegracionTest {

    @Autowired
    private ServicioDeUsuarios servicio;

    @Autowired
    private ServicioDeIngreso ingreso;

    @Autowired
    private JdbcTemplate jdbc;

    private final String base = "integracion-" + UUID.randomUUID().toString().substring(0, 8);

    @Test
    void dosAltasConElMismoEmailEnDistintoCasoDejanUnaSolaFila() {
        servicio.crear(base + "@example.com", "contrasena-valida", Rol.RECEPCION, null);

        assertThatThrownBy(() -> servicio.crear(base.toUpperCase() + "@EXAMPLE.com", "contrasena-valida",
                Rol.ADMINISTRADOR, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("El email ya está en uso");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email = ?", Long.class,
                base + "@example.com")).isEqualTo(1L);
    }

    @Test
    void trasElBlanqueoIngresaConLaContrasenaNuevaYNoConLaAnterior() {
        Long id = servicio.crear(base + "@example.com", "contrasena-anterior", Rol.RECEPCION, null).usuario().getId();

        servicio.blanquearContrasena(-1L, id, "contrasena-nueva");

        assertThat(ingreso.ingresar(base + "@example.com", "contrasena-nueva").usuario().getId()).isEqualTo(id);
        assertThatThrownBy(() -> ingreso.ingresar(base + "@example.com", "contrasena-anterior"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
