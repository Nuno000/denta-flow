package ar.dentaflow.presentacion.usuarios;

import ar.dentaflow.dominio.recursos.Profesional;

/** Profesional activo sin usuario, para elegirlo al vincular un usuario con rol profesional. */
public record ProfesionalDisponible(Long id, String nombre, String apellido) {

    public static ProfesionalDisponible de(Profesional profesional) {
        return new ProfesionalDisponible(profesional.getId(), profesional.getNombre(), profesional.getApellido());
    }
}
