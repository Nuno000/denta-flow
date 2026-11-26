package ar.dentaflow.presentacion.publico;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec reserva-online — "Catálogo público para reservar" (design D3): prestaciones activas y profesionales activos
 * que realizan una prestación, sin token y sin emails, matrículas ni sillones. Revierte todo al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogoPublicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private long limpieza;
    private long ortodoncia;
    private long ana;

    private long prestacion(String nombre, int minutos, boolean activa) {
        jdbc.update("INSERT INTO prestacion (nombre, duracion_minutos, activa) VALUES (?, ?, ?)", nombre, minutos, activa);
        return jdbc.queryForObject("SELECT id FROM prestacion WHERE nombre = ?", Long.class, nombre);
    }

    private long profesional(String nombre, String apellido, boolean activo) {
        String email = nombre.toLowerCase() + "-" + sufijo + "@example.com";
        jdbc.update("INSERT INTO profesional (nombre, apellido, matricula, email, activo) VALUES (?, ?, ?, ?, ?)", nombre,
                apellido, "MP-" + nombre + sufijo, email, activo);
        return jdbc.queryForObject("SELECT id FROM profesional WHERE email = ?", Long.class, email);
    }

    private void realiza(long profesionalId, long prestacionId) {
        jdbc.update("INSERT INTO profesional_prestacion (profesional_id, prestacion_id) VALUES (?, ?)", profesionalId,
                prestacionId);
    }

    @BeforeEach
    void crearCatalogo() {
        limpieza = prestacion("Zz Limpieza " + sufijo, 30, true);
        prestacion("Zz Blanqueamiento " + sufijo, 60, true);
        ortodoncia = prestacion("Zz Ortodoncia " + sufijo, 45, false);
        ana = profesional("Ana", "Pérez", true);
        long luis = profesional("Luis", "Gómez", true);
        long bea = profesional("Bea", "Inactiva", false);
        realiza(ana, limpieza);
        realiza(bea, limpieza);
        realiza(luis, ortodoncia);
    }

    @Test
    void prestacionesActivasOrdenadasPorNombreConDuracionSinToken() throws Exception {
        String filtro = "$[?(@.nombre =~ /Zz .* " + sufijo + "/)]";
        mockMvc.perform(get("/api/publico/prestaciones"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath(filtro, hasSize(2)))
                .andExpect(jsonPath(filtro + ".nombre").value(
                        org.hamcrest.Matchers.contains("Zz Blanqueamiento " + sufijo, "Zz Limpieza " + sufijo)))
                .andExpect(jsonPath("$[?(@.id == " + limpieza + ")].duracionMinutos").value(
                        org.hamcrest.Matchers.contains(30)))
                .andExpect(jsonPath("$[?(@.id == " + limpieza + ")]", hasSize(1)))
                .andExpect(jsonPath("$[0]", aMapWithSize(3)));
    }

    @Test
    void profesionalesActivosQueRealizanLaPrestacionSoloConIdNombreYApellido() throws Exception {
        mockMvc.perform(get("/api/publico/profesionales").param("prestacionId", String.valueOf(limpieza)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ana))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[0].apellido").value("Pérez"))
                .andExpect(jsonPath("$[0]", aMapWithSize(3)))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].matricula").doesNotExist());
    }

    @Test
    void prestacionInactivaOInexistenteDa404() throws Exception {
        mockMvc.perform(get("/api/publico/profesionales").param("prestacionId", String.valueOf(ortodoncia)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        mockMvc.perform(get("/api/publico/profesionales").param("prestacionId", String.valueOf(Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinPrestacionDa400() throws Exception {
        mockMvc.perform(get("/api/publico/profesionales"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/publico/profesionales").param("prestacionId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaPrestacionSinProfesionalesDevuelveListaVacia() throws Exception {
        long sinNadie = prestacion("Zz Sin profesionales " + sufijo, 20, true);

        mockMvc.perform(get("/api/publico/profesionales").param("prestacionId", String.valueOf(sinNadie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void laDocumentacionListaLosEndpointsBajoReservaPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/publico/prestaciones'].get.tags[0]").value("Reserva pública"))
                .andExpect(jsonPath("$.paths['/api/publico/profesionales'].get.tags[0]").value("Reserva pública"))
                .andExpect(jsonPath("$.paths['/api/publico/contacto'].get.tags[0]").value("Reserva pública"));
    }
}
