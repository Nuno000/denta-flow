package ar.dentaflow.presentacion.recursos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Spec gestion-recursos sobre HTTP para sillones: alta, edición, duplicados, estado y permisos (design D3). */
class SillonesControllerTest extends ApiDeRecursosTestBase {

    private long crear(String nombre) throws Exception {
        return id(mockMvc.perform(json(admin, post("/api/sillones"), "{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated()).andReturn());
    }

    @Test
    void elAdministradorDaDeAltaUnSillon() throws Exception {
        mockMvc.perform(json(admin, post("/api/sillones"), "{\"nombre\":\"Box " + sufijo + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/sillones/")))
                .andExpect(jsonPath("$.nombre").value("Box " + sufijo))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void elAdministradorRenombraUnSillon() throws Exception {
        long id = crear("Box " + sufijo);

        mockMvc.perform(json(admin, put("/api/sillones/" + id), "{\"nombre\":\"Box nuevo " + sufijo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Box nuevo " + sufijo));
    }

    @Test
    void desactivarDosVecesYConsultarElInactivo() throws Exception {
        long id = crear("Box " + sufijo);

        mockMvc.perform(como(admin, post("/api/sillones/" + id + "/desactivar"))).andExpect(status().isOk());
        mockMvc.perform(como(admin, post("/api/sillones/" + id + "/desactivar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(como(profesional, get("/api/sillones/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(como(admin, post("/api/sillones/" + id + "/activar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void nombreEnBlancoEsDatoInvalido() throws Exception {
        mockMvc.perform(json(admin, post("/api/sillones"), "{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void nombreRepetidoConOtrasMayusculasEsConflicto() throws Exception {
        crear("Box " + sufijo);

        mockMvc.perform(json(admin, post("/api/sillones"), "{\"nombre\":\"box " + sufijo + " \"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Ya existe un sillón con ese nombre"));
    }

    @Test
    void sillonInexistenteEsNoEncontrado() throws Exception {
        mockMvc.perform(como(admin, get("/api/sillones/999999"))).andExpect(status().isNotFound());
        mockMvc.perform(json(admin, put("/api/sillones/999999"), "{\"nombre\":\"X\"}")).andExpect(status().isNotFound());
        mockMvc.perform(como(admin, post("/api/sillones/999999/desactivar"))).andExpect(status().isNotFound());
    }

    @Test
    void noHayBorrado() throws Exception {
        long id = crear("Box " + sufijo);

        mockMvc.perform(como(admin, delete("/api/sillones/" + id)))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(como(admin, get("/api/sillones/" + id))).andExpect(status().isOk());
    }

    // --- Permisos (spec "Consulta de recursos por el personal interno", "Modificación solo por el administrador")

    @Test
    void recepcionYProfesionalListanLosSillones() throws Exception {
        crear("Box " + sufijo);

        mockMvc.perform(como(recepcion, get("/api/sillones")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Box " + sufijo)));
        mockMvc.perform(como(profesional, get("/api/sillones"))).andExpect(status().isOk());
    }

    @Test
    void recepcionNoPuedeCrear() throws Exception {
        mockMvc.perform(json(recepcion, post("/api/sillones"), "{\"nombre\":\"Box " + sufijo + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(admin, get("/api/sillones")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Box " + sufijo))));
    }

    @Test
    void profesionalNoPuedeEditarNiDesactivar() throws Exception {
        long id = crear("Box " + sufijo);

        mockMvc.perform(json(profesional, put("/api/sillones/" + id), "{\"nombre\":\"Otro\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(profesional, post("/api/sillones/" + id + "/desactivar")))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(admin, get("/api/sillones/" + id)))
                .andExpect(jsonPath("$.nombre").value("Box " + sufijo))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void sinTokenEs401() throws Exception {
        mockMvc.perform(get("/api/sillones"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
    }

    // --- Documentación (tarea 4.6)

    @Test
    void laDocumentacionOpenApiListaLasRutasDeRecursosConSuEtiqueta() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/sillones'].post.tags[0]").value("Sillones"))
                .andExpect(jsonPath("$.paths['/api/sillones/{id}/desactivar'].post.tags[0]").value("Sillones"))
                .andExpect(jsonPath("$.paths['/api/prestaciones/{id}'].put.tags[0]").value("Prestaciones"))
                .andExpect(jsonPath("$.paths['/api/profesionales'].get.tags[0]").value("Profesionales"))
                .andExpect(jsonPath("$.paths['/api/profesionales/{id}/prestaciones'].put.tags[0]").value("Profesionales"))
                .andExpect(jsonPath("$.paths['/api/profesionales'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/sillones/{id}'].delete").doesNotExist());
    }
}
