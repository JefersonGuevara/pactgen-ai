package dev.pactgen.core.llm;

import java.nio.file.Path;

/**
 * Configuración fija del modelo (Tabla 3 de la memoria).
 *
 * @param provider    "anthropic", "openai" (cualquier API compatible, incluido Ollama) o "replay"
 * @param baseUrl     URL base; para Ollama: http://localhost:11434
 * @param apiKeyEnv   nombre de la variable de entorno con la clave (nunca la clave en sí)
 * @param replayDir   directorio con respuestas archivadas (solo para "replay")
 */
public record LlmConfig(String provider, String model, double temperature, int maxTokens,
                        String baseUrl, String apiKeyEnv, Path replayDir) {

    public static LlmConfig defaults() {
        return new LlmConfig("anthropic", "claude-haiku-4-5-20251001", 0.2, 4096,
                "https://api.anthropic.com", "ANTHROPIC_API_KEY", null);
    }

    public LlmClient createClient() {
        return switch (provider) {
            case "anthropic" -> new AnthropicClient(this);
            case "openai" -> new OpenAiCompatibleClient(this);
            case "replay" -> new ReplayClient(replayDir, model);
            default -> throw new IllegalArgumentException("Proveedor desconocido: " + provider);
        };
    }

    String apiKey() {
        if (apiKeyEnv == null || apiKeyEnv.isBlank()) return "";
        String k = System.getenv(apiKeyEnv);
        return k == null ? "" : k;
    }
}
