package dev.pactgen.maven;

import dev.pactgen.core.diff.OpenApiDiff;
import dev.pactgen.core.openapi.OpenApiLoader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Goal {@code pactgen:diff} (RF-08/RF-09): compara dos versiones de un OpenAPI, como {@code pactgen diff}.
 * Sin fase por defecto; no necesita proyecto:
 * {@code mvn dev.pactgen:pactgen-maven-plugin:0.1.0-SNAPSHOT:diff -Dpactgen.old=v1.yaml -Dpactgen.new=v2.yaml}.
 */
@Mojo(name = "diff", requiresProject = false, threadSafe = true)
public class DiffMojo extends AbstractMojo {

    @Parameter(property = "pactgen.old", required = true)
    private File oldSpec;
    @Parameter(property = "pactgen.new", required = true)
    private File newSpec;
    @Parameter(property = "pactgen.json")
    private File json;
    /** Falla el build si hay cambios incompatibles (la CLI devuelve 1). */
    @Parameter(property = "pactgen.failOnBreaking", defaultValue = "true")
    private boolean failOnBreaking;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        OpenApiDiff.Report report;
        try {
            OpenApiLoader loader = new OpenApiLoader();
            report = new OpenApiDiff().compare(loader.load(oldSpec.toPath()), loader.load(newSpec.toPath()));
        } catch (OpenApiLoader.InvalidSpecException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        for (OpenApiDiff.Change c : report.changes()) {
            String line = String.format("%-12s %s: %s", c.kind(), c.operation(), c.message());
            if (c.kind() == OpenApiDiff.Kind.INCOMPATIBLE) getLog().warn(line); else getLog().info(line);
        }
        getLog().info(String.format("Cambios: %d | incompatibles: %d | compatibles: %d", report.changes().size(),
                report.count(OpenApiDiff.Kind.INCOMPATIBLE), report.count(OpenApiDiff.Kind.COMPATIBLE)));
        if (json != null) ValidateMojo.write(json, report);
        if (report.hasBreaking() && failOnBreaking) {
            throw new MojoFailureException("pactgen:diff: " + report.count(OpenApiDiff.Kind.INCOMPATIBLE)
                    + " cambios incompatibles");
        }
    }
}
