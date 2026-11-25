package ar.dentaflow.infraestructura.persistencia.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.configuracion.ClaveParametro;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Spec parametros-consultorio — "Catálogo y base coinciden": ni claves de más ni de menos. */
@SpringBootTest
@ActiveProfiles("test")
class CatalogoDeParametrosConsistenciaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lasClavesDeLaTablaSonExactamenteLasDelCatalogo() {
        List<String> enLaBase = jdbc.queryForList("SELECT clave FROM parametro", String.class);
        List<String> enElCatalogo = Arrays.stream(ClaveParametro.values()).map(ClaveParametro::codigo).toList();

        assertThat(enLaBase).containsExactlyInAnyOrderElementsOf(enElCatalogo);
    }
}
