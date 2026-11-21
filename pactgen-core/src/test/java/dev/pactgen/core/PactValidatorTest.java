package dev.pactgen.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.openapi.OpenApiLoader;
import dev.pactgen.core.validation.PactValidator;
import dev.pactgen.core.validation.PactValidator.InteractionResult;
import dev.pactgen.core.validation.PactValidator.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** pactgen validate (RF-08) con un OpenAPI y contratos sintéticos. */
class PactValidatorTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ApiModel API = new OpenApiLoader().load(Path.of("src/test/resources/fixtures/validate/openapi.yaml"));

    private static final String ITEM = """
            {"id":1,"name":"a","kind":"A","price":2.5,"active":true,"day":"2026-11-18",
             "updated":"2026-11-18T15:30:00.000+0000","note":null}""";

    /** Interacción V4 mínima. */
    private static String http(String method, String path, String extraReq, int status, String resBody) {
        String body = resBody == null ? "" : ",\"body\":{\"content\":" + resBody + ",\"contentType\":\"application/json\",\"encoded\":false}";
        return "{\"type\":\"Synchronous/HTTP\",\"description\":\"d\",\"request\":{\"method\":\"" + method + "\",\"path\":\""
                + path + "\"" + extraReq + "},\"response\":{\"status\":" + status + body + "}}";
    }

    private static InteractionResult one(String interaction, boolean strict) throws Exception {
        JsonNode pact = JSON.readTree("{\"interactions\":[" + interaction + "]}");
        List<InteractionResult> r = new PactValidator(API, strict).validate("p.json", pact);
        assertEquals(1, r.size());
        return r.get(0);
    }

    private static InteractionResult one(String interaction) throws Exception {
        return one(interaction, false);
    }

    private static final String TENANT = ",\"headers\":{\"x-tenant\":[\"t1\"]}";

    @Test
    void validInteractionIsOk() throws Exception {
        InteractionResult r = one(http("GET", "/api/items/7", TENANT, 200, ITEM));
        assertEquals(Level.OK, r.level(), r.reasons().toString());
        assertEquals("getItem", r.operationId());
    }

    @Test
    void literalPathWinsOverTemplate() throws Exception {
        // /api/items/current casa con las dos plantillas; gana la concreta (sin cabecera obligatoria)
        InteractionResult r = one(http("GET", "/api/items/current", "", 200, ITEM));
        assertEquals("getCurrentItem", r.operationId());
        assertEquals(Level.OK, r.level(), r.reasons().toString());
    }

    @Test
    void unknownPathAndMethod() throws Exception {
        assertTrue(one(http("GET", "/api/other", "", 200, null)).reasons().get(0).contains("no existe"));
        InteractionResult m = one(http("DELETE", "/api/items/7", "", 204, null));
        assertEquals(Level.ERROR, m.level());
        assertTrue(m.reasons().get(0).contains("DELETE no está declarado"), m.reasons().toString());
    }

    @Test
    void undeclaredStatusIsError() throws Exception {
        InteractionResult r = one(http("GET", "/api/items/7", TENANT, 500, null));
        assertEquals(Level.ERROR, r.level());
        assertTrue(r.reasons().get(0).contains("500 no está declarado"), r.reasons().toString());
    }

    @Test
    void requiredQueryAndHeader() throws Exception {
        assertTrue(one(http("GET", "/api/items/7", "", 404, null)).reasons().get(0).contains("cabecera obligatoria \"X-Tenant\""));
        assertTrue(one(http("GET", "/api/items", "", 200, "[]")).reasons().get(0).contains("consulta obligatorio \"q\""));
        assertEquals(Level.OK, one(http("GET", "/api/items", ",\"query\":{\"q\":[\"x\"]}", 200, "[" + ITEM + "]")).level());
        assertEquals(Level.OK, one(http("GET", "/api/items", ",\"query\":\"q=x\"", 200, "[]")).level(), "query de Pact V3");
        InteractionResult extra = one(http("GET", "/api/items", ",\"query\":{\"q\":[\"x\"],\"page\":[\"2\"]}", 200, "[]"));
        assertEquals(Level.AVISO, extra.level());
    }

    @Test
    void responseSchemaTypesFormatsAndEnum() throws Exception {
        String bad = """
                {"id":"1","name":"a","kind":"C","price":"2.5","active":"yes","day":"18/11/2026","updated":"2026-11-18 15:30"}""";
        InteractionResult r = one(http("GET", "/api/items/7", TENANT, 200, bad));
        assertEquals(Level.ERROR, r.level());
        String all = String.join("\n", r.reasons());
        for (String expected : List.of("$.id: se esperaba entero", "$.kind: \"C\" no es un valor del enum",
                "$.price: se esperaba número", "$.active: se esperaba booleano", "$.day: \"18/11/2026\" no tiene formato date",
                "$.updated: \"2026-11-18 15:30\" no tiene formato date-time")) {
            assertTrue(all.contains(expected), "falta «" + expected + "» en:\n" + all);
        }
    }

    @Test
    void requiredFieldsFromAllOf() throws Exception {
        InteractionResult r = one(http("GET", "/api/items/7", TENANT, 200, "{\"name\":\"a\"}"));
        String all = String.join("\n", r.reasons());
        assertTrue(all.contains("falta el campo obligatorio \"kind\""), all);
        assertTrue(all.contains("falta el campo obligatorio \"id\""), all);
    }

    @Test
    void integralDecimalAndOffsetVariantsAreAccepted() throws Exception {
        String item = "{\"id\":1.0,\"name\":\"a\",\"kind\":\"B\",\"updated\":\"2026-11-18T15:30:00Z\"}";
        assertEquals(Level.OK, one(http("GET", "/api/items/7", TENANT, 200, item)).level());
    }

    @Test
    void undeclaredFieldIsWarningOrErrorWithStrict() throws Exception {
        String item = "{\"id\":1,\"name\":\"a\",\"kind\":\"A\",\"color\":\"red\"}";
        InteractionResult normal = one(http("GET", "/api/items/7", TENANT, 200, item));
        assertEquals(Level.AVISO, normal.level());
        assertTrue(normal.reasons().get(0).contains("$.color: campo no declarado"), normal.reasons().toString());
        assertEquals(Level.ERROR, one(http("GET", "/api/items/7", TENANT, 200, item), true).level());
    }

    @Test
    void nullInOptionalNonNullableFieldIsWarning() throws Exception {
        String item = "{\"id\":1,\"name\":\"a\",\"kind\":\"A\",\"price\":null}";
        InteractionResult r = one(http("GET", "/api/items/7", TENANT, 200, item));
        assertEquals(Level.AVISO, r.level(), r.reasons().toString());
    }

    @Test
    void requestBodyIsCheckedOnlyForSuccessResponses() throws Exception {
        String invalid = ",\"body\":{\"content\":{\"name\":\"a\"},\"contentType\":\"application/json\",\"encoded\":false}";
        InteractionResult ok201 = one(http("POST", "/api/items", invalid, 201, ITEM));
        assertEquals(Level.ERROR, ok201.level());
        assertTrue(ok201.reasons().get(0).contains("petición $: falta el campo obligatorio \"kind\""), ok201.reasons().toString());
        // La misma petición inválida esperando un 400 es lo que el contrato quiere comprobar
        InteractionResult err400 = one(http("POST", "/api/items", invalid, 400, null));
        assertEquals(Level.OK, err400.level());
        assertTrue(err400.notes().get(0).contains("no comprobado"), err400.notes().toString());
        assertTrue(ok201.notes().isEmpty());
        // Cuerpo de petición en una operación que no lo declara
        InteractionResult get = one(http("GET", "/api/items/current", invalid, 200, ITEM));
        assertTrue(get.reasons().get(0).contains("no declara cuerpo de petición"), get.reasons().toString());
    }

    @Test
    void pactV3BodiesAreRead() throws Exception {
        String v3 = "{\"description\":\"v3\",\"request\":{\"method\":\"GET\",\"path\":\"/api/items/current\"},"
                + "\"response\":{\"status\":200,\"body\":{\"id\":\"x\",\"name\":\"a\",\"kind\":\"A\"}}}";
        InteractionResult r = one(v3);
        assertEquals(Level.ERROR, r.level());
        assertTrue(r.reasons().get(0).contains("$.id: se esperaba entero"), r.reasons().toString());
    }

    @Test
    void filesAndFolders(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.json"), "{\"interactions\":[" + http("GET", "/api/items/current", "", 200, ITEM) + "]}");
        Files.writeString(dir.resolve("b.json"), "{\"interactions\":[" + http("GET", "/api/nope", "", 200, null) + "]}");
        PactValidator.Report report = new PactValidator(API, false).validate(dir);
        assertEquals(2, report.interactions().size());
        assertEquals("a.json", report.interactions().get(0).file());
        assertEquals(1, report.count(Level.ERROR));
        assertTrue(report.hasErrors());

        Files.writeString(dir.resolve("c.json"), "{no es json");
        assertThrows(PactValidator.UnreadablePactException.class, () -> new PactValidator(API, false).validate(dir));
        assertThrows(PactValidator.UnreadablePactException.class,
                () -> new PactValidator(API, false).validate(dir.resolve("no-existe.json")));
    }
}
