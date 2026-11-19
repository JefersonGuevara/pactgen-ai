# Asignación y orden de las sesiones

Diseño contrabalanceado (memoria, sección 3.4.3).

> **Ajuste del protocolo (18 nov 2026, antes de recoger datos).** P1 y P2 intercambian grupo respecto a la versión de la Entrega 2. Motivo: las pruebas generadas para P1 (customers-service) se revisaron durante el desarrollo (17 nov) y el prompt se ajustó con ellas; con P1 en el grupo A, la condición manual de P1 se escribiría después de haber visto la salida de la herramienta. En el grupo B esa exposición previa es la que el diseño contrabalanceado ya contempla. Además, el reparto de operaciones queda más equilibrado (A: 9, B: 11; antes A: 14, B: 6). Los resultados se reportan también sin P1 (análisis de sensibilidad, 12 operaciones), porque P1 se usó para ajustar el prompt. Semilla registrada: **20261128** (`aleatorizar.py`).

| Grupo | Proveedores | Sesión 1 | Sesión 2 |
|---|---|---|---|
| A | P2, P3, P5 | Manual | PactGen-AI |
| B | P1, P4 | PactGen-AI | Manual |

Salida de `python aleatorizar.py`:

```
Sesión 1: P3 (manual), P4 (pactgen), P5 (manual), P2 (manual), P1 (pactgen)
Sesión 2: P4 (manual), P5 (pactgen), P3 (pactgen), P1 (manual), P2 (pactgen)
```

## Calendario

Cada bloque (un proveedor en una condición) dura como máximo 90 minutos, con al menos 15 minutos de pausa entre bloques.

| Fecha | Sesión | Bloques en orden |
|---|---|---|
| Sáb 28 nov 2026 | 1 | P3 manual · P4 PactGen-AI · P5 manual |
| Dom 29 nov 2026 | 1 | P2 manual · P1 PactGen-AI |
| Sáb 5 dic 2026 | 2 | P4 manual · P5 PactGen-AI · P3 PactGen-AI |
| Dom 6 dic 2026 | 2 | P1 manual · P2 PactGen-AI |

Separación entre condiciones de un mismo proveedor: 7 días en todos los casos (P3, P4, P5: 28 nov → 5 dic; P1, P2: 29 nov → 6 dic).

| # | Proveedor | Consumidor real | Ops. |
|---|---|---|---|
| P1 | customers-service | api-gateway | 8 |
| P2 | visits-service | api-gateway | 3 |
| P3 | account-service | notification-service | 4 |
| P4 | statistics-service | account-service | 3 |
| P5 | auth-service | account-service | 2 |
