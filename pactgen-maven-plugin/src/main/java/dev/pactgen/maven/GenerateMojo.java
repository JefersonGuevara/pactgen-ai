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

/** Goal {@code pactgen:generate} (RF-09): genera las pruebas de consumidor antes de compilar los tests. */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(property = "pactgen.openapi", required = true)
    private File openapi;
    @Parameter(property = "pactgen.consumer", required = true)
    private String consumer;
    @Parameter(property = "pactgen.provider", required = true)
    private String provider;
    @Parameter
    private List<String> include = List.of();
    @Parameter(defaultValue = "pactgen.generated")
    private String testPackage;
    @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/pactgen")
    private File outputDirectory;
    @Parameter(property = "pactgen.llmProvider", defaultValue = "anthropic")
    private String llmProvider;
    @Parameter(property = "pactgen.model", defaultValue = "claude-haiku-4-5-20251001")
    private String model;
    @Parameter(defaultValue = "0.2")
    private double temperature;
    @Parameter(defaultValue = "4096")
    private int maxTokens;
    @Parameter(property = "pactgen.baseUrl", defaultValue = "https://api.anthropic.com")
    private String baseUrl;
    @Parameter(defaultValue = "ANTHROPIC_API_KEY")
    private String apiKeyEnv;
    @Parameter(property = "pactgen.replay")
    private File replay;
    @Parameter(defaultValue = "2")
    private int maxRepairs;
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
        String p = replay != null ? "replay" : llmProvider;
        LlmConfig llm = new LlmConfig(p, model, temperature, maxTokens, baseUrl, apiKeyEnv,
                replay != null ? replay.toPath() : null);
        try {
            GenerationEngine.Summary s = new GenerationEngine().run(new GenerationEngine.Request(
                    openapi.toPath(), consumer, provider, new HashSet<>(include), outputDirectory.toPath(),
                    testPackage, llm, maxRepairs, "v1.1", 6000));
            getLog().info(String.format("PactGen-AI: %d/%d operaciones generadas, costo estimado USD %.4f",
                    s.generated(), s.operations(), s.costUsd()));
            project.addTestCompileSourceRoot(new File(outputDirectory, "src/test/java").getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Error en PactGen-AI: " + e.getMessage(), e);
        }
    }
}
