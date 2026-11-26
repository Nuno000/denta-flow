package ar.dentaflow.presentacion.publico;

import ar.dentaflow.dominio.recursos.Profesional;

/** Profesional visible para el paciente: solo id, nombre y apellido (sin email ni matrícula). */
public record ProfesionalPublico(Long id, String nombre, String apellido) {

    static ProfesionalPublico de(Profesional profesional) {
        return new ProfesionalPublico(profesional.getId(), profesional.getNombre(), profesional.getApellido());
    }
}
