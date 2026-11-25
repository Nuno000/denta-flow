package ar.dentaflow.infraestructura.seed;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Administrador inicial (C-04, design D11): al arrancar, si no hay ningún administrador activo, crea uno
 * con {@code APP_ADMIN_EMAIL} y {@code APP_ADMIN_PASSWORD}, con las mismas validaciones que el alta por
 * API. Corre en todos los perfiles salvo {@code test} (incluida producción, porque es la única forma de
 * tener el primer administrador). Nunca escribe el email ni la contraseña en el log (Ley 25.326).
 */
@Component
@ConditionalOnProperty(name = "app.seed.admin", havingValue = "true")
public class SeedAdministradorInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SeedAdministradorInicial.class);

    private final UsuarioRepositorio usuarios;
    private final ServicioDeUsuarios servicio;
    private final String email;
    private final String contrasena;

    public SeedAdministradorInicial(UsuarioRepositorio usuarios, ServicioDeUsuarios servicio,
            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String contrasena) {
        this.usuarios = usuarios;
        this.servicio = servicio;
        this.email = email;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.existeAdministradorActivo()) {
            LOG.info("Administrador inicial omitido: ya hay un administrador activo");
            return;
        }
        if (email == null || email.isBlank() || contrasena == null || contrasena.isBlank()) {
            LOG.warn("No hay ningún administrador activo y faltan APP_ADMIN_EMAIL o APP_ADMIN_PASSWORD: "
                    + "definilas en el entorno y reiniciá para crear el primer administrador");
            return;
        }
        try {
            servicio.crear(email, contrasena, Rol.ADMINISTRADOR, null);
            LOG.info("Administrador inicial creado a partir de APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD");
        } catch (ValidacionDeDominioException | ConflictoException e) {
            LOG.warn("No se pudo crear el administrador inicial con APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD: {}",
                    e.getMessage());
        }
    }
}
