package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.configuracion.AvisoPrivacidad;

/**
 * Aviso de privacidad para la reserva online (US-040, RN-PR-01). Solo texto y leyenda: ni la marca
 * provisoria ni quién lo editó (spec privacidad-datos-personales, "Aviso de privacidad público").
 */
public record AvisoPrivacidadRespuesta(String texto, String leyenda) {

    static AvisoPrivacidadRespuesta de(AvisoPrivacidad aviso) {
        return new AvisoPrivacidadRespuesta(aviso.texto(), aviso.leyendaAceptacion());
    }
}
