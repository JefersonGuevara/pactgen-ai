package dev.pactgen.core.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pactgen.core.context.ContextBuilder;
import dev.pactgen.core.context.ContextBuilder.PromptContext;
import dev.pactgen.core.generator.PactTestRenderer;
import dev.pactgen.core.llm.LlmClient;
import dev.pactgen.core.llm.LlmClient.LlmRequest;
import dev.pactgen.core.llm.LlmClient.LlmResponse;
import dev.pactgen.core.llm.LlmConfig;
import dev.pactgen.core.llm.Pricing;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.openapi.OpenApiLoader;
import dev.pactgen.core.prompt.PromptTemplate;
import dev.pactgen.core.validation.HeaderFilter;
import dev.pactgen.core.validation.InteractionDeduplicator;
import dev.pactgen.core.validation.OutputValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orquesta CU-01: OpenAPI → contexto → LLM → validación/reparación → prueba JUnit 5 + manifiesto.
 * Cada operación se procesa de forma independiente; un fallo no detiene a las demás.
 */
public final class GenerationEngine {

    /** Parámetros de una ejecución de {@code generate}. */
    public record Request(Path openapi, String consumer, String provider, Set<String> include,
                          Path outDir, String testPackage, LlmConfig llm, int maxRepairs,
                          String promptVersion, int tokenBudget, boolean dedupe) {}

    public record OperationResult(String operationId, String status, int attempts, int interactions,
                                  int inputTokens, int outputTokens, double costUsd, List<String> lastErrors,
                                  int duplicatesRemoved, int headersRemoved) {}

    /** Resultado agregado; incluye los archivos escritos en esta ejecución para poder archivarlos. */
    public record Summary(int operations, int generated, double costUsd, Path manifest,
                          List<Path> rawFiles, List<Path> testFiles, int interactions, int duplicatesRemoved) {}

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public Summary run(Request r) throws IOException {
        ApiModel api = new OpenApiLoader().load(r.openapi());
        PromptTemplate prompt = PromptTemplate.load(r.promptVersion());
        LlmClient llm = r.llm().createClient();
        ContextBuilder contexts = new ContextBuilder(r.tokenBudget());
        OutputValidator validator = new OutputValidator();
        PactTestRenderer renderer = new PactTestRenderer();

        Path rawDir = r.outDir().resolve("pactgen/raw");
        Path testDir = r.outDir().resolve("src/test/java").resolve(r.testPackage().replace('.', '/'));
        Files.createDirectories(rawDir);
        Files.createDirectories(testDir);

        Instant started = Instant.now();
        List<OperationResult> results = new ArrayList<>();
        List<Path> rawFiles = new ArrayList<>();
        List<Path> testFiles = new ArrayList<>();
        for (ApiOperation op : api.operations()) {
            if (!r.include().isEmpty() && !r.include().contains(op.operationId())) continue;
            results.add(generate(op, r, prompt, llm, contexts, validator, renderer, rawDir, testDir,
                    rawFiles, testFiles));
        }

        Path manifest = writeManifest(r, api, prompt, llm, started, results);
        int ok = (int) results.stream().filter(x -> x.status().equals("generated")).count();
        double cost = results.stream().mapToDouble(OperationResult::costUsd).sum();
        return new Summary(results.size(), ok, cost, manifest, rawFiles, testFiles,
                results.stream().mapToInt(OperationResult::interactions).sum(),
                results.stream().mapToInt(OperationResult::duplicatesRemoved).sum());
    }

    private OperationResult generate(ApiOperation op, Request r, PromptTemplate prompt, LlmClient llm,
                                     ContextBuilder contexts, OutputValidator validator, PactTestRenderer renderer,
                                     Path rawDir, Path testDir, List<Path> rawFiles,
                                     List<Path> testFiles) throws IOException {
        PromptContext ctx = contexts.build(op, r.consumer(), r.provider());
        String user = prompt.user(ctx.variables());
        int in = 0, out = 0, attempt = 0;
        List<String> errors = List.of();
        OutputValidator.Result result = null;
        String lastText = "";
        while (attempt <= r.maxRepairs()) {
            String key = op.operationId() + "-" + attempt;
            String message = attempt == 0 ? user : user + "\n\n" + prompt.repair(lastText, String.join("\n", errors));
            LlmResponse resp;
            try {
                resp = llm.complete(new LlmRequest(key, prompt.system(), message));
            } catch (Exception e) {
                System.err.println("[engine] " + op.operationId() + ": error al invocar el modelo: " + e.getMessage());
                return new OperationResult(op.operationId(), "llm_error", attempt + 1, 0, in, out,
                        Pricing.costUsd(llm.modelId(), in, out), List.of(String.valueOf(e.getMessage())), 0, 0);
            }
            in += resp.inputTokens();
            out += resp.outputTokens();
            lastText = resp.text();
            Path rawFile = rawDir.resolve(key + ".json");
            archive(rawFile, resp);
            rawFiles.add(rawFile);
            result = validator.validate(resp.text(), op);
            errors = result.errors();
            attempt++;
            if (result.valid()) break;
            System.err.println("[engine] " + op.operationId() + ": respuesta inválida (intento " + attempt + "): " + errors);
        }
        double cost = Pricing.costUsd(llm.modelId(), in, out);
        if (result == null || !result.valid()) {
            return new OperationResult(op.operationId(), "invalid", attempt, 0, in, out, cost, errors, 0, 0);
        }
        List<JsonNode> interactions = new ArrayList<>();
        result.output().path("interactions").forEach(interactions::add);
        int headersRemoved = new HeaderFilter().filter(interactions, op);
        int removed = 0;
        if (r.dedupe()) {
            InteractionDeduplicator.Result d = new InteractionDeduplicator().dedupe(interactions);
            interactions = d.kept();
            removed = d.removed();
        }
        PactTestRenderer.Rendered test = renderer.render(r.testPackage(), r.consumer(), r.provider(), op, interactions);
        Path testFile = testDir.resolve(test.className() + ".java");
        Files.writeString(testFile, test.source());
        testFiles.add(testFile);
        return new OperationResult(op.operationId(), "generated", attempt, interactions.size(), in, out, cost,
                List.of(), removed, headersRemoved);
    }

    private static void archive(Path file, LlmResponse resp) throws IOException {
        ObjectNode n = JSON.createObjectNode();
        n.put("text", resp.text());
        n.put("inputTokens", resp.inputTokens());
        n.put("outputTokens", resp.outputTokens());
        n.put("raw", resp.rawJson());
        JSON.writeValue(file.toFile(), n);
    }

    private Path writeManifest(Request r, ApiModel api, PromptTemplate prompt, LlmClient llm,
                               Instant started, List<OperationResult> results) throws IOException {
        ObjectNode m = JSON.createObjectNode();
        m.put("tool", "pactgen-ai 0.1.0-SNAPSHOT (v1.5)");
        m.put("startedAt", started.toString());
        m.put("finishedAt", Instant.now().toString());
        m.put("openapi", r.openapi().toString());
        m.put("openapiSha256", PromptTemplate.sha256(Files.readString(r.openapi())));
        m.put("apiTitle", api.title());
        m.put("apiVersion", api.version());
        m.put("consumer", r.consumer());
        m.put("provider", r.provider());
        ObjectNode model = m.putObject("model");
        model.put("provider", r.llm().provider());
        model.put("id", llm.modelId());
        model.put("temperature", r.llm().temperature());
        model.put("maxTokens", r.llm().maxTokens());
        model.put("maxRepairs", r.maxRepairs());
        m.put("dedupe", r.dedupe());
        m.put("promptVersion", prompt.version());
        m.put("promptSha256", prompt.sha256());
        ArrayNode ops = m.putArray("operations");
        for (OperationResult o : results) ops.addPOJO(o);
        m.put("totalCostUsd", results.stream().mapToDouble(OperationResult::costUsd).sum());
        Path file = r.outDir().resolve("pactgen/manifest-" + started.toString().replace(':', '-') + ".json");
        JSON.writeValue(file.toFile(), m);
        return file;
    }
}
