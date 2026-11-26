package ar.dentaflow.infraestructura.persistencia.turnos;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Arrays;
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

    private static Long excluir(Long excluirTurnoId) {
        return excluirTurnoId == null ? SIN_EXCLUSION : excluirTurnoId;
    }

    private static List<EstadoTurno> estadosActivos() {
        return Arrays.stream(EstadoTurno.values()).filter(EstadoTurno::ocupaRecursos).toList();
    }
}
