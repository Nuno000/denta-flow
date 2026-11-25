package ar.dentaflow.aplicacion.usuarios;

import ar.dentaflow.dominio.usuarios.Usuario;

/** Resultado de un ingreso correcto: el token de acceso y el usuario que ingresó. */
public record ResultadoIngreso(TokenEmitido token, Usuario usuario) {
}
