package dev.pactgen.core.llm;

/** Puerto del LLM Engine (M3). Los adaptadores implementan un proveedor concreto (RF-04). */
public interface LlmClient {

    /**
     * @param key identificador estable de la llamada (operación + intento), usado para archivar y reproducir
     */
    record LlmRequest(String key, String system, String user) {}

    record LlmResponse(String text, int inputTokens, int outputTokens, String rawJson) {}

    LlmResponse complete(LlmRequest request) throws Exception;

    /** Identificador exacto del modelo, tal como se registra en el manifiesto. */
    String modelId();
}
