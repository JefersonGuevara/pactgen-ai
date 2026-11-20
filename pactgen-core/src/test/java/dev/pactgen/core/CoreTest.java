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
                llm, 2, "v1", 6000, true));
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

    @Test
    void dedupeRemovesInteractionsThatOnlyChangeValues() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<com.fasterxml.jackson.databind.JsonNode> its = java.util.List.of(
                m.readTree("""
                        {"providerState":"existe 1","request":{"method":"PUT","path":"/owners/1/pets/5","body":{"name":"Leo","birthDate":"2020-01-01"}},"response":{"status":204}}"""),
                m.readTree("""
                        {"providerState":"existe 2","request":{"method":"PUT","path":"/owners/1/pets/6","body":{"name":"Max","birthDate":"2021-05-05"}},"response":{"status":204}}"""),
                m.readTree("""
                        {"providerState":"no existe","request":{"method":"PUT","path":"/owners/999/pets/5","body":{"name":"Leo","birthDate":"2020-01-01"}},"response":{"status":404}}"""),
                m.readTree("""
                        {"providerState":"lista vacia","request":{"method":"GET","path":"/owners"},"response":{"status":200,"body":[]}}"""),
                m.readTree("""
                        {"providerState":"con datos","request":{"method":"GET","path":"/owners"},"response":{"status":200,"body":[{"id":1}]}}"""));
        dev.pactgen.core.validation.InteractionDeduplicator.Result r =
                new dev.pactgen.core.validation.InteractionDeduplicator().dedupe(its);
        assertEquals(1, r.removed(), "solo el segundo 204 es redundante");
        assertEquals(4, r.kept().size(), "la lista vacía y la lista con datos tienen formas distintas");
    }

    @Test
    void headerFilterRemovesUndeclaredRequestHeaders() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        ApiOperation op = new OpenApiLoader().load(SPEC).operations().stream()
                .filter(o -> o.operationId().equals("findOwner")).findFirst().orElseThrow();
        java.util.List<com.fasterxml.jackson.databind.JsonNode> its = java.util.List.of(m.readTree("""
                {"request":{"method":"GET","path":"/owners/1","headers":{"Authorization":"Bearer x","Accept":"application/json"}},"response":{"status":200}}"""));
        int removed = new dev.pactgen.core.validation.HeaderFilter().filter(its, op);
        assertEquals(1, removed);
        assertFalse(its.get(0).path("request").path("headers").has("Authorization"));
        assertTrue(its.get(0).path("request").path("headers").has("Accept"));
    }

    @Test
    void errorInteractionsUseExactRequestBody() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        ApiOperation op = new OpenApiLoader().load(SPEC).operations().stream()
                .filter(o -> o.operationId().equals("createOwner")).findFirst().orElseThrow();
        java.util.List<com.fasterxml.jackson.databind.JsonNode> its = java.util.List.of(m.readTree("""
                {"description":"sin nombre","providerState":"s","request":{"method":"POST","path":"/owners","body":{"firstName":""}},"response":{"status":400}}"""));
        String src = new dev.pactgen.core.generator.PactTestRenderer()
                .render("pactgen.generated", "api-gateway", "customers-service", op, its).source();
        assertTrue(src.contains(".body(\"{\\\"firstName\\\":\\\"\\\"}\", \"application/json\")"), src);
    }

    @Test
    void matchersFollowTheOpenApiSchemaNotTheExampleValue() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        ApiOperation op = new OpenApiLoader().load(SPEC).operations().stream()
                .filter(o -> o.operationId().equals("createPet")).findFirst().orElseThrow();
        java.util.List<com.fasterxml.jackson.databind.JsonNode> its = java.util.List.of(m.readTree("""
                {"description":"alta","providerState":"s","request":{"method":"POST","path":"/owners/1/pets","body":{"name":"Leo","birthDate":"2020-05-15","typeId":1}},"response":{"status":201,"body":{"id":1,"name":"Leo","birthDate":"2020-05-15"}}}"""));
        String src = new dev.pactgen.core.generator.PactTestRenderer()
                .render("pactgen.generated", "api-gateway", "customers-service", op, its).source();
        assertTrue(src.contains("resBody.stringType(\"birthDate\""), "date-time en la respuesta: cualquier texto");
        assertTrue(src.contains("reqBody.stringMatcher(\"birthDate\""), "date en la petición: regla de fecha");
    }

    @Test
    void contextIncludesTheCauseOfEachResponseCode() throws Exception {
        ApiOperation op = new OpenApiLoader().load(SPEC).operations().stream()
                .filter(o -> o.operationId().equals("createPet")).findFirst().orElseThrow();
        String responses = new dev.pactgen.core.context.ContextBuilder(6000)
                .build(op, "api-gateway", "customers-service").variables().get("responses");
        assertTrue(responses.contains("- 400 (ownerId menor que 1)"), responses);
    }

    @Test
    void headerFilterKeepsHeadersRequiredBySecuritySchemes() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, ApiOperation> ops = new java.util.HashMap<>();
        new OpenApiLoader().load(Path.of("src/test/resources/fixtures/secured.yaml")).operations()
                .forEach(o -> ops.put(o.operationId(), o));
        String req = """
                {"request":{"method":"GET","path":"/x","headers":{"Authorization":"Bearer t","X-API-Key":"k"}},"response":{"status":200}}""";
        java.util.List<com.fasterxml.jackson.databind.JsonNode> secured = java.util.List.of(m.readTree(req));
        java.util.List<com.fasterxml.jackson.databind.JsonNode> open = java.util.List.of(m.readTree(req));
        java.util.List<com.fasterxml.jackson.databind.JsonNode> keyed = java.util.List.of(m.readTree(req));
        dev.pactgen.core.validation.HeaderFilter f = new dev.pactgen.core.validation.HeaderFilter();
        assertEquals(1, f.filter(secured, ops.get("getItem")), "seguridad global bearer: se conserva Authorization");
        assertTrue(secured.get(0).path("request").path("headers").has("Authorization"));
        assertEquals(2, f.filter(open, ops.get("getPublic")), "security: [] elimina ambas");
        assertEquals(1, f.filter(keyed, ops.get("getWithKey")), "apiKey en cabecera: se conserva X-API-Key");
        assertTrue(keyed.get(0).path("request").path("headers").has("X-API-Key"));
    }
}
