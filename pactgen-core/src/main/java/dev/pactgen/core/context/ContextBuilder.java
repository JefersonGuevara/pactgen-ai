package dev.pactgen.core.context;

import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.model.ApiModel.ApiParameter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * M2 — Convierte una operación en las variables del prompt (RF-03).
 * Solo incluye lo útil para generar interacciones; servidores y esquemas de seguridad
 * nunca llegan aquí porque el modelo interno no los conserva (RNF-05).
 */
public final class ContextBuilder {

    /** Contexto listo para el prompt, con una estimación de tokens (≈ 4 caracteres por token). */
    public record PromptContext(ApiOperation operation, Map<String, String> variables, int estimatedTokens) {}

    private final int tokenBudget;

    public ContextBuilder(int tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    public PromptContext build(ApiOperation op, String consumer, String provider) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("consumer", consumer);
        v.put("provider", provider);
        v.put("operationId", op.operationId());
        v.put("method", op.method());
        v.put("path", op.path());
        v.put("summary", op.summary().isBlank() ? "(sin descripción)" : op.summary());
        v.put("parameters", op.parameters().isEmpty() ? "(ninguno)"
                : op.parameters().stream().map(ContextBuilder::param).collect(Collectors.joining("\n")));
        v.put("requestSchema", op.requestBodySchema().isBlank() ? "(sin cuerpo)" : op.requestBodySchema());
        v.put("responses", op.responses().entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + (e.getValue().isBlank() ? "(sin cuerpo)" : e.getValue()))
                .collect(Collectors.joining("\n")));
        int chars = v.values().stream().mapToInt(String::length).sum();
        int tokens = chars / 4;
        if (tokens > tokenBudget) {
            System.err.printf("[context] %s: contexto estimado de %d tokens supera el presupuesto de %d%n",
                    op.operationId(), tokens, tokenBudget);
        }
        return new PromptContext(op, v, tokens);
    }

    private static String param(ApiParameter p) {
        return "- " + p.name() + " (" + p.in() + (p.required() ? ", obligatorio" : ", opcional") + "): "
                + (p.schemaJson().isBlank() ? "{}" : p.schemaJson());
    }
}
