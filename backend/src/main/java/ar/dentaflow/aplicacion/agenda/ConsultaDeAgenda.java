package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de la agenda interna (C-11; US-020, US-023; spec agenda-interna). Valida el rango (máximo 7 días del
 * consultorio), lo convierte a instantes UTC según {@code America/Argentina/Buenos_Aires} y aplica la regla de
 * "propios" (design D2): el rol profesional solo ve los turnos de su profesional.
 */
@Service
public class ConsultaDeAgenda {

    static final int MAXIMO_DE_DIAS = 7;

    private static final List<EstadoTurno> SIN_CANCELADOS = Arrays.stream(EstadoTurno.values())
            .filter(estado -> estado != EstadoTurno.CANCELADO)
            .toList();

    private final LecturaDeAgenda lectura;

    public ConsultaDeAgenda(LecturaDeAgenda lectura) {
        this.lectura = lectura;
    }

    @Transactional(readOnly = true)
    public List<FilaDeAgenda> consultar(PedidoDeAgenda pedido, Rol rol, Long profesionalDelUsuario) {
        Objects.requireNonNull(pedido, "pedido");
        validarRango(pedido.desde(), pedido.hasta());
        List<EstadoTurno> estados = estados(pedido.estado());
        Long profesionalId = profesionalPermitido(pedido.profesionalId(), rol, profesionalDelUsuario);

        Instant desde = inicioDelDia(pedido.desde());
        Instant hasta = inicioDelDia(pedido.hasta().plusDays(1));
        return lectura.buscar(new CriterioDeAgenda(desde, hasta, profesionalId, pedido.sillonId(), estados));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionDeDominioException("Las fechas desde y hasta son obligatorias");
        }
        if (hasta.isBefore(desde)) {
            throw new ValidacionDeDominioException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DE_DIAS) {
            throw new ValidacionDeDominioException("El rango de la agenda no puede superar 7 días");
        }
    }

    /** Sin estado, todos menos cancelado. El mensaje de error no repite el valor recibido (C-15, design D7). */
    private static List<EstadoTurno> estados(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return SIN_CANCELADOS;
        }
        return Arrays.stream(EstadoTurno.values())
                .filter(estado -> estado.codigo().equals(codigo))
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new ValidacionDeDominioException("Estado de turno desconocido"));
    }

    private static Long profesionalPermitido(Long pedido, Rol rol, Long profesionalDelUsuario) {
        if (rol != Rol.PROFESIONAL) {
            return pedido;
        }
        if (profesionalDelUsuario == null) {
            throw new AccessDeniedException("Tu usuario no está vinculado a un profesional");
        }
        if (pedido != null && !pedido.equals(profesionalDelUsuario)) {
            throw new AccessDeniedException("Solo podés ver tu propia agenda");
        }
        return profesionalDelUsuario;
    }

    private static Instant inicioDelDia(LocalDate dia) {
        return ZonaHorariaConsultorio.aInstante(dia.atStartOfDay());
    }
}
