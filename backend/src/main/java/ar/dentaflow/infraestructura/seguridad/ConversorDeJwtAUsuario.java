package ar.dentaflow.infraestructura.seguridad;

import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT ya validado en la autenticación del request, releyendo el usuario por {@code sub}
 * (design D3): si no existe o está desactivado, el request se rechaza con 401 aunque el token no haya
 * vencido; el rol se toma de la base, así un cambio de rol se aplica en el request siguiente.
 */
@Component
public class ConversorDeJwtAUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepositorio usuarios;

    public ConversorDeJwtAUsuario(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = idDelToken(jwt)
                .flatMap(usuarios::buscarPorId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new InvalidBearerTokenException("El usuario del token no existe o está desactivado"));
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRol(),
                usuario.getProfesionalId());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
    }

    private static java.util.Optional<Long> idDelToken(Jwt jwt) {
        try {
            return java.util.Optional.of(Long.valueOf(jwt.getSubject()));
        } catch (NumberFormatException | NullPointerException e) {
            return java.util.Optional.empty();
        }
    }
}
