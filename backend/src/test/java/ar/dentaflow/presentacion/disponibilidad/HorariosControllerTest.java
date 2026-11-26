package ar.dentaflow.presentacion.disponibilidad;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Spec horarios-atencion sobre HTTP: alta, edición, baja, consulta y permisos por rol. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HorariosControllerTest {

    private static final String LUNES_9_A_13 = "{\"dia\":\"LUNES\",\"inicio\":\"09:00\",\"fin\":\"13:00\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private Long profesionalA;
    private Long profesionalB;
    private String admin;
    private String recepcion;
    private String profesionalDeA;

    private String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    private String token(Rol rol, Long profesionalId) {
        Long id = usuarios.crear(email(rol.codigo()), "contrasena-valida", rol, profesionalId).usuario().getId();
        return "Bearer " + emisor.emitir(id).token();
    }

    @BeforeEach
    void preparar() {
        profesionalA = profesionales.guardar(Profesional.nuevo("Laura", "Horarios", null, email("laura"))).getId();
        profesionalB = profesionales.guardar(Profesional.nuevo("Martín", "Horarios", null, email("martin"))).getId();
        admin = token(Rol.ADMINISTRADOR, null);
        recepcion = token(Rol.RECEPCION, null);
        profesionalDeA = token(Rol.PROFESIONAL, profesionalA);
    }

    private String horariosDe(Long profesionalId) {
        return "/api/profesionales/" + profesionalId + "/horarios";
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String cuerpo, String autorizacion) {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo).header("Authorization", autorizacion);
    }

    private long crear(Long profesionalId, String cuerpo) throws Exception {
        String respuesta = mockMvc.perform(json(post(horariosDe(profesionalId)), cuerpo, admin))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @Test
    void altaDevuelve201ConLocationYLosDatos() throws Exception {
        mockMvc.perform(json(post(horariosDe(profesionalA)), LUNES_9_A_13, admin))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/horarios/\\d+")))
                .andExpect(jsonPath("$.profesionalId").value(profesionalA))
                .andExpect(jsonPath("$.dia").value("LUNES"))
                .andExpect(jsonPath("$.inicio").value("09:00"))
                .andExpect(jsonPath("$.fin").value("13:00"));
    }

    @Test
    void superpuestoEs409EnFormatoDeProblema() throws Exception {
        crear(profesionalA, LUNES_9_A_13);

        mockMvc.perform(json(post(horariosDe(profesionalA)),
                        "{\"dia\":\"LUNES\",\"inicio\":\"12:00\",\"fin\":\"16:00\"}", admin))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail", containsString("09:00")));
    }

    @Test
    void datosInvalidosSon400() throws Exception {
        mockMvc.perform(json(post(horariosDe(profesionalA)), "{\"dia\":\"LUNES\",\"inicio\":\"13:00\",\"fin\":\"09:00\"}", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post(horariosDe(profesionalA)), "{\"dia\":\"FERIADO\",\"inicio\":\"09:00\",\"fin\":\"13:00\"}", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post(horariosDe(profesionalA)), "{\"dia\":\"LUNES\",\"inicio\":\"09:00:30\",\"fin\":\"13:00\"}", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post(horariosDe(profesionalA)), "{}", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dia").exists())
                .andExpect(jsonPath("$.errores.inicio").exists())
                .andExpect(jsonPath("$.errores.fin").exists());
    }

    @Test
    void profesionalInexistenteEs404() throws Exception {
        mockMvc.perform(json(post(horariosDe(-1L)), LUNES_9_A_13, admin)).andExpect(status().isNotFound());
        mockMvc.perform(get(horariosDe(-1L)).header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void consultaOrdenadaPorDiaYHora() throws Exception {
        crear(profesionalA, "{\"dia\":\"MARTES\",\"inicio\":\"09:00\",\"fin\":\"13:00\"}");
        crear(profesionalA, "{\"dia\":\"LUNES\",\"inicio\":\"14:00\",\"fin\":\"18:00\"}");
        crear(profesionalA, LUNES_9_A_13);

        mockMvc.perform(get(horariosDe(profesionalA)).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].dia").value("LUNES"))
                .andExpect(jsonPath("$[0].inicio").value("09:00"))
                .andExpect(jsonPath("$[1].inicio").value("14:00"))
                .andExpect(jsonPath("$[2].dia").value("MARTES"));
    }

    @Test
    void edicionYBaja() throws Exception {
        long manana = crear(profesionalA, LUNES_9_A_13);
        crear(profesionalA, "{\"dia\":\"LUNES\",\"inicio\":\"14:00\",\"fin\":\"18:00\"}");

        mockMvc.perform(json(put("/api/horarios/" + manana), "{\"dia\":\"LUNES\",\"inicio\":\"09:00\",\"fin\":\"14:00\"}", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fin").value("14:00"));
        mockMvc.perform(json(put("/api/horarios/" + manana), "{\"dia\":\"LUNES\",\"inicio\":\"09:00\",\"fin\":\"15:00\"}", admin))
                .andExpect(status().isConflict());
        mockMvc.perform(json(put("/api/horarios/-1"), LUNES_9_A_13, admin)).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/horarios/" + manana).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/horarios/" + manana).header("Authorization", admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(horariosDe(profesionalA)).header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void recepcionYProfesionalNoModifican() throws Exception {
        long id = crear(profesionalA, LUNES_9_A_13);

        for (String autorizacion : new String[] {recepcion, profesionalDeA}) {
            mockMvc.perform(json(post(horariosDe(profesionalA)), "{\"dia\":\"MARTES\",\"inicio\":\"09:00\",\"fin\":\"13:00\"}", autorizacion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(json(put("/api/horarios/" + id), "{\"dia\":\"LUNES\",\"inicio\":\"09:00\",\"fin\":\"12:00\"}", autorizacion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/horarios/" + id).header("Authorization", autorizacion))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(horariosDe(profesionalA)).header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fin").value("13:00"));
    }

    @Test
    void lecturaSegunElRol() throws Exception {
        crear(profesionalB, LUNES_9_A_13);

        mockMvc.perform(get(horariosDe(profesionalB)).header("Authorization", recepcion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(horariosDe(profesionalA)).header("Authorization", profesionalDeA))
                .andExpect(status().isOk());
        mockMvc.perform(get(horariosDe(profesionalB)).header("Authorization", profesionalDeA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    void sinTokenEs401() throws Exception {
        mockMvc.perform(get(horariosDe(profesionalA)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
        mockMvc.perform(post(horariosDe(profesionalA)).contentType(MediaType.APPLICATION_JSON).content(LUNES_9_A_13))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiListaLosEndpointsBajoHorarios() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/profesionales/{profesionalId}/horarios'].get.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths['/api/profesionales/{profesionalId}/horarios'].post.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths['/api/horarios/{id}'].put.tags[0]").value("Horarios"))
                .andExpect(jsonPath("$.paths['/api/horarios/{id}'].delete.tags[0]").value("Horarios"));
    }
}
