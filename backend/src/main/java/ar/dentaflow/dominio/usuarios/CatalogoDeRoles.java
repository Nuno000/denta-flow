package ar.dentaflow.dominio.usuarios;

import java.util.List;

/** Puerto de solo lectura del catálogo de roles (tabla {@code rol}, design D4 y D5). */
public interface CatalogoDeRoles {

    /** Los roles del catálogo en orden fijo: administrador, recepción, profesional. */
    List<RolDisponible> listar();
}
