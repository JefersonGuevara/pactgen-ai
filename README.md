# PactGen-AI

Herramienta de código abierto que genera **pruebas de contrato Pact V4** (consumidor, JUnit 5 + Pact JVM) para microservicios Spring Boot a partir de **especificaciones OpenAPI 3.x** mediante LLMs, con modelo, parámetros y prompt fijados y archivados para que cada ejecución sea reproducible.

> Trabajo Fin de Máster — Máster Universitario en Ingeniería de Software y Sistemas Informáticos (UNIR).
> Estado: **spike I0** (prueba de concepto de punta a punta).

## Arquitectura

| Módulo | Paquete | Responsabilidad |
|---|---|---|
| M1 OpenAPI Parser | `dev.pactgen.core.openapi` | Lee y normaliza OpenAPI 3.x (swagger-parser) |
| M2 Context Builder | `dev.pactgen.core.context` | Variables del prompt por operación, presupuesto de tokens |
| M3 LLM Engine | `dev.pactgen.core.llm`, `.validation`, `.engine` | Prompt versionado, adaptadores (Anthropic, OpenAI-compatible/Ollama, replay), validación y reparación |
| M4 Pact Generator | `dev.pactgen.core.generator` | Plantilla determinista → prueba JUnit 5 con Pact JVM |
| M5 Interfaces | `pactgen-cli`, `pactgen-maven-plugin` | CLI (picocli) y goal `pactgen:generate` |

El LLM **no escribe Java**: devuelve un JSON con `analysis` e `interactions`; la herramienta lo valida contra el OpenAPI y genera el código.

## Requisitos

- Java 21 y Maven 3.9
- Para generar con el modelo de referencia: variable de entorno `ANTHROPIC_API_KEY`
- Alternativa local sin costo: [Ollama](https://ollama.com) con un modelo de código (p. ej. `qwen2.5-coder:7b`)

## Compilar

```bash
mvn -B verify          # compila los tres módulos y ejecuta las pruebas de pactgen-core
```

La CLI queda en `pactgen-cli/target/pactgen.jar`.

## Spike I0: de OpenAPI a contrato Pact

```bash
# 1) Generar la prueba de consumidor para GET /owners/{ownerId}
java -jar pactgen-cli/target/pactgen.jar generate \
  --openapi validation/microservices/customers-service/openapi.yaml \
  --consumer api-gateway --provider customers-service \
  --include findOwner \
  --out examples/spike-consumer

# 2) Ejecutar la prueba: produce el contrato en examples/spike-consumer/target/pacts/
cd examples/spike-consumer && mvn -B test
```

Otras formas de ejecutar el paso 1:

```bash
# Sin API key, con la respuesta sintética de ejemplo (prueba offline del pipeline)
java -jar pactgen-cli/target/pactgen.jar generate ... --replay pactgen-core/src/test/resources/fixtures/replay

# Con un modelo local en Ollama
java -jar pactgen-cli/target/pactgen.jar generate ... --llm-provider openai --model qwen2.5-coder:7b --api-key-env ""
```

Cada ejecución deja en `<out>/pactgen/`:

- `manifest-<fecha>.json`: modelo, temperatura, hash SHA-256 del prompt y del OpenAPI, tokens, costo e intentos por operación.
- `raw/<operationId>-<intento>.json`: respuestas crudas del modelo, reutilizables con `--replay` (RF-12).

## Plugin Maven (en el `pom.xml` del consumidor)

```xml
<plugin>
  <groupId>dev.pactgen</groupId>
  <artifactId>pactgen-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals><goal>generate</goal></goals>
      <configuration>
        <openapi>${project.basedir}/contracts/customers-service.yaml</openapi>
        <consumer>api-gateway</consumer>
        <provider>customers-service</provider>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## Estructura

```
pactgen-core/            núcleo (M1–M4), prompts en src/main/resources/prompts/v1
pactgen-cli/             CLI
pactgen-maven-plugin/    plugin Maven
examples/spike-consumer/ proyecto consumidor mínimo donde se escriben y ejecutan las pruebas
validation/              OpenAPIs del corpus, mutantes (bc/*) y scripts de análisis del experimento
```

## Pendiente (iteraciones I1–I4)

- `validate` y `diff` en la CLI; publicación en Pact Broker (RF-10)
- Paralelizar operaciones y añadir pruebas de integración con Testcontainers
- OpenAPIs de visits-, account-, statistics- y auth-service; catálogo de mutantes

## Licencia

MIT
