package dev.pactgen.core.llm;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Reintentos con espera exponencial ante errores de red, 429 y 5xx (flujo alternativo 4b de CU-01). */
final class Retry {

    private static final int MAX_ATTEMPTS = 4;

    private Retry() {}

    static HttpResponse<String> send(HttpClient http, HttpRequest req) throws IOException, InterruptedException {
        long wait = 2000;
        for (int attempt = 1; ; attempt++) {
            try {
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                int s = res.statusCode();
                if (s == 429 || s >= 500) {
                    if (attempt >= MAX_ATTEMPTS) throw new IOException("HTTP " + s + ": " + res.body());
                } else if (s >= 400) {
                    throw new IOException("HTTP " + s + ": " + res.body());
                } else {
                    return res;
                }
            } catch (IOException e) {
                if (attempt >= MAX_ATTEMPTS || e.getMessage() != null && e.getMessage().startsWith("HTTP 4")) throw e;
                System.err.println("[llm] reintento " + attempt + ": " + e.getMessage());
            }
            Thread.sleep(wait);
            wait *= 2;
        }
    }
}
