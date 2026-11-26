package ar.dentaflow.presentacion.recursos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Spec gestion-recursos sobre HTTP para profesionales y la asignación de sus prestaciones (design D3, D5).
 */
class ProfesionalesControllerTest extends ApiDeRecursosTestBase {

    private String cuerpo(String nombre, String matricula, String email) {
        String mat = matricula == null ? "null" : "\"" + matricula + "\"";
        return "{\"nombre\":\"" + nombre + "\",\"apellido\":\"Muestra\",\"matricula\":" + mat
                + ",\"email\":\"" + email + "\"}";
    }

    private long crearProfesional(String base, String matricula) throws Exception {
        return id(mockMvc.perform(json(admin, post("/api/profesionales"), cuerpo("Ana", matricula, email(base))))
                .andExpect(status().isCreated()).andReturn());
    }

    private long crearPrestacion(String nombre) throws Exception {
        return id(mockMvc.perform(json(admin, post("/api/prestaciones"),
                        "{\"nombre\":\"" + nombre + " " + sufijo + "\",\"duracionMinutos\":30}"))
                .andExpect(status().isCreated()).andReturn());
    }

    private String ids(long... ids) {
        StringBuilder texto = new StringBuilder("{\"prestacionIds\":[");
        for (int i = 0; i < ids.length; i++) {
            texto.append(i == 0 ? "" : ",").append(ids[i]);
        }
        return texto.append("]}").toString();
    }

    @Test
    void altaSinMatriculaQuedaActivaYSinPrestaciones() throws Exception {
        mockMvc.perform(json(admin, post("/api/profesionales"), cuerpo("Ana", null, email("ana"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/profesionales/")))
                .andExpect(jsonPath("$.matricula").doesNotExist())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.prestacionIds", hasSize(0)));
    }

    @Test
    void edicionYQuitarLaMatricula() throws Exception {
        long id = crearProfesional("ana", "MP-" + sufijo);

        mockMvc.perform(json(admin, put("/api/profesionales/" + id), cuerpo("Ana María", "", email("ana"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"))
                .andExpect(jsonPath("$.matricula").doesNotExist());
    }

    @Test
    void datosInvalidos() throws Exception {
        mockMvc.perform(json(admin, post("/api/profesionales"),
                        "{\"nombre\":\"Ana\",\"apellido\":\" \",\"email\":\"" + email("ana") + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.apellido").exists());
        mockMvc.perform(json(admin, post("/api/profesionales"), cuerpo("Ana", null, "mal-formado")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("email")));
    }

    @Test
    void emailOMatriculaDeOtroSonConflicto() throws Exception {
        crearProfesional("ana", "MP-" + sufijo);
        long otro = crearProfesional("otro", null);

        mockMvc.perform(json(admin, post("/api/profesionales"), cuerpo("Eva", null, email("ana").toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un profesional con ese email"));
        mockMvc.perform(json(admin, put("/api/profesionales/" + otro), cuerpo("Ana", "mp-" + sufijo, email("otro"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un profesional con esa matrícula"));
    }

    @Test
    void desactivarConservaLasPrestacionesYNoHayBorrado() throws Exception {
        long id = crearProfesional("ana", null);
        long consulta = crearPrestacion("Consulta");
        mockMvc.perform(json(admin, put("/api/profesionales/" + id + "/prestaciones"), ids(consulta)))
                .andExpect(status().isOk());

        mockMvc.perform(como(admin, post("/api/profesionales/" + id + "/desactivar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.prestacionIds[0]").value(consulta));
        mockMvc.perform(como(admin, delete("/api/profesionales/" + id))).andExpect(status().isMethodNotAllowed());
    }

    // --- Asignación de prestaciones (spec "Asignación de prestaciones a un profesional")

    @Test
    void asignarDevuelveLosIdsOrdenados() throws Exception {
        long id = crearProfesional("ana", null);
        long consulta = crearPrestacion("Consulta");
        long extraccion = crearPrestacion("Extracción");

        mockMvc.perform(json(admin, put("/api/profesionales/" + id + "/prestaciones"), ids(extraccion, consulta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestacionIds", hasSize(2)))
                .andExpect(jsonPath("$.prestacionIds[0]").value(Math.min(consulta, extraccion)))
                .andExpect(jsonPath("$.prestacionIds[1]").value(Math.max(consulta, extraccion)));
    }

    @Test
    void asignarUnaInactivaNuevaOUnaInexistenteEsDatoInvalido() throws Exception {
        long id = crearProfesional("ana", null);
        long blanqueamiento = crearPrestacion("Blanqueamiento");
        mockMvc.perform(como(admin, post("/api/prestaciones/" + blanqueamiento + "/desactivar")))
                .andExpect(status().isOk());

        mockMvc.perform(json(admin, put("/api/profesionales/" + id + "/prestaciones"), ids(blanqueamiento)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(admin, put("/api/profesionales/" + id + "/prestaciones"), ids(999999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("999999")));
        mockMvc.perform(como(admin, get("/api/profesionales/" + id)))
                .andExpect(jsonPath("$.prestacionIds", hasSize(0)));
    }

    @Test
    void asignarSinListaEsDatoInvalido() throws Exception {
        long id = crearProfesional("ana", null);

        mockMvc.perform(json(admin, put("/api/profesionales/" + id + "/prestaciones"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.prestacionIds").exists());
    }

    @Test
    void asignarAUnProfesionalInexistenteEsNoEncontrado() throws Exception {
        mockMvc.perform(json(admin, put("/api/profesionales/999999/prestaciones"), ids()))
                .andExpect(status().isNotFound());
    }

    // --- Permisos

    @Test
    void permisos() throws Exception {
        long id = crearProfesional("ana", null);

        mockMvc.perform(como(recepcion, get("/api/profesionales")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(email("ana"))));
        mockMvc.perform(como(profesional, get("/api/profesionales/" + id))).andExpect(status().isOk());
        mockMvc.perform(json(recepcion, put("/api/profesionales/" + id + "/prestaciones"), ids()))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(profesional, post("/api/profesionales"), cuerpo("Eva", null, email("eva"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(recepcion, post("/api/profesionales/" + id + "/desactivar")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profesionales/" + id)).andExpect(status().isUnauthorized());
    }
}
