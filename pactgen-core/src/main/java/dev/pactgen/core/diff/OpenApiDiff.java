package dev.pactgen.core.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import dev.pactgen.core.model.ApiModel;
import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.model.ApiModel.ApiParameter;
import dev.pactgen.core.validation.SchemaChecker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@code pactgen diff} (RF-08): compara dos versiones de un OpenAPI y clasifica los cambios para el consumidor.
 *
 * <p>Incompatibles: operación eliminada o movida (ruta o método); código de éxito cambiado; campo de respuesta
 * eliminado; tipo o formato de un campo cambiado (petición o respuesta); campo obligatorio nuevo en la petición;
 * parámetro que pasa a obligatorio (o parámetro obligatorio nuevo); valor de {@code enum} eliminado en una respuesta.
 * Compatibles: operación nueva; campo de respuesta nuevo; campo opcional nuevo en la petición; cambios de descripción.
 * Otros cambios (longitudes, patrones, códigos de error, ejemplos) no se informan.
 *
 * <p>Las operaciones se emparejan por método y ruta, con los nombres de las variables de ruta normalizados
 * ({@code /owners/{id}} = {@code /owners/{ownerId}}). Una operación que desaparece de su ruta pero conserva el
 * {@code operationId} en otra se informa como movida.
 */
public final class OpenApiDiff {

    public enum Kind { INCOMPATIBLE, COMPATIBLE }

    /** Un cambio; {@code operation} es "MÉTODO /ruta" en la versión nueva o, si se eliminó, en la antigua. */
    public record Change(Kind kind, String operation, String message) {}

    public record Report(List<Change> changes) {
        public long count(Kind k) { return changes.stream().filter(c -> c.kind() == k).count(); }
        public boolean hasBreaking() { return count(Kind.INCOMPATIBLE) > 0; }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    public Report compare(ApiModel oldApi, ApiModel newApi) {
        List<Change> out = new ArrayList<>();
        Map<String, ApiOperation> olds = index(oldApi);
        Map<String, ApiOperation> news = index(newApi);

        for (Map.Entry<String, ApiOperation> e : olds.entrySet()) {
            ApiOperation o = e.getValue();
            ApiOperation n = news.get(e.getKey());
            if (n == null) {
                ApiOperation moved = newApi.operations().stream()
                        .filter(x -> x.operationId().equals(o.operationId()) && !olds.containsKey(key(x))).findFirst().orElse(null);
                if (moved != null) {
                    out.add(new Change(Kind.INCOMPATIBLE, label(o), "operación movida a " + label(moved)));
                } else {
                    out.add(new Change(Kind.INCOMPATIBLE, label(o), "operación eliminada"));
                }
                continue;
            }
            operation(o, n, out);
        }
        for (Map.Entry<String, ApiOperation> e : news.entrySet()) {
            if (olds.containsKey(e.getKey())) continue;
            ApiOperation n = e.getValue();
            boolean isMove = oldApi.operations().stream()
                    .anyMatch(x -> x.operationId().equals(n.operationId()) && !news.containsKey(key(x)));
            if (!isMove) out.add(new Change(Kind.COMPATIBLE, label(n), "operación nueva"));
        }
        return new Report(out);
    }

    private void operation(ApiOperation o, ApiOperation n, List<Change> out) {
        String op = label(n);
        // Descripciones
        if (!Objects.equals(o.summary(), n.summary())) out.add(new Change(Kind.COMPATIBLE, op, "descripción de la operación cambiada"));
        for (String status : o.responseDescriptions().keySet()) {
            String nd = n.responseDescriptions().get(status);
            if (nd != null && !nd.equals(o.responseDescriptions().get(status))) {
                out.add(new Change(Kind.COMPATIBLE, op, "descripción de la respuesta " + status + " cambiada"));
            }
        }

        // Códigos de éxito
        Set<String> oldOk = success(o);
        Set<String> newOk = success(n);
        if (!oldOk.isEmpty() && !newOk.containsAll(oldOk)) {
            out.add(new Change(Kind.INCOMPATIBLE, op, "código de éxito cambiado: " + oldOk + " → " + newOk));
        }

        // Parámetros
        for (ApiParameter np : n.parameters()) {
            if ("path".equals(np.in())) continue;
            ApiParameter opar = o.parameters().stream()
                    .filter(p -> p.name().equals(np.name()) && Objects.equals(p.in(), np.in())).findFirst().orElse(null);
            if (opar == null) {
                out.add(new Change(np.required() ? Kind.INCOMPATIBLE : Kind.COMPATIBLE, op,
                        "parámetro " + np.in() + " \"" + np.name() + "\" nuevo" + (np.required() ? " y obligatorio" : " (opcional)")));
            } else {
                if (np.required() && !opar.required()) {
                    out.add(new Change(Kind.INCOMPATIBLE, op, "el parámetro " + np.in() + " \"" + np.name() + "\" pasa a obligatorio"));
                }
                typeChange(schema(opar.schemaJson()), schema(np.schemaJson()), "parámetro " + np.name(), op, out);
            }
        }

        // Cuerpo de la petición
        JsonNode oldReq = schema(o.requestBodySchema());
        JsonNode newReq = schema(n.requestBodySchema());
        if (!newReq.isMissingNode()) walk(oldReq, newReq, "petición $", op, true, out);

        // Cuerpos de respuesta (códigos presentes en las dos versiones)
        for (String status : o.responses().keySet()) {
            if (!n.responses().containsKey(status)) continue;
            JsonNode or = schema(o.responses().get(status));
            JsonNode nr = schema(n.responses().get(status));
            if (!or.isMissingNode() && nr.isMissingNode()) {
                out.add(new Change(Kind.INCOMPATIBLE, op, "la respuesta " + status + " deja de tener cuerpo"));
            } else if (!or.isMissingNode()) {
                walk(or, nr, "respuesta " + status + " $", op, false, out);
            }
        }
    }

    /** Recorre dos esquemas en paralelo. {@code request}: la dirección del cambio se interpreta como petición. */
    private void walk(JsonNode o, JsonNode n, String at, String op, boolean request, List<Change> out) {
        if (n.isMissingNode()) return;
        n = SchemaChecker.mergeAllOf(n);
        if (o.isMissingNode()) {
            // Cuerpo de petición nuevo: sus obligatorios son nuevos
            if (request) for (JsonNode r : n.path("required")) {
                out.add(new Change(Kind.INCOMPATIBLE, op, at + ": campo obligatorio nuevo \"" + r.asText() + "\""));
            }
            return;
        }
        o = SchemaChecker.mergeAllOf(o);
        if (typeChange(o, n, at, op, out)) return;
        if (!Objects.equals(o.path("description").asText(null), n.path("description").asText(null))) {
            out.add(new Change(Kind.COMPATIBLE, op, at + ": descripción cambiada"));
        }

        // enum: valores eliminados en una respuesta
        if (!request && o.path("enum").isArray() && n.path("enum").isArray()) {
            Set<String> removed = new TreeSet<>();
            o.path("enum").forEach(v -> removed.add(v.asText()));
            n.path("enum").forEach(v -> removed.remove(v.asText()));
            if (!removed.isEmpty()) out.add(new Change(Kind.INCOMPATIBLE, op, at + ": valores del enum eliminados " + removed));
        }

        switch (SchemaChecker.type(n)) {
            case "array" -> walk(o.path("items"), n.path("items"), at + "[]", op, request, out);
            case "object" -> {
                Set<String> oldReq = names(o.path("required"));
                Set<String> newReq = names(n.path("required"));
                JsonNode op1 = o.path("properties");
                JsonNode np = n.path("properties");
                Set<String> all = new LinkedHashSet<>();
                op1.properties().forEach(e -> all.add(e.getKey()));
                np.properties().forEach(e -> all.add(e.getKey()));
                newReq.forEach(all::add);
                for (String f : all) {
                    String child = at + "." + f;
                    boolean inOld = op1.has(f);
                    boolean inNew = np.has(f) || newReq.contains(f);
                    if (inOld && !inNew) {
                        if (!request) out.add(new Change(Kind.INCOMPATIBLE, op, child + ": campo eliminado de la respuesta"));
                    } else if (!inOld && inNew) {
                        if (request) {
                            boolean req = newReq.contains(f);
                            out.add(new Change(req ? Kind.INCOMPATIBLE : Kind.COMPATIBLE, op,
                                    child + ": campo " + (req ? "obligatorio" : "opcional") + " nuevo en la petición"));
                        } else {
                            out.add(new Change(Kind.COMPATIBLE, op, child + ": campo nuevo en la respuesta"));
                        }
                    } else {
                        if (request && newReq.contains(f) && !oldReq.contains(f)) {
                            out.add(new Change(Kind.INCOMPATIBLE, op, child + ": el campo pasa a obligatorio en la petición"));
                        }
                        walk(op1.path(f), np.path(f), child, op, request, out);
                    }
                }
            }
            default -> { }
        }
    }

    /** Tipo o formato cambiado (incompatible). Devuelve true si lo hubo, para no seguir comparando por dentro. */
    private static boolean typeChange(JsonNode o, JsonNode n, String at, String op, List<Change> out) {
        if (o.isMissingNode() || n.isMissingNode()) return false;
        String ot = SchemaChecker.type(SchemaChecker.mergeAllOf(o));
        String nt = SchemaChecker.type(SchemaChecker.mergeAllOf(n));
        if (!ot.isEmpty() && !nt.isEmpty() && !ot.equals(nt)) {
            out.add(new Change(Kind.INCOMPATIBLE, op, at + ": tipo cambiado " + ot + " → " + nt));
            return true;
        }
        String of = o.path("format").asText("");
        String nf = n.path("format").asText("");
        if (!of.equals(nf)) {
            out.add(new Change(Kind.INCOMPATIBLE, op, at + ": formato cambiado " + (of.isEmpty() ? "(ninguno)" : of)
                    + " → " + (nf.isEmpty() ? "(ninguno)" : nf)));
            return true;
        }
        return false;
    }

    private static Set<String> success(ApiOperation op) {
        Set<String> s = new TreeSet<>();
        for (String code : op.responses().keySet()) if (code.startsWith("2")) s.add(code);
        return s;
    }

    private static Set<String> names(JsonNode arr) {
        Set<String> s = new LinkedHashSet<>();
        arr.forEach(x -> s.add(x.asText()));
        return s;
    }

    private static JsonNode schema(String json) {
        if (json == null || json.isBlank()) return MissingNode.getInstance();
        try {
            return JSON.readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Esquema no legible", e);
        }
    }

    private static Map<String, ApiOperation> index(ApiModel api) {
        Map<String, ApiOperation> m = new LinkedHashMap<>();
        for (ApiOperation op : api.operations()) m.put(key(op), op);
        return m;
    }

    private static String key(ApiOperation op) {
        return op.method() + " " + op.path().replaceAll("\\{[^}]*}", "{}");
    }

    private static String label(ApiOperation op) {
        return op.method() + " " + op.path();
    }
}
