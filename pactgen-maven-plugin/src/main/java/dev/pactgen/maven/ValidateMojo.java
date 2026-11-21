package dev.pactgen.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pactgen.core.openapi.OpenApiLoader;
import dev.pactgen.core.validation.PactValidator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Goal {@code pactgen:validate} (RF-08/RF-09): comprueba los contratos Pact frente al OpenAPI, como
 * {@code pactgen validate}. Por defecto en la fase verify, cuando {@code mvn test} ya escribió target/pacts.
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ValidateMojo extends AbstractMojo {

    @Parameter(property = "pactgen.openapi", required = true)
    private File openapi;
    /** Contrato (.json) o carpeta con contratos. */
    @Parameter(property = "pactgen.pact", defaultValue = "${project.build.directory}/pacts")
    private File pact;
    @Parameter(property = "pactgen.json")
    private File json;
    /** Los avisos (campos o parámetros no declarados) cuentan como errores. */
    @Parameter(property = "pactgen.strict", defaultValue = "false")
    private boolean strict;
    /** Falla el build si hay errores (la CLI devuelve 1). */
    @Parameter(property = "pactgen.failOnError", defaultValue = "true")
    private boolean failOnError;
    @Parameter(property = "pactgen.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("PactGen-AI validate omitido (pactgen.skip=true)");
            return;
        }
        PactValidator.Report report;
        try {
            report = new PactValidator(new OpenApiLoader().load(openapi.toPath()), strict).validate(pact.toPath());
        } catch (OpenApiLoader.InvalidSpecException | PactValidator.UnreadablePactException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        for (PactValidator.InteractionResult r : report.interactions()) {
            String head = String.format("%-5s %s [%d] %s %s -> %s  %s", r.level(), r.file(), r.index(), r.method(),
                    r.path(), r.status() == null ? "?" : r.status(), r.description());
            if (r.level() == PactValidator.Level.ERROR) getLog().error(head);
            else if (r.level() == PactValidator.Level.AVISO) getLog().warn(head);
            else getLog().info(head);
            for (String reason : r.reasons()) getLog().info("        - " + reason);
            for (String note : r.notes()) getLog().info("        nota: " + note);
        }
        long sinComprobar = report.interactions().stream().filter(r -> !r.notes().isEmpty()).count();
        getLog().info(String.format("Interacciones: %d | OK: %d | AVISO: %d | ERROR: %d | petición no comprobada (4xx/5xx): %d",
                report.interactions().size(), report.count(PactValidator.Level.OK),
                report.count(PactValidator.Level.AVISO), report.count(PactValidator.Level.ERROR), sinComprobar));
        if (json != null) write(json, report);
        if (report.hasErrors() && failOnError) {
            throw new MojoFailureException("pactgen:validate: " + report.count(PactValidator.Level.ERROR)
                    + " interacciones con errores");
        }
    }

    static void write(File json, Object report) throws MojoExecutionException {
        try {
            if (json.getAbsoluteFile().getParentFile() != null) Files.createDirectories(json.getAbsoluteFile().getParentFile().toPath());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(json, report);
        } catch (IOException e) {
            throw new MojoExecutionException("No se pudo escribir " + json + ": " + e.getMessage(), e);
        }
    }
}
