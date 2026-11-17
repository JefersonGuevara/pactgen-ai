package dev.pactgen.core.llm;

import java.util.Map;

/** Tarifas publicadas (USD por millón de tokens) para estimar el costo de cada llamada. */
public final class Pricing {

    private record Rate(double in, double out) {}

    // Fuente: documentación de Anthropic, consultada el 8/11/2026. Actualizar si cambia.
    private static final Map<String, Rate> RATES = Map.of(
            "claude-haiku-4-5-20251001", new Rate(1.00, 5.00));

    private Pricing() {}

    public static double costUsd(String model, int inputTokens, int outputTokens) {
        Rate r = RATES.get(model.replace(" (replay)", ""));
        if (r == null) return 0.0; // modelos locales o sin tarifa conocida
        return inputTokens * r.in() / 1_000_000 + outputTokens * r.out() / 1_000_000;
    }
}
