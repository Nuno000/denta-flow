package ar.dentaflow.presentacion.publico;

import ar.dentaflow.dominio.recursos.Prestacion;

/** Prestación que el paciente puede reservar: sin la marca de activa ni otros datos internos. */
public record PrestacionPublica(Long id, String nombre, Integer duracionMinutos) {

    static PrestacionPublica de(Prestacion prestacion) {
        return new PrestacionPublica(prestacion.getId(), prestacion.getNombre(), prestacion.getDuracionMinutos());
    }
}
