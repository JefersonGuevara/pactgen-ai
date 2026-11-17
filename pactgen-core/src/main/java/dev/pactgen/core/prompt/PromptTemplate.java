package dev.pactgen.core.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Prompt versionado (Anexo B). Las plantillas viven en {@code prompts/<version>/} y su
 * hash SHA-256 se registra en el manifiesto de cada ejecución (RNF-01).
 */
public final class PromptTemplate {

    private final String version;
    private final String system;
    private final String user;
    private final String repair;
    private final String sha256;

    private PromptTemplate(String version, String system, String user, String repair) {
        this.version = version;
        this.system = system;
        this.user = user;
        this.repair = repair;
        this.sha256 = sha256(system + "\u0000" + user + "\u0000" + repair);
    }

    public static PromptTemplate load(String version) {
        String base = "/prompts/" + version + "/";
        return new PromptTemplate(version, read(base + "system.md") + "\n\n" + read(base + "examples.md"),
                read(base + "user.md"), read(base + "repair.md"));
    }

    public String version() { return version; }
    public String sha256() { return sha256; }
    public String system() { return system; }

    public String user(Map<String, String> vars) { return fill(user, vars); }

    public String repair(String previousResponse, String errors) {
        return fill(repair, Map.of("previous", previousResponse, "errors", errors));
    }

    static String fill(String template, Map<String, String> vars) {
        String out = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private static String read(String resource) {
        try (InputStream in = PromptTemplate.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("No se encontró el recurso " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
