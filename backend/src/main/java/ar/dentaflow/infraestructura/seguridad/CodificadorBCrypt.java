package ar.dentaflow.infraestructura.seguridad;

import ar.dentaflow.dominio.usuarios.CodificadorDeCredenciales;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hash adaptativo con sal (BCrypt) para las contraseñas del personal (design D4). */
@Component
public class CodificadorBCrypt implements CodificadorDeCredenciales {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String codificar(String contrasena) {
        return bcrypt.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        return contrasena != null && hash != null && bcrypt.matches(contrasena, hash);
    }
}
