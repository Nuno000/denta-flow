package ar.dentaflow.infraestructura.persistencia.listaespera;

import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListaEsperaRepositorioJpa extends JpaRepository<ListaEspera, Long>, ListaEsperaRepositorio {

    @Override
    default ListaEspera guardar(ListaEspera entrada) {
        return save(entrada);
    }

    @Override
    default Optional<ListaEspera> buscarPorId(long id) {
        return findById(id);
    }

    @Override
    @Query("SELECT l FROM ListaEspera l WHERE l.tokenAceptacion = :token")
    Optional<ListaEspera> buscarPorToken(@Param("token") String token);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ListaEspera l WHERE l.tokenAceptacion = :token")
    Optional<ListaEspera> buscarPorTokenParaActualizar(@Param("token") String token);

    @Override
    @Query("SELECT l FROM ListaEspera l WHERE (:estado IS NULL OR l.estado = :estado) ORDER BY l.creadoEn DESC")
    List<ListaEspera> listarPorEstadoOpcional(@Param("estado") EstadoListaEspera estado);

    @Override
    @Query("""
            SELECT l FROM ListaEspera l
            WHERE l.estado = ar.dentaflow.dominio.listaespera.EstadoListaEspera.ACTIVA
              AND l.prestacionId = :prestacionId
              AND (l.profesionalId IS NULL OR l.profesionalId = :profesionalId)
              AND l.fechaDesde <= :fechaLocal
              AND l.fechaHasta >= :fechaLocal
            ORDER BY l.creadoEn ASC
            """)
    List<ListaEspera> buscarActivasCompatibles(
            @Param("prestacionId") long prestacionId,
            @Param("profesionalId") long profesionalId,
            @Param("fechaLocal") LocalDate fechaLocal);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ListaEspera l
            SET l.estado = ar.dentaflow.dominio.listaespera.EstadoListaEspera.VENCIDA
            WHERE l.estado = ar.dentaflow.dominio.listaespera.EstadoListaEspera.OFRECIDA
              AND l.profesionalOfrecidoId = :profesionalOfrecidoId
              AND l.inicioOfrecido = :inicioOfrecido
              AND l.id <> :entradaGanadoraId
            """)
    int vencerOtrasOfertasDelHueco(
            @Param("profesionalOfrecidoId") long profesionalOfrecidoId,
            @Param("inicioOfrecido") Instant inicioOfrecido,
            @Param("entradaGanadoraId") long entradaGanadoraId);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ListaEspera l
            SET l.estado = ar.dentaflow.dominio.listaespera.EstadoListaEspera.VENCIDA
            WHERE l.estado = ar.dentaflow.dominio.listaespera.EstadoListaEspera.OFRECIDA
              AND l.ofrecidaHasta <= :ahora
            """)
    int vencerExpiradas(@Param("ahora") Instant ahora);
}
