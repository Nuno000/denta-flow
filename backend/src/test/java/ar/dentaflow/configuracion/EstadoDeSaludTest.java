package ar.dentaflow.configuracion;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Spec configuracion-entorno — "Estado de salud". */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstadoDeSaludTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respondeUpSinDetallesNiComponentes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void respondeSinTokenNiCredenciales() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Authorization", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Desde C-04 todo lo no público exige ingresar: sin token da 401. Con un usuario autenticado,
     * los demás endpoints de Actuator siguen sin existir (404), que es lo que este test protege.
     */
    @Test
    void noExponeOtrosEndpointsDeActuator() throws Exception {
        mockMvc.perform(get("/actuator/env").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }
}
