package ar.dentaflow.dominio.recursos;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ValidacionesDeTexto;

/**
 * Tratamiento que ofrece el consultorio. Su duración define cuánto dura el turno (RN-TU-03).
 * Se desactiva en lugar de borrarse: los turnos pasados la siguen referenciando.
 */
public class Prestacion {

    public static final int NOMBRE_MAXIMO = 100;

    private Long id;
    private String nombre;
    private Integer duracionMinutos;
    private boolean activa;

    protected Prestacion() {
    }

    private Prestacion(String nombre, Integer duracionMinutos) {
        this.nombre = ValidacionesDeTexto.obligatorio(nombre, "nombre de la prestación", NOMBRE_MAXIMO);
        this.duracionMinutos = validarDuracion(duracionMinutos);
        this.activa = true;
    }

    /** Crea una prestación activa; la duración debe ser mayor que cero. */
    public static Prestacion nueva(String nombre, Integer duracionMinutos) {
        return new Prestacion(nombre, duracionMinutos);
    }

    /** Cambia la duración; no afecta a los turnos ya creados. Si es inválida no cambia nada. */
    public void cambiarDuracion(Integer duracionMinutos) {
        this.duracionMinutos = validarDuracion(duracionMinutos);
    }

    public void desactivar() {
        this.activa = false;
    }

    public void activar() {
        this.activa = true;
    }

    private static Integer validarDuracion(Integer duracionMinutos) {
        if (duracionMinutos == null || duracionMinutos <= 0) {
            throw new ValidacionDeDominioException("La duración debe ser mayor que cero");
        }
        return duracionMinutos;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public boolean isActiva() {
        return activa;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Prestacion that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Prestacion.class.hashCode();
    }
}
