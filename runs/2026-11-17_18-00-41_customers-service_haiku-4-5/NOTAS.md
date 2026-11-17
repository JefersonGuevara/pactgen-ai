# Ejecución 2026-11-17_18-00-41 — customers-service

- Modelo: claude-haiku-4-5-20251001 (temperatura 0.2)
- Prompt: v1.1
- Operaciones: 8 | generadas: 8 (todas al primer intento, sin reparaciones)
- Interacciones: 26
- Costo estimado: USD 0.0467
- Manifiesto: manifest-2026-11-17T23-00-04.755742900Z.json

## Resultado de las pruebas (mvn test)

- Pruebas: 26/26 pasan con Pact JVM 4.6.17. Contratos Pact V4.

## Comparación con la ejecución anterior (prompt v1, 2026-11-17)

| Aspecto | v1 | v1.1 |
|---|---|---|
| Respuestas envueltas en bloque Markdown | 8/8 | 8/8 |
| Interacciones redundantes (mismo estado y forma) | ~6 | ~4 |
| getPetTypes | 3 interacciones | 2 |
| findOwner | 3 | 4 |
| Costo | USD 0.0451 | USD 0.0467 |

## Observaciones

- Las reglas nuevas del prompt (sin Markdown, un comportamiento por interacción, máximo 4) se cumplieron solo en parte: el modelo siguió usando bloques de código y mantuvo variantes que solo cambian datos (segundo 201 en createPet, segundo 204 en updatePet).
- findOwner añadió GET /owners/999 -> 400 con el estado "no existe el propietario": es semánticamente incorrecto (el 400 corresponde a un id inválido; un id inexistente devuelve 200 con cuerpo vacío en petclinic).
- Con una sola ejecución no se puede atribuir la diferencia al prompt, por el no determinismo del modelo.
- Decisión: resolver la redundancia con un filtro determinista de duplicados (firma = método, plantilla de ruta, estado y forma de los cuerpos) en lugar de reglas en lenguaje natural; tratar los bloques Markdown como paso de normalización del validador.
