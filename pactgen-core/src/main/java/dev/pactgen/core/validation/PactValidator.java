package dev.pactgen.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.model.ApiModel.ApiParameter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * {@code pactgen validate} (RF-08): comprueba que un contrato Pact (V4; también V3) es coherente con el OpenAPI, sin
 * proveedor en marcha.
 *
 * <p>Por interacción: el método y la ruta existen (plantillas {@code {id}} emparejadas con valores concretos, con la
 * misma regla que {@link OutputValidator}); el código de estado está declarado; los parámetros de consulta y las
 * cabeceras obligatorios están presentes; los cuerpos cumplen el esquema ({@link SchemaChecker}). Los campos o
 * parámetros no declarados son avisos (errores con {@code strict}). El cuerpo de la petición solo se compara con el
 * esquema cuando la respuesta esperada es de éxito: en una interacción de error, la petición inválida es a propósito.
 */
public final class PactValidator {

    public enum Level { OK, AVISO, ERROR }

    /**
     * Resultado de una interacción; {@code index} es su posición (desde 1) en el archivo. {@code reasons}: errores y
     * avisos; {@code notes}: información que no cambia el nivel (p. ej., qué no se comprobó).
     */
    public record InteractionResult(String file, int index, String description, String method, String path,
                                    Integer status, String operationId, Level level, List<String> reasons,
                                    List<String> notes) {}

    public record Report(List<InteractionResult> interactions) {
        public long count(Level l) { return interactions.stream().filter(i -> i.level() == l).count(); }
        public boolean hasErrors() { return count(Level.ERROR) > 0; }
    }

    /** El contrato no se puede leer (no existe, no es JSON o no tiene "interactions"). */
    public static class UnreadablePactException extends RuntimeException {
        public UnreadablePactException(String msg) { super(msg); }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ApiModel api;
    private final boolean strict;
    private final SchemaChecker schemas = new SchemaChecker();

    public PactValidator(ApiModel api, boolean strict) {
        this.api = api;
        this.strict = strict;
    }

    /** Un archivo .json o una carpeta (sus *.json, en orden alfabético). */
    public Report validate(Path pactOrDir) {
        List<Path> files;
        if (Files.isDirectory(pactOrDir)) {
            try (Stream<Path> s = Files.list(pactOrDir)) {
                files = s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".json")).sorted().toList();
            } catch (IOException e) {
                throw new UnreadablePactException("No se puede leer la carpeta " + pactOrDir + ": " + e.getMessage());
            }
            if (files.isEmpty()) throw new UnreadablePactException("No hay archivos .json en " + pactOrDir);
        } else if (Files.isRegularFile(pactOrDir)) {
            files = List.of(pactOrDir);
        } else {
            throw new UnreadablePactException("No existe el contrato: " + pactOrDir);
        }
        List<InteractionResult> out = new ArrayList<>();
        for (Path f : files) out.addAll(validate(f.getFileName().toString(), read(f)));
        return new Report(out);
    }

    private static JsonNode read(Path f) {
        try {
            JsonNode pact = JSON.readTree(f.toFile());
            if (pact == null || !pact.path("interactions").isArray()) {
                throw new UnreadablePactException(f + " no es un contrato Pact (falta \"interactions\")");
            }
            return pact;
        } catch (IOException e) {
            throw new UnreadablePactException("No se puede leer " + f + ": " + e.getMessage());
        }
    }

    public List<InteractionResult> validate(String file, JsonNode pact) {
        List<InteractionResult> out = new ArrayList<>();
        JsonNode interactions = pact.path("interactions");
        for (int i = 0; i < interactions.size(); i++) out.add(interaction(file, i + 1, interactions.get(i)));
        return out;
    }

    private InteractionResult interaction(String file, int index, JsonNode it) {
        String description = it.path("description").asText("");
        JsonNode req = it.path("request");
        JsonNode res = it.path("response");
        String method = req.path("method").asText("").toUpperCase(Locale.ROOT);
        String path = req.path("path").asText("");
        Integer status = res.path("status").canConvertToInt() ? res.path("status").asInt() : null;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        String type = it.path("type").asText("Synchronous/HTTP");
        if (!type.equals("Synchronous/HTTP")) {
            warnings.add("interacción de tipo " + type + ": solo se validan las HTTP síncronas");
            return result(file, index, description, method, path, status, null, errors, warnings);
        }

        // 1. Método y ruta
        List<ApiOperation> byPath = api.operations().stream()
                .filter(op -> OutputValidator.templateToRegex(op.path()).matcher(path).matches()).toList();
        if (byPath.isEmpty()) {
            errors.add("la ruta " + path + " no existe en el OpenAPI");
            return result(file, index, description, method, path, status, null, errors, warnings);
        }
        // La plantilla más concreta gana: /accounts/current antes que /accounts/{name}
        ApiOperation op = byPath.stream().filter(o -> o.method().equals(method))
                .min(Comparator.comparingLong(o -> o.path().chars().filter(c -> c == '{').count())).orElse(null);
        if (op == null) {
            errors.add(method + " no está declarado para " + path + " (declarados: "
                    + byPath.stream().map(ApiOperation::method).distinct().toList() + ")");
            return result(file, index, description, method, path, status, null, errors, warnings);
        }

        // 2. Código de estado
        if (status == null) errors.add("la respuesta no tiene código de estado");
        else if (!op.declaresStatus(status)) {
            errors.add("el código " + status + " no está declarado para " + op.operationId() + " " + op.responses().keySet());
        }

        // 3. Parámetros de consulta y cabeceras
        Map<String, List<String>> query = query(req.path("query"));
        Map<String, List<String>> headers = new LinkedHashMap<>();
        multiMap(req.path("headers")).forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), v));
        for (ApiParameter p : op.parameters()) {
            if (!p.required()) continue;
            if ("query".equals(p.in()) && !query.containsKey(p.name())) {
                errors.add("falta el parámetro de consulta obligatorio \"" + p.name() + "\"");
            } else if ("header".equals(p.in()) && !headers.containsKey(p.name().toLowerCase(Locale.ROOT))) {
                errors.add("falta la cabecera obligatoria \"" + p.name() + "\"");
            }
        }
        for (String q : query.keySet()) {
            boolean declared = op.parameters().stream().anyMatch(p -> "query".equals(p.in()) && p.name().equals(q));
            if (!declared) warnings.add("parámetro de consulta no declarado \"" + q + "\"");
        }

        // 4. Cuerpos
        JsonNode reqBody = body(req.path("body"), headers, warnings, "petición");
        if (reqBody != null) {
            if (op.requestBodySchema().isBlank()) errors.add("la operación no declara cuerpo de petición");
            // Con una respuesta de error (>= 400), una petición que no cumple el esquema es lo que el contrato
            // pretende verificar (p. ej., «alta sin firstName» -> 400): solo se exige el esquema en las de éxito
            else if (status == null || status < 400) {
                addIssues(schemas.check(reqBody, op.requestBodySchema()), "petición", errors, warnings);
            } else {
                notes.add("cuerpo de la petición no comprobado contra el esquema: la respuesta es " + status
                        + " y la petición puede ser inválida a propósito");
            }
        }
        Map<String, List<String>> resHeaders = new LinkedHashMap<>();
        multiMap(res.path("headers")).forEach((k, v) -> resHeaders.put(k.toLowerCase(Locale.ROOT), v));
        JsonNode resBody = body(res.path("body"), resHeaders, warnings, "respuesta");
        if (resBody != null && status != null) {
            String schema = responseSchema(op, status);
            if (schema == null || schema.isBlank()) {
                warnings.add("la respuesta " + status + " no declara cuerpo en el OpenAPI");
            } else {
                addIssues(schemas.check(resBody, schema), "respuesta", errors, warnings);
            }
        }
        return result(file, index, description, method, path, status, op.operationId(), errors, warnings, notes);
    }

    private InteractionResult result(String file, int index, String description, String method, String path,
                                     Integer status, String operationId, List<String> errors, List<String> warnings) {
        return result(file, index, description, method, path, status, operationId, errors, warnings, List.of());
    }

    private InteractionResult result(String file, int index, String description, String method, String path,
                                     Integer status, String operationId, List<String> errors, List<String> warnings,
                                     List<String> notes) {
        List<String> reasons = new ArrayList<>(errors);
        reasons.addAll(warnings);
        Level level = !errors.isEmpty() || (strict && !warnings.isEmpty()) ? Level.ERROR
                : warnings.isEmpty() ? Level.OK : Level.AVISO;
        return new InteractionResult(file, index, description, method, path, status, operationId, level, reasons,
                List.copyOf(notes));
    }

    private static void addIssues(List<SchemaChecker.Issue> issues, String where, List<String> errors, List<String> warnings) {
        for (SchemaChecker.Issue i : issues) (i.error() ? errors : warnings).add(where + " " + i);
    }

    private static String responseSchema(ApiOperation op, int status) {
        Map<String, String> r = op.responses();
        String exact = String.valueOf(status);
        if (r.containsKey(exact)) return r.get(exact);
        String range = exact.charAt(0) + "XX";
        if (r.containsKey(range)) return r.get(range);
        return r.get("default");
    }

    /**
     * Cuerpo JSON de una petición o respuesta. V4: {"content": ..., "contentType": ..., "encoded": false|"base64"|"json"};
     * V3: el JSON directamente. Devuelve null si no hay cuerpo o no es JSON.
     */
    private static JsonNode body(JsonNode body, Map<String, List<String>> headers, List<String> warnings, String where) {
        if (body.isMissingNode() || body.isNull()) return null;
        boolean v4 = body.isObject() && body.has("content") && (body.has("contentType") || body.has("encoded"));
        if (!v4) return body;
        String contentType = body.path("contentType").asText(
                headers.getOrDefault("content-type", List.of("application/json")).get(0));
        if (!contentType.toLowerCase(Locale.ROOT).contains("json")) return null;
        JsonNode content = body.path("content");
        String encoded = body.path("encoded").asText("false");
        try {
            if (encoded.equalsIgnoreCase("base64")) {
                return JSON.readTree(new String(Base64.getDecoder().decode(content.asText()), StandardCharsets.UTF_8));
            }
            if (content.isTextual()) return JSON.readTree(content.asText());
        } catch (Exception e) {
            warnings.add("el cuerpo de la " + where + " no es JSON legible");
            return null;
        }
        return content;
    }

    /** V4: {"name": ["v1"]}; V3: {"name": ["v1"]} o "a=1&b=2". */
    private static Map<String, List<String>> query(JsonNode q) {
        if (q.isTextual()) {
            Map<String, List<String>> out = new LinkedHashMap<>();
            for (String part : q.asText().split("&")) {
                if (part.isBlank()) continue;
                int eq = part.indexOf('=');
                String k = URLDecoder.decode(eq < 0 ? part : part.substring(0, eq), StandardCharsets.UTF_8);
                String v = eq < 0 ? "" : URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                out.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
            }
            return out;
        }
        return multiMap(q);
    }

    private static Map<String, List<String>> multiMap(JsonNode n) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if (!n.isObject()) return out;
        for (Map.Entry<String, JsonNode> e : n.properties()) {
            List<String> vals = new ArrayList<>();
            if (e.getValue().isArray()) e.getValue().forEach(v -> vals.add(v.asText()));
            else vals.add(e.getValue().asText());
            out.put(e.getKey(), vals);
        }
        return out;
    }
}
