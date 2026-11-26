package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.listaespera.AceptadorDeOfertaListaEspera;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/lista-espera")
@Tag(name = "Público — Lista de espera", description = "Consulta y aceptación de ofertas por enlace")
public class ListaEsperaPublicoController {

    private final AceptadorDeOfertaListaEspera aceptador;

    public ListaEsperaPublicoController(AceptadorDeOfertaListaEspera aceptador) {
        this.aceptador = aceptador;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Consultar oferta de lista de espera por token")
    public OfertaListaEsperaRespuesta consultar(@PathVariable String token) {
        return OfertaListaEsperaRespuesta.de(aceptador.consultarPorToken(token));
    }

    @PostMapping("/{token}/aceptar")
    @Operation(summary = "Aceptar oferta de lista de espera y reservar el turno")
    public OfertaListaEsperaRespuesta aceptar(@PathVariable String token) {
        return OfertaListaEsperaRespuesta.de(aceptador.aceptar(token));
    }
}
