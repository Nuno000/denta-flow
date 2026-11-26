package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.aplicacion.auditoria.ServicioDeAuditoria;
import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.TipoDeActor;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import ar.dentaflow.dominio.turnos.CambioDeEstado;
import ar.dentaflow.dominio.turnos.CambioDeReprogramacion;
import ar.dentaflow.dominio.turnos.CerrojoDeRecursos;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.GeneradorDeTokens;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.RecursoOcupado;
import ar.dentaflow.dominio.turnos.RestriccionDeAgenda;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoPropuesto;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Única vía de escritura de turnos (RN-GL-02, design D5 y D6). Garantiza que un profesional o un sillón no
 * tengan dos turnos superpuestos, con bloqueo pesimista en orden fijo (profesional, sillón, turno), lectura del
 * último estado confirmado ({@code READ COMMITTED}) y auditoría en la misma transacción (C-08).
 *
 * <p><b>Contrato para C-10..C-17:</b>
 * <ul>
 *   <li>Llamar a {@link #crear} y {@link #reprogramar} fuera de transacciones ajenas, o dentro de una con
 *       aislamiento {@code READ COMMITTED}: con otro aislamiento fallan con {@link IllegalStateException} en vez de
 *       verificar con una foto vieja.</li>
 *   <li>Quien prueba varios candidatos (C-10) debe llamar a {@link #crear} una vez por transacción y capturar
 *       {@link TurnoSolapadoException} por candidato: cada llamada es atómica y una falla no deja nada escrito.</li>
 *   <li>Los errores de bloqueo de la base ({@code PessimisticLockingFailureException}) se propagan sin reintento.</li>
 *   <li>El rol (recepción o administrador para el sobreturno, RN-SO-01) y la anticipación mínima (C-17) los verifica
 *       quien llama antes de invocar.</li>
 *   <li>El paciente se registra antes con {@code ServicioDePacientes}, en otra transacción.</li>
 * </ul>
 */
@Service
public class ServicioDeTurnos {

    private final TurnoRepositorio turnos;
    private final PacienteRepositorio pacientes;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final SillonRepositorio sillones;
    private final CerrojoDeRecursos cerrojo;
    private final GeneradorDeTokens generadorDeTokens;
    private final ObjectProvider<RestriccionDeAgenda> restricciones;
    private final ServicioDeAuditoria auditoria;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public ServicioDeTurnos(TurnoRepositorio turnos, PacienteRepositorio pacientes,
            PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales, SillonRepositorio sillones,
            CerrojoDeRecursos cerrojo, GeneradorDeTokens generadorDeTokens,
            ObjectProvider<RestriccionDeAgenda> restricciones, ServicioDeAuditoria auditoria, Clock reloj) {
        this(turnos, pacientes, prestaciones, profesionales, sillones, cerrojo, generadorDeTokens, restricciones,
                auditoria, evento -> {}, reloj);
    }

    @Autowired
    public ServicioDeTurnos(TurnoRepositorio turnos, PacienteRepositorio pacientes,
            PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales, SillonRepositorio sillones,
            CerrojoDeRecursos cerrojo, GeneradorDeTokens generadorDeTokens,
            ObjectProvider<RestriccionDeAgenda> restricciones, ServicioDeAuditoria auditoria,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.cerrojo = cerrojo;
        this.generadorDeTokens = generadorDeTokens;
        this.restricciones = restricciones;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Crea un turno {@code pendiente} y lo audita como {@code creacion}. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TurnoCreado crear(SolicitudDeTurno solicitud) {
        exigirAislamientoReadCommitted();
        Long creador = validarOrigenYActor(solicitud);
        pacientes.buscarPorId(solicitud.pacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El paciente no existe"));
        Prestacion prestacion = prestaciones.buscarPorId(solicitud.prestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación no existe"));
        if (!prestacion.isActiva()) {
            throw new ValidacionDeDominioException("La prestación está inactiva");
        }
        validarInicioNoPasado(solicitud.inicio());

        cerrojo.bloquearProfesional(solicitud.profesionalId());
        cerrojo.bloquearSillon(solicitud.sillonId());
        validarRecursos(solicitud.profesionalId(), solicitud.sillonId(), prestacion);

        Turno turno = Turno.nuevo(solicitud.pacienteId(), solicitud.profesionalId(), solicitud.sillonId(),
                solicitud.prestacionId(), solicitud.inicio(), prestacion.getDuracionMinutos(), solicitud.origen(),
                solicitud.esSobreturno(), creador, reloj.instant(), generadorDeTokens.nuevoToken());

        verificarAgenda(turno, null);
        Turno guardado = turnos.guardar(turno);
        auditoria.registrarCambio(guardado.getId(), AccionDeTurno.CREACION, solicitud.actor(), null,
                guardado.valoresDeAlta());
        return new TurnoCreado(guardado, guardado.tokenAcceso());
    }

    /** {@code pendiente} → {@code confirmado}; auditado como {@code confirmacion}. Idempotente. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Turno confirmar(long turnoId, Actor actor) {
        return cambiarEstado(turnoId, EstadoTurno.CONFIRMADO, actor);
    }

    /** Cancela el turno y libera el horario; auditado como {@code cancelacion}. Idempotente. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Turno cancelar(long turnoId, Actor actor) {
        return cambiarEstado(turnoId, EstadoTurno.CANCELADO, actor);
    }

    /**
     * Cambia el estado según la tabla de transiciones. Confirmar y cancelar se auditan con su acción propia; la
     * asistencia y el resto como {@code cambio_estado}. Repetir el estado actual no cambia ni audita nada. Solo bloquea
     * la fila del turno (nunca recursos).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Turno cambiarEstado(long turnoId, EstadoTurno nuevo, Actor actor) {
        Turno turno = turnos.buscarPorIdParaActualizar(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
        CambioDeEstado cambio = turno.cambiarEstado(nuevo);
        if (!cambio.hayCambio()) {
            return turno;
        }
        Turno guardado = turnos.guardar(turno);
        auditoria.registrarCambio(turnoId, accionPara(nuevo), actor, Map.of("estado", cambio.anterior().codigo()),
                Map.of("estado", cambio.nuevo().codigo()));
        if (nuevo == EstadoTurno.CANCELADO) {
            eventos.publishEvent(new TurnoCanceladoEvent(
                    guardado.getId(), guardado.getPrestacionId(), guardado.getProfesionalId(), guardado.getInicio()));
        }
        return guardado;
    }

    /**
     * Mueve un turno {@code pendiente} o {@code confirmado} a otro horario, profesional o sillón con la misma
     * garantía de no solapamiento (excluye al propio turno). Conserva estado, paciente y token; recalcula el fin con
     * la duración vigente de la prestación; audita solo los campos que cambiaron.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Turno reprogramar(long turnoId, SolicitudDeReprogramacion solicitud, Actor actor) {
        exigirAislamientoReadCommitted();
        cerrojo.bloquearProfesional(solicitud.profesionalId());
        cerrojo.bloquearSillon(solicitud.sillonId());
        Turno turno = turnos.buscarPorIdParaActualizar(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
        turno.exigirReprogramable();
        validarInicioNoPasado(solicitud.inicio());
        Prestacion prestacion = prestaciones.buscarPorId(turno.getPrestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación no existe"));
        validarRecursos(solicitud.profesionalId(), solicitud.sillonId(), prestacion);

        Instant nuevoFin = solicitud.inicio().plusSeconds(prestacion.getDuracionMinutos() * 60L);
        TurnoPropuesto propuesto = new TurnoPropuesto(solicitud.profesionalId(), solicitud.sillonId(),
                turno.getPrestacionId(), solicitud.inicio(), nuevoFin, turno.isEsSobreturno());
        verificarAgenda(propuesto, turnoId);

        CambioDeReprogramacion cambio = turno.reprogramar(solicitud.inicio(), prestacion.getDuracionMinutos(),
                solicitud.profesionalId(), solicitud.sillonId());
        if (!cambio.hayCambios()) {
            return turno;
        }
        Turno guardado = turnos.guardar(turno);
        auditoria.registrarCambio(turnoId, AccionDeTurno.REPROGRAMACION, actor, cambio.anterior(), cambio.nuevo());
        return guardado;
    }

    @Transactional(readOnly = true)
    public Optional<Turno> buscarPorId(long turnoId) {
        return turnos.buscarPorId(turnoId);
    }

    /** Busca por token de acceso; un formato inválido devuelve vacío. */
    @Transactional(readOnly = true)
    public Optional<Turno> buscarPorToken(String token) {
        return turnos.buscarPorToken(token);
    }

    private static AccionDeTurno accionPara(EstadoTurno nuevo) {
        return switch (nuevo) {
            case CONFIRMADO -> AccionDeTurno.CONFIRMACION;
            case CANCELADO -> AccionDeTurno.CANCELACION;
            default -> AccionDeTurno.CAMBIO_ESTADO;
        };
    }

    /** Valida origen y actor coherentes y devuelve el id del usuario creador (solo en recepción). */
    private static Long validarOrigenYActor(SolicitudDeTurno solicitud) {
        Actor actor = solicitud.actor();
        if (solicitud.origen() == null || actor == null) {
            throw new ValidacionDeDominioException("El origen y el actor del turno son obligatorios");
        }
        if (solicitud.origen() == OrigenTurno.ONLINE && actor.tipo() != TipoDeActor.PACIENTE) {
            throw new ValidacionDeDominioException("Un turno online lo crea el paciente");
        }
        if (solicitud.origen() == OrigenTurno.RECEPCION && actor.tipo() != TipoDeActor.USUARIO) {
            throw new ValidacionDeDominioException("Un turno de recepción lo crea un usuario del personal");
        }
        if (solicitud.esSobreturno() && solicitud.origen() != OrigenTurno.RECEPCION) {
            throw new ValidacionDeDominioException("El sobreturno solo puede crearlo recepción");
        }
        return actor.usuarioId();
    }

    private void validarInicioNoPasado(Instant inicio) {
        if (inicio == null) {
            throw new ValidacionDeDominioException("El inicio del turno es obligatorio");
        }
        if (inicio.isBefore(reloj.instant())) {
            throw new ValidacionDeDominioException("El inicio del turno no puede ser anterior al momento actual");
        }
    }

    /** Con los bloqueos tomados: existen, están activos y el profesional realiza la prestación. */
    private void validarRecursos(long profesionalId, long sillonId, Prestacion prestacion) {
        Profesional profesional = profesionales.buscarPorId(profesionalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El profesional no existe"));
        Sillon sillon = sillones.buscarPorId(sillonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El sillón no existe"));
        if (!profesional.isActivo()) {
            throw new ValidacionDeDominioException("El profesional está inactivo");
        }
        if (!sillon.isActivo()) {
            throw new ValidacionDeDominioException("El sillón está inactivo");
        }
        if (!profesional.realiza(prestacion.getId())) {
            throw new ValidacionDeDominioException("El profesional no realiza la prestación");
        }
    }

    private void verificarAgenda(Turno turno, Long excluirTurnoId) {
        verificarAgenda(new TurnoPropuesto(turno.getProfesionalId(), turno.getSillonId(), turno.getPrestacionId(),
                turno.getInicio(), turno.getFin(), turno.isEsSobreturno()), excluirTurnoId);
    }

    /** Reglas de agenda registradas, y después el solapamiento (el sobreturno no lo verifica, Q5). */
    private void verificarAgenda(TurnoPropuesto propuesto, Long excluirTurnoId) {
        List<RestriccionDeAgenda> reglas = restricciones.orderedStream().toList();
        reglas.forEach(regla -> regla.verificar(propuesto));
        if (propuesto.esSobreturno()) {
            return;
        }
        if (turnos.haySolapamientoDeProfesional(propuesto.profesionalId(), propuesto.inicio(), propuesto.fin(),
                excluirTurnoId)) {
            throw new TurnoSolapadoException(RecursoOcupado.PROFESIONAL);
        }
        if (turnos.haySolapamientoDeSillon(propuesto.sillonId(), propuesto.inicio(), propuesto.fin(),
                excluirTurnoId)) {
            throw new TurnoSolapadoException(RecursoOcupado.SILLON);
        }
    }

    /**
     * Si hay una transacción en curso con otro aislamiento (Spring ignora el declarado al unirse a ella), falla:
     * verificar el solapamiento con una foto vieja permitiría la doble ocupación (design D5).
     */
    private static void exigirAislamientoReadCommitted() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED)
                        .equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())) {
            throw new IllegalStateException("ServicioDeTurnos exige una transacción READ COMMITTED: no lo envuelvas en "
                    + "una transacción con otro aislamiento");
        }
    }
}
