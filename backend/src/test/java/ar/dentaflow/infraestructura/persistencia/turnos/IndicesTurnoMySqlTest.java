package ar.dentaflow.infraestructura.persistencia.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec turnos — "Integridad de turnos en la base" (índices de solapamiento, DD-09): lee
 * {@code information_schema.statistics} de MySQL 8 real y exige las columnas en el orden de igualdad primero y
 * rango después.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class IndicesTurnoMySqlTest {

    @Autowired
    private JdbcTemplate jdbc;

    private List<String> columnasDe(String indice) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'turno' AND index_name = ? "
                + "ORDER BY seq_in_index", String.class, indice);
    }

    @Test
    void indiceDeSolapamientoDelProfesional() {
        assertThat(columnasDe("ix_turno_profesional_solapamiento"))
                .containsExactly("profesional_id", "estado", "inicio", "fin");
    }

    @Test
    void indiceDeSolapamientoDelSillon() {
        assertThat(columnasDe("ix_turno_sillon_solapamiento")).containsExactly("sillon_id", "estado", "inicio", "fin");
    }

    @Test
    void indicesDePacienteYDeEstadoConInicio() {
        assertThat(columnasDe("ix_turno_paciente")).containsExactly("paciente_id");
        assertThat(columnasDe("ix_turno_estado_inicio")).containsExactly("estado", "inicio");
    }
}
