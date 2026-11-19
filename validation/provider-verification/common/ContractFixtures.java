package dev.pactgen.verification;

import au.com.dius.pact.core.model.IRequest;
import au.com.dius.pact.core.model.IResponse;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.SynchronousRequestResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Datos de prueba derivados del contrato para la verificación del proveedor (PactGen-AI, sección 3.4.4).
 *
 * <p>Los estados del proveedor de los contratos están en lenguaje natural y no pueden programarse por adelantado.
 * En su lugar, antes de cada interacción se construyen las entidades que devolverían los repositorios o servicios
 * a partir del cuerpo de respuesta esperado por el contrato. La construcción es por reflexión y tolerante: un campo
 * que ya no existe o cuyo tipo cambió (por un mutante) se deja vacío, y es la serialización del proveedor la que
 * revela la incompatibilidad.
 */
public final class ContractFixtures {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ContractFixtures() {}

    /** Lo que el contrato espera en una interacción HTTP. */
    public record Expected(String method, String path, int status, JsonNode requestBody, JsonNode responseBody) {
        public boolean ok() { return status < 400; }
        public boolean is(String m, String pathRegex) { return method.equalsIgnoreCase(m) && path.matches(pathRegex); }
    }

    public static Expected expected(Interaction interaction) {
        SynchronousRequestResponse rr = interaction.asSynchronousRequestResponse();
        if (rr == null) throw new IllegalStateException("Solo se verifican interacciones HTTP síncronas");
        IRequest req = rr.getRequest();
        IResponse res = rr.getResponse();
        return new Expected(req.getMethod().toUpperCase(), req.getPath(), res.getStatus(),
                json(req.getBody()), json(res.getBody()));
    }

    private static JsonNode json(OptionalBody body) {
        if (body == null || !body.isPresent()) return null;
        try {
            return JSON.readTree(body.valueAsString());
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ construcción por reflexión

    /** Construye una instancia de {@code type} con los campos presentes en {@code json}; null si no hay JSON. */
    public static <T> T build(Class<T> type, JsonNode json) {
        if (json == null || json.isNull() || json.isMissingNode()) return null;
        return type.cast(convert(type, json, null));
    }

    public static <T> List<T> buildList(Class<T> type, JsonNode json) {
        List<T> out = new ArrayList<>();
        if (json != null && json.isArray()) json.forEach(n -> out.add(build(type, n)));
        return out;
    }

    /** Asigna un campo por nombre (también heredado); no hace nada si el campo no existe. */
    public static void setField(Object target, String name, Object value) {
        Field f = field(target.getClass(), name);
        if (f == null) return;
        try {
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {
            // tipo incompatible tras un mutante: se deja el valor por defecto
        }
    }

    /** Copia el campo {@code id} del cuerpo esperado a la entidad (simula el id generado al guardar). */
    public static <T> T withId(T entity, JsonNode expectedBody) {
        if (entity != null && expectedBody != null && expectedBody.has("id")) {
            Field f = field(entity.getClass(), "id");
            if (f != null) {
                try {
                    f.setAccessible(true);
                    f.set(entity, convert(f.getGenericType(), expectedBody.get("id"), f));
                } catch (Exception ignored) {
                }
            }
        }
        return entity;
    }

    private static Object convert(Type type, JsonNode v, Field owner) {
        if (v == null || v.isNull() || v.isMissingNode()) return null;
        Class<?> c = raw(type);
        try {
            if (c == String.class) return v.isValueNode() ? v.asText() : v.toString();
            if (c == Integer.class || c == int.class) return v.canConvertToInt() ? v.asInt() : Integer.valueOf(v.asText());
            if (c == Long.class || c == long.class) return v.canConvertToLong() ? v.asLong() : Long.valueOf(v.asText());
            if (c == Double.class || c == double.class) return v.isNumber() ? v.asDouble() : Double.valueOf(v.asText());
            if (c == Float.class || c == float.class) return (float) (v.isNumber() ? v.asDouble() : Double.parseDouble(v.asText()));
            if (c == BigDecimal.class) return v.isNumber() ? v.decimalValue() : new BigDecimal(v.asText());
            if (c == Boolean.class || c == boolean.class) {
                if (v.isBoolean()) return v.asBoolean();
                throw new IllegalArgumentException("no es booleano");
            }
            if (c == LocalDate.class) return LocalDate.parse(v.asText().substring(0, 10));
            if (c == LocalDateTime.class) return LocalDateTime.parse(v.asText().replaceAll("(Z|[+-]\\d{2}:?\\d{2})$", ""));
            if (c == Instant.class) return parseInstant(v.asText());
            if (Date.class.isAssignableFrom(c)) return date(v.asText(), owner);
            if (c.isEnum()) return enumValue(c, v.asText());
            if (Collection.class.isAssignableFrom(c)) {
                Type elem = typeArg(type, 0);
                Collection<Object> col = Set.class.isAssignableFrom(c) ? new LinkedHashSet<>() : new ArrayList<>();
                if (v.isArray()) v.forEach(n -> col.add(convert(elem, n, null)));
                return c.isInterface() ? col : copyInto(c, col);
            }
            if (Map.class.isAssignableFrom(c)) {
                Class<?> kc = raw(typeArg(type, 0));
                Type vt = typeArg(type, 1);
                Map<Object, Object> map = kc.isEnum() ? enumMap(kc) : new HashMap<>();
                v.properties().forEach(e -> {
                    Object k = kc.isEnum() ? enumValue(kc, e.getKey()) : e.getKey();
                    if (k != null) map.put(k, convert(vt, e.getValue(), null));
                });
                return map;
            }
            if (!v.isObject()) return null;
            Object obj = instantiate(c);
            for (Field f : fields(c)) {
                if (Modifier.isStatic(f.getModifiers()) || !v.has(f.getName())) continue;
                Object value = convert(f.getGenericType(), v.get(f.getName()), f);
                if (value == null && f.getType().isPrimitive()) continue;
                try {
                    f.setAccessible(true);
                    f.set(obj, value);
                } catch (Exception ignored) {
                }
            }
            return obj;
        } catch (RuntimeException e) {
            return null; // valor no convertible (p. ej., tipo cambiado por un mutante)
        }
    }

    /** Las columnas @Temporal(DATE) se leen de JPA como java.sql.Date, que Jackson serializa como yyyy-MM-dd. */
    private static Date date(String text, Field owner) {
        boolean sqlDate = owner != null && isTemporalDate(owner);
        if (text.length() == 10) {
            LocalDate d = LocalDate.parse(text);
            return sqlDate ? java.sql.Date.valueOf(d) : Date.from(d.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        }
        Date d = Date.from(parseInstant(text));
        return sqlDate ? new java.sql.Date(d.getTime()) : d;
    }

    private static boolean isTemporalDate(Field f) {
        for (Annotation a : f.getAnnotations()) {
            String n = a.annotationType().getName();
            if (n.endsWith(".Temporal") && a.toString().contains("DATE") && !a.toString().contains("TIMESTAMP")) return true;
        }
        return false;
    }

    private static Instant parseInstant(String text) {
        try {
            return OffsetDateTime.parse(text.replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2")).toInstant();
        } catch (Exception e) {
            return LocalDate.parse(text.substring(0, 10)).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> c, String name) {
        try {
            return Enum.valueOf((Class<Enum>) c, name);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<Object, Object> enumMap(Class<?> c) {
        return new EnumMap(c);
    }

    @SuppressWarnings("unchecked")
    private static Object copyInto(Class<?> c, Collection<Object> items) {
        try {
            Collection<Object> col = (Collection<Object>) instantiate(c);
            col.addAll(items);
            return col;
        } catch (RuntimeException e) {
            return Set.class.isAssignableFrom(c) ? new HashSet<>(items) : items;
        }
    }

    private static Object instantiate(Class<?> c) {
        try {
            Constructor<?> k = c.getDeclaredConstructor();
            k.setAccessible(true);
            return k.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Sin constructor vacío: " + c.getName(), e);
        }
    }

    private static List<Field> fields(Class<?> c) {
        List<Field> out = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) out.add(f);
        }
        return out;
    }

    private static Field field(Class<?> c, String name) {
        for (Field f : fields(c)) if (f.getName().equals(name)) return f;
        return null;
    }

    private static Class<?> raw(Type t) {
        if (t instanceof Class<?> c) return c;
        if (t instanceof ParameterizedType p) return (Class<?>) p.getRawType();
        return Object.class;
    }

    private static Type typeArg(Type t, int i) {
        if (t instanceof ParameterizedType p && p.getActualTypeArguments().length > i) return p.getActualTypeArguments()[i];
        return Object.class;
    }
}
