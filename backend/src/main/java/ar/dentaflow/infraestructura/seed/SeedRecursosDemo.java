package ar.dentaflow.infraestructura.seed;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de demostración de los recursos del consultorio (C-03, design D9) y de sus horarios de
 * atención (C-06, design D9). Solo existe cuando {@code app.seed.demo=true} (perfil {@code dev}, o
 * {@code APP_SEED_DEMO=true} en una demo): nunca corre en {@code test} ni en {@code it-mysql}. Es
 * idempotente: si ya hay algún profesional, sillón o prestación, no crea recursos; y solo agrega
 * horarios a los profesionales de demostración que todavía no tienen ninguno (así una base
 * sembrada antes de C-06 también los recibe). Todos los datos son ficticios (dominio
 * {@code example.com}).
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class SeedRecursosDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SeedRecursosDemo.class);

    private static final String EMAIL_LAURA = "laura.ejemplo@example.com";
    private static final String EMAIL_MARTIN = "martin.prueba@example.com";
    private static final String EMAIL_SOFIA = "sofia.demo@example.com";
    private static final Set<String> EMAILS_DEMO = Set.of(EMAIL_LAURA, EMAIL_MARTIN, EMAIL_SOFIA);

    private static final List<DayOfWeek> DIAS_HABILES = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
    private static final List<LocalTime[]> RANGOS_DEL_DIA = List.of(
            new LocalTime[] {LocalTime.of(9, 0), LocalTime.of(13, 0)},
            new LocalTime[] {LocalTime.of(14, 0), LocalTime.of(18, 0)});

    private final ProfesionalRepositorio profesionales;
    private final SillonRepositorio sillones;
    private final PrestacionRepositorio prestaciones;
    private final HorarioAtencionRepositorio horarios;

    public SeedRecursosDemo(ProfesionalRepositorio profesionales, SillonRepositorio sillones,
            PrestacionRepositorio prestaciones, HorarioAtencionRepositorio horarios) {
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.prestaciones = prestaciones;
        this.horarios = horarios;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sembrarRecursos();
        sembrarHorarios();
    }

    private void sembrarRecursos() {
        if (hayRecursos()) {
            LOG.info("Seed de recursos omitido: ya hay profesionales, sillones o prestaciones cargados");
            return;
        }

        Prestacion consulta = prestaciones.guardar(Prestacion.nueva("Consulta y diagnóstico", 30));
        Prestacion limpieza = prestaciones.guardar(Prestacion.nueva("Limpieza (profilaxis)", 40));
        Prestacion obturacion = prestaciones.guardar(Prestacion.nueva("Obturación (arreglo)", 45));
        Prestacion extraccion = prestaciones.guardar(Prestacion.nueva("Extracción simple", 60));
        Prestacion blanqueamiento = prestaciones.guardar(Prestacion.nueva("Blanqueamiento", 90));

        sillones.guardar(Sillon.nuevo("Sillón 1"));
        sillones.guardar(Sillon.nuevo("Sillón 2"));

        crearProfesional("Laura", "Ejemplo", "MP-0001", EMAIL_LAURA,
                consulta, limpieza, obturacion, extraccion, blanqueamiento);
        crearProfesional("Martín", "Prueba", null, EMAIL_MARTIN,
                consulta, limpieza, obturacion);
        crearProfesional("Sofía", "Demo", "MP-0003", EMAIL_SOFIA,
                consulta, extraccion, blanqueamiento);

        LOG.info("Seed de recursos cargado: 3 profesionales, 2 sillones y 5 prestaciones de demostración");
    }

    /** Lunes a viernes 09:00–13:00 y 14:00–18:00 para cada profesional demo sin horario cargado. */
    private void sembrarHorarios() {
        int completados = 0;
        for (Profesional profesional : profesionales.listarTodos()) {
            if (!EMAILS_DEMO.contains(profesional.getEmail())
                    || !horarios.listarDelProfesional(profesional.getId()).isEmpty()) {
                continue;
            }
            for (DayOfWeek dia : DIAS_HABILES) {
                for (LocalTime[] rango : RANGOS_DEL_DIA) {
                    horarios.guardar(HorarioAtencion.nuevo(profesional.getId(), dia, rango[0], rango[1]));
                }
            }
            completados++;
        }
        if (completados > 0) {
            LOG.info("Seed de horarios cargado para {} profesionales de demostración", completados);
        }
    }

    private boolean hayRecursos() {
        return !profesionales.listarTodos().isEmpty()
                || !sillones.listarTodos().isEmpty()
                || !prestaciones.listarTodas().isEmpty();
    }

    private void crearProfesional(String nombre, String apellido, String matricula, String email,
            Prestacion... habilitadas) {
        Profesional profesional = Profesional.nuevo(nombre, apellido, matricula, email);
        for (Prestacion prestacion : habilitadas) {
            profesional.habilitarPrestacion(prestacion);
        }
        profesionales.guardar(profesional);
    }
}
