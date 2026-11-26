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

/** Spec reserva-online — "Contacto del consultorio público" (design D3). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContactoPublicoControllerTest {

    private static final String CONTACTO = "/api/publico/contacto";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Test
    void consultaSinTokenDevuelveSoloElContactoSinCache() throws Exception {
        mockMvc.perform(get(CONTACTO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.contacto")
                        .value("Comunicate con el consultorio por teléfono para coordinar tu turno."));
    }

    @Test
    void reflejaAlInstanteLoQueGuardaElAdministrador() throws Exception {
        String email = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Long adminId = usuarios.crear(email, "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();

        mockMvc.perform(put("/api/parametros/contacto-consultorio")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + emisor.emitir(adminId).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"Llamanos al 0261 555-0000 de lunes a viernes de 9 a 18\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(CONTACTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contacto").value("Llamanos al 0261 555-0000 de lunes a viernes de 9 a 18"));
    }
}
