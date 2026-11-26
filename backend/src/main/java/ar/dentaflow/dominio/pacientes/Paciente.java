package ar.dentaflow.dominio.pacientes;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ValidacionesDeTexto;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Persona que reserva turnos. Se identifica por DNI (único) y tiene nombre, apellido, teléfono y email
 * obligatorios (PA-13). No se borra (retención: PA-12). Los datos personales no se imprimen en
 * {@code toString}.
 */
public class Paciente {

    public static final int NOMBRE_MAXIMO = 100;
    public static final int APELLIDO_MAXIMO = 100;
    public static final int EMAIL_MAXIMO = 254;

    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");
    private static final Pattern SEPARADORES_DE_TELEFONO = Pattern.compile("[\\s\\-().]");
    private static final Pattern FORMATO_TELEFONO = Pattern.compile("^\\+?\\d{8,15}$");

    private Long id;
    private String dni;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private Instant consentimientoPrivacidadEn;
    private Instant creadoEn;

    protected Paciente() {
    }

    private Paciente(String dni, String nombre, String apellido, String telefono, String email, Instant creadoEn) {
        if (creadoEn == null) {
            throw new ValidacionDeDominioException("La fecha de alta del paciente es obligatoria");
        }
        this.dni = Dni.normalizar(dni);
        asignarContacto(nombre, apellido, telefono, email);
        this.creadoEn = creadoEn;
    }

    /** Alta con los cinco datos obligatorios; todos se validan y normalizan. */
    public static Paciente nuevo(String dni, String nombre, String apellido, String telefono, String email,
            Instant creadoEn) {
        return new Paciente(dni, nombre, apellido, telefono, email, creadoEn);
    }

    /** Reemplaza nombre, apellido, teléfono y email con las mismas validaciones del alta; si falla no cambia nada. */
    public void actualizarContacto(String nombre, String apellido, String telefono, String email) {
        asignarContacto(nombre, apellido, telefono, email);
    }

    /** Guarda el instante de aceptación del aviso de privacidad; una nueva aceptación reemplaza la anterior. */
    public void registrarConsentimiento(Instant instante) {
        if (instante == null) {
            throw new ValidacionDeDominioException("La fecha del consentimiento es obligatoria");
        }
        this.consentimientoPrivacidadEn = instante;
    }

    private void asignarContacto(String nombre, String apellido, String telefono, String email) {
        String nombreValido = ValidacionesDeTexto.obligatorio(nombre, "nombre del paciente", NOMBRE_MAXIMO);
        String apellidoValido = ValidacionesDeTexto.obligatorio(apellido, "apellido del paciente", APELLIDO_MAXIMO);
        String telefonoValido = validarTelefono(telefono);
        String emailValido = validarEmail(email);
        this.nombre = nombreValido;
        this.apellido = apellidoValido;
        this.telefono = telefonoValido;
        this.email = emailValido;
    }

    private static String validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            throw new ValidacionDeDominioException("El teléfono del paciente es obligatorio");
        }
        String normalizado = SEPARADORES_DE_TELEFONO.matcher(telefono.strip()).replaceAll("");
        if (!FORMATO_TELEFONO.matcher(normalizado).matches()) {
            throw new ValidacionDeDominioException(
                    "El teléfono del paciente debe tener entre 8 y 15 dígitos, con un + inicial opcional");
        }
        return normalizado;
    }

    private static String validarEmail(String email) {
        String valido = ValidacionesDeTexto.obligatorio(email, "email del paciente", EMAIL_MAXIMO)
                .toLowerCase(Locale.ROOT);
        if (!FORMATO_EMAIL.matcher(valido).matches()) {
            throw new ValidacionDeDominioException("El email del paciente no tiene un formato válido");
        }
        return valido;
    }

    public Long getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public Instant getConsentimientoPrivacidadEn() {
        return consentimientoPrivacidadEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Paciente that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Paciente.class.hashCode();
    }

    @Override
    public String toString() {
        return "Paciente[id=" + id + "]";
    }
}
