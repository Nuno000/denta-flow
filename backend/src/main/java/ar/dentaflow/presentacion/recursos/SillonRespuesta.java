package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.dominio.recursos.Sillon;

/** Sillón tal como lo ve el personal interno. */
public record SillonRespuesta(Long id, String nombre, boolean activo) {

    public static SillonRespuesta de(Sillon sillon) {
        return new SillonRespuesta(sillon.getId(), sillon.getNombre(), sillon.isActivo());
    }
}
