# Verificación de los contratos contra los proveedores

Sirve para medir BC_recall y FP_rate (memoria, secciones 3.4.4 y 3.4.6): un contrato **detecta** un mutante si se verifica correctamente contra el proveedor sin mutar (línea base) y falla contra el proveedor mutado.

## Diseño

- **Nivel de controlador.** Cada proveedor se verifica con una prueba `@WebMvcTest` de Pact JVM (`junit5spring` 4.6.17) añadida al proyecto original. Se cargan los controladores, los DTO, la validación y la serialización JSON reales; los repositorios o servicios se simulan con Mockito. No hace falta base de datos, registro de servicios, servidor de configuración ni OAuth2.
- **Datos derivados del contrato** (`common/ContractFixtures.java`). Los estados del proveedor de los contratos están en lenguaje natural (los escribe el modelo o el autor) y no se pueden programar por adelantado. Antes de cada interacción, los simuladores devuelven entidades construidas por reflexión a partir de la respuesta esperada. Para ser fieles al comportamiento real: las búsquedas devuelven fechas `@Temporal(DATE)` como `java.sql.Date` (como JPA) y `save()` devuelve la misma entidad recibida con el id generado.
- **Tolerancia a mutantes.** Si un mutante elimina un campo o cambia su tipo, la construcción lo deja vacío y es la serialización real del proveedor la que revela la incompatibilidad.
- **Alcance.** Se verifica la interfaz (rutas, códigos, validación, forma y tipos del JSON), que es lo que cubren los contratos y donde actúan los cinco tipos de cambio incompatible (Tabla 8). No se verifica la persistencia ni la lógica de negocio.

## Uso

```powershell
# desde la raíz de pactgen-ai; clona el proveedor en ..\providers la primera vez
.\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts <carpeta con los .json de Pact>
.\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts <carpeta> -Rama bc/customers-service/BC1
```

## Estado

| Proveedor | Arnés |
|---|---|
| P1 customers-service | `customers-service/CustomersContractVerificationTest.java` |
| P2 visits-service | pendiente |
| P3–P5 (piggymetrics, Spring Boot 2.0) | pendiente: requiere decidir entre ejecutar en Java 8/Pact 4.1 o portar los controladores |
