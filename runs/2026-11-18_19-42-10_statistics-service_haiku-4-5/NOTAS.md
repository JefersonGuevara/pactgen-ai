# Ejecución 2026-11-18_19-42-10 — statistics-service (P4)

- Modelo: claude-haiku-4-5-20251001 (temperatura 0.2)
- Prompt: v1.1 | Herramienta: v1.2 (filtro de duplicados activo)
- Operaciones: 3 | generadas: 3 (todas al primer intento, sin reparaciones)
- Interacciones: 9 (redundantes descartadas: 2)
- Costo estimado: USD 0.0287 (≈ 0.0096 por operación; esquemas más grandes que en customers-service)
- Manifiesto: manifest-2026-11-19T00-41-40.493624500Z.json

## Resultado de las pruebas (mvn test)

- Pruebas: 9/9 pasan con Pact JVM 4.6.17. Contratos Pact V4.

## Observaciones

- getStatisticsByAccountName: buenos casos límite (cuenta con datos, sin datos y solo con gastos).
- getCurrentAccountStatistics: el modelo inventó la cabecera `Authorization: Bearer valid-token`, aunque el OpenAPI indica que la autenticación no forma parte del contrato.
- saveAccountStatistics: los dos 400 (título vacío; falta `saving`) se generaron con reglas por tipo en el cuerpo de la petición, de modo que el contrato no distingue la petición inválida de la válida.
- Decisión (v1.3): filtro determinista que elimina las cabeceras de petición no declaradas en el OpenAPI (salvo Content-Type y Accept) y cuerpos de petición exactos en las interacciones con respuesta 4xx/5xx.
- Reprocesada en modo replay con v1.3: se eliminan las 4 cabeceras Authorization y los 400 quedan con cuerpo exacto, sin coste.
