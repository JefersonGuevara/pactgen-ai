package dev.pactgen.cli;

import dev.pactgen.core.engine.GenerationEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Bitácora de ejecuciones (opción {@code --archive}): copia a una carpeta propia el manifiesto,
 * las respuestas crudas y las pruebas generadas de una ejecución, y crea un NOTAS.md con el resumen
 * y un espacio para las observaciones del autor. El contrato Pact (target/pacts) no se copia porque
 * solo existe después de ejecutar las pruebas con {@code mvn test}.
 */
final class RunArchiver {

    private RunArchiver() {}

    static Path archive(Path root, GenerationEngine.Summary s, String provider, String model,
                        String promptVersion) throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path dir = root.resolve(stamp + "_" + provider + "_" + shortModel(model));
        Files.createDirectories(dir.resolve("raw"));

        Files.copy(s.manifest(), dir.resolve(s.manifest().getFileName()), StandardCopyOption.REPLACE_EXISTING);
        for (Path raw : s.rawFiles()) {
            Files.copy(raw, dir.resolve("raw").resolve(raw.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        for (Path test : s.testFiles()) {
            Files.copy(test, dir.resolve(test.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }

        String notes = String.format(Locale.ROOT, """
                # Ejecución %s — %s

                - Modelo: %s
                - Prompt: %s
                - Operaciones: %d | generadas: %d
                - Interacciones: %d (redundantes descartadas: %d)
                - Costo estimado: USD %.4f
                - Manifiesto: %s

                ## Resultado de las pruebas (mvn test)

                - Pruebas: _/_ pasan.

                ## Observaciones

                -
                """, stamp, provider, model, promptVersion, s.operations(), s.generated(), s.interactions(), s.duplicatesRemoved(), s.costUsd(),
                s.manifest().getFileName());
        Files.writeString(dir.resolve("NOTAS.md"), notes, StandardCharsets.UTF_8);
        return dir;
    }

    private static String shortModel(String model) {
        return model.replaceAll("^claude-", "").replaceAll("-\\d{8}$", "").replaceAll("[^A-Za-z0-9.-]", "-");
    }
}
