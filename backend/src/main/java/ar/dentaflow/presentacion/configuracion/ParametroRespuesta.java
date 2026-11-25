package ar.dentaflow.presentacion.configuracion;

import ar.dentaflow.dominio.configuracion.ClaveParametro;
import ar.dentaflow.dominio.configuracion.Parametro;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Locale;

/** Parámetro del consultorio con las reglas de su clave, para que el cliente valide antes de enviar. */
@Schema(description = "Parámetro de configuración del consultorio")
public record ParametroRespuesta(
        @Schema(description = "Código del parámetro, en minúsculas con guiones") String clave,
        String descripcion,
        @Schema(allowableValues = {"entero", "texto"}) String tipo,
        @Schema(description = "Solo para enteros", example = "horas") String unidad,
        @Schema(description = "Solo para enteros") Integer minimo,
        @Schema(description = "Solo para enteros") Integer maximo,
        @Schema(description = "Solo para textos") Integer longitudMaxima,
        @Schema(description = "Siempre como texto, también los enteros") String valor,
        @Schema(description = "Verdadero hasta que un administrador guarda el valor") boolean provisorio,
        @Schema(description = "Instante UTC de la última edición; nulo si nunca se editó") Instant actualizadoEn,
        @Schema(description = "Id del administrador que hizo la última edición") Long actualizadoPor) {

    public static ParametroRespuesta de(Parametro parametro) {
        ClaveParametro clave = parametro.clave();
        return new ParametroRespuesta(clave.codigo(), clave.descripcion(),
                clave.tipo().name().toLowerCase(Locale.ROOT), clave.unidad(), clave.minimo(), clave.maximo(),
                clave.longitudMaxima(), parametro.valor(), parametro.isProvisorio(), parametro.actualizadoEn(),
                parametro.actualizadoPor());
    }
}
