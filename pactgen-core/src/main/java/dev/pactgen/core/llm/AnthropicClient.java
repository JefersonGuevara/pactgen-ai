package dev.pactgen.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Adaptador para la API Messages de Anthropic, sin SDK (solo java.net.http). */
final class AnthropicClient implements LlmClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final LlmConfig cfg;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    AnthropicClient(LlmConfig cfg) {
        this.cfg = cfg;
        if (cfg.apiKey().isBlank()) {
            throw new IllegalStateException("Define la variable de entorno " + cfg.apiKeyEnv());
        }
    }

    @Override
    public String modelId() { return cfg.model(); }

    @Override
    public LlmResponse complete(LlmRequest req) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", cfg.model());
        body.put("max_tokens", cfg.maxTokens());
        body.put("temperature", cfg.temperature()); // solo temperatura: top_p no se envía (Tabla 3)
        body.put("system", req.system());
        ObjectNode msg = body.putArray("messages").addObject();
        msg.put("role", "user");
        msg.put("content", req.user());

        HttpRequest httpReq = HttpRequest.newBuilder(URI.create(cfg.baseUrl() + "/v1/messages"))
                .timeout(Duration.ofSeconds(120))
                .header("content-type", "application/json")
                .header("x-api-key", cfg.apiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();
        HttpResponse<String> res = Retry.send(http, httpReq);
        JsonNode root = JSON.readTree(res.body());
        StringBuilder text = new StringBuilder();
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText());
        }
        return new LlmResponse(text.toString(), root.path("usage").path("input_tokens").asInt(),
                root.path("usage").path("output_tokens").asInt(), res.body());
    }
}
