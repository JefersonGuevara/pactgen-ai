package dev.pactgen.core.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Modelo interno normalizado de una especificación OpenAPI (M1). */
public final class ApiModel {

    /** Parámetro de una operación (path, query o header). */
    public record ApiParameter(String name, String in, boolean required, String schemaJson) {}

    /**
     * Operación normalizada.
     *
     * @param responses código de estado (como texto: "200", "404", "default") → esquema JSON del cuerpo, o "" si no tiene
     * @param responseDescriptions código de estado → descripción de la respuesta en el OpenAPI (causa del código)
     */
    public record ApiOperation(String operationId, String method, String path, String summary,
                               List<String> tags, List<ApiParameter> parameters,
                               String requestBodySchema, Map<String, String> responses,
                               Map<String, String> responseDescriptions) {

        public ApiOperation(String operationId, String method, String path, String summary, List<String> tags,
                            List<ApiParameter> parameters, String requestBodySchema, Map<String, String> responses) {
            this(operationId, method, path, summary, tags, parameters, requestBodySchema, responses, Map.of());
        }

        public boolean declaresStatus(int status) {
            return responses.containsKey(String.valueOf(status)) || responses.containsKey("default")
                    || responses.containsKey(String.valueOf(status).charAt(0) + "XX");
        }
    }

    private final String title;
    private final String version;
    private final List<ApiOperation> operations;

    public ApiModel(String title, String version, List<ApiOperation> operations) {
        this.title = title;
        this.version = version;
        this.operations = List.copyOf(operations);
    }

    public String title() { return title; }
    public String version() { return version; }
    public List<ApiOperation> operations() { return operations; }

    public Optional<ApiOperation> find(String operationId) {
        return operations.stream().filter(o -> o.operationId().equals(operationId)).findFirst();
    }
}
