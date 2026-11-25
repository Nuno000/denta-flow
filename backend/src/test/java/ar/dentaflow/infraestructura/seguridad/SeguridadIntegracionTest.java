package ar.dentaflow.infraestructura.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.dominio.usuarios.CodificadorDeCredenciales;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Specs autorizacion-por-rol ("Denegar por defecto", "Rutas públicas", "Errores de acceso en JSON") y
 * autenticacion-personal ("Token de acceso con vencimiento", "El estado del usuario se verifica en cada
 * request"). Design D3 y D8.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SeguridadIntegracionTest.ControladorDePrueba.class)
class SeguridadIntegracionTest {

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/api/prueba/protegida")
        String protegida(@AuthenticationPrincipal UsuarioAutenticado usuario) {
            return usuario.rol().codigo();
        }

        @SoloAdministrador
        @GetMapping("/api/prueba/solo-admin")
        String soloAdmin() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private CodificadorDeCredenciales codificador;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Autowired
    private PropiedadesAuth propiedades;

    private Usuario crear(Rol rol) {
        String email = rol.codigo() + "-" + UUID.randomUUID() + "@example.com";
        return usuarios.guardar(Usuario.nuevo(email, codificador.codificar("contrasena-de-prueba"), rol, null));
    }

    private String token(Usuario usuario) {
        return "Bearer " + emisor.emitir(usuario.getId()).token();
    }

    @Test
    void lasRutasPublicasRespondenSinToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void laDocumentacionOpenApiPublicaLosEndpointsYElEsquemaBearer() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios'].get.security[0].bearerAuth").exists());
    }

    @Test
    void unaRutaProtegidaSinTokenDa401EnFormatoProblema() throws Exception {
        mockMvc.perform(get("/api/prueba/protegida"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail", containsString("ingresar")));
    }

    @Test
    void unaRutaInexistenteSinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/no-existe")).andExpect(status().isUnauthorized());
    }

    @Test
    void conUnTokenValidoDeUnUsuarioActivoPasa() throws Exception {
        Usuario recepcion = crear(Rol.RECEPCION);

        mockMvc.perform(get("/api/prueba/protegida").header("Authorization", token(recepcion)))
                .andExpect(status().isOk())
                .andExpect(content().string("recepcion"));
    }

    @Test
    void unTokenVencidoDa401() throws Exception {
        Usuario recepcion = crear(Rol.RECEPCION);
        Clock haceNueveHoras = Clock.fixed(Instant.now().minus(Duration.ofHours(9)), ZoneOffset.UTC);
        ConfiguracionTokens configuracion = new ConfiguracionTokens();
        EmisorDeTokensJwt emisorViejo = new EmisorDeTokensJwt(
                configuracion.codificadorJwt(configuracion.claveDeFirma(propiedades)), propiedades, haceNueveHoras);

        mockMvc.perform(get("/api/prueba/protegida")
                        .header("Authorization", "Bearer " + emisorViejo.emitir(recepcion.getId()).token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unTokenAdulteradoDa401() throws Exception {
        Usuario recepcion = crear(Rol.RECEPCION);
        Usuario admin = crear(Rol.ADMINISTRADOR);
        String[] partes = emisor.emitir(recepcion.getId()).token().split("[.]");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"" + recepcion.getId() + "\"", "\"" + admin.getId() + "\"");
        String alterado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        mockMvc.perform(get("/api/prueba/protegida").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioDesactivadoConTokenVigenteDa401() throws Exception {
        Usuario recepcion = crear(Rol.RECEPCION);
        String token = token(recepcion);
        recepcion.desactivar();
        usuarios.guardar(recepcion);

        mockMvc.perform(get("/api/prueba/protegida").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unRolNoPermitidoDa403EnFormatoProblema() throws Exception {
        Usuario recepcion = crear(Rol.RECEPCION);

        mockMvc.perform(get("/api/prueba/solo-admin").header("Authorization", token(recepcion)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail", containsString("permiso")));
    }

    @Test
    void elCambioDeRolSeAplicaEnElRequestSiguienteConElMismoToken() throws Exception {
        Usuario usuario = crear(Rol.RECEPCION);
        String token = token(usuario);
        mockMvc.perform(get("/api/prueba/solo-admin").header("Authorization", token))
                .andExpect(status().isForbidden());

        usuario.cambiarRol(Rol.ADMINISTRADOR, null);
        usuarios.guardar(usuario);

        mockMvc.perform(get("/api/prueba/solo-admin").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/prueba/protegida").header("Authorization", token))
                .andExpect(content().string("administrador"));
    }

    @Test
    void unTokenDeUnUsuarioInexistenteDa401() throws Exception {
        mockMvc.perform(get("/api/prueba/protegida")
                        .header("Authorization", "Bearer " + emisor.emitir(Long.MAX_VALUE).token()))
                .andExpect(status().isUnauthorized());
    }
}
