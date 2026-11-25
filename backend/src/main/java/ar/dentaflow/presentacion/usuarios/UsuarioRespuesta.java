package ar.dentaflow.presentacion.usuarios;

import ar.dentaflow.aplicacion.usuarios.UsuarioDetalle;
import ar.dentaflow.dominio.usuarios.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

/** Datos públicos de un usuario: nunca incluye la contraseña ni su hash (design D10). */
@Schema(description = "Usuario del personal, sin datos de la credencial")
public record UsuarioRespuesta(
        Long id,
        String email,
        @Schema(description = "Código del rol", allowableValues = {"administrador", "recepcion", "profesional"})
        String rol,
        Long profesionalId,
        @Schema(description = "Nombre y apellido del profesional vinculado, si tiene") String profesionalNombre,
        boolean activo) {

    public static UsuarioRespuesta de(UsuarioDetalle detalle) {
        Usuario usuario = detalle.usuario();
        return new UsuarioRespuesta(usuario.getId(), usuario.getEmail(), usuario.getRol().codigo(),
                usuario.getProfesionalId(), detalle.profesionalNombre(), usuario.isActivo());
    }
}
