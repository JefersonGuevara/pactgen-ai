package dev.pactgen.core.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.model.ApiModel.ApiParameter;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** M1 — Lee, valida y normaliza especificaciones OpenAPI 3.x (RF-01). */
public final class OpenApiLoader {

    public static class InvalidSpecException extends RuntimeException {
        public InvalidSpecException(String msg) { super(msg); }
    }

    public ApiModel load(Path spec) {
        if (!Files.isRegularFile(spec)) {
            throw new InvalidSpecException("No existe el archivo OpenAPI: " + spec);
        }
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(spec.toAbsolutePath().toString(), null, options);
        OpenAPI api = result.getOpenAPI();
        if (api == null) {
            throw new InvalidSpecException("OpenAPI inválido: " + result.getMessages());
        }
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            // Avisos del parser: se informan, pero no detienen la ejecución si el modelo se pudo construir.
            result.getMessages().forEach(m -> System.err.println("[openapi] " + m));
        }
        List<ApiOperation> ops = new ArrayList<>();
        if (api.getPaths() != null) {
            api.getPaths().forEach((path, item) -> item.readOperationsMap()
                    .forEach((method, op) -> ops.add(toOperation(path, item, method, op, api))));
        }
        String title = api.getInfo() != null ? api.getInfo().getTitle() : "";
        String version = api.getInfo() != null ? api.getInfo().getVersion() : "";
        return new ApiModel(title, version, ops);
    }

    private ApiOperation toOperation(String path, PathItem item, PathItem.HttpMethod method, Operation op, OpenAPI api) {
        String m = method.name().toUpperCase(Locale.ROOT);
        String id = op.getOperationId() != null ? op.getOperationId()
                : (m.toLowerCase(Locale.ROOT) + path.replaceAll("[^A-Za-z0-9]+", "_"));

        List<ApiParameter> params = new ArrayList<>();
        List<Parameter> all = new ArrayList<>();
        if (item.getParameters() != null) all.addAll(item.getParameters());
        if (op.getParameters() != null) all.addAll(op.getParameters());
        for (Parameter p : all) {
            params.add(new ApiParameter(p.getName(), p.getIn(), Boolean.TRUE.equals(p.getRequired()),
                    toJson(p.getSchema())));
        }

        String requestSchema = "";
        if (op.getRequestBody() != null) {
            requestSchema = toJson(jsonSchema(op.getRequestBody().getContent()));
        }

        Map<String, String> responses = new LinkedHashMap<>();
        Map<String, String> descriptions = new LinkedHashMap<>();
        if (op.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> e : op.getResponses().entrySet()) {
                responses.put(e.getKey(), toJson(jsonSchema(e.getValue().getContent())));
                String d = e.getValue().getDescription();
                if (d != null && !d.isBlank()) descriptions.put(e.getKey(), d.strip().replaceAll("\\s+", " "));
            }
        }
        List<String> tags = op.getTags() != null ? op.getTags() : List.of();
        String summary = op.getSummary() != null ? op.getSummary()
                : (op.getDescription() != null ? op.getDescription() : "");
        return new ApiOperation(id, m, path, summary, tags, params, requestSchema, responses, descriptions,
                securityHeaders(op, api));
    }

    /**
     * Cabeceras que exigen los esquemas de seguridad aplicables a la operación (v1.6): http (bearer/basic) y
     * oauth2/openIdConnect → Authorization; apiKey en cabecera → su nombre. Se usan para no eliminar esas
     * cabeceras en el filtro de cabeceras no declaradas.
     */
    private static java.util.Set<String> securityHeaders(Operation op, OpenAPI api) {
        java.util.Set<String> out = new java.util.TreeSet<>();
        List<io.swagger.v3.oas.models.security.SecurityRequirement> reqs =
                op.getSecurity() != null ? op.getSecurity() : api.getSecurity();
        if (reqs == null || api.getComponents() == null || api.getComponents().getSecuritySchemes() == null) return out;
        for (io.swagger.v3.oas.models.security.SecurityRequirement req : reqs) {
            for (String name : req.keySet()) {
                io.swagger.v3.oas.models.security.SecurityScheme sc = api.getComponents().getSecuritySchemes().get(name);
                if (sc == null || sc.getType() == null) continue;
                switch (sc.getType()) {
                    case HTTP, OAUTH2, OPENIDCONNECT -> out.add("authorization");
                    case APIKEY -> {
                        if (sc.getIn() == io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER && sc.getName() != null) {
                            out.add(sc.getName().toLowerCase(Locale.ROOT));
                        }
                    }
                    default -> { }
                }
            }
        }
        return out;
    }

    private static Schema<?> jsonSchema(Content content) {
        if (content == null || content.isEmpty()) return null;
        MediaType mt = content.get("application/json");
        if (mt == null) mt = content.values().iterator().next();
        return mt.getSchema();
    }

    private static String toJson(Schema<?> schema) {
        if (schema == null) return "";
        try {
            return Json.mapper().writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el esquema", e);
        }
    }
}
