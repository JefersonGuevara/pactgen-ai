Ejemplos (API de inventario ajena al corpus de evaluación):

### Ejemplo 1 — entrada
Consumidor: order-service. Proveedor: inventory-service. Operación: getItem — GET /items/{itemId}.
Descripción: Obtiene un artículo del inventario.
Parámetros:
- itemId (path, obligatorio): {"type":"integer","format":"int64"}
Cuerpo de petición: (sin cuerpo)
Respuestas:
- 200: {"type":"object","properties":{"id":{"type":"integer"},"sku":{"type":"string"},"name":{"type":"string"},"stock":{"type":"integer"},"price":{"type":"number"},"updatedAt":{"type":"string","format":"date"}}}
- 404: {"type":"object","properties":{"message":{"type":"string"}}}

### Ejemplo 1 — salida
{"analysis":"order-service consulta un artículo para comprobar el stock y el precio antes de confirmar un pedido. Estados relevantes: el artículo existe, el artículo existe sin stock y el artículo no existe. Casos: caso feliz con stock; borde con stock 0, que el consumidor trata como no disponible; error 404 para un id inexistente.","interactions":[{"description":"una petición de un artículo existente con stock","providerState":"existe el artículo con id 10 y stock 25","request":{"method":"GET","path":"/items/10"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"body":{"id":10,"sku":"SKU-0010","name":"Teclado mecánico","stock":25,"price":149.9}}},{"description":"una petición de un artículo sin stock","providerState":"existe el artículo con id 11 y stock 0","request":{"method":"GET","path":"/items/11"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"body":{"id":11,"sku":"SKU-0011","name":"Ratón inalámbrico","stock":0,"price":39.5}}},{"description":"una petición de un artículo inexistente","providerState":"no existe el artículo con id 999","request":{"method":"GET","path":"/items/999"},"response":{"status":404,"headers":{"Content-Type":"application/json"},"body":{"message":"Item 999 not found"}}}]}

### Ejemplo 2 — entrada
Consumidor: purchasing-service. Proveedor: inventory-service. Operación: createItem — POST /items.
Descripción: Da de alta un artículo.
Parámetros: (ninguno)
Cuerpo de petición: {"type":"object","required":["sku","name"],"properties":{"sku":{"type":"string"},"name":{"type":"string"},"stock":{"type":"integer","minimum":0}}}
Respuestas:
- 201: {"type":"object","properties":{"id":{"type":"integer"},"sku":{"type":"string"},"name":{"type":"string"},"stock":{"type":"integer"}}}
- 400: {"type":"object","properties":{"message":{"type":"string"}}}
- 409: {"type":"object","properties":{"message":{"type":"string"}}}

### Ejemplo 2 — salida
{"analysis":"purchasing-service da de alta artículos nuevos y necesita el id asignado. Estados relevantes: el SKU no existe y el SKU ya existe. Casos: alta correcta con 201; petición sin campo obligatorio con 400; SKU duplicado con 409.","interactions":[{"description":"una petición para crear un artículo nuevo","providerState":"no existe ningún artículo con SKU SKU-0200","request":{"method":"POST","path":"/items","headers":{"Content-Type":"application/json"},"body":{"sku":"SKU-0200","name":"Monitor 27 pulgadas","stock":5}},"response":{"status":201,"headers":{"Content-Type":"application/json"},"body":{"id":200,"sku":"SKU-0200","name":"Monitor 27 pulgadas","stock":5}}},{"description":"una petición de alta sin el nombre obligatorio","providerState":"el servicio de inventario está disponible","request":{"method":"POST","path":"/items","headers":{"Content-Type":"application/json"},"body":{"sku":"SKU-0201"}},"response":{"status":400,"headers":{"Content-Type":"application/json"},"body":{"message":"name is required"}}},{"description":"una petición de alta con un SKU duplicado","providerState":"existe un artículo con SKU SKU-0010","request":{"method":"POST","path":"/items","headers":{"Content-Type":"application/json"},"body":{"sku":"SKU-0010","name":"Teclado mecánico"}},"response":{"status":409,"headers":{"Content-Type":"application/json"},"body":{"message":"SKU already exists"}}}]}
