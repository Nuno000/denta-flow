package ar.dentaflow.aplicacion.usuarios;

import ar.dentaflow.dominio.usuarios.CodificadorDeCredenciales;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Ingreso del personal con email y contraseña (US-003, design D6). Ante email inexistente, contraseña
 * incorrecta o usuario desactivado responde siempre lo mismo, y aun sin usuario compara la contraseña
 * contra un hash ficticio para que el tiempo de respuesta no revele si el email existe.
 */
@Service
public class ServicioDeIngreso {

    private final UsuarioRepositorio usuarios;
    private final CodificadorDeCredenciales codificador;
    private final EmisorDeTokens emisor;
    private final LimitadorDeIntentosDeIngreso limitador;
    private final String hashFicticio;

    public ServicioDeIngreso(UsuarioRepositorio usuarios, CodificadorDeCredenciales codificador,
            EmisorDeTokens emisor, LimitadorDeIntentosDeIngreso limitador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.emisor = emisor;
        this.limitador = limitador;
        this.hashFicticio = codificador.codificar("contrasena-ficticia-solo-para-igualar-tiempos");
    }

    public ResultadoIngreso ingresar(String email, String contrasena) {
        String normalizado = Usuario.normalizarEmail(email);
        limitador.verificar(normalizado);

        Optional<Usuario> encontrado = usuarios.buscarPorEmail(normalizado);
        boolean coincide = codificador.coincide(contrasena,
                encontrado.map(Usuario::getCredencialHash).orElse(hashFicticio));

        if (encontrado.isEmpty() || !coincide || !encontrado.get().isActivo()) {
            limitador.registrarFallo(normalizado);
            throw new CredencialesInvalidasException();
        }
        limitador.registrarExito(normalizado);
        Usuario usuario = encontrado.get();
        return new ResultadoIngreso(emisor.emitir(usuario.getId()), usuario);
    }
}
