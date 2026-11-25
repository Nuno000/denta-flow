package ar.dentaflow.presentacion.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeIngreso;
import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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

/**
 * Spec gestion-usuarios y autorizacion-por-rol ("Rol verificado en el backend") sobre HTTP: cada endpoint
 * de usuarios y roles es solo para administradores (design D10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuariosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios servicio;

    @Autowired
    private ServicioDeIngreso ingreso;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Autowired
    private ProfesionalRepositorio profesionales;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private Long adminId;
    private String admin;
    private String recepcion;
    private String profesional;

    private String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    private Long profesionalNuevo(String base) {
        return profesionales.guardar(Profesional.nuevo("Laura", "Ejemplo", null, email(base))).getId();
    }

    private String token(Long id) {
        return "Bearer " + emisor.emitir(id).token();
    }

    @BeforeEach
    void preparar() {
        adminId = servicio.crear(email("admin"), "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();
        admin = token(adminId);
        recepcion = token(servicio.crear(email("recepcion"), "contrasena-valida", Rol.RECEPCION, null).usuario().getId());
        profesional = token(servicio.crear(email("prof"), "contrasena-valida", Rol.PROFESIONAL,
                profesionalNuevo("prof-vinculado")).usuario().getId());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String cuerpo) {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private List<MockHttpServletRequestBuilder> todosLosEndpoints() {
        return List.of(
                get("/api/usuarios"),
                json(post("/api/usuarios"), "{\"email\":\"x@example.com\",\"contrasena\":\"contrasena-valida\",\"rol\":\"recepcion\"}"),
                json(put("/api/usuarios/" + adminId + "/rol"), "{\"rol\":\"recepcion\"}"),
                post("/api/usuarios/" + adminId + "/desactivar"),
                post("/api/usuarios/" + adminId + "/activar"),
                json(put("/api/usuarios/" + adminId + "/contrasena"), "{\"contrasena\":\"contrasena-nueva\"}"),
                get("/api/usuarios/profesionales-disponibles"),
                get("/api/roles"));
    }

    @Test
    void recepcionYProfesionalReciben403YSinTokenSe401() throws Exception {
        for (MockHttpServletRequestBuilder pedido : todosLosEndpoints()) {
            mockMvc.perform(pedido.header("Authorization", recepcion)).andExpect(status().isForbidden());
        }
        for (MockHttpServletRequestBuilder pedido : todosLosEndpoints()) {
            mockMvc.perform(pedido.header("Authorization", profesional)).andExpect(status().isForbidden());
        }
        for (MockHttpServletRequestBuilder pedido : todosLosEndpoints()) {
            mockMvc.perform(pedido.header("Authorization", "")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void altaDevuelve201ConLocationYSinCredencial() throws Exception {
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"" + email("Nuevo")
                        + "\",\"contrasena\":\"contrasena-valida\",\"rol\":\"recepcion\"}").header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/usuarios/")))
                .andExpect(jsonPath("$.email").value(email("nuevo")))
                .andExpect(jsonPath("$.rol").value("recepcion"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(content().string(not(containsString("contrasena-valida"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void altaConDatosInvalidosDa400() throws Exception {
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"" + email("x")
                        + "\",\"contrasena\":\"contrasena-valida\",\"rol\":\"superusuario\"}").header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("administrador, recepcion, profesional")));
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"" + email("x")
                        + "\",\"contrasena\":\"contrasena-valida\"}").header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.rol").value("El rol es obligatorio"));
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"recepcion-sin-arroba\""
                        + ",\"contrasena\":\"contrasena-valida\",\"rol\":\"recepcion\"}").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"" + email("x")
                        + "\",\"contrasena\":\"1234567\",\"rol\":\"recepcion\"}").header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena", containsString("8")));
    }

    @Test
    void emailRepetidoYProfesionalYaVinculadoDan409() throws Exception {
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"" + email("RECEPCION")
                        + "\",\"contrasena\":\"contrasena-valida\",\"rol\":\"recepcion\"}").header("Authorization", admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El email ya está en uso"));

        Long vinculado = profesionalNuevo("ya-vinculado");
        servicio.crear(email("primero"), "contrasena-valida", Rol.PROFESIONAL, vinculado);
        mockMvc.perform(json(post("/api/usuarios"), "{\"email\":\"" + email("segundo")
                        + "\",\"contrasena\":\"contrasena-valida\",\"rol\":\"profesional\",\"profesionalId\":" + vinculado + "}")
                        .header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void cambioDeRolDesactivarActivarYCasosBorde() throws Exception {
        Long id = servicio.crear(email("cambiar"), "contrasena-valida", Rol.RECEPCION, null).usuario().getId();
        Long disponible = profesionalNuevo("disponible");

        mockMvc.perform(json(put("/api/usuarios/" + id + "/rol"),
                        "{\"rol\":\"profesional\",\"profesionalId\":" + disponible + "}").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("profesional"))
                .andExpect(jsonPath("$.profesionalId").value(disponible))
                .andExpect(jsonPath("$.profesionalNombre").value("Laura Ejemplo"));
        mockMvc.perform(post("/api/usuarios/" + id + "/desactivar").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(post("/api/usuarios/" + id + "/activar").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(post("/api/usuarios/999999999/desactivar").header("Authorization", admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/usuarios/" + adminId + "/desactivar").header("Authorization", admin))
                .andExpect(status().isConflict());
        mockMvc.perform(json(put("/api/usuarios/" + adminId + "/rol"), "{\"rol\":\"recepcion\"}")
                        .header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void blanqueoDeContrasena() throws Exception {
        Long id = servicio.crear(email("blanqueo"), "contrasena-anterior", Rol.RECEPCION, null).usuario().getId();

        mockMvc.perform(json(put("/api/usuarios/" + id + "/contrasena"), "{\"contrasena\":\"1234567\"}")
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(put("/api/usuarios/" + id + "/contrasena"), "{\"contrasena\":\"contrasena-nueva\"}")
                        .header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(json(put("/api/usuarios/" + adminId + "/contrasena"), "{\"contrasena\":\"contrasena-nueva\"}")
                        .header("Authorization", admin))
                .andExpect(status().isConflict());

        org.assertj.core.api.Assertions.assertThat(ingreso.ingresar(email("blanqueo"), "contrasena-nueva")
                .usuario().getId()).isEqualTo(id);
    }

    @Test
    void elListadoIncluyeElNombreDelProfesionalYLosInactivos() throws Exception {
        Long inactivo = servicio.crear(email("inactivo"), "contrasena-valida", Rol.RECEPCION, null).usuario().getId();
        servicio.desactivar(adminId, inactivo);

        String cuerpo = mockMvc.perform(get("/api/usuarios").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(email("inactivo"))))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andReturn().getResponse().getContentAsString();

        List<Boolean> activoDelInactivo = JsonPath.read(cuerpo, "$[?(@.id == " + inactivo + ")].activo");
        List<String> nombreDelProfesional = JsonPath.read(cuerpo, "$[?(@.email == '" + email("prof") + "')].profesionalNombre");
        org.assertj.core.api.Assertions.assertThat(activoDelInactivo).containsExactly(false);
        org.assertj.core.api.Assertions.assertThat(nombreDelProfesional).containsExactly("Laura Ejemplo");
    }

    @Test
    void profesionalesDisponiblesYCatalogoDeRoles() throws Exception {
        Long disponible = profesionalNuevo("libre");

        String cuerpo = mockMvc.perform(get("/api/usuarios/profesionales-disponibles").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(disponible.intValue())))
                .andExpect(jsonPath("$[*].nombre", everyItem(org.hamcrest.Matchers.notNullValue())))
                .andReturn().getResponse().getContentAsString();
        List<Object> conUsuario = JsonPath.read(cuerpo, "$[?(@.email)]");
        org.assertj.core.api.Assertions.assertThat(conUsuario).as("no expone el email del profesional").isEmpty();

        mockMvc.perform(get("/api/roles").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("administrador"))
                .andExpect(jsonPath("$[1].nombre").value("Recepción"));
    }

    @Test
    void noExisteElBorradoDeUsuarios() throws Exception {
        // No hay ninguna ruta de borrado: Spring responde 404 (o 405 si la ruta existiera con otro método).
        mockMvc.perform(delete("/api/usuarios/" + adminId).header("Authorization", admin))
                .andExpect(status().is4xxClientError());
        org.assertj.core.api.Assertions.assertThat(servicio.obtener(adminId).usuario().isActivo()).isTrue();
    }
}
