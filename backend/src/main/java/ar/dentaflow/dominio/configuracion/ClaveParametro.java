package ar.dentaflow.dominio.configuracion;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ValidacionesDeTexto;

/**
 * Catálogo cerrado de parámetros del consultorio (design D2). El orden de las constantes es el orden
 * en que se listan. Cada clave conoce su tipo y sus reglas; agregar un parámetro es agregar una
 * constante y una fila en la migración de su change.
 */
public enum ClaveParametro {

    ANTICIPACION_MINIMA_HORAS("anticipacion-minima-horas",
            "Anticipación mínima, en horas, para reservar, reprogramar o cancelar un turno",
            TipoParametro.ENTERO, "horas", 0, 168, null, "anticipación mínima"),
    AVISO_PRIVACIDAD_TEXTO("aviso-privacidad-texto",
            "Texto del aviso de privacidad que se muestra al paciente (Ley 25.326)",
            TipoParametro.TEXTO, null, null, null, 4000, "texto del aviso de privacidad"),
    AVISO_PRIVACIDAD_LEYENDA("aviso-privacidad-leyenda",
            "Leyenda de la casilla de aceptación del aviso de privacidad",
            TipoParametro.TEXTO, null, null, null, 200, "texto de la leyenda de aceptación"),
    /** C-16: se muestra en la reserva pública cuando no hay huecos o el horario ya no está disponible. */
    CONTACTO_CONSULTORIO("contacto-consultorio",
            "Contacto del consultorio que ve el paciente cuando no puede reservar online",
            TipoParametro.TEXTO, null, null, null, 300, "contacto del consultorio"),
    /** C-21: mensaje de WhatsApp prellenado que recepción abre desde la agenda (design D3). */
    WHATSAPP_PLANTILLA("whatsapp-plantilla",
            "Plantilla del mensaje de WhatsApp para recordar o confirmar un turno; admite {nombre}, {fecha}, {hora}, "
                    + "{prestacion}, {profesional} y {enlace}",
            TipoParametro.TEXTO, null, null, null, 1000, "plantilla del mensaje de WhatsApp");

    private final String codigo;
    private final String descripcion;
    private final TipoParametro tipo;
    private final String unidad;
    private final Integer minimo;
    private final Integer maximo;
    private final Integer longitudMaxima;
    private final String nombreDelDato;

    ClaveParametro(String codigo, String descripcion, TipoParametro tipo, String unidad, Integer minimo,
            Integer maximo, Integer longitudMaxima, String nombreDelDato) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.unidad = unidad;
        this.minimo = minimo;
        this.maximo = maximo;
        this.longitudMaxima = longitudMaxima;
        this.nombreDelDato = nombreDelDato;
    }

    /** Devuelve la clave por su código; falla con "no encontrado" si no está en el catálogo. */
    public static ClaveParametro desdeCodigo(String codigo) {
        for (ClaveParametro clave : values()) {
            if (clave.codigo.equals(codigo)) {
                return clave;
            }
        }
        throw new RecursoNoEncontradoException("No existe el parámetro " + codigo);
    }

    /** Valida y normaliza un valor para esta clave; no modifica nada si falla. */
    String normalizar(String valor) {
        if (tipo == TipoParametro.TEXTO) {
            return ValidacionesDeTexto.obligatorio(valor, nombreDelDato, longitudMaxima);
        }
        String recortado = valor == null ? "" : valor.strip();
        if (recortado.matches("-?\\d{1,9}")) {
            int entero = Integer.parseInt(recortado);
            if (entero >= minimo && entero <= maximo) {
                return String.valueOf(entero);
            }
        }
        throw new ValidacionDeDominioException("La " + nombreDelDato + " debe ser un número entero de "
                + unidad + " entre " + minimo + " y " + maximo);
    }

    public String codigo() {
        return codigo;
    }

    public String descripcion() {
        return descripcion;
    }

    public TipoParametro tipo() {
        return tipo;
    }

    public String unidad() {
        return unidad;
    }

    public Integer minimo() {
        return minimo;
    }

    public Integer maximo() {
        return maximo;
    }

    public Integer longitudMaxima() {
        return longitudMaxima;
    }
}
