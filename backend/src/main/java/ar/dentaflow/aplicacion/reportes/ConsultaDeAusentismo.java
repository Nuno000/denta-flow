package ar.dentaflow.aplicacion.reportes;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.reportes.CalculadorDeAusentismo;
import ar.dentaflow.dominio.reportes.RegistroAsistenciaParaReporte;
import ar.dentaflow.dominio.reportes.ReporteDeAusentismo;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta de ausentismo (C-23, US-031; design D1–D6). Valida rango (máximo 366 días en
 * {@code America/Argentina/Buenos_Aires}) y umbral, aplica la regla de "propios" para el rol profesional y delega
 * el cálculo puro a {@link CalculadorDeAusentismo}.
 */
@Service
public class ConsultaDeAusentismo {

    static final int MAXIMO_DE_DIAS = 366;
    static final int MINIMO_AUSENCIAS_DEFECTO = 2;

    private final LecturaDeAusentismo lectura;

    public ConsultaDeAusentismo(LecturaDeAusentismo lectura) {
        this.lectura = lectura;
    }

    @Transactional(readOnly = true)
    public ReporteDeAusentismo consultar(PedidoDeAusentismo pedido, Rol rol, Long profesionalDelUsuario) {
        Objects.requireNonNull(pedido, "pedido");
        validarRango(pedido.desde(), pedido.hasta());
        int minimoAusencias = resolverMinimoAusencias(pedido.minimoAusencias());
        Long profesionalEfectivo = profesionalPermitido(pedido.profesionalId(), rol, profesionalDelUsuario);

        Instant desde = ZonaHorariaConsultorio.aInstante(pedido.desde().atStartOfDay());
        Instant hasta = ZonaHorariaConsultorio.aInstante(pedido.hasta().plusDays(1).atStartOfDay());
        List<RegistroAsistenciaParaReporte> registros = lectura.buscarEvaluados(desde, hasta, profesionalEfectivo);

        return CalculadorDeAusentismo.calcular(registros, pedido.desde(), pedido.hasta(), profesionalEfectivo,
                minimoAusencias);
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionDeDominioException("Las fechas desde y hasta son obligatorias");
        }
        if (hasta.isBefore(desde)) {
            throw new ValidacionDeDominioException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DE_DIAS) {
            throw new ValidacionDeDominioException("El rango de ausentismo no puede superar 366 días");
        }
    }

    private static int resolverMinimoAusencias(Integer minimo) {
        if (minimo == null) {
            return MINIMO_AUSENCIAS_DEFECTO;
        }
        if (minimo < 1) {
            throw new ValidacionDeDominioException("El mínimo de ausencias debe ser al menos 1");
        }
        return minimo;
    }

    private static Long profesionalPermitido(Long pedido, Rol rol, Long profesionalDelUsuario) {
        if (rol != Rol.PROFESIONAL) {
            return pedido;
        }
        if (profesionalDelUsuario == null) {
            throw new AccessDeniedException("Tu usuario no está vinculado a un profesional");
        }
        if (pedido != null && !pedido.equals(profesionalDelUsuario)) {
            throw new AccessDeniedException("Solo podés ver tus propios indicadores de ausentismo");
        }
        return profesionalDelUsuario;
    }
}
