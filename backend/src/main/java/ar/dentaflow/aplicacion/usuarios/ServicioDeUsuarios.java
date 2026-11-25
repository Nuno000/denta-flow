package ar.dentaflow.aplicacion.usuarios;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.CatalogoDeRoles;
import ar.dentaflow.dominio.usuarios.CodificadorDeCredenciales;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.RolDisponible;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de usuarios por el administrador (US-003, design D6): alta, listado, cambio de rol,
 * desactivación, reactivación y blanqueo de contraseña. El administrador no puede desactivarse, cambiarse
 * el rol ni blanquearse la contraseña a sí mismo, así siempre queda uno activo.
 */
@Service
@Transactional
public class ServicioDeUsuarios {

    public static final int CONTRASENA_MINIMO = 8;
    /** Límite de BCrypt: procesa a lo sumo 72 bytes. */
    public static final int CONTRASENA_MAXIMO = 72;

    private static final String EMAIL_EN_USO = "El email ya está en uso";
    private static final String PROFESIONAL_CON_USUARIO = "El profesional elegido ya tiene un usuario";

    private final UsuarioRepositorio usuarios;
    private final ProfesionalRepositorio profesionales;
    private final CodificadorDeCredenciales codificador;
    private final CatalogoDeRoles catalogo;

    public ServicioDeUsuarios(UsuarioRepositorio usuarios, ProfesionalRepositorio profesionales,
            CodificadorDeCredenciales codificador, CatalogoDeRoles catalogo) {
        this.usuarios = usuarios;
        this.profesionales = profesionales;
        this.codificador = codificador;
        this.catalogo = catalogo;
    }

    public UsuarioDetalle crear(String email, String contrasena, Rol rol, Long profesionalId) {
        validarContrasena(contrasena);
        String normalizado = Usuario.normalizarEmail(email);
        if (usuarios.buscarPorEmail(normalizado).isPresent()) {
            throw new ConflictoException(EMAIL_EN_USO);
        }
        Profesional profesional = profesionalParaVincular(rol, profesionalId, null);
        Usuario usuario = Usuario.nuevo(normalizado, codificador.codificar(contrasena), rol, profesionalId);
        return detalle(guardar(usuario), profesional);
    }

    @Transactional(readOnly = true)
    public UsuarioDetalle obtener(Long id) {
        return detalle(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDetalle> listar() {
        Map<Long, Profesional> porId = profesionales.listarTodos().stream()
                .collect(Collectors.toMap(Profesional::getId, Function.identity()));
        return usuarios.listarTodos().stream()
                .map(usuario -> detalle(usuario, usuario.getProfesionalId() == null
                        ? null : porId.get(usuario.getProfesionalId())))
                .toList();
    }

    public UsuarioDetalle cambiarRol(Long actorId, Long id, Rol rol, Long profesionalId) {
        Usuario usuario = buscar(id);
        rechazarSobreSiMismo(actorId, id, "No podés cambiarte el rol a vos mismo");
        Profesional profesional = profesionalParaVincular(rol, profesionalId, usuario.getProfesionalId());
        usuario.cambiarRol(rol, profesionalId);
        return detalle(guardar(usuario), profesional);
    }

    public UsuarioDetalle desactivar(Long actorId, Long id) {
        Usuario usuario = buscar(id);
        rechazarSobreSiMismo(actorId, id, "No podés desactivarte a vos mismo");
        usuario.desactivar();
        return detalle(guardar(usuario));
    }

    public UsuarioDetalle activar(Long id) {
        Usuario usuario = buscar(id);
        usuario.activar();
        return detalle(guardar(usuario));
    }

    public void blanquearContrasena(Long actorId, Long id, String contrasena) {
        Usuario usuario = buscar(id);
        rechazarSobreSiMismo(actorId, id, "No podés blanquear tu propia contraseña desde la gestión de usuarios");
        validarContrasena(contrasena);
        usuario.cambiarCredencial(codificador.codificar(contrasena));
        guardar(usuario);
    }

    @Transactional(readOnly = true)
    public List<RolDisponible> listarRoles() {
        return catalogo.listar();
    }

    /** Profesionales activos que todavía no tienen usuario, para vincular uno con rol profesional. */
    @Transactional(readOnly = true)
    public List<Profesional> listarProfesionalesDisponibles() {
        Set<Long> conUsuario = usuarios.idsDeProfesionalesConUsuario();
        return profesionales.listarActivos().stream()
                .filter(profesional -> !conUsuario.contains(profesional.getId()))
                .toList();
    }

    /** Las reglas de longitud de la contraseña, compartidas por el alta, el blanqueo y el administrador inicial. */
    public static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < CONTRASENA_MINIMO) {
            throw new ValidacionDeDominioException(
                    "La contraseña debe tener al menos " + CONTRASENA_MINIMO + " caracteres");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > CONTRASENA_MAXIMO) {
            throw new ValidacionDeDominioException("La contraseña no puede superar los " + CONTRASENA_MAXIMO
                    + " caracteres (las letras con acento o la ñ cuentan doble)");
        }
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    private static void rechazarSobreSiMismo(Long actorId, Long id, String mensaje) {
        if (Objects.equals(actorId, id)) {
            throw new OperacionNoPermitidaException(mensaje);
        }
    }

    /**
     * Valida el profesional a vincular cuando el rol es profesional: debe existir, estar activo y no tener
     * otro usuario ({@code vinculadoActual} es el que el usuario ya tiene, que no cuenta como conflicto).
     * Para los otros roles devuelve {@code null}; el dominio rechaza un profesional indicado de más.
     */
    private Profesional profesionalParaVincular(Rol rol, Long profesionalId, Long vinculadoActual) {
        if (rol != Rol.PROFESIONAL || profesionalId == null) {
            return null;
        }
        Profesional profesional = profesionales.buscarPorId(profesionalId)
                .filter(Profesional::isActivo)
                .orElseThrow(() -> new ValidacionDeDominioException("El profesional elegido no existe o está inactivo"));
        if (!profesionalId.equals(vinculadoActual) && usuarios.existeUsuarioDelProfesional(profesionalId)) {
            throw new ConflictoException(PROFESIONAL_CON_USUARIO);
        }
        return profesional;
    }

    /** Guarda traduciendo la violación de unicidad de la base (p. ej. dos altas simultáneas) a 409. */
    private Usuario guardar(Usuario usuario) {
        try {
            return usuarios.guardar(usuario);
        } catch (DataIntegrityViolationException e) {
            String mensaje = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (mensaje.contains("uk_usuario_email")) {
                throw new ConflictoException(EMAIL_EN_USO);
            }
            if (mensaje.contains("uk_usuario_profesional")) {
                throw new ConflictoException(PROFESIONAL_CON_USUARIO);
            }
            throw e;
        }
    }

    private UsuarioDetalle detalle(Usuario usuario) {
        Long profesionalId = usuario.getProfesionalId();
        return detalle(usuario, profesionalId == null ? null : profesionales.buscarPorId(profesionalId).orElse(null));
    }

    private static UsuarioDetalle detalle(Usuario usuario, Profesional profesional) {
        return new UsuarioDetalle(usuario,
                profesional == null ? null : profesional.getNombre() + " " + profesional.getApellido());
    }
}
