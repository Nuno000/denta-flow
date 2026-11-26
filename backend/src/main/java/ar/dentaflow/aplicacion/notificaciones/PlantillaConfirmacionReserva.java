package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Email de confirmación de reserva (spec "Email de confirmación de reserva", design D4): fecha y hora en la zona
 * del consultorio, prestación, profesional y los tres enlaces, en texto plano y en HTML escapado. Sin motor de
 * plantillas. C-18 arma la de recordatorio con el mismo criterio.
 */
@Component
public class PlantillaConfirmacionReserva {

    static final DateTimeFormatter FECHA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public MensajeEmail armar(DatosDelEmail datos) {
        String cuando = FECHA_Y_HORA.format(ZonaHorariaConsultorio.aHoraLocal(datos.inicio()));
        EnlacesDelTurno enlaces = datos.enlaces();
        String asunto = "Tu turno del " + cuando + ": confirmá tu asistencia";
        String texto = """
                Hola, %s:

                Reservaste un turno en el consultorio.

                Fecha y hora: %s (hora de Argentina)
                Prestación: %s
                Profesional: %s

                Confirmá tu asistencia: %s
                Cancelar el turno: %s
                Reprogramar el turno: %s

                Si no podés venir, cancelá o reprogramá con anticipación para liberar el horario.
                Los enlaces son personales: no los compartas.
                """.formatted(datos.nombrePaciente(), cuando, datos.prestacion(), datos.profesional(),
                enlaces.confirmar(), enlaces.cancelar(), enlaces.reprogramar());
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <body style="font-family: Arial, sans-serif; color: #1f2937; line-height: 1.5;">
                <p>Hola, %s:</p>
                <p>Reservaste un turno en el consultorio.</p>
                <ul>
                <li><strong>Fecha y hora:</strong> %s (hora de Argentina)</li>
                <li><strong>Prestación:</strong> %s</li>
                <li><strong>Profesional:</strong> %s</li>
                </ul>
                <p><a href="%s">Confirmá tu asistencia</a></p>
                <p><a href="%s">Cancelar el turno</a> · <a href="%s">Reprogramar el turno</a></p>
                <p>Si no podés venir, cancelá o reprogramá con anticipación para liberar el horario.<br>
                Los enlaces son personales: no los compartas.</p>
                </body>
                </html>
                """.formatted(escapar(datos.nombrePaciente()), escapar(cuando), escapar(datos.prestacion()),
                escapar(datos.profesional()), escapar(enlaces.confirmar()), escapar(enlaces.cancelar()),
                escapar(enlaces.reprogramar()));
        return new MensajeEmail(datos.destinatario(), asunto, texto, html);
    }

    static String escapar(String texto) {
        StringBuilder salida = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '&' -> salida.append("&amp;");
                case '<' -> salida.append("&lt;");
                case '>' -> salida.append("&gt;");
                case '"' -> salida.append("&quot;");
                case '\'' -> salida.append("&#39;");
                default -> salida.append(c);
            }
        }
        return salida.toString();
    }
}
