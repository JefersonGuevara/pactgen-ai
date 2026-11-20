package dev.pactgen.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.pactgen.core.model.ApiModel.ApiOperation;
import dev.pactgen.core.model.ApiModel.ApiParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filtro determinista de cabeceras de petición (v1.3).
 *
 * <p>Elimina de cada interacción las cabeceras de petición que el documento OpenAPI no declara como
 * parámetros {@code in: header} ni exige mediante sus esquemas de seguridad ({@code securitySchemes}, v1.6).
 * Se conservan siempre {@code Content-Type} y {@code Accept}. Evita que el
 * contrato exija cabeceras inventadas por el modelo, por ejemplo un {@code Authorization} con un token
 * ficticio cuando la autenticación no forma parte del contrato.
 */
public final class HeaderFilter {

    private static final Set<String> ALWAYS_ALLOWED = Set.of("content-type", "accept");

    /** Elimina las cabeceras no declaradas y devuelve cuántas se eliminaron. */
    public int filter(List<JsonNode> interactions, ApiOperation op) {
        Set<String> declared = op.parameters().stream()
                .filter(p -> "header".equalsIgnoreCase(p.in()))
                .map(ApiParameter::name)
                .map(n -> n.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.HashSet::new));
        declared.addAll(op.securityHeaders()); // v1.6: cabeceras exigidas por securitySchemes
        int removed = 0;
        for (JsonNode it : interactions) {
            JsonNode headers = it.path("request").path("headers");
            if (!(headers instanceof ObjectNode obj)) continue;
            List<String> names = new ArrayList<>();
            obj.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                String key = name.toLowerCase(Locale.ROOT);
                if (!ALWAYS_ALLOWED.contains(key) && !declared.contains(key)) {
                    obj.remove(name);
                    removed++;
                }
            }
        }
        return removed;
    }
}
