package ar.dentaflow.aplicacion.recursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec gestion-recursos — "Duplicado que llega a la base" (design D4). Los servicios se arman con
 * repositorios reales cuyo listado devuelve vacío, así la verificación previa no ve al otro recurso y la
 * constraint UNIQUE de la base es la que rechaza; el servicio la debe traducir a {@link ConflictoException}.
 * Las subclases eligen el motor (H2 en modo MySQL o MySQL 8 real). Nombres únicos por ejecución y rollback
 * al terminar cada prueba.
 */
@Transactional
abstract class RespaldoDuplicadosRecursosBase {

    @Autowired
    private SillonRepositorio sillonesReales;

    @Autowired
    private PrestacionRepositorio prestacionesReales;

    @Autowired
    private ProfesionalRepositorio profesionalesReales;

    @Autowired
    private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void altaDeSillonDuplicadaEsConflicto() {
        ServicioDeSillones servicio = new ServicioDeSillones(sillonesSinListado());
        servicio.crear("Sillón " + sufijo);

        assertThatThrownBy(() -> servicio.crear("Sillón " + sufijo))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(ServicioDeSillones.NOMBRE_EN_USO);
    }

    @Test
    void edicionDeSillonQueChocaConOtroEsConflicto() {
        ServicioDeSillones servicio = new ServicioDeSillones(sillonesSinListado());
        servicio.crear("Uno " + sufijo);
        Sillon dos = servicio.crear("Dos " + sufijo);

        assertThatThrownBy(() -> servicio.renombrar(dos.getId(), "Uno " + sufijo))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(ServicioDeSillones.NOMBRE_EN_USO);
    }

    @Test
    void altaDePrestacionDuplicadaEsConflicto() {
        ServicioDePrestaciones servicio = new ServicioDePrestaciones(prestacionesSinListado());
        servicio.crear("Prestación " + sufijo, 30);

        assertThatThrownBy(() -> servicio.crear("Prestación " + sufijo, 45))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(ServicioDePrestaciones.NOMBRE_EN_USO);
    }

    @Test
    void altaDeProfesionalConEmailDuplicadoEsConflicto() {
        ServicioDeProfesionales servicio = new ServicioDeProfesionales(profesionalesSinListado(), prestacionesReales);
        servicio.crear("Ana", "Muestra", null, "ana-" + sufijo + "@example.com");

        assertThatThrownBy(() -> servicio.crear("Otra", "Persona", null, "ana-" + sufijo + "@example.com"))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(ServicioDeProfesionales.EMAIL_EN_USO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profesional WHERE email = ?", Long.class,
                "ana-" + sufijo + "@example.com")).isEqualTo(1L);
    }

    @Test
    void altaDeProfesionalConMatriculaDuplicadaEsConflicto() {
        ServicioDeProfesionales servicio = new ServicioDeProfesionales(profesionalesSinListado(), prestacionesReales);
        servicio.crear("Ana", "Muestra", "MP-" + sufijo, "ana-" + sufijo + "@example.com");

        assertThatThrownBy(() -> servicio.crear("Otra", "Persona", "MP-" + sufijo, "otra-" + sufijo + "@example.com"))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(ServicioDeProfesionales.MATRICULA_EN_USO);
    }

    private SillonRepositorio sillonesSinListado() {
        return new SillonRepositorio() {
            @Override
            public Sillon guardar(Sillon sillon) {
                return sillonesReales.guardar(sillon);
            }

            @Override
            public Optional<Sillon> buscarPorId(Long id) {
                return sillonesReales.buscarPorId(id);
            }

            @Override
            public List<Sillon> listarTodos() {
                return List.of();
            }

            @Override
            public List<Sillon> listarActivos() {
                return List.of();
            }
        };
    }

    private PrestacionRepositorio prestacionesSinListado() {
        return new PrestacionRepositorio() {
            @Override
            public Prestacion guardar(Prestacion prestacion) {
                return prestacionesReales.guardar(prestacion);
            }

            @Override
            public Optional<Prestacion> buscarPorId(Long id) {
                return prestacionesReales.buscarPorId(id);
            }

            @Override
            public List<Prestacion> listarTodas() {
                return List.of();
            }

            @Override
            public List<Prestacion> listarActivas() {
                return List.of();
            }
        };
    }

    private ProfesionalRepositorio profesionalesSinListado() {
        return new ProfesionalRepositorio() {
            @Override
            public Profesional guardar(Profesional profesional) {
                return profesionalesReales.guardar(profesional);
            }

            @Override
            public Optional<Profesional> buscarPorId(Long id) {
                return profesionalesReales.buscarPorId(id);
            }

            @Override
            public List<Profesional> listarTodos() {
                return List.of();
            }

            @Override
            public List<Profesional> listarActivos() {
                return List.of();
            }

            @Override
            public List<Profesional> listarActivosQueRealizan(Long prestacionId) {
                return List.of();
            }
        };
    }
}
