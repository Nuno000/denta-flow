package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.aplicacion.agenda.ConsultaDeHuecos;
import ar.dentaflow.aplicacion.agenda.MotorDeDisponibilidad;
import ar.dentaflow.aplicacion.agenda.ReglasDeConsultaDeHuecos;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sugiere hasta 3 horarios libres del mismo profesional y prestación, los más cercanos al inicio pedido, dentro del
 * día del consultorio pedido y los 6 siguientes (C-12, design D3). Una sola consulta de solo lectura al motor de C-10,
 * que ya descarta el pasado y respeta horario, bloqueos y ocupaciones.
 */
@Component
public class SugeridorDeAlternativas {

    private static final Logger LOG = LoggerFactory.getLogger(SugeridorDeAlternativas.class);

    static final int MAXIMO_DE_ALTERNATIVAS = 3;
    static final int DIAS_DE_VENTANA = 7;

    private final MotorDeDisponibilidad motor;
    private final ReglasDeConsultaDeHuecos reglas;

    public SugeridorDeAlternativas(MotorDeDisponibilidad motor, ReglasDeConsultaDeHuecos reglas) {
        this.motor = motor;
        this.reglas = reglas;
    }

    /**
     * Alternativas ordenadas por distancia al {@code inicio} pedido (a igual distancia, la más temprana). El sillón de
     * cada una es el pedido si está entre los candidatos del hueco; si no, el primero. Si el motor falla devuelve una
     * lista vacía: una falla al sugerir no debe tapar el conflicto original.
     */
    public List<AlternativaDeTurno> sugerir(long prestacionId, long profesionalId, long sillonId, Instant inicio) {
        LocalDate desde = ZonaHorariaConsultorio.aHoraLocal(inicio).toLocalDate();
        int dias = Math.min(DIAS_DE_VENTANA, reglas.diasMaximos());
        try {
            List<Hueco> huecos = motor.buscarHuecos(
                    new ConsultaDeHuecos(prestacionId, profesionalId, desde, desde.plusDays(dias - 1L), null));
            return huecos.stream()
                    .sorted(Comparator
                            .comparing((Hueco hueco) -> Duration.between(inicio, hueco.inicio()).abs())
                            .thenComparing(Hueco::inicio))
                    .limit(MAXIMO_DE_ALTERNATIVAS)
                    .map(hueco -> new AlternativaDeTurno(hueco.inicio(), hueco.fin(), profesionalId,
                            sillonPara(hueco, sillonId)))
                    .toList();
        } catch (RuntimeException falla) {
            LOG.debug("No se pudieron calcular alternativas ({})", falla.getClass().getSimpleName());
            return List.of();
        }
    }

    private static long sillonPara(Hueco hueco, long sillonPedido) {
        List<CandidatoDeReserva> candidatos = hueco.candidatos();
        boolean estaElPedido = candidatos.stream().anyMatch(candidato -> candidato.sillonId() == sillonPedido);
        return estaElPedido ? sillonPedido : candidatos.getFirst().sillonId();
    }
}
