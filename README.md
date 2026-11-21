# PactGen-AI

Herramienta de código abierto que genera **pruebas de contrato Pact V4** (consumidor, JUnit 5 + Pact JVM) para microservicios Spring Boot a partir de **especificaciones OpenAPI 3.x** mediante LLMs. El modelo, sus parámetros y el prompt quedan fijados y archivados, de modo que cada ejecución es reproducible.

> Trabajo Fin de Máster — Máster Universitario en Ingeniería de Software y Sistemas Informáticos (UNIR).
> **Estado (21 nov 2026):** versión 1.8 (congelada para el experimento con la etiqueta `experimento-v1`), piloto sobre el corpus completo (20/20 operaciones, 60/60 pruebas en verde) y verificación de proveedor para los cinco proveedores (línea base: P1 22/22, P2 9/9, P3 12/12, P4 9/9 y P5 5/5; P2, P3 y P5 a ciegas) y 35 mutantes (25/25 incompatibles detectados, 0/10 falsos positivos); comandos `validate` y `diff` y plugin Maven probado. Pendiente: experimento (sesiones del 28 nov al 6 dic).

## Cómo funciona

```
OpenAPI ─► M1 Parser ─► M2 Contexto ─► M3 Modelo (JSON) ─► Validación ─► Filtros deterministas ─► M4 Generador ─► Prueba JUnit 5 + Pact
                                                        └─ reparación (≤ 2)                                         └─► contrato Pact V4
```

El LLM **no escribe Java**: devuelve un JSON con `analysis` e `interactions`. La herramienta lo valida contra el OpenAPI, lo filtra y genera el código con una plantilla determinista. Cuando una regla en lenguaje natural del prompt no se cumplió de forma fiable, se sustituyó por un filtro en el código (ver *Historial de versiones*).

| Módulo | Paquete | Responsabilidad |
|---|---|---|
| M1 OpenAPI Parser | `dev.pactgen.core.openapi` | Lee y normaliza OpenAPI 3.x (swagger-parser), incluidas las descripciones de cada respuesta |
| M2 Context Builder | `dev.pactgen.core.context` | Variables del prompt por operación; presupuesto de tokens |
| M3 LLM Engine | `.llm`, `.validation`, `.engine` | Prompt versionado; adaptadores Anthropic, OpenAI-compatible/Ollama y *replay*; validación, reparación y filtros |
| M4 Pact Generator | `dev.pactgen.core.generator` | Plantilla determinista; reglas de coincidencia derivadas del esquema |
| M5 Interfaces | `pactgen-cli`, `pactgen-maven-plugin` | CLI (picocli) y goal `pactgen:generate` |

## Requisitos

- Java 17 y Maven 3.9 o superior
- Modelo de referencia: Claude Haiku 4.5 (`claude-haiku-4-5-20251001`), con la clave en `ANTHROPIC_API_KEY` (defínela solo en la sesión de la terminal)
- Alternativa local sin costo: cualquier servidor compatible con OpenAI (Ollama, LM Studio)

## Compilar

```bash
mvn -B verify      # compila los tres módulos y ejecuta las pruebas de pactgen-core
```

La CLI queda en `pactgen-cli/target/pactgen.jar`.

## Uso

```bash
# Generar las pruebas de consumidor de un servicio (todas sus operaciones o las indicadas con --include)
java -jar pactgen-cli/target/pactgen.jar generate \
  --openapi validation/microservices/customers-service/openapi.yaml \
  --consumer api-gateway --provider customers-service \
  --out examples/spike-consumer --archive runs

# Ejecutar las pruebas: produce el contrato en examples/spike-consumer/target/pacts/
mvn -f examples/spike-consumer/pom.xml clean test
```

| Opción | Por defecto | Descripción |
|---|---|---|
| `--include` | todas | Operaciones (operationId) a generar, separadas por comas |
| `--llm-provider` | `anthropic` | `anthropic`, `openai` (compatible: Ollama, LM Studio) o `replay` |
| `--model` / `--temperature` / `--max-tokens` | `claude-haiku-4-5-20251001` / 0.2 / 4096 | Parámetros del modelo |
| `--replay <dir>` | — | Reutiliza respuestas archivadas (`raw/`): regenera sin llamar al modelo y sin costo |
| `--archive <dir>` | — | Copia el manifiesto, las respuestas crudas y las pruebas a `<dir>/<fecha>_<proveedor>_<modelo>/` y crea un `NOTAS.md` |
| `--prompt-version` | `v1.1` | Versión del prompt (`v1`, `v1.1`) |
| `--max-repairs` | 2 | Intentos de reparación si la salida no es válida |
| `--no-dedupe` | — | Desactiva el filtro de interacciones redundantes (solo para comparar) |
| `--no-filters` | — | Desactiva todos los filtros deterministas (v1.2–v1.4 y v1.6): análisis de ablación, normalmente con `--replay` |

Cada ejecución deja en `<out>/pactgen/` el **manifiesto** (modelo, temperatura, hashes SHA-256 del prompt y del OpenAPI, versiones de Java, sistema operativo, swagger-parser y Jackson, y por operación: tokens, costo, intentos, interacciones descartadas, cabeceras eliminadas y el modelo que declara la propia respuesta de la API) y las **respuestas crudas** del modelo (`raw/`).

**Proyecto consumidor.** Pact JVM 4.6.17 arrastra `junit-jupiter-api` 5.9.2, incompatible con los motores JUnit 5.10 o superiores. El `pom.xml` del consumidor debe importar `junit-bom` y declarar `junit-jupiter-api` y `junit-platform-commons` de forma explícita (ver `examples/spike-consumer/pom.xml`).

### `pactgen validate`: contrato frente al OpenAPI

```bash
java -jar pactgen-cli/target/pactgen.jar validate \
  --openapi validation/microservices/customers-service/openapi.yaml \
  --pact examples/spike-consumer/target/pacts            # un .json o una carpeta
```

| Opción | Descripción |
|---|---|
| `--openapi` | Documento OpenAPI 3.x |
| `--pact` | Contrato Pact (V4; también V3) o carpeta con contratos `*.json` |
| `--json <archivo>` | Escribe además el informe en JSON |
| `--strict` | Los avisos (campos o parámetros no declarados) cuentan como errores |

Comprueba, sin proveedor en marcha y por cada interacción HTTP:
- que el método y la ruta existen (las plantillas `{id}` se emparejan con valores concretos, y gana la más concreta);
- que el código de estado está declarado;
- que están los parámetros de consulta y las cabeceras obligatorios;
- que los cuerpos cumplen el esquema: campos obligatorios, tipos, `enum`, formatos `date` y `date-time` (acepta `Z`, `+00:00` y `+0000`), y `allOf`, `oneOf` y `anyOf`.

Los campos y los parámetros de consulta no declarados son avisos, y errores con `--strict`. Un `null` en un campo opcional no *nullable* es aviso, y en uno obligatorio, error.

La salida tiene una línea por interacción (`OK`, `AVISO` o `ERROR`, con los motivos debajo) y un total. El código de salida es 0 sin errores, 1 con errores y 2 si no se pueden leer los archivos.

**Qué no comprueba:**
- longitudes (`minLength`, `maxLength`, `minItems`, `maxItems`);
- patrones (`pattern`) y formatos distintos de `date` y `date-time` (`email`, `uuid`…);
- rangos numéricos (`minimum`, `maximum`, `multipleOf`);
- unicidad (`uniqueItems`);
- las reglas de coincidencia del contrato (solo valida los valores de ejemplo);
- la seguridad.

**Cuerpo de la petición en las respuestas de error**: solo se compara con el esquema cuando la respuesta esperada es de éxito (< 400). En una interacción como «alta sin `firstName` → 400», la petición inválida es lo que el contrato quiere comprobar. Esas interacciones llevan la nota «cuerpo de la petición no comprobado», y el total las cuenta aparte.

### `pactgen diff`: dos versiones de un OpenAPI

```bash
java -jar pactgen-cli/target/pactgen.jar diff --old openapi-v1.yaml --new openapi-v2.yaml [--json informe.json]
```

| Para el consumidor | Cambios |
|---|---|
| **Incompatibles** | operación eliminada o movida (misma `operationId` en otra ruta o método); código de éxito cambiado; campo de respuesta eliminado (o respuesta sin cuerpo); tipo o formato de un campo cambiado; campo obligatorio nuevo en la petición (o campo que pasa a obligatorio); parámetro nuevo obligatorio o que pasa a obligatorio; valor de `enum` eliminado en una respuesta |
| **Compatibles** | operación nueva; campo de respuesta nuevo; campo opcional nuevo en la petición; parámetro opcional nuevo; cambios de descripción |

Las operaciones se emparejan por método y ruta, y el nombre de las variables de ruta no cuenta: `/owners/{id}` es la misma que `/owners/{ownerId}`.

No se informa de cambios en longitudes, patrones, rangos, ejemplos ni códigos de error.

El código de salida es 0 sin incompatibles, 1 con alguno y 2 si no se puede leer un OpenAPI.

Las pruebas de `OpenApiDiffTest` reproducen con OpenAPI sintéticos los cinco tipos BC y los dos NB de la Tabla 8. La lista prerregistrada de mutantes no se reclasifica con `diff`.

### Plugin Maven (RF-09)

El plugin ofrece tres *goals*, que delegan en el mismo núcleo que la CLI:

| *Goal* | Fase por defecto | Equivale a |
|---|---|---|
| `pactgen:generate` | `generate-test-sources` | `pactgen generate`. Añade las pruebas generadas a las fuentes de prueba y falla el build si alguna operación no se genera |
| `pactgen:validate` | `verify` | `pactgen validate` sobre `target/pacts`. Falla el build si hay errores (`failOnError`) |
| `pactgen:diff` | — (no necesita proyecto) | `pactgen diff`. Falla el build si hay incompatibles (`failOnBreaking`) |

Configuración real del ejemplo `examples/plugin-consumer/pom.xml`, que regenera P1 en modo *replay*, sin clave ni coste. Primero hay que instalar el plugin con `mvn -B install` en la raíz del repositorio. Después, `mvn -f examples/plugin-consumer/pom.xml verify` genera las pruebas, las ejecuta y valida el contrato.

```xml
<plugin>
  <groupId>dev.pactgen</groupId>
  <artifactId>pactgen-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <configuration>
    <openapi>${corpus.dir}/validation/microservices/customers-service/openapi.yaml</openapi>
  </configuration>
  <executions>
    <execution>
      <id>generar</id>
      <goals><goal>generate</goal></goals>
      <configuration>
        <consumer>api-gateway</consumer>
        <provider>customers-service</provider>
        <include><operationId>createPet</operationId></include>
        <replay>${corpus.dir}/runs/2026-11-18_21-53-22_customers-service_haiku-4-5/raw</replay>
      </configuration>
    </execution>
    <execution>
      <id>validar</id>
      <goals><goal>validate</goal></goals>
    </execution>
  </executions>
</plugin>
```

Parámetros de `generate`, con los mismos valores por defecto que la CLI:

| Parámetro | Por defecto |
|---|---|
| `openapi`, `consumer`, `provider` | obligatorios |
| `include` | todas las operaciones |
| `outDir` | `target/generated-test-sources/pactgen` |
| `testPackage` | `pactgen.generated` |
| `llmProvider` | `anthropic` |
| `model` | `claude-haiku-4-5-20251001` |
| `temperature`, `maxTokens` | 0.2, 4096 |
| `baseUrl`, `apiKeyEnv` | según el proveedor, `ANTHROPIC_API_KEY` |
| `replay` | — |
| `maxRepairs` | 2 |
| `promptVersion` | `v1.1` |
| `tokenBudget` | 6000 |
| `noDedupe`, `noFilters` | `false` |
| `skip` | `false` |

Cada parámetro admite también la propiedad `-Dpactgen.<nombre>`.

`diff` se ejecuta sin proyecto:

```bash
mvn dev.pactgen:pactgen-maven-plugin:0.1.0-SNAPSHOT:diff -Dpactgen.old=v1.yaml -Dpactgen.new=v2.yaml
```

Comprobación del 21 nov: con las mismas respuestas crudas, el `.java` que genera el plugin es idéntico byte a byte al de la CLI (`CreatePetPactTest.java`, SHA-256 `6f5a42d7…`). `mvn verify` da 3/3 pruebas en verde y `validate` 3/3 OK.

## Corpus de evaluación

| # | Proveedor | Consumidor real | Proyecto (commit) | Ops. |
|---|---|---|---|---|
| P1 | customers-service | api-gateway | spring-petclinic-microservices (v3.4.1, f9fd559) | 8 |
| P2 | visits-service | api-gateway | spring-petclinic-microservices (v3.4.1, f9fd559) | 3 |
| P3 | account-service | notification-service | piggymetrics (6bb2cf9) | 4 |
| P4 | statistics-service | account-service | piggymetrics (6bb2cf9) | 3 |
| P5 | auth-service | account-service | piggymetrics (6bb2cf9) | 2 |

Los documentos OpenAPI (`validation/microservices/`) se redactaron a partir de los controladores y las clases de dominio de cada commit; cada uno explica en su descripción las decisiones tomadas (seguridad fuera del contrato, códigos reales como el 500 de auth-service, etc.).

**Piloto (18 nov 2026, prompt v1.1, herramienta v1.3):** 20/20 operaciones generadas y 60/60 pruebas en verde; ≈ USD 0,14 por ejecución del corpus completo. Las pruebas de P2, P3 y P5 se ejecutaron a ciegas (solo totales) para no contaminar la condición manual del experimento.

## Verificación contra los proveedores

`validation/provider-verification/` verifica los contratos contra el código real de cada proveedor, a nivel de controlador (`@WebMvcTest` + Pact JVM `junit5spring`), con los repositorios simulados mediante datos derivados de cada contrato. piggymetrics (P3–P5, Spring Boot 2.0) se verifica sobre un portado fiel de sus controladores a Spring Boot 3.4.1 (`validation/provider-verification/piggymetrics/`, ver su `NOTICE`). Sirve para validar el corpus antes del experimento (línea base) y para medir la detección de cambios incompatibles (mutantes). Ver su README.

```powershell
.\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts examples\spike-consumer\target\pacts
.\validation\provider-verification\verificar.ps1 -Proveedor P3 -Pacts <carpeta temporal> -Ciego   # grupo A: solo totales
```

La línea base de customers-service reveló una inconsistencia del propio petclinic (`POST /owners/{id}/pets` serializa `birthDate` con fecha y hora; las lecturas, solo con fecha) y motivó las versiones 1.4 y 1.5.

## Experimento

`experiment/` contiene el protocolo de comparación con la escritura manual (diseño contrabalanceado, sorteo reproducible con semilla 20261128, guion de cada bloque, plantillas de registro y consentimiento del evaluador). Ver `experiment/README.md`.

`runs/` es la bitácora de las ejecuciones de desarrollo: cada carpeta contiene el manifiesto, las respuestas crudas, las pruebas generadas y un `NOTAS.md` con el resultado y las decisiones que motivó.

## Historial de versiones

| Versión | Fecha | Cambio | Motivo (evidencia en `runs/`) |
|---|---|---|---|
| 0.1 (I0) | 17 nov | Cadena completa OpenAPI → modelo → prueba → contrato V4 | Prueba de concepto con `findOwner` |
| prompt v1.1 | 17 nov | Sin Markdown; un comportamiento por interacción; máximo 4 | Interacciones redundantes en customers-service. Se cumplió solo en parte |
| 1.2 | 17 nov | **Filtro de redundantes**: se descartan interacciones con la misma firma (método, consulta, forma de los cuerpos y estado) | La regla del prompt no era fiable: 26 → 22 interacciones |
| 1.3 | 18 nov | **Filtro de cabeceras** no declaradas (salvo `Content-Type` y `Accept`) y **cuerpos exactos** en las peticiones con respuesta de error | statistics-service: `Authorization` inventada; errores 400 expresados con reglas por tipo |
| 1.4 | 18 nov | **Reglas de coincidencia guiadas por el esquema**: `date` → fecha; otro `string` → texto; `integer` → entero; `number` → número | El tipo del contrato dependía del valor de ejemplo del modelo |
| 1.5 | 18 nov | **Causa de cada código** en el contexto (descripción de la respuesta) | El modelo usó el 400 de `createPet` para una causa no declarada |
| 1.6 | 20 nov | Regla ISO 8601 para `date-time`; se conservan las cabeceras exigidas por `securitySchemes`; versiones del entorno y modelo declarado por la API en el manifiesto; opción `--no-filters` | Revisión externa del diseño (18 nov) |
| 1.7 | 20 nov | Las peticiones con cuerpo declaran `Content-Type: application/json; charset=UTF-8` | Línea base de visits-service: un cuerpo exacto con tildes no coincidía porque el servidor simulado de Pact lo decodificaba como ISO-8859-1 |
| 1.8 | 21 nov | Comandos **`validate`** (contrato frente al OpenAPI) y **`diff`** (cambios incompatibles entre versiones del OpenAPI); plugin Maven con *goals* `generate`, `validate` y `diff`, probado en *replay* (`examples/plugin-consumer`). **La generación no cambia** | RF-08 y RF-09 (*Must*). No regresión: con `--replay`, los `.java` de P1 (8) y P4 (3) son idénticos a los de la v1.7, y el manifiesto solo cambia en `tool` |

Las respuestas archivadas permiten reprocesar ejecuciones anteriores con una versión nueva sin coste (`--replay`).

## Estructura

```
pactgen-core/                      núcleo (M1–M4); prompts en src/main/resources/prompts/{v1,v1.1}
pactgen-cli/                       CLI
pactgen-maven-plugin/              plugin Maven
examples/spike-consumer/           proyecto consumidor donde se generan y ejecutan las pruebas
examples/plugin-consumer/          ejemplo del plugin Maven (P1 createPet en replay)
validation/microservices/          OpenAPI del corpus (P1–P5)
validation/provider-verification/  verificación de contratos contra los proveedores
validation/breaking-changes/       catálogo de cambios incompatibles (mutantes)
experiment/                        protocolo, asignación, plantillas y datos del experimento
runs/                              bitácora de ejecuciones de desarrollo
```

## Pendiente

- Experimento con la herramienta congelada (`experimento-v1`): sesiones del 28 nov al 6 dic
- Publicación en Pact Broker (RF-10)

## Licencia

MIT
