package ar.dentaflow.infraestructura.persistencia.listaespera;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListaEsperaRepositorioTest {

    @Autowired
    private ListaEsperaRepositorio repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    private long usuarioId;
    private long pacienteId1;
    private long pacienteId2;
    private long prestacionId;
    private long profesionalId1;
    private long profesionalId2;
    private long turnoId;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES (?, 'x', 'recepcion')",
                "rec-" + sufijo + "@example.com");
        usuarioId = jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class,
                "rec-" + sufijo + "@example.com");

        jdbc.update("INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en) VALUES (?, 'Ana', 'Gomez', '1122334455', ?, CURRENT_TIMESTAMP)",
                "30111" + sufijo.substring(0, 3), "ana-" + sufijo + "@example.com");
        pacienteId1 = jdbc.queryForObject("SELECT id FROM paciente WHERE email = ?", Long.class,
                "ana-" + sufijo + "@example.com");

        jdbc.update("INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en) VALUES (?, 'Luis', 'Paz', '1122334466', ?, CURRENT_TIMESTAMP)",
                "30222" + sufijo.substring(0, 3), "luis-" + sufijo + "@example.com");
        pacienteId2 = jdbc.queryForObject("SELECT id FROM paciente WHERE email = ?", Long.class,
                "luis-" + sufijo + "@example.com");

        jdbc.update("INSERT INTO prestacion (nombre, duracion_minutos, activa) VALUES (?, 30, TRUE)",
                "Limpieza " + sufijo);
        prestacionId = jdbc.queryForObject("SELECT id FROM prestacion WHERE nombre = ?", Long.class,
                "Limpieza " + sufijo);

        jdbc.update("INSERT INTO profesional (nombre, apellido, matricula, email, activo) VALUES ('Maria', 'Lopez', ?, ?, TRUE)",
                "MAT1-" + sufijo, "maria-" + sufijo + "@example.com");
        profesionalId1 = jdbc.queryForObject("SELECT id FROM profesional WHERE matricula = ?", Long.class,
                "MAT1-" + sufijo);

        jdbc.update("INSERT INTO profesional (nombre, apellido, matricula, email, activo) VALUES ('Juan', 'Perez', ?, ?, TRUE)",
                "MAT2-" + sufijo, "juan-" + sufijo + "@example.com");
        profesionalId2 = jdbc.queryForObject("SELECT id FROM profesional WHERE matricula = ?", Long.class,
                "MAT2-" + sufijo);

        jdbc.update("INSERT INTO sillon (nombre, activo) VALUES (?, TRUE)", "Sillon " + sufijo);
        long sillonId = jdbc.queryForObject("SELECT id FROM sillon WHERE nombre = ?", Long.class,
                "Sillon " + sufijo);

        jdbc.update("""
                INSERT INTO turno (paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin, estado, origen, es_sobreturno, creado_en, token_acceso)
                VALUES (?, ?, ?, ?, TIMESTAMP '2026-12-02 13:00:00', TIMESTAMP '2026-12-02 13:30:00', 'cancelado', 'online', FALSE, CURRENT_TIMESTAMP, ?)
                """, pacienteId1, profesionalId1, sillonId, prestacionId, "tok-turno-" + sufijo);
        turnoId = jdbc.queryForObject("SELECT id FROM turno WHERE token_acceso = ?", Long.class,
                "tok-turno-" + sufijo);
    }

    @Test
    void buscaCompatiblesYVenceOtrasOfertasYExpiradas() {
        Instant ahora = Instant.parse("2026-12-01T12:00:00Z");
        ListaEspera sinPreferencia = repositorio.guardar(ListaEspera.nueva(
                pacienteId1, prestacionId, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                LocalTime.of(9, 0), LocalTime.of(13, 0), usuarioId, ahora));
        ListaEspera mismoProfesional = repositorio.guardar(ListaEspera.nueva(
                pacienteId2, prestacionId, profesionalId1,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                null, null, usuarioId, ahora.plusSeconds(10)));
        repositorio.guardar(ListaEspera.nueva(
                pacienteId2, prestacionId, profesionalId2,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                null, null, usuarioId, ahora.plusSeconds(20)));

        List<ListaEspera> compatibles = repositorio.buscarActivasCompatibles(
                prestacionId, profesionalId1, LocalDate.of(2026, 12, 2));
        assertThat(compatibles).extracting(ListaEspera::getId)
                .containsExactly(sinPreferencia.getId(), mismoProfesional.getId());

        Instant inicioHueco = Instant.parse("2026-12-02T13:00:00Z");
        sinPreferencia.ofrecer(turnoId, profesionalId1, inicioHueco, "tok-a", ahora, ahora.plusSeconds(3600));
        mismoProfesional.ofrecer(turnoId, profesionalId1, inicioHueco, "tok-b", ahora, ahora.plusSeconds(3600));
        repositorio.guardar(sinPreferencia);
        repositorio.guardar(mismoProfesional);

        assertThat(repositorio.buscarPorTokenParaActualizar("tok-a")).isPresent();

        int vencidas = repositorio.vencerOtrasOfertasDelHueco(profesionalId1, inicioHueco, sinPreferencia.getId());
        assertThat(vencidas).isEqualTo(1);
        assertThat(repositorio.buscarPorId(mismoProfesional.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoListaEspera.VENCIDA);

        int expiradas = repositorio.vencerExpiradas(ahora.plusSeconds(7200));
        assertThat(expiradas).isEqualTo(1);
        assertThat(repositorio.buscarPorId(sinPreferencia.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoListaEspera.VENCIDA);
    }
}
