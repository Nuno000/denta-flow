package ar.dentaflow.infraestructura.persistencia.auditoria;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guarda los valores anterior y nuevo de un cambio de turno como texto JSON (design D3). Respeta el orden
 * de los campos para que el texto sea estable y legible. Sin valores guarda {@code NULL}. Si el JSON no
 * entra en la columna lo rechaza antes del INSERT: nunca trunca, y el cambio auditado se revierte.
 */
@Converter
public class ConversorValoresJson implements AttributeConverter<Map<String, String>, String> {

    /** Largo de {@code valor_anterior} y {@code valor_nuevo} en {@code V008_1}. */
    public static final int LARGO_MAXIMO = 4000;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, String>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> valores) {
        if (valores == null || valores.isEmpty()) {
            return null;
        }
        String json = escribir(valores);
        if (json.length() > LARGO_MAXIMO) {
            throw new ValidacionDeDominioException("Los valores del cambio ocupan " + json.length()
                    + " caracteres y el máximo es " + LARGO_MAXIMO);
        }
        return json;
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String json) {
        if (json == null) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.readValue(json, TIPO);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El historial de turnos tiene un valor que no es JSON válido", e);
        }
    }

    private static String escribir(Map<String, String> valores) {
        try {
            return JSON.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los valores del cambio", e);
        }
    }
}
