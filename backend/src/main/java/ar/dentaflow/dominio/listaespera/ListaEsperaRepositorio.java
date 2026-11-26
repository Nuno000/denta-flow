package ar.dentaflow.dominio.listaespera;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepositorio {

    ListaEspera guardar(ListaEspera entrada);

    Optional<ListaEspera> buscarPorId(long id);

    Optional<ListaEspera> buscarPorToken(String token);

    Optional<ListaEspera> buscarPorTokenParaActualizar(String token);

    List<ListaEspera> listarPorEstadoOpcional(EstadoListaEspera estado);

    List<ListaEspera> buscarActivasCompatibles(long prestacionId, long profesionalId, LocalDate fechaLocal);

    int vencerOtrasOfertasDelHueco(long profesionalOfrecidoId, Instant inicioOfrecido, long entradaGanadoraId);

    int vencerExpiradas(Instant ahora);
}
