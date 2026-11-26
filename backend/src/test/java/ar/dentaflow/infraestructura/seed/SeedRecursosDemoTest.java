package ar.dentaflow.infraestructura.seed;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Spec recursos-consultorio — "Datos de demostración". Usa su propia base en memoria porque el
 * {@code ApplicationRunner} corre (y confirma) al arrancar el contexto; cada prueba parte de una
 * base vacía y revierte lo que haga.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SeedRecursosDemo.class)
@TestPropertySource(properties = {
        "app.seed.demo=true",
        "spring.datasource.url=jdbc:h2:mem:seed_demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class SeedRecursosDemoTest {

    @Autowired
    private SeedRecursosDemo seed;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private PrestacionRepositorio prestaciones;

    @Autowired
    private HorarioAtencionRepositorio horarios;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private int filas(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    @Test
    void alArrancarConLaPropiedadEnTrueLaBaseVaciaQuedaSembrada() {
        assertThat(filas("profesional")).isEqualTo(3);
        assertThat(filas("sillon")).isEqualTo(2);
        assertThat(filas("prestacion")).isEqualTo(5);
        assertThat(filas("horario_atencion")).isEqualTo(30);
    }

    private void vaciarTablas() {
        jdbc.update("DELETE FROM bloqueo");
        jdbc.update("DELETE FROM horario_atencion");
        jdbc.update("DELETE FROM profesional_prestacion");
        jdbc.update("DELETE FROM profesional");
        jdbc.update("DELETE FROM sillon");
        jdbc.update("DELETE FROM prestacion");
        em.clear();
    }

    @Nested
    class SobreBaseVacia {

        @BeforeEach
        void baseVacia() {
            vaciarTablas();
        }

        @Test
        void creaTresProfesionalesDosSillonesYCincoPrestacionesActivos() {
            seed.run(null);
            em.flush();

            assertThat(profesionales.listarActivos()).hasSize(3);
            assertThat(sillones.listarActivos()).hasSize(2);
            assertThat(prestaciones.listarActivas()).hasSize(5);
            assertThat(profesionales.listarTodos()).hasSize(3);
        }

        @Test
        void lasPrestacionesTienenCincoDuracionesDistintas() {
            seed.run(null);

            assertThat(prestaciones.listarActivas().stream().map(Prestacion::getDuracionMinutos)
                    .collect(Collectors.toSet())).hasSize(5);
        }

        @Test
        void cadaProfesionalRealizaAlMenosUnaPrestacionYUsaEmailDeEjemplo() {
            seed.run(null);

            List<Profesional> todos = profesionales.listarActivos();
            assertThat(todos).allSatisfy(profesional -> {
                assertThat(profesional.prestacionIds()).isNotEmpty();
                assertThat(profesional.getEmail()).endsWith("@example.com");
            });
        }

        @Test
        void unaPrestacionLaRealizaMasDeUnProfesionalYOtraSoloUno() {
            seed.run(null);
            em.flush();
            em.clear();

            Prestacion consulta = prestaciones.listarActivas().stream()
                    .filter(p -> p.getNombre().startsWith("Consulta")).findFirst().orElseThrow();
            Prestacion blanqueamiento = prestaciones.listarActivas().stream()
                    .filter(p -> p.getNombre().startsWith("Blanqueamiento")).findFirst().orElseThrow();

            assertThat(profesionales.listarActivosQueRealizan(consulta.getId())).hasSize(3);
            assertThat(profesionales.listarActivosQueRealizan(blanqueamiento.getId())).hasSize(2);
        }

        @Test
        void correrDosVecesNoDuplicaNada() {
            seed.run(null);
            seed.run(null);

            assertThat(filas("profesional")).isEqualTo(3);
            assertThat(filas("sillon")).isEqualTo(2);
            assertThat(filas("prestacion")).isEqualTo(5);
            assertThat(filas("horario_atencion")).isEqualTo(30);
        }

        @Test
        void cadaProfesionalDemoAtiendeDeLunesAViernesMananaYTarde() {
            seed.run(null);
            em.flush();
            em.clear();

            List<String> esperado = new ArrayList<>();
            for (DayOfWeek dia : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
                esperado.add(dia + " 09:00-13:00");
                esperado.add(dia + " 14:00-18:00");
            }
            assertThat(profesionales.listarTodos()).hasSize(3).allSatisfy(profesional ->
                    assertThat(horarios.listarDelProfesional(profesional.getId()))
                            .extracting(SeedRecursosDemoTest::rango)
                            .containsExactlyElementsOf(esperado));
        }

        @Test
        void completaLosHorariosDeProfesionalesDemoSembradosAntesDeC06() {
            seed.run(null);
            em.flush();
            jdbc.update("DELETE FROM horario_atencion");
            em.clear();

            seed.run(null);
            em.flush();

            assertThat(filas("horario_atencion")).isEqualTo(30);
        }

        @Test
        void noAgregaHorariosAProfesionalesQueNoSonDeDemostracion() {
            Profesional propio = profesionales.guardar(
                    Profesional.nuevo("Ana", "Propia", null, "ana.propia@example.com"));
            em.flush();

            seed.run(null);
            em.flush();

            assertThat(horarios.listarDelProfesional(propio.getId())).isEmpty();
            assertThat(filas("horario_atencion")).isZero();
        }

        @Test
        void conUnSillonPreexistenteNoCreaNadaNiLoModifica() {
            sillones.guardar(Sillon.nuevo("Sillón propio"));
            em.flush();

            seed.run(null);
            em.flush();
            em.clear();

            assertThat(filas("profesional")).isZero();
            assertThat(filas("prestacion")).isZero();
            assertThat(sillones.listarTodos()).extracting(Sillon::getNombre).containsExactly("Sillón propio");
        }
    }

    private static String rango(HorarioAtencion horario) {
        return horario.getDia() + " " + hora(horario.getInicio()) + "-" + hora(horario.getFin());
    }

    private static String hora(LocalTime hora) {
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }
}
