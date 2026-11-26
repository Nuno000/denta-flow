package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de los tests HTTP de recursos (spec gestion-recursos): contexto completo sobre H2, rollback por
 * prueba, un token por cada rol interno y nombres únicos por ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ApiDeRecursosTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private EmisorDeTokensJwt emisor;

    protected final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    protected String admin;
    protected String recepcion;
    protected String profesional;

    @BeforeEach
    void prepararTokens() {
        admin = token(usuarios.crear(email("admin"), "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId());
        recepcion = token(usuarios.crear(email("recep"), "contrasena-valida", Rol.RECEPCION, null).usuario().getId());
        Long vinculado = profesionales.guardar(Profesional.nuevo("Vinculado", "Prueba", null, email("vinc"))).getId();
        profesional = token(usuarios.crear(email("prof"), "contrasena-valida", Rol.PROFESIONAL, vinculado)
                .usuario().getId());
    }

    protected String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    protected MockHttpServletRequestBuilder como(String token, MockHttpServletRequestBuilder pedido) {
        return pedido.header(HttpHeaders.AUTHORIZATION, token);
    }

    protected MockHttpServletRequestBuilder json(String token, MockHttpServletRequestBuilder pedido, String cuerpo) {
        return como(token, pedido).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    protected static long id(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String token(Long id) {
        return "Bearer " + emisor.emitir(id).token();
    }
}
