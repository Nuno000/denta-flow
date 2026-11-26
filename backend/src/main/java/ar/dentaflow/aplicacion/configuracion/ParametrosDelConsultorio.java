package ar.dentaflow.aplicacion.configuracion;

import java.time.Duration;

/**
 * Lectura tipada de los parámetros del consultorio para otros procesos (reglas de turnos, reserva
 * pública). Quien la usa depende de esta interfaz y la reemplaza por un doble en sus tests unitarios.
 */
public interface ParametrosDelConsultorio {

    /** Anticipación mínima para reservar, reprogramar o cancelar un turno (PA-04). */
    Duration anticipacionMinima();

    /** Texto y leyenda del aviso de privacidad vigentes, aunque sigan siendo provisorios. */
    AvisoPrivacidad avisoPrivacidad();

    /** Contacto del consultorio para la reserva pública (C-16), aunque siga siendo provisorio. */
    String contactoConsultorio();
}
