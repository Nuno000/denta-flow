package ar.dentaflow.aplicacion.listaespera;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioDeListaDeEspera {

    private final ListaEsperaRepositorio listaEspera;
    private final PacienteRepositorio pacientes;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final Clock reloj;

    public ServicioDeListaDeEspera(ListaEsperaRepositorio listaEspera, PacienteRepositorio pacientes,
            PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales, Clock reloj) {
        this.listaEspera = listaEspera;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.reloj = reloj;
    }

    @Transactional
    public ListaEsperaDetalle registrar(SolicitudListaEspera solicitud, long usuarioId) {
        Paciente paciente = pacientes.buscarPorId(solicitud.pacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El paciente no existe"));
        if (paciente.estaAnonimizado()) {
            throw new ValidacionDeDominioException("El paciente fue dado de baja y no puede agregarse a la lista de espera");
        }
        Prestacion prestacion = prestaciones.buscarPorId(solicitud.prestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación no existe"));
        if (!prestacion.isActiva()) {
            throw new ValidacionDeDominioException("La prestación está inactiva");
        }

        Profesional profesional = null;
        if (solicitud.profesionalId() != null) {
            profesional = profesionales.buscarPorId(solicitud.profesionalId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El profesional no existe"));
            if (!profesional.isActivo()) {
                throw new ValidacionDeDominioException("El profesional está inactivo");
            }
            if (!profesional.realiza(solicitud.prestacionId())) {
                throw new ValidacionDeDominioException("El profesional seleccionado no realiza la prestación");
            }
        }

        Instant ahora = reloj.instant();
        LocalDate hoy = ahora.atZone(ZonaHorariaConsultorio.ZONA).toLocalDate();
        if (solicitud.fechaHasta() != null && solicitud.fechaHasta().isBefore(hoy)) {
            throw new ValidacionDeDominioException("La fecha hasta no puede ser anterior a hoy");
        }

        ListaEspera guardada = listaEspera.guardar(ListaEspera.nueva(
                solicitud.pacienteId(),
                solicitud.prestacionId(),
                solicitud.profesionalId(),
                solicitud.fechaDesde(),
                solicitud.fechaHasta(),
                solicitud.horaDesde(),
                solicitud.horaHasta(),
                usuarioId,
                ahora));

        return aDetalle(guardada, paciente, prestacion, profesional);
    }

    @Transactional
    public List<ListaEsperaDetalle> listar(EstadoListaEspera estadoOpcional) {
        listaEspera.vencerExpiradas(reloj.instant());
        return listaEspera.listarPorEstadoOpcional(estadoOpcional).stream()
                .map(this::enriquecer)
                .toList();
    }

    @Transactional
    public void darDeBaja(long id) {
        ListaEspera entrada = listaEspera.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La entrada de lista de espera no existe"));
        entrada.darDeBaja();
        listaEspera.guardar(entrada);
    }

    @Transactional
    public int vencerOfertasExpiradas() {
        return listaEspera.vencerExpiradas(reloj.instant());
    }

    private ListaEsperaDetalle enriquecer(ListaEspera entrada) {
        Paciente paciente = pacientes.buscarPorId(entrada.getPacienteId()).orElse(null);
        Prestacion prestacion = prestaciones.buscarPorId(entrada.getPrestacionId()).orElse(null);
        Long profId = entrada.getProfesionalOfrecidoId() != null
                ? entrada.getProfesionalOfrecidoId()
                : entrada.getProfesionalId();
        Profesional profesional = profId == null ? null : profesionales.buscarPorId(profId).orElse(null);
        return aDetalle(entrada, paciente, prestacion, profesional);
    }

    private ListaEsperaDetalle aDetalle(ListaEspera entrada, Paciente paciente, Prestacion prestacion,
            Profesional profesional) {
        String pacienteNombre = paciente == null ? "Paciente" : paciente.getNombre() + " " + paciente.getApellido();
        String pacienteDni = paciente == null ? null : paciente.getDni();
        String prestacionNombre = prestacion == null ? "" : prestacion.getNombre();
        String profesionalNombre = profesional == null
                ? null
                : profesional.getNombre() + " " + profesional.getApellido();
        return new ListaEsperaDetalle(
                entrada.getId(),
                entrada.getPacienteId(),
                pacienteNombre,
                pacienteDni,
                entrada.getPrestacionId(),
                prestacionNombre,
                entrada.getProfesionalId(),
                profesionalNombre,
                entrada.getFechaDesde(),
                entrada.getFechaHasta(),
                entrada.getHoraDesde(),
                entrada.getHoraHasta(),
                entrada.getEstado().codigo(),
                entrada.getInicioOfrecido(),
                entrada.getOfrecidaHasta(),
                entrada.getCreadoEn());
    }
}
