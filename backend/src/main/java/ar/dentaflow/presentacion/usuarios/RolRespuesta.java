package ar.dentaflow.presentacion.usuarios;

import ar.dentaflow.dominio.usuarios.RolDisponible;

/** Un rol del catálogo: su código y su nombre visible. */
public record RolRespuesta(String codigo, String nombre) {

    public static RolRespuesta de(RolDisponible rol) {
        return new RolRespuesta(rol.rol().codigo(), rol.nombre());
    }
}
