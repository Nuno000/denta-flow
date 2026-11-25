package ar.dentaflow.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.usuarios.TokenEmitido;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Spec autenticacion-personal — "Token de acceso con vencimiento" (design D1 y D2). */
class EmisorDeTokensJwtTest {

    private static final Instant AHORA = Instant.parse("2026-11-25T13:00:00Z");
    private static final PropiedadesAuth PROPIEDADES = propiedades("0123456789abcdef0123456789abcdef-clave-a");

    private final ConfiguracionTokens configuracion = new ConfiguracionTokens();

    private static PropiedadesAuth propiedades(String secreto) {
        return new PropiedadesAuth(secreto, Duration.ofHours(8), "dentaflow", 5, Duration.ofMinutes(15));
    }

    private static Clock relojEn(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private EmisorDeTokensJwt emisor(PropiedadesAuth propiedades, Clock reloj) {
        return new EmisorDeTokensJwt(configuracion.codificadorJwt(configuracion.claveDeFirma(propiedades)),
                propiedades, reloj);
    }

    private JwtDecoder decodificador(PropiedadesAuth propiedades, Clock reloj) {
        return configuracion.decodificadorJwt(configuracion.claveDeFirma(propiedades), propiedades, reloj);
    }

    @Test
    void elTokenLlevaElUsuarioElEmisorYVenceEnOchoHoras() {
        TokenEmitido emitido = emisor(PROPIEDADES, relojEn(AHORA)).emitir(42L);

        Jwt jwt = decodificador(PROPIEDADES, relojEn(AHORA)).decode(emitido.token());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("dentaflow");
        assertThat(jwt.getIssuedAt()).isEqualTo(AHORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(emitido.venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void justoAntesDeVencerSigueSiendoValido() {
        TokenEmitido emitido = emisor(PROPIEDADES, relojEn(AHORA)).emitir(42L);

        Jwt jwt = decodificador(PROPIEDADES, relojEn(AHORA.plus(Duration.ofHours(8)).minusSeconds(1)))
                .decode(emitido.token());

        assertThat(jwt.getSubject()).isEqualTo("42");
    }

    @Test
    void unTokenVencidoSeRechaza() {
        TokenEmitido emitido = emisor(PROPIEDADES, relojEn(AHORA)).emitir(42L);
        JwtDecoder masTarde = decodificador(PROPIEDADES, relojEn(AHORA.plus(Duration.ofHours(8)).plusSeconds(1)));

        assertThatThrownBy(() -> masTarde.decode(emitido.token())).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        TokenEmitido ajeno = emisor(propiedades("otra-clave-distinta-de-32-bytes-o-mas!!"), relojEn(AHORA)).emitir(42L);

        assertThatThrownBy(() -> decodificador(PROPIEDADES, relojEn(AHORA)).decode(ajeno.token()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenConElContenidoAlteradoSeRechaza() {
        String[] partes = emisor(PROPIEDADES, relojEn(AHORA)).emitir(42L).token().split("[.]");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"42\"", "\"1\"");
        String alterado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        assertThatThrownBy(() -> decodificador(PROPIEDADES, relojEn(AHORA)).decode(alterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenDeOtroEmisorSeRechaza() {
        PropiedadesAuth otroEmisor = new PropiedadesAuth(PROPIEDADES.secreto(), Duration.ofHours(8), "otro", 5,
                Duration.ofMinutes(15));
        TokenEmitido emitido = emisor(otroEmisor, relojEn(AHORA)).emitir(42L);

        assertThatThrownBy(() -> decodificador(PROPIEDADES, relojEn(AHORA)).decode(emitido.token()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void basuraNoEsUnToken() {
        assertThatThrownBy(() -> decodificador(PROPIEDADES, relojEn(AHORA)).decode("no-es-un-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
