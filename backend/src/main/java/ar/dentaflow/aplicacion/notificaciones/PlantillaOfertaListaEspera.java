package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PlantillaOfertaListaEspera {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_AR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES_AR);

    public MensajeEmail armar(String destinatario, String nombrePaciente, String prestacion, String profesional,
            Instant inicioHueco, Instant ofrecidaHasta, String enlaceAceptacion) {
        ZonedDateTime inicioLocal = inicioHueco.atZone(ZonaHorariaConsultorio.ZONA);
        ZonedDateTime venceLocal = ofrecidaHasta.atZone(ZonaHorariaConsultorio.ZONA);
        String fecha = FECHA.format(inicioLocal);
        String hora = HORA.format(inicioLocal);
        String fechaVence = FECHA.format(venceLocal) + " a las " + HORA.format(venceLocal);

        String asunto = "Se liberó un turno de " + prestacion + " — DentaFlow";
        String cuerpoTexto = """
                Hola %s,

                Se liberó un turno compatible con tu búsqueda en la lista de espera:
                - Prestación: %s
                - Profesional: %s
                - Fecha y hora: %s a las %s hs (hora de Argentina)

                Si querés tomarlo, confirmalo acá antes del %s:
                %s

                El primer paciente en confirmar obtiene el turno.
                """.formatted(nombrePaciente, prestacion, profesional, fecha, hora, fechaVence, enlaceAceptacion);

        String cuerpoHtml = """
                <p>Hola %s,</p>
                <p>Se liberó un turno compatible con tu búsqueda en la lista de espera:</p>
                <ul>
                  <li><strong>Prestación:</strong> %s</li>
                  <li><strong>Profesional:</strong> %s</li>
                  <li><strong>Fecha y hora:</strong> %s a las %s hs (hora de Argentina)</li>
                </ul>
                <p><a href="%s">Confirmar y tomar este turno</a></p>
                <p>Oferta vigente hasta el %s. El primer paciente en confirmar obtiene el turno.</p>
                """.formatted(
                PlantillaConfirmacionReserva.escapar(nombrePaciente),
                PlantillaConfirmacionReserva.escapar(prestacion),
                PlantillaConfirmacionReserva.escapar(profesional),
                PlantillaConfirmacionReserva.escapar(fecha),
                PlantillaConfirmacionReserva.escapar(hora),
                PlantillaConfirmacionReserva.escapar(enlaceAceptacion),
                PlantillaConfirmacionReserva.escapar(fechaVence));

        return new MensajeEmail(destinatario, asunto, cuerpoTexto, cuerpoHtml);
    }
}
