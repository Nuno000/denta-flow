package ar.dentaflow.infraestructura.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spec privacidad-datos-personales, "Transporte cifrado en el despliegue": Render y Vercel terminan TLS
 * en su proxy y avisan el esquema con {@code X-Forwarded-Proto}; con HTTPS la API manda HSTS (design D8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HstsDetrasDelProxyTest {

    private static final String HSTS = "Strict-Transport-Security";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestHttpsAtravesDelProxyRecibeHsts() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Forwarded-Proto", "https"))
                .andExpect(status().isOk())
                .andExpect(header().string(HSTS, containsString("max-age=31536000")));
    }

    @Test
    void requestHttpPlanoNoRecibeHsts() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HSTS));
    }
}
