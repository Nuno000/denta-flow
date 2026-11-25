package ar.dentaflow.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Spec configuracion-entorno — "Pruebas contra MySQL real separadas". Solo corre con -Pit-mysql. */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class ConexionMySqlTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void seConectaAMySql8Real() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            DatabaseMetaData metadatos = conexion.getMetaData();
            assertThat(conexion.isValid(2)).isTrue();
            assertThat(metadatos.getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(metadatos.getDatabaseMajorVersion()).isEqualTo(8);
        }
    }
}
