package ar.dentaflow.dominio.reportes;

/**
 * Franjas horarias del consultorio para agrupar el ausentismo según la hora local
 * ({@code America/Argentina/Buenos_Aires}, C-23 design D4).
 */
public enum FranjaHoraria {
    MANANA("manana", "Mañana (antes de las 12:00)"),
    TARDE("tarde", "Tarde (12:00 a 17:59)"),
    NOCHE("noche", "Noche (desde las 18:00)");

    private final String codigo;
    private final String etiqueta;

    FranjaHoraria(String codigo, String etiqueta) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
    }

    public String codigo() {
        return codigo;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static FranjaHoraria desdeHoraLocal(int hora) {
        if (hora < 12) {
            return MANANA;
        }
        if (hora < 18) {
            return TARDE;
        }
        return NOCHE;
    }
}
