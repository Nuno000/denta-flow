package ar.dentaflow.aplicacion.agenda;

import java.time.LocalDate;

/** Parámetros de consulta de huecos disponibles (C-10). */
public record ConsultaDeHuecos(
        Long prestacionId,
        Long profesionalId,
        LocalDate desde,
        LocalDate hasta,
        Long turnoAExcluir) {
}
