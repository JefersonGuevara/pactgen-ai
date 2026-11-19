# Experimento de evaluación de PactGen-AI

Materiales del experimento descrito en la sección 3.4 de la memoria: comparación de PactGen-AI con la escritura manual de pruebas de contrato sobre cinco microservicios (P1–P5, 20 operaciones).

| Archivo o carpeta | Contenido |
|---|---|
| `asignacion.md` | Grupos, calendario y orden aleatorio de los bloques (semilla 20261128) |
| `aleatorizar.py` | Script que reproduce el orden aleatorio |
| `PROTOCOLO_SESION.md` | Guion paso a paso de cada bloque y reglas del experimento |
| `preparar.ps1` | Crea el proyecto consumidor de un bloque y muestra los comandos |
| `plantilla-consumidor/` | `pom.xml` base (Pact JVM 4.6.17, JUnit 5.11.3, Java 17) |
| `workspace/s<n>/<Px>-<condición>/` | Pruebas escritas a mano o generadas en cada bloque (se versionan) |
| `datos/registro_tiempos.csv` | Tiempos y métricas por bloque y ejecución |
| `datos/runs/` | Bitácora de cada ejecución de la herramienta (`--archive`): manifiesto, respuestas crudas, pruebas |
| `evaluacion/rubrica_Qsem.csv` | Puntuaciones de calidad semántica (Anexo C) |
| `evaluacion/consentimiento_evaluador.md` | Consentimiento del segundo evaluador |

## Configuración congelada

Antes de la sesión 1 se crea la etiqueta `experimento-v1`. Desde ese momento no cambia:

| Parámetro | Valor |
|---|---|
| Modelo | `claude-haiku-4-5-20251001` |
| Temperatura / tokens máximos / reparaciones | 0,2 / 4096 / 2 |
| Prompt | v1.1 (hash SHA-256 en cada manifiesto) |
| Filtro de interacciones redundantes | Activo (v1.2) |
| Pact JVM / JUnit | 4.6.17 / 5.11.3 |

## Decisiones de análisis

- **Ejecución revisada.** De las tres ejecuciones por proveedor, la primera es la que se revisa y cronometra; sus pruebas revisadas son las que se usan para Q_sem y BC_recall frente a la condición manual.
- **Variabilidad.** Las ejecuciones 2 y 3 se lanzan sin revisión humana; COV_op, TC_rate y el costo se reportan con media y desviación típica de las tres, y la comparación principal usa la mediana por operación (sección 3.4.7).
- **P1 como conjunto de desarrollo.** El prompt se ajustó con customers-service (P1); por eso P1 está en el grupo B (herramienta primero) y los resultados se reportan con y sin P1. Antes del experimento no se abren las pruebas generadas para P2, P3 y P5.
- **Mutantes.** Se aplican después de la sesión 2, sobre las pruebas ya congeladas de ambas condiciones (`validation/breaking-changes/`).
