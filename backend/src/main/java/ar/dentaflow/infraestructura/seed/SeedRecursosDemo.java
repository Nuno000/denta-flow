package ar.dentaflow.infraestructura.seed;

import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de demostración de los recursos del consultorio (C-03, design D9). Solo existe cuando
 * {@code app.seed.demo=true} (perfil {@code dev}, o {@code APP_SEED_DEMO=true} en una demo): nunca
 * corre en {@code test} ni en {@code it-mysql}. Es idempotente: si ya hay algún profesional,
 * sillón o prestación, no hace nada. Todos los datos son ficticios (dominio {@code example.com}).
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class SeedRecursosDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SeedRecursosDemo.class);

    private final ProfesionalRepositorio profesionales;
    private final SillonRepositorio sillones;
    private final PrestacionRepositorio prestaciones;

    public SeedRecursosDemo(ProfesionalRepositorio profesionales, SillonRepositorio sillones,
            PrestacionRepositorio prestaciones) {
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.prestaciones = prestaciones;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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

        crearProfesional("Laura", "Ejemplo", "MP-0001", "laura.ejemplo@example.com",
                consulta, limpieza, obturacion, extraccion, blanqueamiento);
        crearProfesional("Martín", "Prueba", null, "martin.prueba@example.com",
                consulta, limpieza, obturacion);
        crearProfesional("Sofía", "Demo", "MP-0003", "sofia.demo@example.com",
                consulta, extraccion, blanqueamiento);

        LOG.info("Seed de recursos cargado: 3 profesionales, 2 sillones y 5 prestaciones de demostración");
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
