package ar.dentaflow.aplicacion.usuarios;

import java.time.Instant;

/** Token de acceso firmado y el instante (UTC) en que vence. */
public record TokenEmitido(String token, Instant venceEn) {
}
