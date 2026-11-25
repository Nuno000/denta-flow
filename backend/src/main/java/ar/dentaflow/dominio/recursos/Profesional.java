package ar.dentaflow.dominio.recursos;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ValidacionesDeTexto;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Profesional que atiende en el consultorio. Se identifica por email (único) y referencia las
 * prestaciones que realiza por id. Se desactiva en lugar de borrarse; desactivarlo conserva sus
 * prestaciones.
 */
public class Profesional {

    public static final int NOMBRE_MAXIMO = 100;
    public static final int APELLIDO_MAXIMO = 100;
    public static final int MATRICULA_MAXIMO = 30;
    public static final int EMAIL_MAXIMO = 254;

    /** Formato mínimo: algo@algo.algo, sin espacios ni segunda arroba. */
    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private Long id;
    private String nombre;
    private String apellido;
    private String matricula;
    private String email;
    private boolean activo;
    private Set<Long> prestacionIds = new HashSet<>();

    protected Profesional() {
    }

    private Profesional(String nombre, String apellido, String matricula, String email) {
        this.nombre = ValidacionesDeTexto.obligatorio(nombre, "nombre del profesional", NOMBRE_MAXIMO);
        this.apellido = ValidacionesDeTexto.obligatorio(apellido, "apellido del profesional", APELLIDO_MAXIMO);
        this.matricula = ValidacionesDeTexto.opcional(matricula, "matrícula", MATRICULA_MAXIMO);
        this.email = validarEmail(email);
        this.activo = true;
    }

    /** Crea un profesional activo y sin prestaciones; la matrícula es opcional. */
    public static Profesional nuevo(String nombre, String apellido, String matricula, String email) {
        return new Profesional(nombre, apellido, matricula, email);
    }

    /** Habilita una prestación activa y ya guardada; repetirla no la duplica. */
    public void habilitarPrestacion(Prestacion prestacion) {
        if (prestacion == null || prestacion.getId() == null) {
            throw new ValidacionDeDominioException("La prestación debe existir para habilitarla");
        }
        if (!prestacion.isActiva()) {
            throw new ValidacionDeDominioException("No se puede habilitar una prestación inactiva");
        }
        prestacionIds.add(prestacion.getId());
    }

    /** Quita la prestación del profesional; si no la tenía no hace nada. */
    public void deshabilitarPrestacion(Long prestacionId) {
        prestacionIds.remove(prestacionId);
    }

    public boolean realiza(Long prestacionId) {
        return prestacionIds.contains(prestacionId);
    }

    /** Copia inmodificable de los ids de las prestaciones que realiza. */
    public Set<Long> prestacionIds() {
        return Collections.unmodifiableSet(new HashSet<>(prestacionIds));
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    private static String validarEmail(String email) {
        String valido = ValidacionesDeTexto.obligatorio(email, "email del profesional", EMAIL_MAXIMO)
                .toLowerCase(Locale.ROOT);
        if (!FORMATO_EMAIL.matcher(valido).matches()) {
            throw new ValidacionDeDominioException("El email del profesional no tiene un formato válido");
        }
        return valido;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Profesional that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Profesional.class.hashCode();
    }
}
