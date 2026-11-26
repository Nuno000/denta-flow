package ar.dentaflow.dominio.turnos;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turno de un paciente con un profesional, en un sillón y para una prestación. Nace {@code pendiente}; el
 * fin se guarda (inicio + duración de la prestación, Q4). Los cambios de estado pasan por la tabla de
 * {@link EstadoTurno}. Referencia a las demás entidades por id. El token de acceso no sale en
 * {@code toString}, en la auditoría ni en logs.
 */
public class Turno {

    private Long id;
    private Long pacienteId;
    private Long profesionalId;
    private Long sillonId;
    private Long prestacionId;
    private Instant inicio;
    private Instant fin;
    private EstadoTurno estado;
    private OrigenTurno origen;
    private boolean esSobreturno;
    private Long creadoPorUsuarioId;
    private Instant creadoEn;
    private String tokenAcceso;

    protected Turno() {
    }

    private Turno(Long pacienteId, Long profesionalId, Long sillonId, Long prestacionId, Instant inicio,
            int duracionMinutos, OrigenTurno origen, boolean esSobreturno, Long creadoPorUsuarioId, Instant creadoEn,
            String tokenAcceso) {
        this.pacienteId = obligatorio(pacienteId, "paciente");
        this.profesionalId = obligatorio(profesionalId, "profesional");
        this.sillonId = obligatorio(sillonId, "sillón");
        this.prestacionId = obligatorio(prestacionId, "prestación");
        this.origen = obligatorio(origen, "origen");
        this.creadoEn = obligatorio(creadoEn, "fecha de creación");
        if (tokenAcceso == null || tokenAcceso.isBlank()) {
            throw new ValidacionDeDominioException("El token de acceso del turno es obligatorio");
        }
        this.tokenAcceso = tokenAcceso;
        validarOrigen(origen, esSobreturno, creadoPorUsuarioId);
        this.esSobreturno = esSobreturno;
        this.creadoPorUsuarioId = creadoPorUsuarioId;
        this.inicio = validarInicio(inicio);
        this.fin = calcularFin(this.inicio, duracionMinutos);
        this.estado = EstadoTurno.PENDIENTE;
    }

    public static Turno nuevo(Long pacienteId, Long profesionalId, Long sillonId, Long prestacionId, Instant inicio,
            int duracionMinutos, OrigenTurno origen, boolean esSobreturno, Long creadoPorUsuarioId, Instant creadoEn,
            String tokenAcceso) {
        return new Turno(pacienteId, profesionalId, sillonId, prestacionId, inicio, duracionMinutos, origen,
                esSobreturno, creadoPorUsuarioId, creadoEn, tokenAcceso);
    }

    /** Pasa al estado indicado si la tabla lo permite; repetir el estado actual devuelve un cambio vacío. */
    public CambioDeEstado cambiarEstado(EstadoTurno nuevo) {
        if (nuevo == null) {
            throw new ValidacionDeDominioException("El nuevo estado del turno es obligatorio");
        }
        if (nuevo == estado) {
            return CambioDeEstado.sinCambio(estado);
        }
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionDeEstadoInvalidaException(estado, nuevo);
        }
        EstadoTurno anterior = estado;
        estado = nuevo;
        return new CambioDeEstado(anterior, nuevo);
    }

    /**
     * Mueve el turno a otro horario, profesional y sillón. Solo desde {@code pendiente} o {@code confirmado};
     * conserva estado, paciente y token. Devuelve únicamente los campos que cambiaron.
     */
    public CambioDeReprogramacion reprogramar(Instant nuevoInicio, int duracionMinutos, Long nuevoProfesionalId,
            Long nuevoSillonId) {
        exigirReprogramable();
        Instant inicioValido = validarInicio(nuevoInicio);
        Instant finNuevo = calcularFin(inicioValido, duracionMinutos);
        obligatorio(nuevoProfesionalId, "profesional");
        obligatorio(nuevoSillonId, "sillón");

        Map<String, String> anterior = new LinkedHashMap<>();
        Map<String, String> nuevo = new LinkedHashMap<>();
        registrar(anterior, nuevo, "inicio", inicio.toString(), inicioValido.toString());
        registrar(anterior, nuevo, "fin", fin.toString(), finNuevo.toString());
        registrar(anterior, nuevo, "profesional_id", profesionalId.toString(), nuevoProfesionalId.toString());
        registrar(anterior, nuevo, "sillon_id", sillonId.toString(), nuevoSillonId.toString());

        this.inicio = inicioValido;
        this.fin = finNuevo;
        this.profesionalId = nuevoProfesionalId;
        this.sillonId = nuevoSillonId;
        return new CambioDeReprogramacion(anterior, nuevo);
    }

    /** Falla con {@link TransicionDeEstadoInvalidaException} si el turno no está {@code pendiente} ni {@code confirmado}. */
    public void exigirReprogramable() {
        if (estado != EstadoTurno.PENDIENTE && estado != EstadoTurno.CONFIRMADO) {
            throw new TransicionDeEstadoInvalidaException(estado, "reprogramar");
        }
    }

    /** Valores del alta para la auditoría: sin token ni datos personales; instantes en ISO-8601 UTC. */
    public Map<String, String> valoresDeAlta() {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("paciente_id", pacienteId.toString());
        valores.put("profesional_id", profesionalId.toString());
        valores.put("sillon_id", sillonId.toString());
        valores.put("prestacion_id", prestacionId.toString());
        valores.put("inicio", inicio.toString());
        valores.put("fin", fin.toString());
        valores.put("estado", estado.codigo());
        valores.put("origen", origen.codigo());
        valores.put("es_sobreturno", Boolean.toString(esSobreturno));
        return valores;
    }

    private static void registrar(Map<String, String> anterior, Map<String, String> nuevo, String campo,
            String valorAnterior, String valorNuevo) {
        if (!valorAnterior.equals(valorNuevo)) {
            anterior.put(campo, valorAnterior);
            nuevo.put(campo, valorNuevo);
        }
    }

    private static void validarOrigen(OrigenTurno origen, boolean esSobreturno, Long creadoPorUsuarioId) {
        if (origen == OrigenTurno.ONLINE && creadoPorUsuarioId != null) {
            throw new ValidacionDeDominioException("Un turno online no tiene usuario creador");
        }
        if (origen == OrigenTurno.RECEPCION && creadoPorUsuarioId == null) {
            throw new ValidacionDeDominioException("Un turno de recepción necesita el usuario que lo creó");
        }
        if (esSobreturno && origen != OrigenTurno.RECEPCION) {
            throw new ValidacionDeDominioException("El sobreturno solo puede crearlo recepción");
        }
    }

    private static Instant validarInicio(Instant inicio) {
        obligatorio(inicio, "inicio");
        if (inicio.getEpochSecond() % 60 != 0 || inicio.getNano() != 0) {
            throw new ValidacionDeDominioException("El inicio del turno debe tener precisión de minuto");
        }
        return inicio;
    }

    private static Instant calcularFin(Instant inicio, int duracionMinutos) {
        if (duracionMinutos <= 0) {
            throw new ValidacionDeDominioException("La duración del turno debe ser mayor que cero");
        }
        return inicio.plusSeconds(duracionMinutos * 60L);
    }

    private static <T> T obligatorio(T valor, String dato) {
        if (valor == null) {
            throw new ValidacionDeDominioException("El " + dato + " del turno es obligatorio");
        }
        return valor;
    }

    public Long getId() {
        return id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public Long getSillonId() {
        return sillonId;
    }

    public Long getPrestacionId() {
        return prestacionId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public OrigenTurno getOrigen() {
        return origen;
    }

    public boolean isEsSobreturno() {
        return esSobreturno;
    }

    public Long getCreadoPorUsuarioId() {
        return creadoPorUsuarioId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    /** Token del enlace del paciente (C-14, C-18). Nunca va a logs ni a la auditoría. */
    public String tokenAcceso() {
        return tokenAcceso;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Turno that && id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Turno.class.hashCode();
    }

    @Override
    public String toString() {
        return "Turno[id=" + id + ", estado=" + estado + ", inicio=" + inicio + "]";
    }
}
