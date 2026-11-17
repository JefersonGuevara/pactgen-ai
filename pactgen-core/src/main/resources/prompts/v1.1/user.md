Consumidor: {consumer}. Proveedor: {provider}. Operación: {operationId} — {method} {path}.
Descripción: {summary}
Parámetros:
{parameters}
Cuerpo de petición: {requestSchema}
Respuestas:
{responses}

Antes de proponer las interacciones, escribe en "analysis": (1) qué necesita probablemente el consumidor de esta operación; (2) qué estados del proveedor son relevantes; (3) qué casos cubrirás: el caso feliz y al menos dos casos de error o borde, usando solo los códigos de estado declarados. Si la operación declara un único código, cubre variantes relevantes del caso feliz (por ejemplo, colecciones vacías o datos opcionales ausentes). Después, completa "interactions".
