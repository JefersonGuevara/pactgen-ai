# Ejecución 2026-11-17 — findOwner con Claude Haiku 4.5

- Objetivo: primera generación real (iteración I0/I1).
- Operación: findOwner (GET /owners/{ownerId}), customers-service.
- Modelo: claude-haiku-4-5-20251001, temperatura 0.2, prompt v1.
- Resultado: 1 intento, sin reparaciones, 3 interacciones, ~5 s.
- Tokens: 2073 de entrada / 723 de salida. Costo: USD 0.0057.
- Pruebas: 3/3 pasan con Pact JVM 4.6.17. Contrato Pact V4.

## Observaciones

- No inventó un 404: el OpenAPI no lo declara (petclinic devuelve 200 con cuerpo vacío).
- Incluyó un caso límite: propietario sin mascotas (pets vacío).
- El modelo envolvió el JSON en un bloque de código; el validador lo limpió.
- El patrón de casos se parece al de los ejemplos few-shot (posible amenaza a la validez).
