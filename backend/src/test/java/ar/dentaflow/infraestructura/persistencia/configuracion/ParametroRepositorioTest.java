package ar.dentaflow.infraestructura.persistencia.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.configuracion.ClaveParametro;
import ar.dentaflow.dominio.configuracion.Parametro;
import ar.dentaflow.dominio.configuracion.ParametroRepositorio;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Spec parametros-consultorio — ida y vuelta con instante UTC y guardado de la fila completa. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParametroRepositorioTest {

    @Autowired
    private ParametroRepositorio parametros;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private long usuarioNuevo() {
        String email = "repo-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES (?, 'x', 'administrador')", email);
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    @Test
    void buscaLaFilaProvisoriaDeLaAnticipacion() {
        Parametro parametro = parametros.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS).orElseThrow();

        assertThat(parametro.valorEntero()).isEqualTo(24);
        assertThat(parametro.isProvisorio()).isTrue();
        assertThat(parametro.actualizadoEn()).isNull();
        assertThat(parametro.actualizadoPor()).isNull();
    }

    @Test
    void listaLasTresFilas() {
        assertThat(parametros.listarTodos()).extracting(Parametro::clave)
                .containsExactlyInAnyOrder(ClaveParametro.values());
    }

    @Test
    void guardarYRelerDevuelveValorMarcaAutorEInstanteExactos() {
        long autor = usuarioNuevo();
        Instant instante = Instant.parse("2026-12-02T00:30:00Z");
        Parametro parametro = parametros.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS).orElseThrow();

        parametro.actualizar("48", autor, instante);
        parametros.guardar(parametro);
        em.flush();
        em.clear();

        Parametro leido = parametros.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS).orElseThrow();
        assertThat(leido.valorEntero()).isEqualTo(48);
        assertThat(leido.isProvisorio()).isFalse();
        assertThat(leido.actualizadoPor()).isEqualTo(autor);
        assertThat(leido.actualizadoEn()).isEqualTo(instante);
        assertThat(parametros.buscar(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA).orElseThrow().isProvisorio())
                .isTrue();
    }
}
