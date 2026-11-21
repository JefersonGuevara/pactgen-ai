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

## Implementación (21 nov 2026)

```
generar_parches.py     genera los 35 parches (<servicio>/<ID>.patch) a partir de sustituciones sobre el código
<servicio>/<ID>.patch  P1 y P2: relativos a la raíz del clon de petclinic (v3.4.1); P3–P5: relativos al portado
                       (validation/provider-verification/piggymetrics)
referencia/            contratos de referencia escritos a partir del OpenAPI (generar.py) y su comprobación
resultados/            CSV de mutar.ps1 (referencia y contratos de desarrollo de P1 y P4)
mutar.ps1              línea base + cada mutante: aplica, verifica y revierte (siempre)
```

```powershell
# desde la raíz de pactgen-ai
.\validation\breaking-changes\mutar.ps1 -Proveedor P1 -Pacts <carpeta> [-Mutante BC2] [-Salida <csv>]
.\validation\breaking-changes\mutar.ps1 -Proveedor P3 -Pacts <carpeta temporal> -Ciego -Salida <csv>   # grupo A
```

`mutar.ps1` llama a `verificar.ps1 -Parche <ID>.patch -Resultado <csv>`. El parche se aplica después de preparar el proveedor (en P1 y P2, tras el `checkout --force` del clon) y se revierte en un bloque `finally`. En P3–P5, `verificar.ps1` se niega a ejecutar si el portado tiene cambios sin confirmar en `src/`. El CSV tiene las columnas `proveedor, mutante, tipo, interacciones, base_ok, fallan_con_mutante, detectado` (`detectado = error` si el parche no se aplica o no compila). Con `-Ciego`, en pantalla solo aparece la tabla de totales; sin `-Ciego`, también la posición y la descripción de las interacciones que fallan.

### Cómo se implementó cada tipo

| Tipo | Implementación |
|---|---|
| BC1 (campo eliminado) | `@JsonIgnore` en el campo (P1–P4). P5: el campo es de `Authentication` (Spring Security), que no se puede anotar; el controlador devuelve el mismo JSON (`ObjectMapper.convertValue` con el `ObjectMapper` de la aplicación) sin `name` |
| BC2 (tipo cambiado) | `@JsonSerialize(using = ToStringSerializer.class)` (P1, P2, P4); `@JsonFormat(shape = NUMBER)` en `lastSeen` (P3); P5, como BC1, con `authenticated` convertido a texto |
| BC3 (200 → 204) | `@ResponseStatus(HttpStatus.NO_CONTENT)` en el método del controlador |
| BC4 (campo obligatorio nuevo) | Campo con `@NotBlank`/`@NotNull` en el DTO o la entidad de la petición. P2: `@Transient` y `@JsonProperty(access = WRITE_ONLY)`, para que solo cambie la petición y no la respuesta |
| BC5 (ruta cambiada) | Cambio de la ruta del método. P5: la ruta `/users` de la clase pasa a los métodos (`/users/current` y `/accounts`), para que solo cambie la del POST |
| NB1 (campo nuevo) | Getter calculado (`getFullName`, `getYear`, `getItemCount`); P5, como BC1, añadiendo `clientId` |
| NB2 (operación nueva) | Método nuevo en el controlador. P5 añade `long count()` a `UserService`, que en la verificación es un simulador |

Ninguno de los 35 mutantes se apartó de la lista prerregistrada. Las implementaciones de P5 por `convertValue` son la alternativa más próxima a una anotación cuando el objeto serializado pertenece a una biblioteca.

## Resultados (21 nov 2026)

**No equivalencia** (contratos de referencia, `resultados/referencia-P*.csv`): en los cinco proveedores, los cinco BC hacen fallar al menos una de las dos interacciones de referencia y ningún NB hace fallar ninguna (25/25 BC observables, 0/10 NB).

**Contratos de desarrollo** (grupo B; piloto regenerado con `--replay` y la herramienta v1.7; `resultados/desarrollo-P*.csv`):

| Proveedor | Interacciones (línea base) | BC detectados | Falsos positivos (NB) |
|---|---|---|---|
| P1 customers-service | 22 (22 verificadas) | 5/5 | 0/2 |
| P4 statistics-service | 9 (9 verificadas) | 5/5 | 0/2 |

**Grupo A, a ciegas** (contratos generados en carpetas temporales; solo totales; los CSV quedan junto a los contratos, fuera del repositorio):

| Proveedor | Interacciones (línea base) | BC detectados | Falsos positivos (NB) |
|---|---|---|---|
| P3 account-service | 12 (12 verificadas) | 5/5 | 0/2 |
| P5 auth-service | 5 (5 verificadas) | 5/5 | 0/2 |
| P2 visits-service | pendiente: no hay contratos generados de P2 | — | — |
