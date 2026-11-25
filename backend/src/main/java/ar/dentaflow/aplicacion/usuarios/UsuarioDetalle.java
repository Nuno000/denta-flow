package ar.dentaflow.aplicacion.usuarios;

import ar.dentaflow.dominio.usuarios.Usuario;

/**
 * Un usuario con el nombre de su profesional vinculado ("Nombre Apellido"), o {@code null} si no tiene,
 * para que el listado no obligue a consultar los profesionales aparte (design D10).
 */
public record UsuarioDetalle(Usuario usuario, String profesionalNombre) {
}
