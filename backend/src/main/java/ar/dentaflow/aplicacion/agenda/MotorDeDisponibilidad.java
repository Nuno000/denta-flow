package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango.ProfesionalEnAgenda;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango.SillonEnAgenda;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.CalculadoraDeHuecos;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import ar.dentaflow.dominio.turnos.OcupacionDeRecurso;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor único de disponibilidad (C-10, design D3). Lee horarios, bloqueos y ocupaciones en modo
 * solo lectura sin tomar bloqueos de fila y delega el cálculo en {@link CalculadoraDeHuecos}.
 */
@Service
@Transactional(readOnly = true)
public class MotorDeDisponibilidad {

    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final SillonRepositorio sillones;
    private final HorarioAtencionRepositorio horarios;
    private final BloqueoRepositorio bloqueos;
    private final TurnoRepositorio turnos;
    private final Clock reloj;
    private final ReglasDeConsultaDeHuecos reglas;

    public MotorDeDisponibilidad(PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales,
            SillonRepositorio sillones, HorarioAtencionRepositorio horarios, BloqueoRepositorio bloqueos,
            TurnoRepositorio turnos, Clock reloj, ReglasDeConsultaDeHuecos reglas) {
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.horarios = horarios;
        this.bloqueos = bloqueos;
        this.turnos = turnos;
        this.reloj = reloj;
        this.reglas = reglas;
    }

    /** Devuelve los huecos disponibles para la consulta dada, ordenados por inicio. */
    public List<Hueco> buscarHuecos(ConsultaDeHuecos consulta) {
        if (consulta == null || consulta.prestacionId() == null) {
            throw new ValidacionDeDominioException("La prestación es obligatoria");
        }
        if (consulta.desde() == null || consulta.hasta() == null) {
            throw new ValidacionDeDominioException("Las fechas desde y hasta son obligatorias");
        }
        if (consulta.desde().isAfter(consulta.hasta())) {
            throw new ValidacionDeDominioException("La fecha desde no puede ser posterior a hasta");
        }
        long dias = ChronoUnit.DAYS.between(consulta.desde(), consulta.hasta()) + 1;
        if (dias > reglas.diasMaximos()) {
            throw new ValidacionDeDominioException(
                    "El rango consultado no puede superar los " + reglas.diasMaximos() + " días");
        }

        Prestacion prestacion = prestacionActiva(consulta.prestacionId());
        List<Profesional> candidatos = profesionalesCandidatos(consulta.prestacionId(), consulta.profesionalId());

        LocalDate hoy = ZonaHorariaConsultorio.hoy(reloj);
        if (consulta.hasta().isBefore(hoy) || candidatos.isEmpty()) {
            return List.of();
        }
        LocalDate desdeEfectivo = consulta.desde().isBefore(hoy) ? hoy : consulta.desde();
        AgendaDelRango agenda = armarAgenda(prestacion, candidatos, desdeEfectivo, consulta.hasta(),
                consulta.turnoAExcluir());
        if (agenda == null) {
            return List.of();
        }
        return CalculadoraDeHuecos.calcular(agenda);
    }

    /** Devuelve los candidatos ordenados para un instante concreto (cualquier minuto). */
    public List<CandidatoDeReserva> candidatosPara(Long prestacionId, Long profesionalId, Instant inicio,
            Long turnoAExcluir) {
        if (prestacionId == null) {
            throw new ValidacionDeDominioException("La prestación es obligatoria");
        }
        if (inicio == null) {
            throw new ValidacionDeDominioException("El inicio del turno es obligatorio");
        }
        Prestacion prestacion = prestacionActiva(prestacionId);
        List<Profesional> candidatos = profesionalesCandidatos(prestacionId, profesionalId);
        if (inicio.isBefore(reloj.instant()) || candidatos.isEmpty()) {
            return List.of();
        }
        LocalDate fecha = ZonaHorariaConsultorio.aHoraLocal(inicio).toLocalDate();
        AgendaDelRango agenda = armarAgenda(prestacion, candidatos, fecha, fecha, turnoAExcluir);
        if (agenda == null) {
            return List.of();
        }
        return CalculadoraDeHuecos.candidatosEn(agenda, inicio);
    }

    private Prestacion prestacionActiva(Long prestacionId) {
        Prestacion prestacion = prestaciones.buscarPorId(prestacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la prestación " + prestacionId));
        if (!prestacion.isActiva()) {
            throw new ValidacionDeDominioException("La prestación no está activa");
        }
        return prestacion;
    }

    private List<Profesional> profesionalesCandidatos(Long prestacionId, Long profesionalId) {
        if (profesionalId == null) {
            return profesionales.listarActivosQueRealizan(prestacionId);
        }
        Profesional profesional = profesionales.buscarPorId(profesionalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el profesional " + profesionalId));
        if (!profesional.isActivo()) {
            throw new ValidacionDeDominioException("El profesional no está activo");
        }
        if (!profesional.realiza(prestacionId)) {
            throw new ValidacionDeDominioException("El profesional no realiza la prestación elegida");
        }
        return List.of(profesional);
    }

    private AgendaDelRango armarAgenda(Prestacion prestacion, List<Profesional> candidatos,
            LocalDate desde, LocalDate hasta, Long turnoAExcluir) {
        List<Sillon> sillonesActivos = sillones.listarActivos();
        if (sillonesActivos.isEmpty()) {
            return null;
        }
        Instant desdeUtc = ZonaHorariaConsultorio.aInstante(desde.atStartOfDay());
        Instant hastaUtc = ZonaHorariaConsultorio.aInstante(hasta.plusDays(1).atStartOfDay());

        List<Long> profIds = candidatos.stream().map(Profesional::getId).toList();
        List<Long> sillonIds = sillonesActivos.stream().map(Sillon::getId).toList();

        List<ProfesionalEnAgenda> profsEnAgenda = candidatos.stream()
                .map(p -> new ProfesionalEnAgenda(
                        p.getId(),
                        p.isActivo(),
                        horarios.listarDelProfesional(p.getId()),
                        bloqueos.listarQueAfectanAProfesional(p.getId(), desdeUtc, hastaUtc)))
                .toList();

        List<SillonEnAgenda> sillonesEnAgenda = sillonesActivos.stream()
                .map(s -> new SillonEnAgenda(
                        s.getId(),
                        s.isActivo(),
                        bloqueos.listarQueAfectanASillon(s.getId(), desdeUtc, hastaUtc)))
                .toList();

        List<OcupacionDeRecurso> ocupacionesProf = turnos.listarOcupacionesDeProfesionales(
                profIds, desdeUtc, hastaUtc, turnoAExcluir);
        List<OcupacionDeRecurso> ocupacionesSillon = turnos.listarOcupacionesDeSillones(
                sillonIds, desdeUtc, hastaUtc, turnoAExcluir);

        return new AgendaDelRango(
                prestacion.getDuracionMinutos(),
                desde,
                hasta,
                reloj.instant(),
                reglas.pasoMinutos(),
                profsEnAgenda,
                sillonesEnAgenda,
                ocupacionesProf,
                ocupacionesSillon);
    }
}
