package dev.pactgen.core.validation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Filtro determinista de interacciones redundantes (v1.2).
 *
 * <p>Las pruebas generadas comparan los cuerpos por tipo, no por valor, así que dos interacciones con el
 * mismo método, el mismo código de estado y la misma forma de petición y respuesta verifican lo mismo
 * aunque cambien los identificadores, los datos o el estado del proveedor. La firma de una interacción
 * ignora los valores concretos y conserva solo lo que cambia el contrato: método, nombres de los
 * parámetros de consulta, forma del cuerpo de petición, estado y forma del cuerpo de respuesta.
 * Se conserva la primera interacción de cada firma, en el orden propuesto por el modelo.
 *
 * <p>Sustituye a la regla 6 del prompt v1.1, que el modelo no cumplía de forma fiable.
 */
public final class InteractionDeduplicator {

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public record Result(List<JsonNode> kept, int removed) {}

    public Result dedupe(List<JsonNode> interactions) {
        Set<String> seen = new LinkedHashSet<>();
        List<JsonNode> kept = new ArrayList<>();
        for (JsonNode it : interactions) {
            if (seen.add(signature(it))) kept.add(it);
        }
        return new Result(kept, interactions.size() - kept.size());
    }

    /** Firma independiente de los valores; visible para las pruebas. */
    public static String signature(JsonNode it) {
        JsonNode req = it.path("request");
        JsonNode res = it.path("response");
        return req.path("method").asText().toUpperCase()
                + " ?" + fieldNames(req.path("query"))
                + " req=" + shape(req.path("body"))
                + " -> " + res.path("status").asInt()
                + " res=" + shape(res.path("body"));
    }

    private static String fieldNames(JsonNode obj) {
        List<String> names = new ArrayList<>();
        obj.fieldNames().forEachRemaining(names::add);
        names.sort(String::compareTo);
        return names.toString();
    }

    static String shape(JsonNode n) {
        if (n == null || n.isMissingNode()) return "-";
        if (n.isNull()) return "null";
        if (n.isBoolean()) return "bool";
        if (n.isIntegralNumber()) return "int";
        if (n.isNumber()) return "dec";
        if (n.isTextual()) return ISO_DATE.matcher(n.asText()).matches() ? "date" : "str";
        if (n.isArray()) return n.isEmpty() ? "[]" : "[" + shape(n.get(0)) + "]";
        Map<String, String> fields = new TreeMap<>();
        n.properties().forEach(e -> fields.put(e.getKey(), shape(e.getValue())));
        return fields.toString();
    }
}
