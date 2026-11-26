package ar.dentaflow.infraestructura.persistencia.turnos;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OcupacionDeRecurso;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Implementación Spring Data del puerto {@link TurnoRepositorio}; sin {@code delete*} heredados. Las consultas de
 * solapamiento cuentan turnos con estado que ocupa recursos, que no son sobreturno y que cumplen
 * {@code inicio < :fin AND fin > :inicio} (intervalos semiabiertos: los contiguos no se solapan).
 */
public interface TurnoRepositorioJpa extends Repository<Turno, Long>, TurnoRepositorio {

    /** Formato del token (design D9): 32 bytes en Base64 URL sin relleno. */
    Pattern FORMATO_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

    /** Id que nunca existe: evita pasar un parámetro nulo a la consulta cuando no se excluye ningún turno. */
    long SIN_EXCLUSION = -1L;

    Turno save(Turno turno);

    Optional<Turno> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.id = :id")
    Optional<Turno> findByIdParaActualizar(@Param("id") Long id);

    Optional<Turno> findByTokenAcceso(String tokenAcceso);

    @Query("select count(t) from Turno t where t.profesionalId = :profesionalId and t.estado in :estados "
            + "and t.esSobreturno = false and t.inicio < :fin and t.fin > :inicio and t.id <> :excluir")
    long contarSolapadosDeProfesional(@Param("profesionalId") Long profesionalId,
            @Param("estados") List<EstadoTurno> estados, @Param("inicio") Instant inicio, @Param("fin") Instant fin,
            @Param("excluir") Long excluir);

    @Query("select count(t) from Turno t where t.sillonId = :sillonId and t.estado in :estados "
            + "and t.esSobreturno = false and t.inicio < :fin and t.fin > :inicio and t.id <> :excluir")
    long contarSolapadosDeSillon(@Param("sillonId") Long sillonId, @Param("estados") List<EstadoTurno> estados,
            @Param("inicio") Instant inicio, @Param("fin") Instant fin, @Param("excluir") Long excluir);

    @Query("select new ar.dentaflow.dominio.turnos.OcupacionDeRecurso(t.profesionalId, t.inicio, t.fin) "
            + "from Turno t where t.profesionalId in :ids and t.estado in :estados "
            + "and t.esSobreturno = false and t.inicio < :hasta and t.fin > :desde and t.id <> :excluir")
    List<OcupacionDeRecurso> buscarOcupacionesDeProfesionales(@Param("ids") Collection<Long> ids,
            @Param("estados") List<EstadoTurno> estados, @Param("desde") Instant desde,
            @Param("hasta") Instant hasta, @Param("excluir") Long excluir);

    @Query("select new ar.dentaflow.dominio.turnos.OcupacionDeRecurso(t.sillonId, t.inicio, t.fin) "
            + "from Turno t where t.sillonId in :ids and t.estado in :estados "
            + "and t.esSobreturno = false and t.inicio < :hasta and t.fin > :desde and t.id <> :excluir")
    List<OcupacionDeRecurso> buscarOcupacionesDeSillones(@Param("ids") Collection<Long> ids,
            @Param("estados") List<EstadoTurno> estados, @Param("desde") Instant desde,
            @Param("hasta") Instant hasta, @Param("excluir") Long excluir);

    @Query("select count(t) from Turno t where t.pacienteId = :pacienteId and t.estado in :estados "
            + "and t.inicio > :ahora")
    long contarFuturosDelPaciente(@Param("pacienteId") Long pacienteId,
            @Param("estados") List<EstadoTurno> estados, @Param("ahora") Instant ahora);

    @Override
    default boolean hayTurnosActivosFuturosDelPaciente(Long pacienteId, Instant ahora) {
        return contarFuturosDelPaciente(pacienteId, List.of(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO), ahora) > 0;
    }

    @Override
    default Turno guardar(Turno turno) {
        return save(turno);
    }

    @Override
    default Optional<Turno> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default Optional<Turno> buscarPorIdParaActualizar(Long id) {
        return findByIdParaActualizar(id);
    }

    @Override
    default Optional<Turno> buscarPorToken(String token) {
        if (token == null || !FORMATO_TOKEN.matcher(token).matches()) {
            return Optional.empty();
        }
        return findByTokenAcceso(token);
    }

    @Override
    default boolean haySolapamientoDeProfesional(Long profesionalId, Instant inicio, Instant fin,
            Long excluirTurnoId) {
        return contarSolapadosDeProfesional(profesionalId, estadosActivos(), inicio, fin, excluir(excluirTurnoId)) > 0;
    }

    @Override
    default boolean haySolapamientoDeSillon(Long sillonId, Instant inicio, Instant fin, Long excluirTurnoId) {
        return contarSolapadosDeSillon(sillonId, estadosActivos(), inicio, fin, excluir(excluirTurnoId)) > 0;
    }

    @Override
    default List<OcupacionDeRecurso> listarOcupacionesDeProfesionales(Collection<Long> ids, Instant desde,
            Instant hasta, Long excluirTurnoId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return buscarOcupacionesDeProfesionales(ids, estadosActivos(), desde, hasta, excluir(excluirTurnoId));
    }

    @Override
    default List<OcupacionDeRecurso> listarOcupacionesDeSillones(Collection<Long> ids, Instant desde, Instant hasta,
            Long excluirTurnoId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return buscarOcupacionesDeSillones(ids, estadosActivos(), desde, hasta, excluir(excluirTurnoId));
    }

    private static Long excluir(Long excluirTurnoId) {
        return excluirTurnoId == null ? SIN_EXCLUSION : excluirTurnoId;
    }

    private static List<EstadoTurno> estadosActivos() {
        return Arrays.stream(EstadoTurno.values()).filter(EstadoTurno::ocupaRecursos).toList();
    }
}
