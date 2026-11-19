package dev.pactgen.cli;

import dev.pactgen.core.engine.GenerationEngine;
import dev.pactgen.core.llm.LlmConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

/** M5 — CLI de PactGen-AI (RF-08). Códigos de salida: 0 ok, 1 generación incompleta, 2 entrada inválida. */
@Command(name = "pactgen", mixinStandardHelpOptions = true, version = "pactgen-ai 0.1.0-SNAPSHOT",
        description = "Genera pruebas de contrato Pact V4 desde OpenAPI 3.x mediante LLMs.",
        subcommands = {PactGenCli.Generate.class, PactGenCli.Validate.class, PactGenCli.Diff.class})
public class PactGenCli implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new PactGenCli()).execute(args));
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    @Command(name = "generate", description = "Genera pruebas de consumidor y contratos desde un OpenAPI.")
    static class Generate implements Callable<Integer> {
        @Option(names = "--openapi", required = true, description = "Ruta al documento OpenAPI 3.x")
        Path openapi;
        @Option(names = "--consumer", required = true) String consumer;
        @Option(names = "--provider", required = true) String provider;
        @Option(names = "--include", split = ",", description = "operationIds a procesar (por defecto, todos)")
        List<String> include = List.of();
        @Option(names = "--out", defaultValue = ".", description = "Proyecto consumidor donde escribir src/test/java")
        Path out;
        @Option(names = "--package", defaultValue = "pactgen.generated") String testPackage;
        @Option(names = "--llm-provider", defaultValue = "anthropic", description = "anthropic | openai | replay")
        String llmProvider;
        @Option(names = "--model", defaultValue = "claude-haiku-4-5-20251001") String model;
        @Option(names = "--temperature", defaultValue = "0.2") double temperature;
        @Option(names = "--max-tokens", defaultValue = "4096") int maxTokens;
        @Option(names = "--base-url", description = "Por defecto https://api.anthropic.com; Ollama: http://localhost:11434")
        String baseUrl;
        @Option(names = "--api-key-env", defaultValue = "ANTHROPIC_API_KEY") String apiKeyEnv;
        @Option(names = "--replay", description = "Directorio con respuestas archivadas (modo replay)")
        Path replay;
        @Option(names = "--max-repairs", defaultValue = "2") int maxRepairs;
        @Option(names = "--prompt-version", defaultValue = "v1.1") String promptVersion;
        @Option(names = "--archive", description = "Copia el manifiesto, las respuestas crudas y las pruebas "
                + "generadas a <dir>/<fecha>_<proveedor>_<modelo>/ junto con un NOTAS.md (bitácora)")
        Path archive;
        @Option(names = "--no-dedupe", negatable = false, description = "Desactiva el filtro de interacciones redundantes "
                + "(solo para comparar; por defecto activo)")
        boolean noDedupe;
        @Option(names = "--token-budget", defaultValue = "6000") int tokenBudget;

        @Override
        public Integer call() throws Exception {
            String provider = replay != null ? "replay" : llmProvider;
            String url = baseUrl != null ? baseUrl
                    : (provider.equals("openai") ? "http://localhost:11434" : "https://api.anthropic.com");
            LlmConfig llm = new LlmConfig(provider, model, temperature, maxTokens, url, apiKeyEnv, replay);
            try {
                GenerationEngine.Summary s = new GenerationEngine().run(new GenerationEngine.Request(
                        openapi, consumer, this.provider, new HashSet<>(include), out, testPackage, llm,
                        maxRepairs, promptVersion, tokenBudget, !noDedupe));
                String costo = provider.equals("replay")
                        ? String.format("costo original: USD %.4f (replay: USD 0 gastados)", s.costUsd())
                        : String.format("costo estimado: USD %.4f", s.costUsd());
                System.out.printf("Operaciones: %d | generadas: %d | interacciones: %d (redundantes descartadas: %d) | %s%nManifiesto: %s%n",
                        s.operations(), s.generated(), s.interactions(), s.duplicatesRemoved(), costo, s.manifest());
                if (archive != null) {
                    Path dir = RunArchiver.archive(archive, s, this.provider, llm.model(), promptVersion);
                    System.out.println("Archivado en: " + dir);
                }
                return s.generated() == s.operations() ? 0 : 1;
            } catch (dev.pactgen.core.openapi.OpenApiLoader.InvalidSpecException e) {
                System.err.println(e.getMessage());
                return 2;
            }
        }
    }

    @Command(name = "validate", description = "Valida contratos Pact contra el esquema V4 y el OpenAPI (iteración I3).")
    static class Validate implements Callable<Integer> {
        @Override
        public Integer call() {
            System.err.println("validate: pendiente de implementar en la iteración I3");
            return 3;
        }
    }

    @Command(name = "diff", description = "Compara dos versiones de un OpenAPI y señala contratos afectados (iteración I3).")
    static class Diff implements Callable<Integer> {
        @Override
        public Integer call() {
            System.err.println("diff: pendiente de implementar en la iteración I3");
            return 3;
        }
    }
}
