package ar.dentaflow.dominio.usuarios;

/**
 * Puerto para guardar contraseñas como hash adaptativo con sal. La contraseña en claro solo pasa por
 * acá: el dominio guarda únicamente el resultado de {@link #codificar(String)}.
 */
public interface CodificadorDeCredenciales {

    String codificar(String contrasena);

    boolean coincide(String contrasena, String hash);
}
