package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ValidacionesDeTexto;
import java.time.Instant;
import java.util.Objects;

/**
 * Franja bloqueada de la agenda (US-002, RN-TU-04): de un profesional, de un sillón o de todo el
 * consultorio. Inicio y fin son instantes absolutos (UTC) y la franja es semiabierta
 * {@code [inicio, fin)}. Ámbito y recurso no cambian después del alta; se guarda quién lo grabó
 * por última vez y cuándo.
 */
public class Bloqueo {

    public static final int DETALLE_MAXIMO = 200;

    private Long id;
    private AmbitoBloqueo ambito;
    private Long profesionalId;
    private Long sillonId;
    private Instant inicio;
    private Instant fin;
    private MotivoBloqueo motivo;
    private String detalle;
    private Long modificadoPor;
    private Instant modificadoEn;

    protected Bloqueo() {
    }

    private Bloqueo(AmbitoBloqueo ambito, Long profesionalId, Long sillonId) {
        this.ambito = ambito;
        this.profesionalId = profesionalId;
        this.sillonId = sillonId;
    }

    public static Bloqueo deProfesional(Long profesionalId, Instant inicio, Instant fin, MotivoBloqueo motivo,
            String detalle, Long actor, Instant ahora) {
        if (profesionalId == null) {
            throw new ValidacionDeDominioException("El bloqueo de profesional necesita un profesional existente");
        }
        return conDatos(new Bloqueo(AmbitoBloqueo.PROFESIONAL, profesionalId, null),
                inicio, fin, motivo, detalle, actor, ahora);
    }

    public static Bloqueo deSillon(Long sillonId, Instant inicio, Instant fin, MotivoBloqueo motivo,
            String detalle, Long actor, Instant ahora) {
        if (sillonId == null) {
            throw new ValidacionDeDominioException("El bloqueo de sillón necesita un sillón existente");
        }
        return conDatos(new Bloqueo(AmbitoBloqueo.SILLON, null, sillonId), inicio, fin, motivo, detalle, actor, ahora);
    }

    public static Bloqueo delConsultorio(Instant inicio, Instant fin, MotivoBloqueo motivo, String detalle,
            Long actor, Instant ahora) {
        return conDatos(new Bloqueo(AmbitoBloqueo.CONSULTORIO, null, null), inicio, fin, motivo, detalle, actor, ahora);
    }

    private static Bloqueo conDatos(Bloqueo bloqueo, Instant inicio, Instant fin, MotivoBloqueo motivo,
            String detalle, Long actor, Instant ahora) {
        bloqueo.reprogramar(inicio, fin, motivo, detalle, actor, ahora);
        return bloqueo;
    }

    /**
     * Cambia franja, motivo y detalle con las validaciones del alta y registra al autor; ámbito y
     * recurso no cambian. Si falla no cambia nada.
     */
    public void reprogramar(Instant inicio, Instant fin, MotivoBloqueo motivo, String detalle, Long actor,
            Instant ahora) {
        if (inicio == null) {
            throw new ValidacionDeDominioException("El inicio del bloqueo es obligatorio");
        }
        if (fin == null) {
            throw new ValidacionDeDominioException("El fin del bloqueo es obligatorio");
        }
        if (!inicio.isBefore(fin)) {
            throw new ValidacionDeDominioException("El inicio del bloqueo debe ser anterior al fin");
        }
        if (motivo == null) {
            throw new ValidacionDeDominioException("El motivo del bloqueo es obligatorio");
        }
        if (actor == null || ahora == null) {
            throw new ValidacionDeDominioException("El bloqueo debe registrar quién lo grabó y cuándo");
        }
        String detalleValido = ValidacionesDeTexto.opcional(detalle, "detalle del bloqueo", DETALLE_MAXIMO);
        this.inicio = inicio;
        this.fin = fin;
        this.motivo = motivo;
        this.detalle = detalleValido;
        this.modificadoPor = actor;
        this.modificadoEn = ahora;
    }

    /** Verdadero si es del consultorio o del profesional indicado. */
    public boolean afectaAProfesional(Long id) {
        return ambito == AmbitoBloqueo.CONSULTORIO
                || ambito == AmbitoBloqueo.PROFESIONAL && Objects.equals(profesionalId, id);
    }

    /** Verdadero si es del consultorio o del sillón indicado. */
    public boolean afectaASillon(Long id) {
        return ambito == AmbitoBloqueo.CONSULTORIO
                || ambito == AmbitoBloqueo.SILLON && Objects.equals(sillonId, id);
    }

    /** Superposición de intervalos semiabiertos: tocarse en un borde no cuenta. */
    public boolean seSuperponeCon(Instant desde, Instant hasta) {
        return inicio.isBefore(hasta) && desde.isBefore(fin);
    }

    public Long getId() {
        return id;
    }

    public AmbitoBloqueo getAmbito() {
        return ambito;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public Long getSillonId() {
        return sillonId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public MotivoBloqueo getMotivo() {
        return motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public Long getModificadoPor() {
        return modificadoPor;
    }

    public Instant getModificadoEn() {
        return modificadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Bloqueo that && id != null && Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Bloqueo.class.hashCode();
    }
}
