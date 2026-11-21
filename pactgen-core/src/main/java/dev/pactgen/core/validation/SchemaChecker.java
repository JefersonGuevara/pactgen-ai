package dev.pactgen.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Comprueba un valor JSON (cuerpo de un contrato) contra un esquema OpenAPI 3.0 ya resuelto (sin $ref), tal como lo
 * serializa {@link dev.pactgen.core.openapi.OpenApiLoader} (RF-08, {@code pactgen validate}).
 *
 * <p>Cubre lo que verifica un contrato: obligatorios, tipos, formatos {@code date} y {@code date-time}, {@code enum}
 * y campos no declarados (aviso). {@code allOf} se combina; {@code oneOf}/{@code anyOf} se aceptan si alguna
 * alternativa no da errores. No se comprueban longitudes, patrones ni rangos.
 */
public final class SchemaChecker {

    /** Hallazgo en una ruta JSON ($.a.b[0]). {@code error = false} es un aviso. */
    public record Issue(boolean error, String path, String message) {
        @Override public String toString() { return path + ": " + message; }
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    /** ISO 8601 con hora y zona (Z, +00:00 o +0000), como la regla date-time que genera la herramienta (v1.6). */
    private static final Pattern DATE_TIME =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})");

    /** Esquema serializado ("" o null = sin esquema: no se comprueba nada). */
    public List<Issue> check(JsonNode value, String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return List.of();
        try {
            return check(value, JSON.readTree(schemaJson));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Esquema no legible: " + e.getOriginalMessage(), e);
        }
    }

    public List<Issue> check(JsonNode value, JsonNode schema) {
        List<Issue> out = new ArrayList<>();
        visit(value, schema, "$", out);
        return out;
    }

    private void visit(JsonNode v, JsonNode schema, String at, List<Issue> out) {
        if (schema == null || schema.isMissingNode() || schema.isNull() || schema.isEmpty()) return;
        schema = mergeAllOf(schema);

        if (v == null || v.isNull()) {
            if (!schema.path("nullable").asBoolean(false)) out.add(new Issue(false, at, "valor null en un campo no nullable"));
            return;
        }
        for (String alt : List.of("oneOf", "anyOf")) {
            if (schema.path(alt).isArray() && !schema.path(alt).isEmpty()) {
                boolean any = false;
                for (JsonNode s : schema.path(alt)) {
                    List<Issue> tmp = new ArrayList<>();
                    visit(v, s, at, tmp);
                    if (tmp.stream().noneMatch(Issue::error)) { any = true; break; }
                }
                if (!any) out.add(new Issue(true, at, "no cumple ninguna alternativa de " + alt));
                return;
            }
        }

        String type = type(schema);
        switch (type) {
            case "string" -> {
                if (!v.isTextual()) { out.add(typeIssue(at, "texto", v)); return; }
                String format = schema.path("format").asText("");
                if (format.equals("date") && !isDate(v.asText())) {
                    out.add(new Issue(true, at, "\"" + v.asText() + "\" no tiene formato date (yyyy-MM-dd)"));
                } else if (format.equals("date-time") && !isDateTime(v.asText())) {
                    out.add(new Issue(true, at, "\"" + v.asText() + "\" no tiene formato date-time (ISO 8601 con zona)"));
                }
            }
            case "integer" -> {
                if (!(v.isIntegralNumber() || (v.isNumber() && v.decimalValue().stripTrailingZeros().scale() <= 0))) {
                    out.add(typeIssue(at, "entero", v));
                    return;
                }
            }
            case "number" -> { if (!v.isNumber()) { out.add(typeIssue(at, "número", v)); return; } }
            case "boolean" -> { if (!v.isBoolean()) { out.add(typeIssue(at, "booleano", v)); return; } }
            case "array" -> {
                if (!v.isArray()) { out.add(typeIssue(at, "arreglo", v)); return; }
                for (int i = 0; i < v.size(); i++) visit(v.get(i), schema.path("items"), at + "[" + i + "]", out);
            }
            case "object" -> {
                if (!v.isObject()) { out.add(typeIssue(at, "objeto", v)); return; }
                object(v, schema, at, out);
            }
            default -> { } // sin tipo declarado: solo enum
        }
        JsonNode en = schema.path("enum");
        if (en.isArray() && !en.isEmpty()) {
            boolean found = false;
            for (JsonNode e : en) if (e.equals(v) || (e.isValueNode() && v.isValueNode() && e.asText().equals(v.asText()))) found = true;
            if (!found) out.add(new Issue(true, at, v + " no es un valor del enum " + en));
        }
    }

    private void object(JsonNode v, JsonNode schema, String at, List<Issue> out) {
        JsonNode props = schema.path("properties");
        for (JsonNode r : schema.path("required")) {
            if (!v.has(r.asText())) out.add(new Issue(true, at, "falta el campo obligatorio \"" + r.asText() + "\""));
            else if (v.get(r.asText()).isNull()) out.add(new Issue(true, at + "." + r.asText(), "campo obligatorio con valor null"));
        }
        Set<String> required = new LinkedHashSet<>();
        schema.path("required").forEach(r -> required.add(r.asText()));
        JsonNode additional = schema.path("additionalProperties");
        for (Map.Entry<String, JsonNode> e : v.properties()) {
            String child = at + "." + e.getKey();
            if (e.getValue().isNull() && required.contains(e.getKey())) continue; // ya informado arriba
            if (props.has(e.getKey())) {
                visit(e.getValue(), props.get(e.getKey()), child, out);
            } else if (additional.isObject()) {
                visit(e.getValue(), additional, child, out);
            } else if (!additional.asBoolean(false) && (props.size() > 0 || additional.isBoolean())) {
                out.add(new Issue(false, child, "campo no declarado en el esquema"));
            }
        }
    }

    /** Combina los miembros de allOf (propiedades y obligatorios) con el propio esquema (también lo usa diff). */
    public static JsonNode mergeAllOf(JsonNode schema) {
        JsonNode all = schema.path("allOf");
        if (!all.isArray() || all.isEmpty()) return schema;
        ObjectNode merged = schema.deepCopy();
        merged.remove("allOf");
        ObjectNode props = merged.has("properties") ? (ObjectNode) merged.get("properties") : merged.putObject("properties");
        Set<String> required = new LinkedHashSet<>();
        merged.path("required").forEach(r -> required.add(r.asText()));
        for (JsonNode part : all) {
            JsonNode p = mergeAllOf(part);
            if (!merged.has("type") && p.has("type")) merged.set("type", p.get("type"));
            p.path("properties").properties().forEach(e -> props.set(e.getKey(), e.getValue()));
            p.path("required").forEach(r -> required.add(r.asText()));
        }
        ArrayNode req = JsonNodeFactory.instance.arrayNode();
        required.forEach(req::add);
        merged.set("required", req);
        return merged;
    }

    /** Tipo declarado (o deducido de properties/items); "" si no lo hay. */
    public static String type(JsonNode schema) {
        if (schema.path("type").isTextual()) return schema.path("type").asText();
        // OpenAPI 3.1 serializado por swagger-parser: "types": ["string", "null"]
        for (JsonNode t : schema.path("types")) if (!t.asText().equals("null")) return t.asText();
        if (schema.has("properties")) return "object";
        if (schema.has("items")) return "array";
        return "";
    }

    private static Issue typeIssue(String at, String expected, JsonNode v) {
        return new Issue(true, at, "se esperaba " + expected + " y es " + describe(v));
    }

    private static String describe(JsonNode v) {
        if (v.isTextual()) return "texto \"" + v.asText() + "\"";
        if (v.isNumber()) return "número " + v;
        if (v.isBoolean()) return "booleano " + v;
        if (v.isArray()) return "un arreglo";
        if (v.isObject()) return "un objeto";
        return v.getNodeType().toString().toLowerCase();
    }

    static boolean isDate(String s) {
        if (!DATE.matcher(s).matches()) return false;
        try { LocalDate.parse(s); return true; } catch (Exception e) { return false; }
    }

    static boolean isDateTime(String s) {
        if (!DATE_TIME.matcher(s).matches()) return false;
        try {
            OffsetDateTime.parse(s.replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
