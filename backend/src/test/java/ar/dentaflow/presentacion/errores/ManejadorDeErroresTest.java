package ar.dentaflow.presentacion.errores;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.CredencialesInvalidasException;
import ar.dentaflow.aplicacion.usuarios.DemasiadosIntentosException;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Formato de errores común (design D9): ProblemDetail en español, sin trazas. */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ManejadorDeErrores.class, ManejadorDeErroresTest.ControladorDePrueba.class})
class ManejadorDeErroresTest {

    record Datos(@NotBlank String nombre, @Email String email) {
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/{caso}")
        void lanzar(@PathVariable String caso) {
            switch (caso) {
                case "validacion" -> throw new ValidacionDeDominioException("El email no tiene un formato válido");
                case "no-encontrado" -> throw new RecursoNoEncontradoException("No existe el usuario 9");
                case "conflicto" -> throw new ConflictoException("El email ya está en uso");
                case "no-permitida" -> throw new OperacionNoPermitidaException("No podés desactivarte a vos mismo");
                case "credenciales" -> throw new CredencialesInvalidasException();
                case "intentos" -> throw new DemasiadosIntentosException(Duration.ofSeconds(90));
                case "sin-permiso" -> throw new AccessDeniedException("denegado");
                default -> throw new IllegalStateException("detalle interno que no debe salir");
            }
        }

        @PostMapping("/prueba/datos")
        void recibir(@Valid @RequestBody Datos datos) {
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private void esperar(String caso, int estado, String detalle) throws Exception {
        mockMvc.perform(get("/prueba/" + caso))
                .andExpect(status().is(estado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(estado))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").value(detalle));
    }

    @Test
    void traduceLasExcepcionesDelDominioYDeLaAplicacion() throws Exception {
        esperar("validacion", 400, "El email no tiene un formato válido");
        esperar("no-encontrado", 404, "No existe el usuario 9");
        esperar("conflicto", 409, "El email ya está en uso");
        esperar("no-permitida", 409, "No podés desactivarte a vos mismo");
        esperar("credenciales", 401, "Email o contraseña incorrectos");
        esperar("sin-permiso", 403, "No tenés permiso para realizar esta operación");
    }

    @Test
    void demasiadosIntentosIndicaCuandoReintentar() throws Exception {
        mockMvc.perform(get("/prueba/intentos"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "90"))
                .andExpect(jsonPath("$.detail", containsString("Demasiados intentos")));
    }

    @Test
    void unErrorInesperadoDa500SinDetallesInternos() throws Exception {
        mockMvc.perform(get("/prueba/otro"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("detalle interno"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void laValidacionDeBeanValidationIncluyeLosErroresPorCampo() throws Exception {
        mockMvc.perform(post("/prueba/datos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"email\":\"sin-arroba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.nombre").isNotEmpty())
                .andExpect(jsonPath("$.errores.email").isNotEmpty());
    }

    @Test
    void unJsonIlegibleDa400() throws Exception {
        mockMvc.perform(post("/prueba/datos").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos inválidos"));
    }
}
