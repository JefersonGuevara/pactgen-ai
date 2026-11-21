# Mutantes del experimento

Memoria, secciones 3.4.4 y 3.4.6 (Tabla 8). Se inyectan en cada proveedor cinco cambios incompatibles (BC1–BC5) y dos compatibles de control (NB1–NB2): 25 incompatibles y 10 compatibles en total.

## Prerregistro (20 nov 2026)

La lista se fijó **antes** de verificar ningún conjunto de contratos contra mutantes y no se modifica a la vista de los resultados. Cualquier desviación (un mutante que no pueda implementarse tal cual) se documenta en este archivo con su motivo y fecha.

Criterios:

- Los incompatibles afectan a elementos que usa el consumidor real. Cuando un tipo no tiene un elemento adecuado en la operación del consumidor (por ejemplo, BC3 exige un GET y el consumidor de P4 solo usa un PUT), se usa la operación más próxima del mismo servicio.
- Cada mutante es un parche mínimo (`<servicio>/<ID>.patch`) sobre el código del proveedor en el commit fijado (P1 y P2) o sobre el portado de piggymetrics (P3–P5).
- **Detección**: un mutante se detecta si al menos una interacción que se verifica en la línea base falla con el mutante. Un NB que hace fallar una interacción verificada en la línea base es un falso positivo.
- **No equivalencia**: cada BC hace fallar al menos una interacción de los contratos de referencia (`referencia/`, escritos a partir del OpenAPI) y ningún NB lo hace.

| ID | P1 customers-service (consumidor: api-gateway, `GET /owners/{ownerId}`) |
|---|---|
| BC1 | `GET /owners/{ownerId}`: `lastName` deja de aparecer en la respuesta (`@JsonIgnore` en `Owner.lastName`) |
| BC2 | `GET /owners/{ownerId}`: `id` del propietario pasa de entero a texto |
| BC3 | `GET /owners/{ownerId}`: 200 → 204 |
| BC4 | `POST /owners`: nuevo campo obligatorio `email` en `OwnerRequest` (`@NotBlank`) |
| BC5 | `GET /owners/{ownerId}` → `GET /owners/{ownerId}/profile` |
| NB1 | Respuestas de `Owner`: campo nuevo `fullName` («nombre apellido») |
| NB2 | Operación nueva `GET /owners/count` |

| ID | P2 visits-service (consumidor: api-gateway, `GET /pets/visits`) |
|---|---|
| BC1 | `GET /pets/visits`: `petId` deja de aparecer en cada visita |
| BC2 | `GET /pets/visits`: `id` de la visita pasa de entero a texto |
| BC3 | `GET /pets/visits`: 200 → 204 |
| BC4 | `POST /owners/{ownerId}/pets/{petId}/visits`: nuevo campo obligatorio `vet` en el cuerpo (`@NotNull`, transitorio) |
| BC5 | `GET /pets/visits` → `GET /pets/visit-history` |
| NB1 | Respuestas de `Visit`: campo nuevo `year` (año de `date`) |
| NB2 | Operación nueva `GET /visits/count` |

| ID | P3 account-service (consumidor: notification-service, `GET /accounts/{name}`) |
|---|---|
| BC1 | `GET /accounts/{name}`: `name` deja de aparecer en la respuesta |
| BC2 | `GET /accounts/{name}`: `lastSeen` pasa de texto (ISO 8601) a número (epoch) |
| BC3 | `GET /accounts/{name}`: 200 → 204 |
| BC4 | `POST /accounts/`: nuevo campo obligatorio `email` en `User` (`@NotNull`) |
| BC5 | `GET /accounts/{name}` → `GET /accounts/by-name/{name}` |
| NB1 | Respuestas de `Account`: campo nuevo `itemCount` (número de ingresos + gastos) |
| NB2 | Operación nueva `GET /accounts/currencies` |

| ID | P4 statistics-service (consumidor: account-service, `PUT /statistics/{accountName}`) |
|---|---|
| BC1 | `GET /statistics/{accountName}`: `rates` deja de aparecer en cada punto (la operación del consumidor no tiene cuerpo de respuesta) |
| BC2 | `GET /statistics/{accountName}`: `amount` de `ItemMetric` pasa de número a texto |
| BC3 | `GET /statistics/{accountName}`: 200 → 204 |
| BC4 | `PUT /statistics/{accountName}`: nuevo campo obligatorio `active` en `Account` (`@NotNull`) |
| BC5 | `PUT /statistics/{accountName}` → `PUT /statistics/accounts/{accountName}` |
| NB1 | Respuestas de `DataPoint`: campo nuevo `itemCount` |
| NB2 | Operación nueva `GET /statistics/count` |

| ID | P5 auth-service (consumidor: account-service, `POST /uaa/users`) |
|---|---|
| BC1 | `GET /uaa/users/current`: `name` deja de aparecer en la respuesta (la operación del consumidor no tiene cuerpo de respuesta) |
| BC2 | `GET /uaa/users/current`: `authenticated` pasa de booleano a texto |
| BC3 | `GET /uaa/users/current`: 200 → 204 |
| BC4 | `POST /uaa/users`: nuevo campo obligatorio `email` en `User` (`@NotNull`) |
| BC5 | `POST /uaa/users` → `POST /uaa/accounts` |
| NB1 | `GET /uaa/users/current`: campo nuevo `clientId` en la respuesta |
| NB2 | Operación nueva `GET /uaa/users/count` |
