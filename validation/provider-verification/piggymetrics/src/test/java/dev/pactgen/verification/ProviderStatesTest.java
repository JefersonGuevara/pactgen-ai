package dev.pactgen.verification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La heurística de estados del arnés (ProviderStates) y la tabla «Convención de redacción de los estados del
 * proveedor» de experiment/PROTOCOLO_SESION.md dicen lo mismo, en los dos sentidos:
 * cada expresión de la tabla la reconoce el arnés, y cada alternativa del arnés figura en la tabla.
 * No usa contratos de P3 ni P5: las expresiones salen de la propia tabla.
 */
class ProviderStatesTest {

    private static final Path PROTOCOLO = Path.of("../../../experiment/PROTOCOLO_SESION.md");
    private static final Pattern EXPRESION = Pattern.compile("«([^»]+)»");

    /** Expresiones «…» de la fila de la tabla cuya primera columna es {@code situacion}. */
    private static List<String> fila(String situacion) throws Exception {
        for (String line : Files.readAllLines(PROTOCOLO)) {
            if (line.startsWith("| " + situacion + " |")) {
                List<String> out = new ArrayList<>();
                Matcher m = EXPRESION.matcher(line);
                while (m.find()) out.add(m.group(1));
                assertFalse(out.isEmpty(), "fila sin expresiones: " + line);
                return out;
            }
        }
        throw new AssertionError("No está la fila «" + situacion + "» en " + PROTOCOLO.toAbsolutePath().normalize());
    }

    private static boolean reconoce(String regex, String estado) {
        // Exactamente la comprobación que usan las clases de verificación
        return new ContractFixtures.Expected("GET", "/", 400, null, null, estado).stateMatches(regex);
    }

    private static void enAmbosSentidos(String situacion, String regex, String otra) throws Exception {
        List<String> tabla = fila(situacion);
        for (String e : tabla) {
            assertTrue(reconoce(regex, e), "«" + e + "» (" + situacion + ") no la reconoce el arnés: " + regex);
            assertTrue(reconoce(regex, e.toUpperCase()), "sin distinguir mayúsculas: «" + e + "»");
            assertFalse(reconoce(otra, e), "«" + e + "» (" + situacion + ") la reconoce también la expresión contraria");
        }
        for (String alternativa : regex.split("\\|")) {
            boolean enTabla = tabla.stream().anyMatch(e -> reconoce(alternativa, e));
            assertTrue(enTabla, "la alternativa /" + alternativa + "/ del arnés no está en la fila «" + situacion + "» de la tabla");
        }
    }

    @Test
    void recursoNoExiste() throws Exception {
        enAmbosSentidos("El recurso no existe", ProviderStates.NOT_FOUND, ProviderStates.ALREADY_EXISTS);
    }

    @Test
    void recursoYaExiste() throws Exception {
        enAmbosSentidos("El recurso ya existe", ProviderStates.ALREADY_EXISTS, ProviderStates.NOT_FOUND);
    }

    @Test
    void casoNormalNoActivaNingunError() throws Exception {
        for (String e : fila("Caso normal")) {
            assertFalse(reconoce(ProviderStates.NOT_FOUND, e), "«" + e + "» activaría «no existe»");
            assertFalse(reconoce(ProviderStates.ALREADY_EXISTS, e), "«" + e + "» activaría «ya existe»");
        }
    }

    @Test
    void frasesCompletasComoLasDeUnContrato() {
        assertTrue(reconoce(ProviderStates.NOT_FOUND, "La cuenta 'carlos' no existe"));
        assertTrue(reconoce(ProviderStates.NOT_FOUND, "account carlos does not exist"));
        assertTrue(reconoce(ProviderStates.ALREADY_EXISTS, "El usuario ana ya existe en el sistema"));
        assertTrue(reconoce(ProviderStates.ALREADY_EXISTS, "user ana ALREADY EXISTS"));
        // «inexistente» contiene «existente»: no debe activar «ya existe»
        assertFalse(reconoce(ProviderStates.ALREADY_EXISTS, "una cuenta inexistente"));
        assertFalse(reconoce(ProviderStates.NOT_FOUND, "existe la cuenta demo"));
    }
}
