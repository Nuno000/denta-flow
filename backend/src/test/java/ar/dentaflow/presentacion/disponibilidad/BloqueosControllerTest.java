package ar.dentaflow.presentacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec bloqueos-agenda sobre HTTP: ámbitos, conversión de zona, validación, edición, baja, consulta
 * por rango, aviso de turnos afectados y permisos por rol. Usa fechas de 2031 para no cruzarse con
 * otros datos de la base compartida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BloqueosControllerTest {

    private static final String AHORA = "2026-12-02T00:30:00Z";

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(Instant.parse(AHORA), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Autowired
    private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private Long profesionalA;
    private Long profesionalB;
    private Long sillon;
    private Long adminId;
    private String admin;
    private String recepcion;
    private String profesionalDeA;

    private String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    private Long usuario(Rol rol, Long profesionalId) {
        return usuarios.crear(email(rol.codigo()), "contrasena-valida", rol, profesionalId).usuario().getId();
    }

    private String token(Long usuarioId) {
        return "Bearer " + emisor.emitir(usuarioId).token();
    }

    @BeforeEach
    void preparar() {
        profesionalA = profesionales.guardar(Profesional.nuevo("Laura", "Bloqueos", null, email("laura"))).getId();
        profesionalB = profesionales.guardar(Profesional.nuevo("Martín", "Bloqueos", null, email("martin"))).getId();
        sillon = sillones.guardar(Sillon.nuevo("Sillón " + sufijo)).getId();
        adminId = usuario(Rol.ADMINISTRADOR, null);
        admin = token(adminId);
        recepcion = token(usuario(Rol.RECEPCION, null));
        profesionalDeA = token(usuario(Rol.PROFESIONAL, profesionalA));
    }

    private static String cuerpo(String ambito, Long profesionalId, Long sillonId, String inicio, String fin) {
        return "{\"ambito\":\"" + ambito + "\",\"profesionalId\":" + profesionalId + ",\"sillonId\":" + sillonId
                + ",\"inicio\":\"" + inicio + "\",\"fin\":\"" + fin + "\",\"motivo\":\"AUSENCIA\"}";
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String cuerpo, String autorizacion) {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo).header("Authorization", autorizacion);
    }

    private long crear(String ambito, Long profesionalId, Long sillonId, String inicio, String fin) throws Exception {
        String respuesta = mockMvc.perform(json(post("/api/bloqueos"),
                        cuerpo(ambito, profesionalId, sillonId, inicio, fin), admin))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.bloqueo.id")).longValue();
    }

    private static String dia31(String hora) {
        return "2031-03-10T" + hora + ":00-03:00";
    }

    private MockHttpServletRequestBuilder consultar(String autorizacion, String extra) {
        return get("/api/bloqueos?desde=" + dia31("00:00") + "&hasta=2031-03-11T00:00:00-03:00" + extra)
                .header("Authorization", autorizacion);
    }

    @Test
    void altaDeCadaAmbitoConAvisoVacio() throws Exception {
        mockMvc.perform(json(post("/api/bloqueos"),
                        cuerpo("PROFESIONAL", profesionalA, null, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/bloqueos/\\d+")))
                .andExpect(jsonPath("$.bloqueo.ambito").value("PROFESIONAL"))
                .andExpect(jsonPath("$.bloqueo.profesionalId").value(profesionalA))
                .andExpect(jsonPath("$.bloqueo.motivo").value("AUSENCIA"))
                .andExpect(jsonPath("$.bloqueo.modificadoPor").value(adminId))
                .andExpect(jsonPath("$.bloqueo.modificadoEn").value(AHORA))
                .andExpect(jsonPath("$.turnosAfectados.length()").value(0));
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("SILLON", null, sillon, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.sillonId").value(sillon));
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("CONSULTORIO", null, null, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.profesionalId").doesNotExist());
    }

    @Test
    void convierteLaZonaYGuardaEnUtc() throws Exception {
        String respuesta = mockMvc.perform(json(post("/api/bloqueos"),
                        cuerpo("CONSULTORIO", null, null, "2026-12-24T08:00-03:00", "2026-12-24T16:00:00Z"), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.inicio").value("2026-12-24T08:00:00-03:00"))
                .andExpect(jsonPath("$.bloqueo.fin").value("2026-12-24T13:00:00-03:00"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.bloqueo.id")).longValue();

        assertThat(jdbc.queryForObject("SELECT inicio FROM bloqueo WHERE id = ?", Timestamp.class, id).toInstant())
                .isEqualTo(Instant.parse("2026-12-24T11:00:00Z"));
    }

    @Test
    void cambioDeDiaEntreHoraLocalYUtc() throws Exception {
        String respuesta = mockMvc.perform(json(post("/api/bloqueos"),
                        cuerpo("CONSULTORIO", null, null, "2026-12-31T21:00-03:00", "2027-01-01T00:00-03:00"), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueo.inicio").value("2026-12-31T21:00:00-03:00"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.bloqueo.id")).longValue();

        assertThat(jdbc.queryForObject("SELECT inicio FROM bloqueo WHERE id = ?", Timestamp.class, id).toInstant())
                .isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));
    }

    @Test
    void datosInvalidosSon400() throws Exception {
        mockMvc.perform(json(post("/api/bloqueos"),
                        cuerpo("CONSULTORIO", null, null, "2026-12-24T08:00", "2026-12-24T13:00"), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("PROFESIONAL", null, null, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("CONSULTORIO", null, sillon, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("CONSULTORIO", null, null, dia31("12:00"), dia31("12:00")), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("CONSULTORIO", null, null, dia31("09:00"), dia31("12:00"))
                        .replace("AUSENCIA", "PARO"), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("CONSULTORIO", null, null, dia31("09:00"), dia31("12:00"))
                        .replace("}", ",\"detalle\":\"" + "x".repeat(201) + "\"}"), admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("200")));
        mockMvc.perform(json(post("/api/bloqueos"), "{}", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.ambito").exists())
                .andExpect(jsonPath("$.errores.inicio").exists())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    void recursoInexistenteEs404EInactivoEs409() throws Exception {
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("PROFESIONAL", -1L, null, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isNotFound());
        Sillon inactivo = Sillon.nuevo("Inactivo " + sufijo);
        inactivo.desactivar();
        Long inactivoId = sillones.guardar(inactivo).getId();
        mockMvc.perform(json(post("/api/bloqueos"), cuerpo("SILLON", null, inactivoId, dia31("09:00"), dia31("12:00")), admin))
                .andExpect(status().isConflict());
    }

    @Test
    void edicionBajaYConsultaPorId() throws Exception {
        long id = crear("PROFESIONAL", profesionalA, null, dia31("09:00"), dia31("12:00"));

        mockMvc.perform(json(put("/api/bloqueos/" + id),
                        "{\"inicio\":\"" + dia31("09:00") + "\",\"fin\":\"" + dia31("13:00") + "\",\"motivo\":\"VACACIONES\"}", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueo.fin").value("2031-03-10T13:00:00-03:00"))
                .andExpect(jsonPath("$.bloqueo.motivo").value("VACACIONES"))
                .andExpect(jsonPath("$.turnosAfectados.length()").value(0));
        mockMvc.perform(json(put("/api/bloqueos/" + id), "{\"inicio\":\"" + dia31("09:00") + "\",\"fin\":\""
                        + dia31("14:00") + "\",\"motivo\":\"OTRO\",\"ambito\":\"CONSULTORIO\"}", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/bloqueos/" + id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fin").value("2031-03-10T13:00:00-03:00"))
                .andExpect(jsonPath("$.ambito").value("PROFESIONAL"));

        mockMvc.perform(delete("/api/bloqueos/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/bloqueos/" + id).header("Authorization", admin)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/bloqueos/" + id).header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void consultaPorRangoConSuperposicionYValidacion() throws Exception {
        crear("PROFESIONAL", profesionalA, null, dia31("08:00"), dia31("10:00"));
        long segundo = crear("PROFESIONAL", profesionalA, null, dia31("10:00"), dia31("12:00"));

        mockMvc.perform(get("/api/bloqueos?desde=" + dia31("10:00") + "&hasta=" + dia31("11:00"))
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(segundo));
        mockMvc.perform(get("/api/bloqueos?hasta=" + dia31("11:00")).header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/bloqueos?desde=2031-01-01T00:00:00-03:00&hasta=2032-01-03T00:00:00-03:00")
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recepcionYProfesionalNoModifican() throws Exception {
        long id = crear("CONSULTORIO", null, null, dia31("09:00"), dia31("12:00"));

        for (String autorizacion : new String[] {recepcion, profesionalDeA}) {
            mockMvc.perform(json(post("/api/bloqueos"), cuerpo("CONSULTORIO", null, null, dia31("13:00"), dia31("14:00")), autorizacion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(json(put("/api/bloqueos/" + id), "{\"inicio\":\"" + dia31("09:00") + "\",\"fin\":\""
                            + dia31("10:00") + "\",\"motivo\":\"OTRO\"}", autorizacion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/bloqueos/" + id).header("Authorization", autorizacion))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(consultar(admin, "")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fin").value("2031-03-10T12:00:00-03:00"));
    }

    @Test
    void vistaSegunElRol() throws Exception {
        long deA = crear("PROFESIONAL", profesionalA, null, dia31("09:00"), dia31("10:00"));
        long deB = crear("PROFESIONAL", profesionalB, null, dia31("09:00"), dia31("10:00"));
        long delSillon = crear("SILLON", null, sillon, dia31("09:00"), dia31("10:00"));
        long general = crear("CONSULTORIO", null, null, dia31("11:00"), dia31("12:00"));

        mockMvc.perform(consultar(recepcion, "")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        for (String filtro : new String[] {"", "&profesionalId=" + profesionalB, "&sillonId=" + sillon}) {
            mockMvc.perform(consultar(profesionalDeA, filtro))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(deA))
                    .andExpect(jsonPath("$[1].id").value(general));
        }
        mockMvc.perform(get("/api/bloqueos/" + deB).header("Authorization", profesionalDeA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.id").doesNotExist());
        mockMvc.perform(get("/api/bloqueos/" + delSillon).header("Authorization", profesionalDeA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bloqueos/" + general).header("Authorization", profesionalDeA))
                .andExpect(status().isOk());
    }

    @Test
    void sinTokenOAdministradorDesactivadoEs401() throws Exception {
        long id = crear("CONSULTORIO", null, null, dia31("09:00"), dia31("12:00"));
        mockMvc.perform(get("/api/bloqueos?desde=" + dia31("00:00") + "&hasta=" + dia31("23:00")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("CONSULTORIO", null, null, dia31("13:00"), dia31("14:00"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bloqueos/" + id))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")));

        Long otroAdmin = usuarios.crear(email("otro-admin"), "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();
        String tokenAnterior = token(otroAdmin);
        usuarios.desactivar(adminId, otroAdmin);
        mockMvc.perform(delete("/api/bloqueos/" + id).header("Authorization", tokenAnterior))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bloqueos/" + id).header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void openApiListaLosEndpointsBajoBloqueos() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/bloqueos'].get.tags[0]").value("Bloqueos"))
                .andExpect(jsonPath("$.paths['/api/bloqueos'].post.tags[0]").value("Bloqueos"))
                .andExpect(jsonPath("$.paths['/api/bloqueos/{id}'].put.tags[0]").value("Bloqueos"))
                .andExpect(jsonPath("$.paths['/api/bloqueos/{id}'].delete.tags[0]").value("Bloqueos"))
                .andExpect(jsonPath("$.components.schemas.ResultadoBloqueoRespuesta.properties.turnosAfectados.description",
                        containsString("C-09")));
    }
}
