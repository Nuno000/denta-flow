package ar.dentaflow.presentacion.publico;

import java.time.OffsetDateTime;

/**
 * Representación pública de un hueco disponible en zona del consultorio. No expone sillones ni
 * candidatos internos (C-10, design D7).
 */
public record HuecoPublicoRespuesta(OffsetDateTime inicio, OffsetDateTime fin, Long profesionalId) {
}
