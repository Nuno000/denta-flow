package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango.ProfesionalEnAgenda;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango.SillonEnAgenda;
import ar.dentaflow.dominio.turnos.OcupacionDeRecurso;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Regla pura de cálculo de huecos (RN-DI-01, RN-TU-04, RN-TU-07, PA-05, PA-06). Reutiliza
 * {@link CalendarioDeDisponibilidad} para horarios y bloqueos y {@link ZonaHorariaConsultorio} para
 * interpretar días y franjas. Sin dependencias de Spring ni JPA.
 */
public final class CalculadoraDeHuecos {

    private CalculadoraDeHuecos() {
    }

    /** Calcula todos los huecos del rango ordenados por inicio, cada uno con sus candidatos. */
    public static List<Hueco> calcular(AgendaDelRango agenda) {
        if (agenda.desde().isAfter(agenda.hasta())) {
            return List.of();
        }
        Duration duracion = Duration.ofMinutes(agenda.duracionMinutos());
        Map<Instant, List<CandidatoDeReserva>> candidatosPorInicio = new TreeMap<>();

        for (LocalDate fecha = agenda.desde(); !fecha.isAfter(agenda.hasta()); fecha = fecha.plusDays(1)) {
            for (ProfesionalEnAgenda profesional : agenda.profesionales()) {
                if (!profesional.activo()) {
                    continue;
                }
                for (FranjaLocal franja : CalendarioDeDisponibilidad.franjasDelDia(profesional.horarios(), fecha)) {
                    agregarHuecosDeFranja(agenda, profesional, fecha, franja, duracion, candidatosPorInicio);
                }
            }
        }

        return candidatosPorInicio.entrySet().stream()
                .map(entry -> new Hueco(entry.getKey(), entry.getKey().plus(duracion), entry.getValue()))
                .toList();
    }

    /** Devuelve los candidatos ordenados para un instante concreto (cualquier minuto). */
    public static List<CandidatoDeReserva> candidatosEn(AgendaDelRango agenda, Instant inicio) {
        if (inicio.isBefore(agenda.ahora())) {
            return List.of();
        }
        Instant fin = inicio.plus(Duration.ofMinutes(agenda.duracionMinutos()));
        List<CandidatoDeReserva> candidatos = new ArrayList<>();
        for (ProfesionalEnAgenda profesional : agenda.profesionales()) {
            if (!profesionalLibre(profesional, agenda.ocupacionesProfesionales(), inicio, fin)) {
                continue;
            }
            agregarCandidatosDeSillones(agenda, profesional.id(), inicio, fin, candidatos);
        }
        return List.copyOf(candidatos);
    }

    private static void agregarHuecosDeFranja(AgendaDelRango agenda, ProfesionalEnAgenda profesional,
            LocalDate fecha, FranjaLocal franja, Duration duracion,
            Map<Instant, List<CandidatoDeReserva>> candidatosPorInicio) {
        LocalDateTime cursor = LocalDateTime.of(fecha, franja.inicio());
        LocalDateTime finFranja = LocalDateTime.of(fecha, franja.fin());
        while (!cursor.plus(duracion).isAfter(finFranja)) {
            Instant inicio = ZonaHorariaConsultorio.aInstante(cursor);
            if (!inicio.isBefore(agenda.ahora())) {
                Instant fin = inicio.plus(duracion);
                if (profesionalLibre(profesional, agenda.ocupacionesProfesionales(), inicio, fin)) {
                    List<CandidatoDeReserva> candidatosDelProfesional = new ArrayList<>();
                    agregarCandidatosDeSillones(agenda, profesional.id(), inicio, fin, candidatosDelProfesional);
                    if (!candidatosDelProfesional.isEmpty()) {
                        candidatosPorInicio
                                .computeIfAbsent(inicio, k -> new ArrayList<>())
                                .addAll(candidatosDelProfesional);
                    }
                }
            }
            cursor = cursor.plusMinutes(agenda.pasoMinutos());
        }
    }

    private static void agregarCandidatosDeSillones(AgendaDelRango agenda, Long profesionalId,
            Instant inicio, Instant fin, List<CandidatoDeReserva> destino) {
        for (SillonEnAgenda sillon : agenda.sillones()) {
            if (sillonLibre(sillon, agenda.ocupacionesSillones(), inicio, fin)) {
                destino.add(new CandidatoDeReserva(profesionalId, sillon.id()));
            }
        }
    }

    private static boolean profesionalLibre(ProfesionalEnAgenda profesional,
            Collection<OcupacionDeRecurso> ocupaciones, Instant inicio, Instant fin) {
        return CalendarioDeDisponibilidad.profesionalDisponible(
                profesional.id(), profesional.activo(), profesional.horarios(), profesional.bloqueos(), inicio, fin)
                && !estaOcupado(profesional.id(), ocupaciones, inicio, fin);
    }

    private static boolean sillonLibre(SillonEnAgenda sillon,
            Collection<OcupacionDeRecurso> ocupaciones, Instant inicio, Instant fin) {
        return CalendarioDeDisponibilidad.sillonDisponible(
                sillon.id(), sillon.activo(), sillon.bloqueos(), inicio, fin)
                && !estaOcupado(sillon.id(), ocupaciones, inicio, fin);
    }

    private static boolean estaOcupado(Long recursoId, Collection<OcupacionDeRecurso> ocupaciones,
            Instant inicio, Instant fin) {
        return ocupaciones.stream()
                .anyMatch(o -> Objects.equals(o.recursoId(), recursoId) && o.seSuperponeCon(inicio, fin));
    }
}
