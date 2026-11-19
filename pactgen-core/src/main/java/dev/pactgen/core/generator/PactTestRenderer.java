package dev.pactgen.core.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pactgen.core.model.ApiModel.ApiOperation;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * M4 — Genera, con una plantilla determinista, la prueba de consumidor JUnit 5 + Pact JVM (4.6.x)
 * para las interacciones validadas (RF-07). El LLM nunca escribe Java: solo propone interacciones.
 * Los cuerpos se traducen a reglas de coincidencia por tipo (PactDslJsonBody).
 */
public final class PactTestRenderer {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private int varCounter;

    public record Rendered(String className, String source) {}

    public Rendered render(String packageName, String consumer, String provider,
                           ApiOperation op, List<JsonNode> interactions) {
        String className = pascal(op.operationId()) + "PactTest";
        StringBuilder s = new StringBuilder();
        if (!packageName.isBlank()) s.append("package ").append(packageName).append(";\n\n");
        s.append("""
                import au.com.dius.pact.consumer.MockServer;
                import au.com.dius.pact.consumer.dsl.DslPart;
                import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
                import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
                import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
                import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
                import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
                import au.com.dius.pact.consumer.junit5.PactTestFor;
                import au.com.dius.pact.core.model.PactSpecVersion;
                import au.com.dius.pact.core.model.V4Pact;
                import au.com.dius.pact.core.model.annotations.Pact;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.extension.ExtendWith;

                import java.net.URI;
                import java.net.http.HttpClient;
                import java.net.http.HttpRequest;
                import java.net.http.HttpResponse;
                import java.util.Map;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                """);
        s.append("/** Generado por PactGen-AI para ").append(op.method()).append(' ').append(op.path())
                .append(". No editar a mano. */\n");
        s.append("@ExtendWith(PactConsumerTestExt.class)\n");
        s.append("@PactTestFor(providerName = ").append(str(provider)).append(", pactVersion = PactSpecVersion.V4)\n");
        s.append("class ").append(className).append(" {\n");

        for (int i = 0; i < interactions.size(); i++) {
            JsonNode it = interactions.get(i);
            JsonNode req = it.path("request");
            JsonNode res = it.path("response");
            String pathWithQuery = req.path("path").asText() + queryString(req.path("query"));

            // ---- método @Pact
            s.append("\n    // ").append(it.path("description").asText().replace("\n", " ")).append('\n');
            s.append("    @Pact(consumer = ").append(str(consumer)).append(", provider = ").append(str(provider)).append(")\n");
            s.append("    public V4Pact pact").append(i).append("(PactDslWithProvider builder) {\n");
            varCounter = 0;
            // En las respuestas de error (4xx/5xx) el cuerpo de la petición se fija con valores exactos (v1.3):
            // el contrato debe decir "esta petición concreta produce el error", no "cualquier cuerpo con esta forma".
            boolean exactRequest = res.path("status").asInt() >= 400 && hasBody(req.path("body"));
            String reqBody = exactRequest
                    ? str(compact(req.path("body"))) + ", \"application/json\""
                    : bodyVar(s, req.path("body"), "req");
            String resBody = bodyVar(s, res.path("body"), "res");
            s.append("        return builder\n");
            s.append("            .given(").append(str(it.path("providerState").asText())).append(")\n");
            s.append("            .uponReceiving(").append(str(it.path("description").asText())).append(")\n");
            s.append("            .path(").append(str(req.path("path").asText())).append(")\n");
            s.append("            .method(").append(str(req.path("method").asText().toUpperCase())).append(")\n");
            if (req.path("query").isObject() && !req.path("query").isEmpty()) {
                s.append("            .query(").append(str(queryString(req.path("query")).substring(1))).append(")\n");
            }
            String reqHeaders = headers(req.path("headers"), reqBody != null);
            if (reqHeaders != null) s.append("            .headers(").append(reqHeaders).append(")\n");
            if (reqBody != null) s.append("            .body(").append(reqBody).append(")\n");
            s.append("            .willRespondWith()\n");
            s.append("            .status(").append(res.path("status").asInt()).append(")\n");
            String resHeaders = headers(res.path("headers"), resBody != null);
            if (resHeaders != null) s.append("            .headers(").append(resHeaders).append(")\n");
            if (resBody != null) s.append("            .body(").append(resBody).append(")\n");
            s.append("            .toPact(V4Pact.class);\n");
            s.append("    }\n");

            // ---- prueba que ejerce la interacción contra el servidor simulado
            s.append("\n    @Test\n");
            s.append("    @PactTestFor(pactMethod = \"pact").append(i).append("\")\n");
            s.append("    void interaction").append(i).append("(MockServer mockServer) throws Exception {\n");
            s.append("        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + ")
                    .append(str(pathWithQuery)).append("));\n");
            if (req.path("headers").isObject()) {
                for (Map.Entry<String, JsonNode> e : req.path("headers").properties()) {
                    if (!e.getKey().equalsIgnoreCase("content-type")) {
                        s.append("        request.header(").append(str(e.getKey())).append(", ")
                                .append(str(e.getValue().asText())).append(");\n");
                    }
                }
            }
            if (reqBody != null) {
                s.append("        request.header(\"Content-Type\", \"application/json\");\n");
                s.append("        request.method(").append(str(req.path("method").asText().toUpperCase()))
                        .append(", HttpRequest.BodyPublishers.ofString(").append(str(compact(req.path("body")))).append("));\n");
            } else {
                s.append("        request.method(").append(str(req.path("method").asText().toUpperCase()))
                        .append(", HttpRequest.BodyPublishers.noBody());\n");
            }
            s.append("        HttpResponse<String> response = HttpClient.newHttpClient()\n");
            s.append("            .send(request.build(), HttpResponse.BodyHandlers.ofString());\n");
            s.append("        assertEquals(").append(res.path("status").asInt()).append(", response.statusCode());\n");
            s.append("    }\n");
        }
        s.append("}\n");
        return new Rendered(className, s.toString());
    }

    // ------------------------------------------------------------------ cuerpos con reglas por tipo

    /** Declara variables para el cuerpo y devuelve el nombre de la variable DslPart, o null si no hay cuerpo. */
    private String bodyVar(StringBuilder s, JsonNode body, String prefix) {
        if (body == null || body.isMissingNode() || body.isNull()) return null;
        String root = prefix + "Body";
        if (body.isObject()) {
            s.append("        PactDslJsonBody ").append(root).append(" = new PactDslJsonBody();\n");
            fields(s, root, body);
            return root;
        }
        if (body.isArray()) {
            JsonNode first = body.isEmpty() ? null : body.get(0);
            if (first != null && first.isObject()) {
                String el = newVar(prefix + "Item");
                s.append("        PactDslJsonBody ").append(el).append(" = PactDslJsonArray.arrayEachLike();\n");
                fields(s, el, first);
                s.append("        DslPart ").append(root).append(" = ").append(el).append(".closeObject();\n");
            } else if (first != null) {
                s.append("        DslPart ").append(root).append(" = PactDslJsonArray.arrayEachLike(")
                        .append(rootValue(first)).append(");\n");
            } else {
                s.append("        DslPart ").append(root).append(" = new PactDslJsonArray();\n");
            }
            return root;
        }
        return null;
    }

    private void fields(StringBuilder s, String var, JsonNode obj) {
        for (Map.Entry<String, JsonNode> e : obj.properties()) {
            String name = str(e.getKey());
            JsonNode v = e.getValue();
            if (v.isTextual()) {
                if (ISO_DATE.matcher(v.asText()).matches()) {
                    s.append("        ").append(var).append(".stringMatcher(").append(name)
                            .append(", \"\\\\d{4}-\\\\d{2}-\\\\d{2}\", ").append(str(v.asText())).append(");\n");
                } else {
                    s.append("        ").append(var).append(".stringType(").append(name).append(", ")
                            .append(str(v.asText())).append(");\n");
                }
            } else if (v.isIntegralNumber()) {
                s.append("        ").append(var).append(".integerType(").append(name).append(", ")
                        .append(v.asLong()).append(v.canConvertToInt() ? "" : "L").append(");\n");
            } else if (v.isNumber()) {
                s.append("        ").append(var).append(".decimalType(").append(name).append(", ")
                        .append(v.asDouble()).append(");\n");
            } else if (v.isBoolean()) {
                s.append("        ").append(var).append(".booleanType(").append(name).append(", ")
                        .append(v.asBoolean()).append(");\n");
            } else if (v.isNull()) {
                s.append("        ").append(var).append(".nullValue(").append(name).append(");\n");
            } else if (v.isObject()) {
                String child = newVar(var + "_" + safe(e.getKey()));
                s.append("        PactDslJsonBody ").append(child).append(" = ").append(var)
                        .append(".object(").append(name).append(");\n");
                fields(s, child, v);
                s.append("        ").append(child).append(".closeObject();\n");
            } else if (v.isArray()) {
                if (v.isEmpty()) {
                    s.append("        ").append(var).append(".array(").append(name).append(").closeArray();\n");
                } else if (v.get(0).isObject()) {
                    String child = newVar(var + "_" + safe(e.getKey()));
                    s.append("        PactDslJsonBody ").append(child).append(" = ").append(var)
                            .append(".eachLike(").append(name).append(");\n");
                    fields(s, child, v.get(0));
                    s.append("        ").append(child).append(".closeObject().closeArray();\n");
                } else {
                    s.append("        ").append(var).append(".eachLike(").append(name).append(", ")
                            .append(rootValue(v.get(0))).append(");\n");
                }
            }
        }
    }

    private static String rootValue(JsonNode v) {
        if (v.isIntegralNumber()) return "PactDslJsonRootValue.integerType(" + v.asLong() + (v.canConvertToInt() ? "" : "L") + ")";
        if (v.isNumber()) return "PactDslJsonRootValue.decimalType(" + v.asDouble() + ")";
        if (v.isBoolean()) return "PactDslJsonRootValue.booleanType(" + v.asBoolean() + ")";
        return "PactDslJsonRootValue.stringType(" + str(v.asText()) + ")";
    }

    private String newVar(String base) {
        return safe(base) + (varCounter++);
    }

    private static String headers(JsonNode h, boolean hasBody) {
        StringBuilder sb = new StringBuilder();
        boolean ct = false;
        if (h != null && h.isObject()) {
            for (Map.Entry<String, JsonNode> e : h.properties()) {
                if (e.getKey().equalsIgnoreCase("content-type")) ct = true;
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(str(e.getKey())).append(", ").append(str(e.getValue().asText()));
            }
        }
        if (hasBody && !ct) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("\"Content-Type\", \"application/json\"");
        }
        return sb.isEmpty() ? null : "Map.of(" + sb + ")";
    }

    private static String queryString(JsonNode q) {
        if (q == null || !q.isObject() || q.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        for (Map.Entry<String, JsonNode> e : q.properties()) {
            if (sb.length() > 1) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue().asText());
        }
        return sb.toString();
    }

    private static boolean hasBody(JsonNode body) {
        return body != null && !body.isMissingNode() && !body.isNull();
    }

    private static String compact(JsonNode n) {
        try { return JSON.writeValueAsString(n); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    static String safe(String s) {
        String r = s.replaceAll("[^A-Za-z0-9_]", "_");
        return Character.isJavaIdentifierStart(r.charAt(0)) ? r : "_" + r;
    }

    static String pascal(String s) {
        StringBuilder sb = new StringBuilder();
        for (String p : s.split("[^A-Za-z0-9]+")) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        String r = sb.toString();
        return r.isEmpty() || !Character.isJavaIdentifierStart(r.charAt(0)) ? "Op" + r : r;
    }
}
