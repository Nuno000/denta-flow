package ar.dentaflow.aplicacion.listaespera;

import ar.dentaflow.aplicacion.agenda.ServicioDeReservaDeHuecos;
import ar.dentaflow.aplicacion.agenda.SolicitudDeReservaDeHueco;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consulta y aceptación pública de ofertas de lista de espera. No lleva {@code @Transactional} a nivel método para
 * respetar la guarda {@code READ_COMMITTED} de {@code ServicioDeTurnos.crear} al delegar en
 * {@link ServicioDeReservaDeHuecos}.
 */
@Service
public class AceptadorDeOfertaListaEspera {

    private final ListaEsperaRepositorio listaEspera;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final ServicioDeReservaDeHuecos reservaDeHuecos;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public AceptadorDeOfertaListaEspera(ListaEsperaRepositorio listaEspera, PrestacionRepositorio prestaciones,
            ProfesionalRepositorio profesionales, ServicioDeReservaDeHuecos reservaDeHuecos,
            TransactionTemplate transaccion, Clock reloj) {
        this.listaEspera = listaEspera;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.reservaDeHuecos = reservaDeHuecos;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    public OfertaListaEsperaDetalle consultarPorToken(String token) {
        return transaccion.execute(status -> {
            ListaEspera entrada = listaEspera.buscarPorToken(token)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El enlace de oferta no es válido"));
            if (entrada.estaVencida(reloj.instant())) {
                entrada.marcarVencida();
                listaEspera.guardar(entrada);
            }
            return aDetalle(entrada);
        });
    }

    public OfertaListaEsperaDetalle aceptar(String token) {
        DatosReservaPendiente datos = transaccion.execute(status -> {
            ListaEspera entrada = listaEspera.buscarPorTokenParaActualizar(token)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El enlace de oferta no es válido"));
            Instant ahora = reloj.instant();
            if (entrada.estaVencida(ahora)) {
                entrada.marcarVencida();
                listaEspera.guardar(entrada);
                return new DatosReservaPendiente(
                        entrada.getId(), 0L, 0L, 0L, null, "El plazo de esta oferta ya venció");
            }
            if (entrada.getEstado() == EstadoListaEspera.ATENDIDA) {
                return new DatosReservaPendiente(
                        entrada.getId(), 0L, 0L, 0L, null, "Esta oferta ya fue confirmada");
            }
            if (entrada.getEstado() != EstadoListaEspera.OFRECIDA) {
                return new DatosReservaPendiente(
                        entrada.getId(), 0L, 0L, 0L, null, "Esta oferta ya no está disponible");
            }
            return new DatosReservaPendiente(
                    entrada.getId(),
                    entrada.getPacienteId(),
                    entrada.getPrestacionId(),
                    entrada.getProfesionalOfrecidoId(),
                    entrada.getInicioOfrecido(),
                    null);
        });

        if (datos.errorConflicto() != null) {
            throw new ConflictoException(datos.errorConflicto());
        }

        TurnoCreado creado;
        try {
            creado = reservaDeHuecos.reservar(new SolicitudDeReservaDeHueco(
                    datos.pacienteId(),
                    datos.prestacionId(),
                    datos.profesionalOfrecidoId(),
                    datos.inicioOfrecido(),
                    OrigenTurno.ONLINE,
                    Actor.paciente()));
        } catch (HuecoNoDisponibleException ex) {
            transaccion.executeWithoutResult(status -> listaEspera.buscarPorTokenParaActualizar(token).ifPresent(e -> {
                if (e.getEstado() == EstadoListaEspera.OFRECIDA) {
                    e.marcarVencida();
                    listaEspera.guardar(e);
                }
            }));
            throw new ConflictoException("El horario ofrecido ya fue tomado por otro paciente");
        }

        return transaccion.execute(status -> {
            ListaEspera entrada = listaEspera.buscarPorTokenParaActualizar(token)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El enlace de oferta no es válido"));
            entrada.marcarAtendida(creado.turno().getId());
            listaEspera.guardar(entrada);
            listaEspera.vencerOtrasOfertasDelHueco(
                    datos.profesionalOfrecidoId(), datos.inicioOfrecido(), entrada.getId());
            return aDetalle(entrada);
        });
    }

    private OfertaListaEsperaDetalle aDetalle(ListaEspera entrada) {
        Prestacion prestacion = prestaciones.buscarPorId(entrada.getPrestacionId()).orElse(null);
        Profesional profesional = entrada.getProfesionalOfrecidoId() == null
                ? null
                : profesionales.buscarPorId(entrada.getProfesionalOfrecidoId()).orElse(null);
        return new OfertaListaEsperaDetalle(
                entrada.getEstado().codigo(),
                prestacion == null ? "" : prestacion.getNombre(),
                profesional == null ? "" : profesional.getNombre() + " " + profesional.getApellido(),
                entrada.getInicioOfrecido(),
                entrada.getOfrecidaHasta(),
                entrada.getTurnoCreadoId());
    }

    private record DatosReservaPendiente(
            long id,
            long pacienteId,
            long prestacionId,
            long profesionalOfrecidoId,
            Instant inicioOfrecido,
            String errorConflicto) {
    }
}
