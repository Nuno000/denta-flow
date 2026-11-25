package ar.dentaflow.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Spec configuracion-entorno — "Perfil de pruebas autocontenido". */
@SpringBootTest
@ActiveProfiles("test")
class PerfilTestTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void arrancaSinServiciosExternosSobreH2EnModoMySql() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            assertThat(conexion.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(conexion.getMetaData().getURL()).startsWith("jdbc:h2:mem:dentaflow");
            assertThat(configuracionH2(conexion, "MODE")).isEqualTo("MySQL");
            assertThat(configuracionH2(conexion, "DATABASE_TO_LOWER")).isEqualToIgnoringCase("TRUE");
        }
    }

    private static String configuracionH2(Connection conexion, String nombre) throws Exception {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME = ?")) {
            consulta.setString(1, nombre);
            try (ResultSet fila = consulta.executeQuery()) {
                return fila.next() ? fila.getString(1) : null;
            }
        }
    }
}
