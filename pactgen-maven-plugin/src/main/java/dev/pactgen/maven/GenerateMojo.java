package dev.pactgen.maven;

import dev.pactgen.core.engine.GenerationEngine;
import dev.pactgen.core.llm.LlmConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Goal {@code pactgen:generate} (RF-09): genera las pruebas de consumidor antes de compilar los tests.
 *
 * <p>Las opciones y sus valores por defecto son los de {@code pactgen generate}; el goal delega en
 * {@link GenerationEngine} sin lógica propia. Falla el build si alguna operación no se genera (la CLI devuelve 1).
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(property = "pactgen.openapi", required = true)
    private File openapi;
    @Parameter(property = "pactgen.consumer", required = true)
    private String consumer;
    @Parameter(property = "pactgen.provider", required = true)
    private String provider;
    /** operationIds a procesar (por defecto, todos). */
    @Parameter(property = "pactgen.include")
    private List<String> include = List.of();
    /** Proyecto donde escribir src/test/java/&lt;paquete&gt; y pactgen/ (manifiesto y respuestas crudas). */
    @Parameter(property = "pactgen.outDir", defaultValue = "${project.build.directory}/generated-test-sources/pactgen")
    private File outDir;
    @Parameter(property = "pactgen.testPackage", defaultValue = "pactgen.generated")
    private String testPackage;
    /** anthropic | openai | replay (con {@code replay}, siempre replay). */
    @Parameter(property = "pactgen.llmProvider", defaultValue = "anthropic")
    private String llmProvider;
    @Parameter(property = "pactgen.model", defaultValue = "claude-haiku-4-5-20251001")
    private String model;
    @Parameter(property = "pactgen.temperature", defaultValue = "0.2")
    private double temperature;
    @Parameter(property = "pactgen.maxTokens", defaultValue = "4096")
    private int maxTokens;
    /** Por defecto, como la CLI: https://api.anthropic.com, o http://localhost:11434 con llmProvider=openai. */
    @Parameter(property = "pactgen.baseUrl")
    private String baseUrl;
    @Parameter(property = "pactgen.apiKeyEnv", defaultValue = "ANTHROPIC_API_KEY")
    private String apiKeyEnv;
    /** Directorio con respuestas archivadas (raw/): regenera sin llamar al modelo y sin costo. */
    @Parameter(property = "pactgen.replay")
    private File replay;
    @Parameter(property = "pactgen.maxRepairs", defaultValue = "2")
    private int maxRepairs;
    @Parameter(property = "pactgen.promptVersion", defaultValue = "v1.1")
    private String promptVersion;
    @Parameter(property = "pactgen.tokenBudget", defaultValue = "6000")
    private int tokenBudget;
    /** Desactiva el filtro de interacciones redundantes (solo para comparar). */
    @Parameter(property = "pactgen.noDedupe", defaultValue = "false")
    private boolean noDedupe;
    /** Desactiva todos los filtros deterministas (solo para ablación). */
    @Parameter(property = "pactgen.noFilters", defaultValue = "false")
    private boolean noFilters;
    @Parameter(property = "pactgen.skip", defaultValue = "false")
    private boolean skip;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("PactGen-AI omitido (pactgen.skip=true)");
            return;
        }
        // Mismos valores efectivos que PactGenCli.Generate
        String p = replay != null ? "replay" : llmProvider;
        String url = baseUrl != null ? baseUrl
                : (p.equals("openai") ? "http://localhost:11434" : "https://api.anthropic.com");
        LlmConfig llm = new LlmConfig(p, model, temperature, maxTokens, url, apiKeyEnv,
                replay != null ? replay.toPath() : null);
        GenerationEngine.Summary s;
        try {
            s = new GenerationEngine().run(new GenerationEngine.Request(
                    openapi.toPath(), consumer, provider, new HashSet<>(include), outDir.toPath(), testPackage, llm,
                    maxRepairs, promptVersion, tokenBudget, !noDedupe, !noFilters));
        } catch (Exception e) {
            throw new MojoExecutionException("Error en PactGen-AI: " + e.getMessage(), e);
        }
        String costo = p.equals("replay")
                ? String.format("costo original: USD %.4f (replay: USD 0 gastados)", s.costUsd())
                : String.format("costo estimado: USD %.4f", s.costUsd());
        getLog().info(String.format("PactGen-AI: operaciones %d | generadas %d | interacciones %d (redundantes descartadas: %d) | %s",
                s.operations(), s.generated(), s.interactions(), s.duplicatesRemoved(), costo));
        getLog().info("Manifiesto: " + s.manifest());
        project.addTestCompileSourceRoot(new File(outDir, "src/test/java").getAbsolutePath());
        if (s.generated() != s.operations()) {
            throw new MojoFailureException(String.format("PactGen-AI: solo se generaron %d de %d operaciones (ver %s)",
                    s.generated(), s.operations(), s.manifest()));
        }
    }
}
