package ar.dentaflow.dominio.reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Contadores agregados de asistencia y tasa de ausentismo redondeada a un decimal (C-23, design D1). */
public record ResumenAusentismo(
        int totalEvaluados,
        int asistidos,
        int ausentesSinAviso,
        int ausentesConAviso,
        int totalAusentes,
        double tasaAusentismo) {

    public static final ResumenAusentismo VACIO = new ResumenAusentismo(0, 0, 0, 0, 0, 0.0);

    public static ResumenAusentismo desdeConteos(int asistidos, int ausentesSinAviso, int ausentesConAviso) {
        int totalAusentes = ausentesSinAviso + ausentesConAviso;
        int totalEvaluados = asistidos + totalAusentes;
        if (totalEvaluados == 0) {
            return VACIO;
        }
        double tasa = BigDecimal.valueOf(totalAusentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalEvaluados), 1, RoundingMode.HALF_UP)
                .doubleValue();
        return new ResumenAusentismo(totalEvaluados, asistidos, ausentesSinAviso, ausentesConAviso, totalAusentes, tasa);
    }
}
