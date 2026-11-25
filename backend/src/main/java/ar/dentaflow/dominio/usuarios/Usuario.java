package ar.dentaflow.dominio.usuarios;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ValidacionesDeTexto;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Persona del consultorio que ingresa al panel interno (US-003). Tiene un solo rol (PA-09); con el rol
 * {@code profesional} se vincula a exactamente un profesional, y con los otros roles a ninguno. Solo
 * conoce el hash de su credencial, nunca la contraseña. Se desactiva en lugar de borrarse.
 */
public class Usuario {

    public static final int EMAIL_MAXIMO = 254;

    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private Long id;
    private String email;
    private String credencialHash;
    private Rol rol;
    private Long profesionalId;
    private boolean activo;

    protected Usuario() {
    }

    private Usuario(String email, String credencialHash, Rol rol, Long profesionalId) {
        this.email = normalizarEmail(email);
        this.credencialHash = validarHash(credencialHash);
        validarVinculo(rol, profesionalId);
        this.rol = rol;
        this.profesionalId = profesionalId;
        this.activo = true;
    }

    /** Crea un usuario activo. */
    public static Usuario nuevo(String email, String credencialHash, Rol rol, Long profesionalId) {
        return new Usuario(email, credencialHash, rol, profesionalId);
    }

    /**
     * Normaliza un email como se guarda y se busca: sin espacios en los extremos y en minúsculas. Falla
     * si está vacío, es demasiado largo o no tiene formato de email.
     */
    public static String normalizarEmail(String email) {
        String valido = ValidacionesDeTexto.obligatorio(email, "email", EMAIL_MAXIMO).toLowerCase(Locale.ROOT);
        if (!FORMATO_EMAIL.matcher(valido).matches()) {
            throw new ValidacionDeDominioException("El email no tiene un formato válido");
        }
        return valido;
    }

    /** Cambia el rol y el profesional vinculado con las mismas reglas del alta; si falla no cambia nada. */
    public void cambiarRol(Rol rol, Long profesionalId) {
        validarVinculo(rol, profesionalId);
        this.rol = rol;
        this.profesionalId = profesionalId;
    }

    /** Reemplaza el hash de la credencial (blanqueo de contraseña); si falla no cambia nada. */
    public void cambiarCredencial(String credencialHash) {
        this.credencialHash = validarHash(credencialHash);
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    private static String validarHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new ValidacionDeDominioException("La credencial del usuario es obligatoria");
        }
        return hash;
    }

    private static void validarVinculo(Rol rol, Long profesionalId) {
        if (rol == null) {
            throw new ValidacionDeDominioException("El rol es obligatorio");
        }
        if (rol == Rol.PROFESIONAL && profesionalId == null) {
            throw new ValidacionDeDominioException("El rol profesional requiere un profesional vinculado");
        }
        if (rol != Rol.PROFESIONAL && profesionalId != null) {
            throw new ValidacionDeDominioException("Solo el rol profesional se vincula a un profesional");
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCredencialHash() {
        return credencialHash;
    }

    public Rol getRol() {
        return rol;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Usuario that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }

    /** Sin la credencial, para que nunca termine en un log. */
    @Override
    public String toString() {
        return "Usuario[id=" + id + ", rol=" + rol + ", activo=" + activo + "]";
    }
}
