package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Completa la plantilla {@code whatsapp-plantilla} (C-21, design D3). Marcadores: {@code {nombre}}, {@code {fecha}},
 * {@code {hora}}, {@code {prestacion}}, {@code {profesional}} y {@code {enlace}}; fecha y hora en la zona del
 * consultorio. Un marcador desconocido queda literal, y un valor que contenga llaves no se vuelve a interpretar.
 */
public final class PlantillaWhatsapp {

    private static final Pattern MARCADOR = Pattern.compile("\\{([a-z]+)}");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private PlantillaWhatsapp() {
    }

    public static String completar(String plantilla, DatosDelMensajeWhatsapp datos) {
        LocalDateTime inicio = ZonaHorariaConsultorio.aHoraLocal(datos.inicio());
        Map<String, String> valores = Map.of(
                "nombre", datos.nombre(),
                "fecha", FECHA.format(inicio),
                "hora", HORA.format(inicio),
                "prestacion", datos.prestacion(),
                "profesional", datos.profesional(),
                "enlace", datos.enlace());
        Matcher marcador = MARCADOR.matcher(plantilla);
        StringBuilder mensaje = new StringBuilder();
        while (marcador.find()) {
            String valor = valores.getOrDefault(marcador.group(1), marcador.group());
            marcador.appendReplacement(mensaje, Matcher.quoteReplacement(valor));
        }
        marcador.appendTail(mensaje);
        return mensaje.toString();
    }
}
