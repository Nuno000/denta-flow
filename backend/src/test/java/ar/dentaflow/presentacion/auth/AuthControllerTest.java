package ar.dentaflow.presentacion.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec autenticacion-personal — "Ingreso con email y contraseña", "Rechazo genérico", "Límite de
 * intentos fallidos" y "Consulta del usuario autenticado" sobre HTTP (design D10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios servicio;

    private final String email = "auth-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

    private ResultActions login(String emailEnviado, String contrasena) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + emailEnviado + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }

    @Test
    void unIngresoCorrectoDevuelveTokenVencimientoYUsuarioSinCredencial() throws Exception {
        servicio.crear(email, "contrasena-correcta", Rol.RECEPCION, null);

        login("  " + email.toUpperCase() + " ", "contrasena-correcta")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.venceEn").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.usuario.rol").value("recepcion"))
                .andExpect(jsonPath("$.usuario.activo").value(true))
                .andExpect(content().string(not(containsString("contrasena-correcta"))))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("credencial"))));
    }

    @Test
    void datosDeIngresoIncompletosDan400SinToken() throws Exception {
        login("recepcion-sin-arroba", "algo").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").isNotEmpty())
                .andExpect(jsonPath("$.token").doesNotExist());
        login(email, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena").isNotEmpty());
    }

    @Test
    void credencialesInvalidasDan401ConElMensajeGenerico() throws Exception {
        servicio.crear(email, "contrasena-correcta", Rol.RECEPCION, null);

        login(email, "contrasena-incorrecta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"))
                .andExpect(jsonPath("$.token").doesNotExist());
        login("nadie-" + email, "contrasena-correcta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
    }

    @Test
    void elSextoIntentoDa429ConRetryAfter() throws Exception {
        servicio.crear(email, "contrasena-correcta", Rol.RECEPCION, null);
        for (int i = 0; i < 5; i++) {
            login(email, "contrasena-incorrecta").andExpect(status().isUnauthorized());
        }

        login(email, "contrasena-correcta")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void yoDevuelveElUsuarioAutenticadoYSinTokenDa401() throws Exception {
        Long id = servicio.crear(email, "contrasena-correcta", Rol.RECEPCION, null).usuario().getId();
        String token = JsonPath.read(login(email, "contrasena-correcta").andReturn().getResponse().getContentAsString(),
                "$.token");

        mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("recepcion"));
        mockMvc.perform(get("/api/auth/yo")).andExpect(status().isUnauthorized());
    }
}
