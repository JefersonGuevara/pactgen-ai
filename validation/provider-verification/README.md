# Verificación de los contratos contra los proveedores

Sirve para medir BC_recall y FP_rate (memoria, secciones 3.4.4 y 3.4.6): un contrato **detecta** un mutante si se verifica correctamente contra el proveedor sin mutar (línea base) y falla contra el proveedor mutado.

## Diseño

- **Nivel de controlador.** Cada proveedor se verifica con una prueba `@WebMvcTest` de Pact JVM (`junit5spring` 4.6.17) añadida al proyecto original (P1, P2) o a un portado fiel de sus controladores (P3–P5, ver más abajo). Se cargan los controladores, los DTO, la validación y la serialización JSON reales; los repositorios o servicios se simulan con Mockito. No hace falta base de datos, registro de servicios, servidor de configuración ni OAuth2.
- **Datos derivados del contrato** (`common/ContractFixtures.java`). Los estados del proveedor de los contratos están en lenguaje natural (los escribe el modelo o el autor) y no se pueden programar por adelantado. Antes de cada interacción, los simuladores devuelven entidades construidas por reflexión a partir de la respuesta esperada (con el constructor vacío o, si la clase no lo tiene, con el de menos parámetros). Un campo enum cuyo valor de ejemplo no es una constante del enum recibe la primera constante: el proveedor real solo puede devolver constantes, y la regla del contrato decide (por tipo, pasa; por valor exacto, falla). Los errores que dependen de los datos del servicio (p. ej., «la cuenta ya existe») se simulan solo cuando el contrato espera ese código y su estado lo describe; los de datos inválidos los produce la validación real. Para ser fieles al comportamiento real: las búsquedas devuelven fechas `@Temporal(DATE)` como `java.sql.Date` (como JPA) y `save()` devuelve la misma entidad recibida con el id generado.
- **Tolerancia a mutantes.** Si un mutante elimina un campo o cambia su tipo, la construcción lo deja vacío y es la serialización real del proveedor la que revela la incompatibilidad.
- **Alcance.** Se verifica la interfaz (rutas, códigos, validación, forma y tipos del JSON), que es lo que cubren los contratos y donde actúan los cinco tipos de cambio incompatible (Tabla 8). No se verifica la persistencia ni la lógica de negocio.

## Uso

```powershell
# desde la raíz de pactgen-ai; P1 y P2 clonan el proveedor en ..\providers la primera vez
.\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts <carpeta con los .json de Pact>
.\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts <carpeta> -Rama bc/customers-service/BC1
# P3–P5: portado local de piggymetrics, sin clonar ni cambiar de rama
.\validation\provider-verification\verificar.ps1 -Proveedor P4 -Pacts <carpeta>
.\validation\provider-verification\verificar.ps1 -Proveedor P3 -Pacts <carpeta> -Ciego
```

Si la carpeta no contiene contratos del proveedor indicado, el script lo dice y termina con código 2.

**Modo a ciegas** (`-Ciego`): para los proveedores del grupo A (P2, P3, P5), cuyas pruebas generadas el autor no debe ver antes del experimento, la salida de Maven se guarda en `target/verificacion.log` del proveedor (en P3–P5, `piggymetrics/target/verificacion.log`) y en pantalla solo aparecen los totales. Si hay fallos, el registro lo revisa un tercero, y solo se corrige el OpenAPI o el arnés, nunca las pruebas.

## Estado

| Proveedor | Arnés |
|---|---|
| P1 customers-service | `customers-service/CustomersContractVerificationTest.java` |
| P2 visits-service | `visits-service/VisitsContractVerificationTest.java` |
| P3 account-service | `piggymetrics/src/test/java/com/piggymetrics/account/controller/AccountContractVerificationTest.java` |
| P4 statistics-service | `piggymetrics/src/test/java/com/piggymetrics/statistics/controller/StatisticsContractVerificationTest.java` |
| P5 auth-service | `piggymetrics/src/test/java/com/piggymetrics/auth/controller/AuthContractVerificationTest.java` |

## piggymetrics (P3–P5): portado a Spring Boot 3.4.1

piggymetrics (commit 6bb2cf9) usa Spring Boot 2.0 y Java 8, y Pact JVM 4.6 exige Java 17 y Spring 6. Decisión (20 nov): **portar** a un proyecto Maven independiente (`piggymetrics/`) solo lo que un contrato verifica, es decir, los controladores, el manejo de errores, el dominio y las interfaces de servicio. Se conservan los paquetes, los nombres, las rutas, las anotaciones de validación y el manejo de errores.

- `piggymetrics/portar.ps1` regenera `src/main/java` desde el clon (`..\providers\piggymetrics`) aplicando solo cuatro adaptaciones: `javax` → `jakarta`, sin `@PreAuthorize`, sin anotaciones de MongoDB y sin la anotación inerte de Jackson 1.
- `piggymetrics/src/test/java/dev/pactgen/verification/Boot20.java` reproduce lo que Boot 2.0 hacía distinto y se ve en el contrato: los context paths (`/accounts`, `/statistics`, `/uaa`), las fechas `…000+0000` de Jackson 2.9, la barra final opcional y el cuerpo JSON de la página de error (validación y 500 no manejado). También inyecta el `Principal`.
- `piggymetrics/NOTICE` recoge la licencia MIT de piggymetrics, lo portado y todas las adaptaciones.
- **Amenaza a la validez**: se verifica una adaptación fiel de los controladores, no el binario original.

Sin contratos en `piggymetrics/target/pacts-to-verify`, `mvn -f validation\provider-verification\piggymetrics\pom.xml test` compila y omite las tres clases (`@IgnoreNoPactsToVerify`).

## Línea base

| Proveedor | Fecha | Contratos | Resultado |
|---|---|---|---|
| P1 customers-service | 20 nov | piloto (8 operaciones, *replay*, herramienta v1.6) | 22/22 interacciones verificadas |
| P4 statistics-service | 20 nov | piloto (3 operaciones, *replay*, herramienta v1.6) | 9/9 interacciones verificadas |
| P3 account-service | 20 nov | generados en carpeta temporal (4 operaciones, herramienta v1.7); a ciegas | 12/12 interacciones verificadas |
| P5 auth-service | 20 nov | generados en carpeta temporal (2 operaciones, herramienta v1.7); a ciegas | 5/5 interacciones verificadas |
| P2 visits-service | — | pendiente | a ciegas (`-Ciego`) |

En la primera pasada a ciegas, P3 dio 11/12 y P5 4/5. Los dos fallos eran del arnés y se corrigieron en él, sin tocar los contratos: (1) un valor de ejemplo fuera de un enum quedaba como `null` en los datos de prueba (corregido en `ContractFixtures`); (2) el simulador de `UserService` no reproducía las precondiciones de `UserServiceImpl.create`, que responden 500 si falta `username` o `password` (corregido en `AuthContractVerificationTest`). Tras la corrección, P1 (22/22) y P4 (9/9) siguen verificándose.
