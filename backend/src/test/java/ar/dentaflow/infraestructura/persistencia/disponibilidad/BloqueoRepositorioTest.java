package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Mapeo de {@code bloqueo}: ida y vuelta de instantes UTC y enums, y consultas por superposición y
 * por recurso afectado. Usa un rango de fechas propio (año 2031) para no depender de otras filas.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BloqueoRepositorioTest {

    private static final Instant AHORA = Instant.parse("2026-12-02T00:30:00Z");

    @Autowired
    private BloqueoRepositorio bloqueos;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private Long a;
    private Long b;
    private Long sillon;
    private Long admin;

    @BeforeEach
    void crearDatos() {
        a = profesionales.guardar(Profesional.nuevo("Laura", "Ejemplo", null, "laura.b@example.com")).getId();
        b = profesionales.guardar(Profesional.nuevo("Martín", "Prueba", null, "martin.b@example.com")).getId();
        sillon = sillones.guardar(Sillon.nuevo("Sillón bloqueos")).getId();
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES ('admin.b@example.com', 'x', 'administrador')");
        admin = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'admin.b@example.com'", Long.class);
    }

    private static Instant h(String hora) {
        return Instant.parse("2031-03-10T" + hora + ":00Z");
    }

    private Bloqueo deProfesional(Long id, String inicio, String fin) {
        return bloqueos.guardar(Bloqueo.deProfesional(id, h(inicio), h(fin), MotivoBloqueo.AUSENCIA, null, admin, AHORA));
    }

    private Bloqueo deSillon(String inicio, String fin) {
        return bloqueos.guardar(Bloqueo.deSillon(sillon, h(inicio), h(fin), MotivoBloqueo.OTRO, null, admin, AHORA));
    }

    private Bloqueo delConsultorio(String inicio, String fin) {
        return bloqueos.guardar(Bloqueo.delConsultorio(h(inicio), h(fin), MotivoBloqueo.FERIADO, null, admin, AHORA));
    }

    private void limpiar() {
        em.flush();
        em.clear();
    }

    @Test
    void idaYVueltaConservaInstanteExactoYEnums() {
        Instant inicio = Instant.parse("2026-12-24T11:00:00.123456Z");
        Long id = bloqueos.guardar(Bloqueo.deSillon(sillon, inicio, inicio.plusSeconds(3600),
                MotivoBloqueo.OTRO, "Mantenimiento", admin, AHORA)).getId();
        limpiar();

        Bloqueo leido = bloqueos.buscarPorId(id).orElseThrow();

        assertThat(leido.getInicio()).isEqualTo(inicio);
        assertThat(leido.getAmbito()).isEqualTo(AmbitoBloqueo.SILLON);
        assertThat(leido.getSillonId()).isEqualTo(sillon);
        assertThat(leido.getMotivo()).isEqualTo(MotivoBloqueo.OTRO);
        assertThat(leido.getDetalle()).isEqualTo("Mantenimiento");
        assertThat(leido.getModificadoPor()).isEqualTo(admin);
        assertThat(leido.getModificadoEn()).isEqualTo(AHORA);
        assertThat(jdbc.queryForObject("SELECT inicio FROM bloqueo WHERE id = ?", Timestamp.class, id).toInstant())
                .as("guardado en UTC").isEqualTo(inicio);
        assertThat(jdbc.queryForObject("SELECT ambito FROM bloqueo WHERE id = ?", String.class, id)).isEqualTo("SILLON");
    }

    @Test
    void superposicionConElRangoYOrdenPorInicio() {
        Bloqueo diezADoce = deProfesional(a, "10:00", "12:00");
        deProfesional(a, "08:00", "10:00");
        Bloqueo nueveYMedia = delConsultorio("09:30", "10:30");
        limpiar();

        List<Bloqueo> encontrados = bloqueos.listarQueSeSuperponen(h("10:00"), h("11:00"), null, null);

        assertThat(encontrados).extracting(Bloqueo::getId).containsExactly(nueveYMedia.getId(), diezADoce.getId());
    }

    @Test
    void filtrosPorProfesionalYPorSillon() {
        Bloqueo deA = deProfesional(a, "10:00", "12:00");
        deProfesional(b, "10:00", "12:00");
        Bloqueo delSillon = deSillon("10:00", "12:00");
        delConsultorio("10:00", "12:00");
        limpiar();

        assertThat(bloqueos.listarQueSeSuperponen(h("09:00"), h("13:00"), a, null))
                .extracting(Bloqueo::getId).containsExactly(deA.getId());
        assertThat(bloqueos.listarQueSeSuperponen(h("09:00"), h("13:00"), null, sillon))
                .extracting(Bloqueo::getId).containsExactly(delSillon.getId());
    }

    @Test
    void bloqueosQueAfectanAUnRecursoIncluyenLosDelConsultorio() {
        Bloqueo deA = deProfesional(a, "10:00", "12:00");
        deProfesional(b, "10:00", "12:00");
        Bloqueo delSillon = deSillon("10:00", "12:00");
        Bloqueo general = delConsultorio("11:00", "13:00");
        deProfesional(a, "14:00", "15:00");
        limpiar();

        assertThat(bloqueos.listarQueAfectanAProfesional(a, h("09:00"), h("13:00")))
                .extracting(Bloqueo::getId).containsExactly(deA.getId(), general.getId());
        assertThat(bloqueos.listarQueAfectanASillon(sillon, h("09:00"), h("13:00")))
                .extracting(Bloqueo::getId).containsExactly(delSillon.getId(), general.getId());
    }

    @Test
    void eliminarBorraElBloqueo() {
        Bloqueo bloqueo = delConsultorio("10:00", "12:00");
        em.flush();

        bloqueos.eliminar(bloqueo);
        limpiar();

        assertThat(bloqueos.buscarPorId(bloqueo.getId())).isEmpty();
    }
}
