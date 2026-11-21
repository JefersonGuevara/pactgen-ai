package dev.pactgen.core;

import dev.pactgen.core.diff.OpenApiDiff;
import dev.pactgen.core.diff.OpenApiDiff.Change;
import dev.pactgen.core.diff.OpenApiDiff.Kind;
import dev.pactgen.core.openapi.OpenApiLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * pactgen diff (RF-08). Cada variante del OpenAPI base reproduce un tipo de la Tabla 8 (BC1–BC5, NB1–NB2) o un
 * caso adicional de la especificación de diff. Documenta que diff los distingue; no reclasifica los mutantes.
 */
class OpenApiDiffTest {

    private static final Path BASE = Path.of("src/test/resources/fixtures/diff/base.yaml");

    @TempDir
    Path dir;

    /** Aplica sustituciones de texto al OpenAPI base y compara base → variante. */
    private OpenApiDiff.Report diff(String... replacements) throws Exception {
        String text = Files.readString(BASE);
        for (int i = 0; i < replacements.length; i += 2) {
            assertTrue(text.contains(replacements[i]), "ancla no encontrada: " + replacements[i]);
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        Path variant = dir.resolve("variant.yaml");
        Files.writeString(variant, text);
        OpenApiLoader loader = new OpenApiLoader();
        return new OpenApiDiff().compare(loader.load(BASE), loader.load(variant));
    }

    private static void assertOnly(OpenApiDiff.Report r, Kind kind, String fragment) {
        List<Change> c = r.changes();
        assertTrue(c.stream().anyMatch(x -> x.kind() == kind && x.message().contains(fragment)),
                "falta " + kind + " «" + fragment + "» en " + c);
        if (kind == Kind.COMPATIBLE) assertFalse(r.hasBreaking(), "no debería haber incompatibles: " + c);
    }

    @Test
    void identicalSpecsHaveNoChanges() throws Exception {
        assertEquals(List.of(), diff().changes());
    }

    @Test
    void bc1ResponseFieldRemoved() throws Exception {
        // lastName sale de Owner (respuesta), no de OwnerRequest
        OpenApiDiff.Report r = diff("      required: [id, firstName, lastName]\n      properties:\n        id: { type: integer }\n"
                + "        firstName: { type: string }\n        lastName: { type: string }\n",
                "      required: [id, firstName]\n      properties:\n        id: { type: integer }\n        firstName: { type: string }\n");
        assertOnly(r, Kind.INCOMPATIBLE, "$.lastName: campo eliminado de la respuesta");
    }

    @Test
    void bc2FieldTypeChanged() throws Exception {
        OpenApiDiff.Report r = diff("        id: { type: integer }\n", "        id: { type: string }\n");
        assertOnly(r, Kind.INCOMPATIBLE, "$.id: tipo cambiado integer → string");
    }

    @Test
    void bc3SuccessCodeChanged() throws Exception {
        OpenApiDiff.Report r = diff("        '200':\n          description: Propietario encontrado",
                "        '204':\n          description: Propietario encontrado");
        assertOnly(r, Kind.INCOMPATIBLE, "código de éxito cambiado: [200] → [204]");
    }

    @Test
    void bc4NewRequiredRequestField() throws Exception {
        OpenApiDiff.Report r = diff("      required: [firstName, lastName]\n      properties:\n        firstName: { type: string }\n",
                "      required: [firstName, lastName, email]\n      properties:\n        email: { type: string }\n"
                        + "        firstName: { type: string }\n");
        assertOnly(r, Kind.INCOMPATIBLE, "$.email: campo obligatorio nuevo en la petición");
    }

    @Test
    void bc5PathRenamed() throws Exception {
        OpenApiDiff.Report r = diff("  /owners/{ownerId}:\n", "  /owners/{ownerId}/profile:\n");
        assertOnly(r, Kind.INCOMPATIBLE, "operación movida a GET /owners/{ownerId}/profile");
        assertEquals(1, r.changes().size(), "el movimiento no se informa además como operación nueva: " + r.changes());
    }

    @Test
    void nb1NewResponseField() throws Exception {
        OpenApiDiff.Report r = diff("        since: { type: string, format: date }\n",
                "        since: { type: string, format: date }\n        fullName: { type: string }\n");
        assertOnly(r, Kind.COMPATIBLE, "$.fullName: campo nuevo en la respuesta");
    }

    @Test
    void nb2NewOperation() throws Exception {
        OpenApiDiff.Report r = diff("  /owners:\n", "  /owners/count:\n    get:\n      operationId: countOwners\n"
                + "      responses:\n        '200': { description: ok }\n  /owners:\n");
        assertOnly(r, Kind.COMPATIBLE, "operación nueva");
        assertEquals("GET /owners/count", r.changes().get(0).operation());
    }

    @Test
    void operationRemoved() throws Exception {
        OpenApiDiff.Report r = diff("    post:\n      operationId: createOwner\n", "    put:\n      operationId: replaceOwners\n");
        assertTrue(r.changes().stream().anyMatch(c -> c.kind() == Kind.INCOMPATIBLE
                && c.operation().equals("POST /owners") && c.message().equals("operación eliminada")), r.changes().toString());
    }

    @Test
    void parameterBecomesRequired() throws Exception {
        OpenApiDiff.Report r = diff("{ name: city, in: query, required: false", "{ name: city, in: query, required: true");
        assertOnly(r, Kind.INCOMPATIBLE, "el parámetro query \"city\" pasa a obligatorio");
    }

    @Test
    void enumValueRemovedInResponse() throws Exception {
        OpenApiDiff.Report r = diff("enum: [ACTIVE, INACTIVE]", "enum: [ACTIVE]");
        assertOnly(r, Kind.INCOMPATIBLE, "valores del enum eliminados [INACTIVE]");
    }

    @Test
    void formatChanged() throws Exception {
        OpenApiDiff.Report r = diff("since: { type: string, format: date }", "since: { type: string, format: date-time }");
        assertOnly(r, Kind.INCOMPATIBLE, "$.since: formato cambiado date → date-time");
    }

    @Test
    void optionalRequestFieldAndDescriptionsAreCompatible() throws Exception {
        OpenApiDiff.Report r = diff("        lastName: { type: string }\n    Owner:",
                "        lastName: { type: string }\n        nickname: { type: string }\n    Owner:",
                "summary: Obtiene un propietario", "summary: Devuelve un propietario por id",
                "description: Propietario encontrado", "description: El propietario");
        assertFalse(r.hasBreaking(), r.changes().toString());
        assertOnly(r, Kind.COMPATIBLE, "$.nickname: campo opcional nuevo en la petición");
        assertOnly(r, Kind.COMPATIBLE, "descripción de la operación cambiada");
        assertOnly(r, Kind.COMPATIBLE, "descripción de la respuesta 200 cambiada");
    }

    @Test
    void pathVariableRenameIsNotAChange() throws Exception {
        OpenApiDiff.Report r = diff("  /owners/{ownerId}:\n", "  /owners/{id}:\n",
                "{ name: ownerId, in: path", "{ name: id, in: path");
        assertEquals(List.of(), r.changes());
    }
}
