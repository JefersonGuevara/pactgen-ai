package dev.pactgen.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pactgen.core.diff.OpenApiDiff;
import dev.pactgen.core.engine.GenerationEngine;
import dev.pactgen.core.llm.LlmConfig;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.openapi.OpenApiLoader;
import dev.pactgen.core.validation.PactValidator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
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
        @Option(names = "--no-filters", negatable = false, description = "Desactiva todos los filtros deterministas "
                + "(redundantes, cabeceras, cuerpos exactos y reglas guiadas por el esquema). Solo para ablación")
        boolean noFilters;
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
                        maxRepairs, promptVersion, tokenBudget, !noDedupe, !noFilters));
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

    @Command(name = "validate", description = "Comprueba que contratos Pact son coherentes con el OpenAPI, sin proveedor "
            + "en marcha. Salida: 0 sin errores, 1 con errores, 2 si no se pueden leer los archivos.")
    static class Validate implements Callable<Integer> {
        @Option(names = "--openapi", required = true, description = "Ruta al documento OpenAPI 3.x")
        Path openapi;
        @Option(names = "--pact", required = true, description = "Contrato Pact (.json) o carpeta con contratos")
        Path pact;
        @Option(names = "--json", description = "Escribe también el informe en JSON en este archivo")
        Path json;
        @Option(names = "--strict", negatable = false, description = "Los avisos (campos o parámetros no declarados) "
                + "cuentan como errores")
        boolean strict;

        @Override
        public Integer call() {
            PactValidator.Report report;
            try {
                ApiModel api = new OpenApiLoader().load(openapi);
                report = new PactValidator(api, strict).validate(pact);
            } catch (OpenApiLoader.InvalidSpecException | PactValidator.UnreadablePactException e) {
                System.err.println(e.getMessage());
                return 2;
            }
            for (PactValidator.InteractionResult r : report.interactions()) {
                String head = String.format("%-5s %s [%d] %s %s -> %s  %s", r.level(), r.file(), r.index(), r.method(),
                        r.path(), r.status() == null ? "?" : r.status(), r.description());
                System.out.println(head);
                for (String reason : r.reasons()) System.out.println("        - " + reason);
                for (String note : r.notes()) System.out.println("        nota: " + note);
            }
            long sinComprobar = report.interactions().stream().filter(r -> !r.notes().isEmpty()).count();
            System.out.printf("Interacciones: %d | OK: %d | AVISO: %d | ERROR: %d | petición no comprobada (4xx/5xx): %d%n",
                    report.interactions().size(), report.count(PactValidator.Level.OK),
                    report.count(PactValidator.Level.AVISO), report.count(PactValidator.Level.ERROR), sinComprobar);
            if (json != null) {
                try {
                    if (json.toAbsolutePath().getParent() != null) Files.createDirectories(json.toAbsolutePath().getParent());
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(json.toFile(), report);
                } catch (java.io.IOException e) {
                    System.err.println("No se pudo escribir " + json + ": " + e.getMessage());
                    return 2;
                }
            }
            return report.hasErrors() ? 1 : 0;
        }
    }

    @Command(name = "diff", description = "Compara dos versiones de un OpenAPI y clasifica los cambios para el consumidor. "
            + "Salida: 0 sin incompatibles, 1 con alguno, 2 si hay errores de lectura.")
    static class Diff implements Callable<Integer> {
        @Option(names = "--old", required = true, description = "Versión anterior del OpenAPI")
        Path oldSpec;
        @Option(names = "--new", required = true, description = "Versión nueva del OpenAPI")
        Path newSpec;
        @Option(names = "--json", description = "Escribe también el informe en JSON en este archivo")
        Path json;

        @Override
        public Integer call() {
            OpenApiDiff.Report report;
            try {
                OpenApiLoader loader = new OpenApiLoader();
                report = new OpenApiDiff().compare(loader.load(oldSpec), loader.load(newSpec));
            } catch (OpenApiLoader.InvalidSpecException e) {
                System.err.println(e.getMessage());
                return 2;
            }
            for (OpenApiDiff.Change c : report.changes()) {
                System.out.printf("%-12s %s: %s%n", c.kind(), c.operation(), c.message());
            }
            System.out.printf("Cambios: %d | incompatibles: %d | compatibles: %d%n", report.changes().size(),
                    report.count(OpenApiDiff.Kind.INCOMPATIBLE), report.count(OpenApiDiff.Kind.COMPATIBLE));
            if (json != null) {
                try {
                    if (json.toAbsolutePath().getParent() != null) Files.createDirectories(json.toAbsolutePath().getParent());
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(json.toFile(), report);
                } catch (java.io.IOException e) {
                    System.err.println("No se pudo escribir " + json + ": " + e.getMessage());
                    return 2;
                }
            }
            return report.hasBreaking() ? 1 : 0;
        }
    }
}
