package ar.dentaflow.presentacion.configuracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec parametros-consultorio sobre HTTP: lectura y edición solo para el administrador, catálogo
 * cerrado, validación con 400 y confirmación con autor e instante.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ParametrosControllerTest {

    private static final String ANTICIPACION = "/api/parametros/anticipacion-minima-horas";

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(Instant.parse("2026-12-02T00:30:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private Long adminId;
    private String admin;
    private String recepcion;

    private String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    private String token(Long id) {
        return "Bearer " + emisor.emitir(id).token();
    }

    @BeforeEach
    void preparar() {
        adminId = usuarios.crear(email("admin"), "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();
        admin = token(adminId);
        recepcion = token(usuarios.crear(email("recepcion"), "contrasena-valida", Rol.RECEPCION, null)
                .usuario().getId());
    }

    private MockHttpServletRequestBuilder cambiar(String ruta, String cuerpo, String autorizacion) {
        return put(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo).header("Authorization", autorizacion);
    }

    @Test
    void elAdministradorListaLosTresParametrosEnElOrdenDelCatalogo() throws Exception {
        mockMvc.perform(get("/api/parametros").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].clave").value("anticipacion-minima-horas"))
                .andExpect(jsonPath("$[0].tipo").value("entero"))
                .andExpect(jsonPath("$[0].unidad").value("horas"))
                .andExpect(jsonPath("$[0].minimo").value(1))
                .andExpect(jsonPath("$[0].maximo").value(168))
                .andExpect(jsonPath("$[0].valor").value("24"))
                .andExpect(jsonPath("$[0].provisorio").value(true))
                .andExpect(jsonPath("$[1].clave").value("aviso-privacidad-texto"))
                .andExpect(jsonPath("$[1].tipo").value("texto"))
                .andExpect(jsonPath("$[1].longitudMaxima").value(4000))
                .andExpect(jsonPath("$[2].clave").value("aviso-privacidad-leyenda"))
                .andExpect(jsonPath("$[2].longitudMaxima").value(200));
    }

    @Test
    void obtenerUnParametroYUnaClaveDesconocida() throws Exception {
        mockMvc.perform(get(ANTICIPACION).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clave").value("anticipacion-minima-horas"))
                .andExpect(jsonPath("$.actualizadoEn").doesNotExist())
                .andExpect(jsonPath("$.actualizadoPor").doesNotExist());

        mockMvc.perform(get("/api/parametros/recordatorio-horas").header("Authorization", admin))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail", containsString("recordatorio-horas")));
    }

    @Test
    void editarConfirmaElValorConAutorEInstanteDelReloj() throws Exception {
        mockMvc.perform(cambiar(ANTICIPACION, "{\"valor\":\"48\"}", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("48"))
                .andExpect(jsonPath("$.provisorio").value(false))
                .andExpect(jsonPath("$.actualizadoPor").value(adminId.intValue()))
                .andExpect(jsonPath("$.actualizadoEn").value("2026-12-02T00:30:00Z"));

        mockMvc.perform(get(ANTICIPACION).header("Authorization", admin))
                .andExpect(jsonPath("$.valor").value("48"))
                .andExpect(jsonPath("$.provisorio").value(false));
    }

    @Test
    void guardarElMismoValorInicialTambienLoConfirma() throws Exception {
        mockMvc.perform(cambiar(ANTICIPACION, "{\"valor\":\"24\"}", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("24"))
                .andExpect(jsonPath("$.provisorio").value(false));
    }

    @Test
    void unValorInvalidoDa400YNoCambiaNada() throws Exception {
        for (String invalido : new String[] {"0", "169", "veinte"}) {
            mockMvc.perform(cambiar(ANTICIPACION, "{\"valor\":\"" + invalido + "\"}", admin))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.detail", containsString("entre 1 y 168")));
        }
        mockMvc.perform(cambiar(ANTICIPACION, "{}", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.valor").value("El valor es obligatorio"));
        mockMvc.perform(cambiar("/api/parametros/aviso-privacidad-texto",
                        "{\"valor\":\"" + "x".repeat(4001) + "\"}", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("4000")));

        mockMvc.perform(get(ANTICIPACION).header("Authorization", admin))
                .andExpect(jsonPath("$.valor").value("24"))
                .andExpect(jsonPath("$.provisorio").value(true));
    }

    @Test
    void editarUnaClaveDesconocidaDa404() throws Exception {
        mockMvc.perform(cambiar("/api/parametros/recordatorio-horas", "{\"valor\":\"3\"}", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void recepcionNoPuedeEditarNiVer() throws Exception {
        mockMvc.perform(cambiar(ANTICIPACION, "{\"valor\":\"48\"}", recepcion)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/parametros").header("Authorization", recepcion)).andExpect(status().isForbidden());

        mockMvc.perform(get(ANTICIPACION).header("Authorization", admin))
                .andExpect(jsonPath("$.valor").value("24"));
    }

    @Test
    void sinTokenDa401ConBearer() throws Exception {
        mockMvc.perform(get("/api/parametros"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
        mockMvc.perform(put(ANTICIPACION).contentType(MediaType.APPLICATION_JSON).content("{\"valor\":\"48\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
    }

    @Test
    void unAdministradorDesactivadoConSuTokenAnteriorDa401YNoCambiaNada() throws Exception {
        Long otro = usuarios.crear(email("otro-admin"), "contrasena-valida", Rol.ADMINISTRADOR, null)
                .usuario().getId();
        String tokenDelOtro = token(otro);
        usuarios.desactivar(adminId, otro);

        mockMvc.perform(cambiar(ANTICIPACION, "{\"valor\":\"48\"}", tokenDelOtro))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(ANTICIPACION).header("Authorization", admin))
                .andExpect(jsonPath("$.valor").value("24"));
    }

    @Test
    void laDocumentacionOpenApiListaLosTresEndpointsBajoParametros() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/parametros'].get.tags[0]").value("Parámetros"))
                .andExpect(jsonPath("$.paths['/api/parametros/{clave}'].get.tags[0]").value("Parámetros"))
                .andExpect(jsonPath("$.paths['/api/parametros/{clave}'].put.tags[0]").value("Parámetros"));
    }
}
