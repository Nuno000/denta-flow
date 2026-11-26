package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.recursos.Profesional;

/** Cómo se nombra al profesional en lo que ve el paciente (email y enlaces): nombre y apellido. */
final class NombresParaElPaciente {

    private NombresParaElPaciente() {
    }

    static String de(Profesional profesional) {
        return profesional.getNombre() + " " + profesional.getApellido();
    }
}
