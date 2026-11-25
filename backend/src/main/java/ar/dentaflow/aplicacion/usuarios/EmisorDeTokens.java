package ar.dentaflow.aplicacion.usuarios;

/** Puerto para emitir el token de acceso de un usuario que ya ingresó correctamente (design D1). */
public interface EmisorDeTokens {

    TokenEmitido emitir(Long usuarioId);
}
