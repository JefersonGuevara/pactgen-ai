package dev.pactgen.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pactgen.core.model.ApiModel.ApiOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filtros 1 y 2 de la sección 4.2.5 (RF-05): sintáctico (estructura del JSON de salida)
 * y semántico frente al OpenAPI (método, ruta y códigos declarados).
 * El filtro 3 (compila y pasa) lo aplica Maven al ejecutar las pruebas generadas.
 */
public final class OutputValidator {

    public record Result(JsonNode output, List<String> errors) {
        public boolean valid() { return errors.isEmpty(); }
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    public Result validate(String llmText, ApiOperation op) {
        List<String> errors = new ArrayList<>();
        JsonNode root;
        try {
            root = JSON.readTree(stripFences(llmText));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return new Result(null, List.of("La respuesta no es JSON válido: " + e.getOriginalMessage()));
        }
        if (root == null || !root.isObject()) {
            return new Result(null, List.of("La respuesta debe ser un objeto JSON"));
        }
        if (!root.path("analysis").isTextual()) errors.add("Falta el campo \"analysis\" (texto)");
        JsonNode interactions = root.path("interactions");
        if (!interactions.isArray() || interactions.isEmpty()) {
            errors.add("\"interactions\" debe ser un arreglo no vacío");
            return new Result(root, errors);
        }
        Pattern pathPattern = templateToRegex(op.path());
        for (int i = 0; i < interactions.size(); i++) {
            JsonNode it = interactions.get(i);
            String at = "interactions[" + i + "]";
            requireText(it, "description", at, errors);
            requireText(it, "providerState", at, errors);
            JsonNode req = it.path("request");
            JsonNode res = it.path("response");
            if (!req.isObject()) { errors.add(at + ".request es obligatorio"); continue; }
            if (!res.isObject()) { errors.add(at + ".response es obligatorio"); continue; }

            String method = req.path("method").asText("").toUpperCase(Locale.ROOT);
            if (!METHODS.contains(method)) errors.add(at + ".request.method no es un método HTTP válido");
            else if (!method.equals(op.method())) {
                errors.add(at + ".request.method es " + method + " pero la operación es " + op.method());
            }
            String path = req.path("path").asText("");
            if (!path.startsWith("/")) errors.add(at + ".request.path debe empezar por /");
            else if (!pathPattern.matcher(path).matches()) {
                errors.add(at + ".request.path \"" + path + "\" no corresponde a la plantilla " + op.path());
            }
            if (req.has("query") && !req.path("query").isObject()) errors.add(at + ".request.query debe ser un objeto");
            if (req.has("headers") && !req.path("headers").isObject()) errors.add(at + ".request.headers debe ser un objeto");
            if (req.has("body") && !(req.path("body").isContainerNode() || req.path("body").isNull())) {
                errors.add(at + ".request.body debe ser un objeto o un arreglo JSON");
            }
            if (req.has("body") && !req.path("body").isNull() && op.requestBodySchema().isBlank()) {
                errors.add(at + ".request.body no está permitido: la operación no declara cuerpo de petición");
            }

            JsonNode status = res.path("status");
            if (!status.canConvertToInt() || !status.isIntegralNumber()) {
                errors.add(at + ".response.status debe ser un entero");
            } else if (!op.declaresStatus(status.asInt())) {
                errors.add(at + ".response.status " + status.asInt() + " no está declarado en el OpenAPI "
                        + op.responses().keySet());
            }
            if (res.has("headers") && !res.path("headers").isObject()) errors.add(at + ".response.headers debe ser un objeto");
            if (res.has("body") && !(res.path("body").isContainerNode() || res.path("body").isNull())) {
                errors.add(at + ".response.body debe ser un objeto o un arreglo JSON");
            }
        }
        return new Result(root, errors);
    }

    private static void requireText(JsonNode n, String field, String at, List<String> errors) {
        if (!n.path(field).isTextual() || n.path(field).asText().isBlank()) {
            errors.add(at + "." + field + " es obligatorio");
        }
    }

    /** "/owners/{ownerId}" → ^/owners/[^/]+$ */
    static Pattern templateToRegex(String template) {
        StringBuilder sb = new StringBuilder("^");
        for (String part : template.split("(?=\\{)|(?<=\\})")) {
            if (part.startsWith("{") && part.endsWith("}")) sb.append("[^/]+");
            else sb.append(Pattern.quote(part));
        }
        return Pattern.compile(sb.append("$").toString());
    }

    /** Tolera respuestas envueltas en bloques ```json ... ``` aunque el prompt lo prohíbe. */
    static String stripFences(String s) {
        String t = s.strip();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            int last = t.lastIndexOf("```");
            if (first > 0 && last > first) t = t.substring(first + 1, last).strip();
        }
        return t;
    }
}
