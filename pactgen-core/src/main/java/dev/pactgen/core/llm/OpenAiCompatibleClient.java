package dev.pactgen.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Adaptador para APIs compatibles con /v1/chat/completions de OpenAI.
 * Sirve para Ollama en local (baseUrl http://localhost:11434, sin clave): cumple RNF-02.
 */
final class OpenAiCompatibleClient implements LlmClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final LlmConfig cfg;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    OpenAiCompatibleClient(LlmConfig cfg) { this.cfg = cfg; }

    @Override
    public String modelId() { return cfg.model(); }

    @Override
    public LlmResponse complete(LlmRequest req) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", cfg.model());
        body.put("temperature", cfg.temperature());
        body.put("max_tokens", cfg.maxTokens());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", req.system());
        messages.addObject().put("role", "user").put("content", req.user());

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(cfg.baseUrl() + "/v1/chat/completions"))
                .timeout(Duration.ofSeconds(300))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        if (!cfg.apiKey().isBlank()) b.header("Authorization", "Bearer " + cfg.apiKey());
        HttpResponse<String> res = Retry.send(http, b.build());
        JsonNode root = JSON.readTree(res.body());
        return new LlmResponse(root.path("choices").path(0).path("message").path("content").asText(),
                root.path("usage").path("prompt_tokens").asInt(),
                root.path("usage").path("completion_tokens").asInt(), res.body());
    }
}
