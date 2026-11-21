"""Contratos de referencia para comprobar la no equivalencia de los mutantes (Tabla 8).

Se escriben a partir del OpenAPI de cada proveedor (validation/microservices/<servicio>/openapi.yaml), no de los
contratos generados. Cubren, para cada proveedor, la operación que leen BC1-BC3 y BC5 (un GET con cuerpo) y la que
recibe el campo obligatorio nuevo de BC4 (o la ruta nueva de BC5, en P4 y P5). Cada BC debe hacer fallar al menos una
interacción y ningún NB debe hacerlo.

Uso: python -I validation/breaking-changes/referencia/generar.py   (escribe referencia/P1..P5/*.json)
"""
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
CONSUMIDOR = "referencia"


def regla(match, **kw):
    return {"combine": "AND", "matchers": [{"match": match, **kw}]}


TIPO = regla("type")
ENTERO = regla("integer")
NUMERO = regla("number")
FECHA = regla("regex", regex=r"\d{4}-\d{2}-\d{2}")
FECHA_HORA = regla("regex", regex=r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:?\d{2})")


def cuerpo(content):
    return {"content": content, "contentType": "application/json", "encoded": False}


def interaccion(desc, estado, metodo, ruta, status, req=None, res=None, reglas=None, query=None):
    request = {"method": metodo, "path": ruta}
    if query:
        request["query"] = query
    if req is not None:
        request["headers"] = {"Content-Type": ["application/json"]}
        request["body"] = cuerpo(req)
    response = {"status": status}
    if res is not None:
        response["headers"] = {"Content-Type": ["application/json"]}
        response["body"] = cuerpo(res)
        if reglas:
            response["matchingRules"] = {"body": reglas}
    return {"type": "Synchronous/HTTP", "key": desc, "description": desc, "providerStates": [{"name": estado}],
            "request": request, "response": response}


def pacto(proveedor, interacciones):
    return {"consumer": {"name": CONSUMIDOR}, "provider": {"name": proveedor}, "interactions": interacciones,
            "metadata": {"pactSpecification": {"version": "4.0"}, "pactgen": {"origen": "referencia (OpenAPI)"}}}


# ---------------------------------------------------------------- P1 customers-service
owner_req = {"firstName": "George", "lastName": "Franklin", "address": "110 W. Liberty St.", "city": "Madison",
             "telephone": "6085551023"}
owner = {"id": 1, **owner_req, "pets": []}
owner_reglas = {"$.id": ENTERO, "$.firstName": TIPO, "$.lastName": TIPO, "$.address": TIPO, "$.city": TIPO,
                "$.telephone": TIPO}
P1 = pacto("customers-service", [
    interaccion("GET /owners/{ownerId}: propietario existente", "existe el propietario 1", "GET", "/owners/1", 200,
                res=owner, reglas=owner_reglas),
    interaccion("POST /owners: alta válida", "no hay propietarios", "POST", "/owners", 201,
                req=owner_req, res=owner, reglas=owner_reglas),
])

# ---------------------------------------------------------------- P2 visits-service
visita = {"id": 1, "date": "2024-01-15", "description": "rabies shot", "petId": 7}
P2 = pacto("visits-service", [
    interaccion("GET /pets/visits: visitas de una mascota", "la mascota 7 tiene una visita", "GET", "/pets/visits", 200,
                query={"petId": ["7"]}, res={"items": [visita]},
                reglas={"$.items": regla("type", min=1), "$.items[*].id": ENTERO, "$.items[*].date": FECHA,
                        "$.items[*].description": TIPO, "$.items[*].petId": ENTERO}),
    interaccion("POST /owners/{ownerId}/pets/{petId}/visits: alta válida", "existe la mascota 7", "POST",
                "/owners/1/pets/7/visits", 201, req={"date": "2024-01-15", "description": "rabies shot"}, res=visita,
                reglas={"$.id": ENTERO, "$.date": FECHA, "$.description": TIPO, "$.petId": ENTERO}),
])

# ---------------------------------------------------------------- P3 account-service
saving = {"amount": 1500.0, "currency": "USD", "interest": 3.5, "deposit": True, "capitalization": False}
item = {"title": "Salario", "amount": 3000.0, "currency": "USD", "period": "MONTH", "icon": "wallet"}
cuenta = {"name": "ana", "lastSeen": "2026-11-18T15:30:00.000+0000", "incomes": [item], "expenses": [],
          "saving": saving, "note": "Ahorro para vacaciones"}
cuenta_reglas = {"$.name": TIPO, "$.lastSeen": FECHA_HORA, "$.saving.amount": NUMERO, "$.saving.currency": TIPO,
                 "$.saving.interest": NUMERO, "$.saving.deposit": TIPO, "$.saving.capitalization": TIPO}
P3 = pacto("account-service", [
    interaccion("GET /accounts/{name}: cuenta existente", "existe la cuenta ana", "GET", "/accounts/ana", 200,
                res=cuenta, reglas={**cuenta_reglas, "$.incomes[*].title": TIPO, "$.incomes[*].amount": NUMERO}),
    interaccion("POST /accounts/: registro válido", "no hay cuenta ana", "POST", "/accounts/", 200,
                req={"username": "ana", "password": "secreto123"},
                res={"name": "ana", "lastSeen": "2026-11-18T15:30:00.000+0000",
                     "saving": {**saving, "amount": 0, "interest": 0, "deposit": False}},
                reglas=cuenta_reglas),
])

# ---------------------------------------------------------------- P4 statistics-service
punto = {"id": {"account": "ana", "date": "2026-11-18T00:00:00.000+0000"},
         "incomes": [{"title": "Salario", "amount": 98.56}], "expenses": [{"title": "Alquiler", "amount": 39.43}],
         "statistics": {"INCOMES_AMOUNT": 98.56, "EXPENSES_AMOUNT": 39.43, "SAVING_AMOUNT": 1500.0},
         "rates": {"USD": 1, "EUR": 0.92, "RUB": 91.5}}
P4 = pacto("statistics-service", [
    interaccion("GET /statistics/{accountName}: serie de una cuenta", "la cuenta ana tiene datos", "GET",
                "/statistics/ana", 200, res=[punto],
                reglas={"$": regla("type", min=1), "$[*].id.account": TIPO, "$[*].id.date": FECHA_HORA,
                        "$[*].incomes[*].title": TIPO, "$[*].incomes[*].amount": NUMERO,
                        "$[*].expenses[*].amount": NUMERO, "$[*].statistics.INCOMES_AMOUNT": NUMERO,
                        "$[*].rates.USD": NUMERO, "$[*].rates.EUR": NUMERO, "$[*].rates.RUB": NUMERO}),
    interaccion("PUT /statistics/{accountName}: estado válido", "existe la cuenta ana", "PUT", "/statistics/ana", 200,
                req={"incomes": [{k: item[k] for k in ("title", "amount", "currency", "period")}],
                     "expenses": [], "saving": saving}),
])

# ---------------------------------------------------------------- P5 auth-service
P5 = pacto("auth-service", [
    interaccion("GET /uaa/users/current: principal autenticado", "el usuario ana está autenticado", "GET",
                "/uaa/users/current", 200,
                res={"name": "ana", "authenticated": True, "authorities": [{"authority": "ROLE_USER"}]},
                reglas={"$.name": TIPO, "$.authenticated": TIPO, "$.authorities[*].authority": TIPO}),
    interaccion("POST /uaa/users: alta válida", "no existe el usuario ana", "POST", "/uaa/users", 200,
                req={"username": "ana", "password": "secreto123"}),
])

for nombre, p in {"P1": P1, "P2": P2, "P3": P3, "P4": P4, "P5": P5}.items():
    d = HERE / nombre
    d.mkdir(exist_ok=True)
    f = d / f"{CONSUMIDOR}-{p['provider']['name']}.json"
    f.write_text(json.dumps(p, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f.relative_to(HERE.parent))
