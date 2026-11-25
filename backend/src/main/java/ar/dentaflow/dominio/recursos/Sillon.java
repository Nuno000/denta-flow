package ar.dentaflow.dominio.recursos;

import ar.dentaflow.dominio.comun.ValidacionesDeTexto;

/**
 * Sillón del consultorio: un turno ocupa un profesional y un sillón a la vez (RN-TU-07).
 * Se desactiva en lugar de borrarse.
 */
public class Sillon {

    public static final int NOMBRE_MAXIMO = 50;

    private Long id;
    private String nombre;
    private boolean activo;

    protected Sillon() {
    }

    private Sillon(String nombre) {
        this.nombre = validarNombre(nombre);
        this.activo = true;
    }

    /** Crea un sillón activo. */
    public static Sillon nuevo(String nombre) {
        return new Sillon(nombre);
    }

    /** Cambia el nombre con la misma validación del alta; si falla no cambia nada. */
    public void renombrar(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    private static String validarNombre(String nombre) {
        return ValidacionesDeTexto.obligatorio(nombre, "nombre del sillón", NOMBRE_MAXIMO);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Sillon that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Sillon.class.hashCode();
    }
}
