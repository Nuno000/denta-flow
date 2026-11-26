package ar.dentaflow.presentacion.reportes;

import ar.dentaflow.dominio.reportes.AusentismoPorDiaSemana;
import ar.dentaflow.dominio.reportes.AusentismoPorFranja;
import ar.dentaflow.dominio.reportes.AusentismoPorPaciente;
import ar.dentaflow.dominio.reportes.AusentismoPorProfesional;
import ar.dentaflow.dominio.reportes.ReporteDeAusentismo;
import ar.dentaflow.dominio.reportes.ResumenAusentismo;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato JSON de {@code GET /api/reportes/ausentismo} (C-23, US-031). Nunca expone entidades JPA ni datos
 * personales sensibles del paciente (DNI, teléfono, email o token; Ley 25.326).
 */
public record ReporteDeAusentismoRespuesta(
        LocalDate desde,
        LocalDate hasta,
        Long profesionalId,
        int minimoAusencias,
        ResumenRespuesta general,
        List<ProfesionalRespuesta> porProfesional,
        List<FranjaRespuesta> porFranja,
        List<DiaSemanaRespuesta> porDiaSemana,
        List<PacienteRespuesta> pacientesReiterados) {

    public record ResumenRespuesta(
            int totalEvaluados,
            int asistidos,
            int ausentesSinAviso,
            int ausentesConAviso,
            int totalAusentes,
            double tasaAusentismo) {

        static ResumenRespuesta de(ResumenAusentismo r) {
            return new ResumenRespuesta(
                    r.totalEvaluados(), r.asistidos(), r.ausentesSinAviso(),
                    r.ausentesConAviso(), r.totalAusentes(), r.tasaAusentismo());
        }
    }

    public record ProfesionalRespuesta(
            long profesionalId,
            String nombre,
            String apellido,
            int totalEvaluados,
            int asistidos,
            int ausentesSinAviso,
            int ausentesConAviso,
            int totalAusentes,
            double tasaAusentismo) {

        static ProfesionalRespuesta de(AusentismoPorProfesional p) {
            ResumenAusentismo r = p.resumen();
            return new ProfesionalRespuesta(
                    p.profesionalId(), p.nombre(), p.apellido(),
                    r.totalEvaluados(), r.asistidos(), r.ausentesSinAviso(),
                    r.ausentesConAviso(), r.totalAusentes(), r.tasaAusentismo());
        }
    }

    public record FranjaRespuesta(
            String codigo,
            String etiqueta,
            int totalEvaluados,
            int asistidos,
            int ausentesSinAviso,
            int ausentesConAviso,
            int totalAusentes,
            double tasaAusentismo) {

        static FranjaRespuesta de(AusentismoPorFranja f) {
            ResumenAusentismo r = f.resumen();
            return new FranjaRespuesta(
                    f.franja().codigo(), f.franja().etiqueta(),
                    r.totalEvaluados(), r.asistidos(), r.ausentesSinAviso(),
                    r.ausentesConAviso(), r.totalAusentes(), r.tasaAusentismo());
        }
    }

    public record DiaSemanaRespuesta(
            int dia,
            String etiqueta,
            int totalEvaluados,
            int asistidos,
            int ausentesSinAviso,
            int ausentesConAviso,
            int totalAusentes,
            double tasaAusentismo) {

        static DiaSemanaRespuesta de(AusentismoPorDiaSemana d) {
            ResumenAusentismo r = d.resumen();
            return new DiaSemanaRespuesta(
                    d.diaSemana().getValue(), d.etiqueta(),
                    r.totalEvaluados(), r.asistidos(), r.ausentesSinAviso(),
                    r.ausentesConAviso(), r.totalAusentes(), r.tasaAusentismo());
        }
    }

    public record PacienteRespuesta(
            long pacienteId,
            String nombre,
            String apellido,
            int totalEvaluados,
            int asistidos,
            int ausentesSinAviso,
            int ausentesConAviso,
            int totalAusentes,
            double tasaAusentismo) {

        static PacienteRespuesta de(AusentismoPorPaciente p) {
            ResumenAusentismo r = p.resumen();
            return new PacienteRespuesta(
                    p.pacienteId(), p.nombre(), p.apellido(),
                    r.totalEvaluados(), r.asistidos(), r.ausentesSinAviso(),
                    r.ausentesConAviso(), r.totalAusentes(), r.tasaAusentismo());
        }
    }

    public static ReporteDeAusentismoRespuesta de(ReporteDeAusentismo reporte) {
        return new ReporteDeAusentismoRespuesta(
                reporte.desde(),
                reporte.hasta(),
                reporte.profesionalId(),
                reporte.minimoAusencias(),
                ResumenRespuesta.de(reporte.general()),
                reporte.porProfesional().stream().map(ProfesionalRespuesta::de).toList(),
                reporte.porFranja().stream().map(FranjaRespuesta::de).toList(),
                reporte.porDiaSemana().stream().map(DiaSemanaRespuesta::de).toList(),
                reporte.pacientesReiterados().stream().map(PacienteRespuesta::de).toList());
    }
}
