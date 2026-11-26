package ar.dentaflow.presentacion.errores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spec privacidad-datos-personales, "Sin datos personales en logs ni en errores": las respuestas 400
 * dicen qué campo está mal, pero nunca repiten el valor que mandó el usuario (design D7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErroresSinDatosDelInputTest {

    private static final String DNI = "30111222";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void error400DeValidacionNoRepiteElValorRechazado() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + DNI + "\",\"contrasena\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).contains("email").doesNotContain(DNI);
    }

    @Test
    void error400DeJsonMalFormadoNoRepiteElCuerpo() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana.prueba@example.com\",\"contrasena\":" + DNI))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(DNI).doesNotContain("ana.prueba@example.com");
    }
}
