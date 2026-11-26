package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;

/**
 * Registro de un email transaccional de un turno (design D2 y D3): uno por turno y tipo. Nace {@code en_curso};
 * cada intento de envío suma en {@code intentos} y termina {@code enviada} o {@code fallida}, o {@code omitida} sin
 * intentos si no corresponde enviarla. Una notificación cerrada no vuelve a cambiar. No guarda datos del paciente
 * ni el texto de error del proveedor.
 */
public class Notificacion {

    private Long id;
    private Long turnoId;
    private TipoNotificacion tipo;
    private CanalNotificacion canal;
    private EstadoNotificacion estado;
    private int intentos;
    private MotivoFallo motivoFallo;
    private Instant creadaEn;
    private Instant actualizadaEn;
    private Instant enviadaEn;

    protected Notificacion() {
    }

    private Notificacion(Long turnoId, TipoNotificacion tipo, Instant ahora) {
        this.turnoId = obligatorio(turnoId, "turno");
        this.tipo = obligatorio(tipo, "tipo");
        this.creadaEn = obligatorio(ahora, "instante");
        this.actualizadaEn = ahora;
        this.canal = CanalNotificacion.EMAIL;
        this.estado = EstadoNotificacion.EN_CURSO;
        this.intentos = 0;
    }

    public static Notificacion nueva(Long turnoId, TipoNotificacion tipo, Instant ahora) {
        return new Notificacion(turnoId, tipo, ahora);
    }

    /** Suma un intento de envío. Solo mientras está {@code en_curso}. */
    public void registrarIntento(Instant ahora) {
        exigirEnCurso("registrar un intento");
        intentos++;
        actualizadaEn = obligatorio(ahora, "instante");
    }

    public void marcarEnviada(Instant ahora) {
        exigirEnCurso("marcarla enviada");
        estado = EstadoNotificacion.ENVIADA;
        enviadaEn = obligatorio(ahora, "instante");
        actualizadaEn = ahora;
    }

    public void marcarFallida(MotivoFallo motivo, Instant ahora) {
        obligatorio(motivo, "motivo de la falla");
        exigirEnCurso("marcarla fallida");
        estado = EstadoNotificacion.FALLIDA;
        motivoFallo = motivo;
        actualizadaEn = obligatorio(ahora, "instante");
    }

    /** No corresponde enviarla (p. ej. paciente anonimizado): queda {@code omitida} sin intentos. */
    public void omitir(Instant ahora) {
        exigirEnCurso("omitirla");
        estado = EstadoNotificacion.OMITIDA;
        actualizadaEn = obligatorio(ahora, "instante");
    }

    private void exigirEnCurso(String accion) {
        if (estado != EstadoNotificacion.EN_CURSO) {
            throw new OperacionNoPermitidaException("La notificación ya está " + estado.codigo()
                    + ": no se puede " + accion);
        }
    }

    private static <T> T obligatorio(T valor, String nombre) {
        if (valor == null) {
            throw new ValidacionDeDominioException("El dato " + nombre + " de la notificación es obligatorio");
        }
        return valor;
    }

    public Long getId() {
        return id;
    }

    public Long getTurnoId() {
        return turnoId;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public MotivoFallo getMotivoFallo() {
        return motivoFallo;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    public Instant getEnviadaEn() {
        return enviadaEn;
    }

    @Override
    public String toString() {
        return "Notificacion[id=" + id + ", turno=" + turnoId + ", tipo=" + tipo.codigo() + ", estado="
                + estado.codigo() + ", intentos=" + intentos + "]";
    }
}
