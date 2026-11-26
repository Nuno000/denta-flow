package ar.dentaflow.dominio.listaespera;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class ListaEspera {

    private Long id;
    private Long pacienteId;
    private Long prestacionId;
    private Long profesionalId;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private LocalTime horaDesde;
    private LocalTime horaHasta;
    private EstadoListaEspera estado;
    private Long turnoOrigenId;
    private Long profesionalOfrecidoId;
    private Instant inicioOfrecido;
    private String tokenAceptacion;
    private Instant ofrecidaEn;
    private Instant ofrecidaHasta;
    private Long turnoCreadoId;
    private Long creadoPorUsuarioId;
    private Instant creadoEn;

    protected ListaEspera() {
    }

    private ListaEspera(Long pacienteId, Long prestacionId, Long profesionalId,
            LocalDate fechaDesde, LocalDate fechaHasta, LocalTime horaDesde, LocalTime horaHasta,
            Long creadoPorUsuarioId, Instant creadoEn) {
        if (pacienteId == null || prestacionId == null || creadoPorUsuarioId == null || creadoEn == null) {
            throw new ValidacionDeDominioException("Faltan datos obligatorios para la lista de espera");
        }
        if (fechaDesde == null || fechaHasta == null || fechaHasta.isBefore(fechaDesde)) {
            throw new ValidacionDeDominioException("El rango de fechas es inválido: la fecha hasta no puede ser anterior a la fecha desde");
        }
        if ((horaDesde == null) != (horaHasta == null)) {
            throw new ValidacionDeDominioException("Si se indica un rango horario deben informarse hora desde y hora hasta");
        }
        if (horaDesde != null && !horaHasta.isAfter(horaDesde)) {
            throw new ValidacionDeDominioException("La hora hasta debe ser posterior a la hora desde");
        }
        this.pacienteId = pacienteId;
        this.prestacionId = prestacionId;
        this.profesionalId = profesionalId;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        this.estado = EstadoListaEspera.ACTIVA;
        this.creadoPorUsuarioId = creadoPorUsuarioId;
        this.creadoEn = creadoEn;
    }

    public static ListaEspera nueva(Long pacienteId, Long prestacionId, Long profesionalId,
            LocalDate fechaDesde, LocalDate fechaHasta, LocalTime horaDesde, LocalTime horaHasta,
            Long creadoPorUsuarioId, Instant creadoEn) {
        return new ListaEspera(pacienteId, prestacionId, profesionalId, fechaDesde, fechaHasta,
                horaDesde, horaHasta, creadoPorUsuarioId, creadoEn);
    }

    public boolean esCompatibleCon(long prestacionId, long profesionalId, LocalDate fechaLocal, LocalTime horaLocal) {
        if (this.estado != EstadoListaEspera.ACTIVA) {
            return false;
        }
        if (this.prestacionId != prestacionId) {
            return false;
        }
        if (this.profesionalId != null && this.profesionalId != profesionalId) {
            return false;
        }
        if (fechaLocal.isBefore(this.fechaDesde) || fechaLocal.isAfter(this.fechaHasta)) {
            return false;
        }
        if (this.horaDesde != null && (horaLocal.isBefore(this.horaDesde) || !horaLocal.isBefore(this.horaHasta))) {
            return false;
        }
        return true;
    }

    public void ofrecer(long turnoOrigenId, long profesionalOfrecidoId, Instant inicioOfrecido,
            String tokenAceptacion, Instant ofrecidaEn, Instant ofrecidaHasta) {
        if (this.estado != EstadoListaEspera.ACTIVA) {
            throw new ConflictoException("Solo una entrada activa puede recibir una oferta");
        }
        this.estado = EstadoListaEspera.OFRECIDA;
        this.turnoOrigenId = turnoOrigenId;
        this.profesionalOfrecidoId = profesionalOfrecidoId;
        this.inicioOfrecido = inicioOfrecido;
        this.tokenAceptacion = tokenAceptacion;
        this.ofrecidaEn = ofrecidaEn;
        this.ofrecidaHasta = ofrecidaHasta;
    }

    public boolean estaVencida(Instant ahora) {
        return this.estado == EstadoListaEspera.OFRECIDA
                && this.ofrecidaHasta != null
                && !ahora.isBefore(this.ofrecidaHasta);
    }

    public void marcarAtendida(long turnoCreadoId) {
        if (this.estado != EstadoListaEspera.OFRECIDA) {
            throw new ConflictoException("Solo una oferta vigente puede marcarse como atendida");
        }
        this.estado = EstadoListaEspera.ATENDIDA;
        this.turnoCreadoId = turnoCreadoId;
    }

    public void marcarVencida() {
        if (this.estado == EstadoListaEspera.ATENDIDA) {
            return;
        }
        this.estado = EstadoListaEspera.VENCIDA;
    }

    public void darDeBaja() {
        if (this.estado != EstadoListaEspera.ACTIVA) {
            throw new ConflictoException("Solo se pueden dar de baja entradas en estado activa");
        }
        this.estado = EstadoListaEspera.VENCIDA;
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getPrestacionId() { return prestacionId; }
    public Long getProfesionalId() { return profesionalId; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public LocalDate getFechaHasta() { return fechaHasta; }
    public LocalTime getHoraDesde() { return horaDesde; }
    public LocalTime getHoraHasta() { return horaHasta; }
    public EstadoListaEspera getEstado() { return estado; }
    public Long getTurnoOrigenId() { return turnoOrigenId; }
    public Long getProfesionalOfrecidoId() { return profesionalOfrecidoId; }
    public Instant getInicioOfrecido() { return inicioOfrecido; }
    public String getTokenAceptacion() { return tokenAceptacion; }
    public Instant getOfrecidaEn() { return ofrecidaEn; }
    public Instant getOfrecidaHasta() { return ofrecidaHasta; }
    public Long getTurnoCreadoId() { return turnoCreadoId; }
    public Long getCreadoPorUsuarioId() { return creadoPorUsuarioId; }
    public Instant getCreadoEn() { return creadoEn; }
}
