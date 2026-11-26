package ar.dentaflow.presentacion.publico;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec privacidad-datos-personales, "Aviso de privacidad público": el aviso se lee sin token, solo con
 * texto y leyenda, y refleja al instante lo que guarda el administrador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AvisoPrivacidadControllerTest {

    private static final String AVISO = "/api/publico/aviso-privacidad";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Test
    void consultaSinTokenDevuelveSoloTextoYLeyenda() throws Exception {
        mockMvc.perform(get(AVISO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-cache")))
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.texto", containsString("Ley 25.326")))
                .andExpect(jsonPath("$.leyenda").value("Leí y acepto el aviso de privacidad"));
    }

    @Test
    void cambioDelTextoVisibleDeInmediato() throws Exception {
        String email = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Long adminId = usuarios.crear(email, "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();

        mockMvc.perform(put("/api/parametros/aviso-privacidad-texto")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + emisor.emitir(adminId).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"Aviso nuevo, revisado por el consultorio.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(AVISO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Aviso nuevo, revisado por el consultorio."));
    }
}
