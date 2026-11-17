package dev.pactgen.core;

import dev.pactgen.core.engine.GenerationEngine;
import dev.pactgen.core.llm.LlmConfig;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.openapi.OpenApiLoader;
import dev.pactgen.core.validation.OutputValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreTest {

    private static final Path SPEC = Path.of("../validation/microservices/customers-service/openapi.yaml");
    private static final Path REPLAY = Path.of("src/test/resources/fixtures/replay");

    @Test
    void loadsCustomersServiceWithEightOperations() {
        ApiModel api = new OpenApiLoader().load(SPEC);
        assertEquals(8, api.operations().size());
        ApiOperation op = api.find("findOwner").orElseThrow();
        assertEquals("GET", op.method());
        assertEquals("/owners/{ownerId}", op.path());
        assertTrue(op.responses().get("200").contains("firstName"), "el $ref debe estar resuelto");
    }

    @Test
    void rejectsUndeclaredStatusAndWrongPath() {
        ApiOperation op = new OpenApiLoader().load(SPEC).find("findOwner").orElseThrow();
        String bad = """
                {"analysis":"x","interactions":[{"description":"d","providerState":"s",
                 "request":{"method":"GET","path":"/pets/1"},"response":{"status":404}}]}""";
        OutputValidator.Result r = new OutputValidator().validate(bad, op);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("no corresponde a la plantilla")));
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("404 no está declarado")));
    }

    @Test
    void generatesTestInReplayMode(@TempDir Path out) throws Exception {
        LlmConfig llm = new LlmConfig("replay", "claude-haiku-4-5-20251001", 0.2, 4096, "", "", REPLAY);
        GenerationEngine.Summary s = new GenerationEngine().run(new GenerationEngine.Request(
                SPEC, "api-gateway", "customers-service", Set.of("findOwner"), out, "pactgen.generated",
                llm, 2, "v1", 6000));
        assertEquals(1, s.generated());
        String src = Files.readString(out.resolve("src/test/java/pactgen/generated/FindOwnerPactTest.java"));
        assertTrue(src.contains(".given(\"existe el propietario con id 1 y una mascota\")"));
        assertTrue(src.contains("eachLike(\"pets\")"));
        assertTrue(Files.exists(s.manifest()));
    }

    @Test
    void loadsPromptV11WithNewRules() {
        dev.pactgen.core.prompt.PromptTemplate v11 = dev.pactgen.core.prompt.PromptTemplate.load("v1.1");
        dev.pactgen.core.prompt.PromptTemplate v1 = dev.pactgen.core.prompt.PromptTemplate.load("v1");
        assertTrue(v11.system().contains("comportamiento distinto"));
        assertFalse(v11.sha256().equals(v1.sha256()), "cada versión del prompt tiene su propio hash");
    }
}
