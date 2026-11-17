package dev.pactgen.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Modo replay (RF-12): devuelve las respuestas archivadas en {@code <dir>/<key>.json}
 * sin invocar ningún modelo. Permite reproducir el análisis aunque el modelo se retire.
 */
final class ReplayClient implements LlmClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path dir;
    private final String model;

    ReplayClient(Path dir, String model) {
        if (dir == null) throw new IllegalArgumentException("El modo replay necesita --replay <directorio>");
        this.dir = dir;
        this.model = model;
    }

    @Override
    public String modelId() { return model + " (replay)"; }

    @Override
    public LlmResponse complete(LlmRequest req) throws Exception {
        Path f = dir.resolve(req.key() + ".json");
        if (!Files.exists(f)) throw new IllegalStateException("No hay respuesta archivada para " + req.key());
        JsonNode n = JSON.readTree(f.toFile());
        return new LlmResponse(n.path("text").asText(), n.path("inputTokens").asInt(),
                n.path("outputTokens").asInt(), n.path("raw").asText(""));
    }
}
