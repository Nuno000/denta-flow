package ar.dentaflow.dominio.usuarios;

/** Un rol del catálogo con su nombre visible para la UI (p. ej. {@code RECEPCION} → "Recepción"). */
public record RolDisponible(Rol rol, String nombre) {
}
