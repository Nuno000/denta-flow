package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.dominio.recursos.Prestacion;

/** Prestación tal como la ve el personal interno. */
public record PrestacionRespuesta(Long id, String nombre, Integer duracionMinutos, boolean activa) {

    public static PrestacionRespuesta de(Prestacion prestacion) {
        return new PrestacionRespuesta(prestacion.getId(), prestacion.getNombre(), prestacion.getDuracionMinutos(),
                prestacion.isActiva());
    }
}
