package ar.dentaflow.infraestructura.seguridad;

import ar.dentaflow.aplicacion.usuarios.EmisorDeTokens;
import ar.dentaflow.aplicacion.usuarios.TokenEmitido;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Emite JWT HS256 con los claims mínimos {@code sub} (id del usuario), {@code iss}, {@code iat} y
 * {@code exp} (design D1). El rol no viaja en el token: se relee del usuario en cada request (D3).
 */
public class EmisorDeTokensJwt implements EmisorDeTokens {

    private final JwtEncoder codificador;
    private final PropiedadesAuth propiedades;
    private final Clock reloj;

    public EmisorDeTokensJwt(JwtEncoder codificador, PropiedadesAuth propiedades, Clock reloj) {
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public TokenEmitido emitir(Long usuarioId) {
        Instant ahora = reloj.instant();
        Instant vence = ahora.plus(propiedades.duracionToken());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(usuarioId.toString())
                .issuedAt(ahora)
                .expiresAt(vence)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(token, vence);
    }
}
