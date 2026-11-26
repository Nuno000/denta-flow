package ar.dentaflow.presentacion.recursos;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

/** Spec gestion-recursos sobre HTTP para prestaciones y sus duraciones (RN-TU-03, design D3). */
class PrestacionesControllerTest extends ApiDeRecursosTestBase {

    private String cuerpo(String nombre, String duracion) {
        return "{\"nombre\":\"" + nombre + "\",\"duracionMinutos\":" + duracion + "}";
    }

    private long crear(String nombre, int duracion) throws Exception {
        return id(mockMvc.perform(json(admin, post("/api/prestaciones"), cuerpo(nombre, String.valueOf(duracion))))
                .andExpect(status().isCreated()).andReturn());
    }

    @Test
    void elAdministradorDaDeAltaUnaPrestacion() throws Exception {
        mockMvc.perform(json(admin, post("/api/prestaciones"), cuerpo("Limpieza " + sufijo, "40")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/prestaciones/")))
                .andExpect(jsonPath("$.nombre").value("Limpieza " + sufijo))
                .andExpect(jsonPath("$.duracionMinutos").value(40))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void cambiarLaDuracionConservandoElNombre() throws Exception {
        long id = crear("Limpieza " + sufijo, 40);

        mockMvc.perform(json(admin, put("/api/prestaciones/" + id), cuerpo("Limpieza " + sufijo, "45")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-15", "null"})
    void duracionInvalidaEsDatoInvalidoYNoCambia(String duracion) throws Exception {
        long id = crear("Limpieza " + sufijo, 40);

        mockMvc.perform(json(admin, put("/api/prestaciones/" + id), cuerpo("Limpieza " + sufijo, duracion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionMinutos").exists());
        mockMvc.perform(json(admin, post("/api/prestaciones"), cuerpo("Otra " + sufijo, duracion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionMinutos").exists());
        mockMvc.perform(como(admin, get("/api/prestaciones/" + id)))
                .andExpect(jsonPath("$.duracionMinutos").value(40));
    }

    @Test
    void duracionQueNoEsNumeroEsDatoInvalido() throws Exception {
        mockMvc.perform(json(admin, post("/api/prestaciones"), cuerpo("Otra " + sufijo, "\"veinte\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void nombreRepetidoEsConflicto() throws Exception {
        crear("Limpieza " + sufijo, 40);

        mockMvc.perform(json(admin, post("/api/prestaciones"), cuerpo("LIMPIEZA " + sufijo, "30")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una prestación con ese nombre"));
    }

    @Test
    void desactivarEsIdempotenteYNoHayBorrado() throws Exception {
        long id = crear("Limpieza " + sufijo, 40);

        mockMvc.perform(como(admin, post("/api/prestaciones/" + id + "/desactivar"))).andExpect(status().isOk());
        mockMvc.perform(como(admin, post("/api/prestaciones/" + id + "/desactivar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
        mockMvc.perform(como(admin, delete("/api/prestaciones/" + id))).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void prestacionInexistenteEsNoEncontrada() throws Exception {
        mockMvc.perform(como(recepcion, get("/api/prestaciones/999999")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void permisos() throws Exception {
        long id = crear("Limpieza " + sufijo, 40);

        mockMvc.perform(como(recepcion, get("/api/prestaciones"))).andExpect(status().isOk());
        mockMvc.perform(como(profesional, get("/api/prestaciones/" + id))).andExpect(status().isOk());
        mockMvc.perform(json(profesional, put("/api/prestaciones/" + id), cuerpo("Limpieza " + sufijo, "90")))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(recepcion, post("/api/prestaciones"), cuerpo("Nueva " + sufijo, "30")))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(admin, get("/api/prestaciones/" + id)))
                .andExpect(jsonPath("$.duracionMinutos").value(40));
        mockMvc.perform(get("/api/prestaciones")).andExpect(status().isUnauthorized());
    }
}
