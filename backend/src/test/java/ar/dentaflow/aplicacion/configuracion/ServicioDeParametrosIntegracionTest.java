package ar.dentaflow.aplicacion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Spec parametros-consultorio — "Cambio visible de inmediato", sin reiniciar y sin caché. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServicioDeParametrosIntegracionTest {

    @Autowired
    private ServicioDeParametros servicio;

    @Autowired
    private ParametrosDelConsultorio lectura;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Test
    void laLecturaSiguienteVeElValorNuevoSinReiniciar() {
        Long admin = usuarios.crear("param-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();
        assertThat(lectura.anticipacionMinima()).isEqualTo(Duration.ofHours(24));

        servicio.actualizar("anticipacion-minima-horas", "48", admin);

        assertThat(lectura.anticipacionMinima()).isEqualTo(Duration.ofHours(48));
        assertThat(servicio.obtener("anticipacion-minima-horas").isProvisorio()).isFalse();
    }

    @Test
    void elAvisoSeLeeConSusValoresIniciales() {
        AvisoPrivacidad aviso = lectura.avisoPrivacidad();

        assertThat(aviso.leyendaAceptacion()).isEqualTo("Leí y acepto el aviso de privacidad");
        assertThat(aviso.texto()).contains("Ley 25.326");
    }
}
