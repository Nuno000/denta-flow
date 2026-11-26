package ar.dentaflow.dominio.reportes;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo puro de métricas de ausentismo a partir de registros de asistencia (C-23, US-031; design D1–D5).
 * Sin dependencias de Spring ni JPA. Solo contabiliza {@code asistio}, {@code ausente} y {@code ausente_con_aviso},
 * clasificando franjas y días de semana en {@code America/Argentina/Buenos_Aires}.
 */
public final class CalculadorDeAusentismo {

    private static final Comparator<AusentismoPorProfesional> ORDEN_PROFESIONALES = Comparator
            .comparingDouble((AusentismoPorProfesional p) -> p.resumen().tasaAusentismo()).reversed()
            .thenComparing(Comparator.comparingInt((AusentismoPorProfesional p) -> p.resumen().totalAusentes()).reversed())
            .thenComparing(AusentismoPorProfesional::apellido, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(AusentismoPorProfesional::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(AusentismoPorProfesional::profesionalId);

    private static final Comparator<AusentismoPorPaciente> ORDEN_PACIENTES = Comparator
            .comparingInt((AusentismoPorPaciente p) -> p.resumen().totalAusentes()).reversed()
            .thenComparing(Comparator.comparingDouble((AusentismoPorPaciente p) -> p.resumen().tasaAusentismo()).reversed())
            .thenComparing(AusentismoPorPaciente::apellido, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(AusentismoPorPaciente::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(AusentismoPorPaciente::pacienteId);

    private CalculadorDeAusentismo() {
    }

    public static ReporteDeAusentismo calcular(
            List<RegistroAsistenciaParaReporte> registros,
            LocalDate desde,
            LocalDate hasta,
            Long profesionalId,
            int minimoAusencias) {

        Acumulador general = new Acumulador();
        Map<Long, AcumuladorNombrado> porProfesional = new LinkedHashMap<>();
        Map<FranjaHoraria, Acumulador> porFranja = new EnumMap<>(FranjaHoraria.class);
        for (FranjaHoraria franja : FranjaHoraria.values()) {
            porFranja.put(franja, new Acumulador());
        }
        Map<DayOfWeek, Acumulador> porDia = new EnumMap<>(DayOfWeek.class);
        Map<Long, AcumuladorNombrado> porPaciente = new LinkedHashMap<>();

        for (RegistroAsistenciaParaReporte registro : registros) {
            if (!esEvaluado(registro.estado())) {
                continue;
            }
            general.sumar(registro.estado());

            LocalDateTime horaLocal = ZonaHorariaConsultorio.aHoraLocal(registro.inicio());
            FranjaHoraria franja = FranjaHoraria.desdeHoraLocal(horaLocal.getHour());
            porFranja.get(franja).sumar(registro.estado());

            DayOfWeek dia = horaLocal.getDayOfWeek();
            porDia.computeIfAbsent(dia, clave -> new Acumulador()).sumar(registro.estado());

            porProfesional
                    .computeIfAbsent(registro.profesionalId(),
                            id -> new AcumuladorNombrado(id, registro.profesionalNombre(), registro.profesionalApellido()))
                    .acumulador.sumar(registro.estado());

            porPaciente
                    .computeIfAbsent(registro.pacienteId(),
                            id -> new AcumuladorNombrado(id, registro.pacienteNombre(), registro.pacienteApellido()))
                    .acumulador.sumar(registro.estado());
        }

        List<AusentismoPorProfesional> profesionales = porProfesional.values().stream()
                .map(p -> new AusentismoPorProfesional(p.id, p.nombre, p.apellido, p.acumulador.aResumen()))
                .sorted(ORDEN_PROFESIONALES)
                .toList();

        List<AusentismoPorFranja> franjas = Arrays.stream(FranjaHoraria.values())
                .map(franja -> new AusentismoPorFranja(franja, porFranja.get(franja).aResumen()))
                .toList();

        List<AusentismoPorDiaSemana> dias = Arrays.stream(DayOfWeek.values())
                .filter(porDia::containsKey)
                .map(dia -> new AusentismoPorDiaSemana(dia, nombreDeDia(dia), porDia.get(dia).aResumen()))
                .toList();

        List<AusentismoPorPaciente> pacientes = porPaciente.values().stream()
                .map(p -> new AusentismoPorPaciente(p.id, p.nombre, p.apellido, p.acumulador.aResumen()))
                .filter(p -> p.resumen().totalAusentes() >= minimoAusencias)
                .sorted(ORDEN_PACIENTES)
                .toList();

        return new ReporteDeAusentismo(
                desde, hasta, profesionalId, minimoAusencias, general.aResumen(),
                profesionales, franjas, dias, pacientes);
    }

    private static boolean esEvaluado(EstadoTurno estado) {
        return estado == EstadoTurno.ASISTIO
                || estado == EstadoTurno.AUSENTE
                || estado == EstadoTurno.AUSENTE_CON_AVISO;
    }

    private static String nombreDeDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }

    private static final class Acumulador {
        private int asistidos;
        private int ausentesSinAviso;
        private int ausentesConAviso;

        void sumar(EstadoTurno estado) {
            switch (estado) {
                case ASISTIO -> asistidos++;
                case AUSENTE -> ausentesSinAviso++;
                case AUSENTE_CON_AVISO -> ausentesConAviso++;
                default -> {
                }
            }
        }

        ResumenAusentismo aResumen() {
            return ResumenAusentismo.desdeConteos(asistidos, ausentesSinAviso, ausentesConAviso);
        }
    }

    private static final class AcumuladorNombrado {
        private final long id;
        private final String nombre;
        private final String apellido;
        private final Acumulador acumulador = new Acumulador();

        AcumuladorNombrado(long id, String nombre, String apellido) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
        }
    }
}
