# Guion de cada bloque del experimento

Un **bloque** es un proveedor en una condición (manual o PactGen-AI). Duración máxima: 90 minutos. Pausa mínima entre bloques: 15 minutos. El orden de los bloques está fijado en `asignacion.md` y no se cambia.

## Antes de la sesión (no se cronometra)

- [ ] `git pull` y `git status` limpio. La herramienta está en la etiqueta `experimento-v1` (`git describe --tags`).
- [ ] `mvn -B clean verify` en verde y `pactgen-cli\target\pactgen.jar` generado.
- [ ] Dependencias de Maven descargadas: `mvn -q -f experiment\plantilla-consumidor\pom.xml dependency:resolve`.
- [ ] Pact Broker arrancado si se va a publicar (no forma parte de la medición).
- [ ] `$env:ANTHROPIC_API_KEY` definida solo en esta sesión de PowerShell.
- [ ] Notificaciones silenciadas; cronómetro y `datos\registro_tiempos.csv` abiertos.

## Bloque en condición manual

1. Prepara el proyecto: `.\experiment\preparar.ps1 -Sesion <n> -Proveedor <Px> -Condicion manual`.
2. **Anota `hora_inicio`** y abre el OpenAPI del proveedor y el código del consumidor real.
3. Escribe las pruebas de consumidor con Pact JVM y JUnit 5, operación por operación.
4. Ejecuta `mvn -q -f <dir>\pom.xml test` cuando quieras. Cuando **la primera prueba compile y pase**, anota `hora_primera_prueba_verde`.
5. Sigue hasta cubrir todas las operaciones del OpenAPI o hasta los 90 minutos. Anota `hora_todas_las_operaciones` (o déjala vacía si no se alcanzó).
6. Registra `operaciones_cubiertas`, `pruebas_totales`, `pruebas_verdes` y las pausas en minutos.
7. `git add experiment && git commit -m "Experimento s<n> <Px> manual"`.

## Bloque en condición PactGen-AI

1. Prepara el proyecto: `.\experiment\preparar.ps1 -Sesion <n> -Proveedor <Px> -Condicion pactgen`.
2. **Anota `hora_inicio`** y abre el OpenAPI y el código del consumidor real.
3. Ejecuta el comando `generate` que muestra el script. Registra `t_herramienta_s` (duración de la ejecución).
4. Ejecuta `mvn -q -f <dir>\pom.xml test`. Si la primera prueba pasa, anota `hora_primera_prueba_verde`.
5. **Revisión humana**: lee las pruebas generadas contra el consumidor real. Corrige solo lo necesario para que compilen, pasen y reflejen el uso real. Cuenta cada archivo modificado en `ediciones_manuales` y describe los cambios en `observaciones`.
6. Anota `hora_todas_las_operaciones` cuando todas las operaciones tengan pruebas verdes y revisadas.
7. Completa el `NOTAS.md` de la carpeta creada en `experiment\datos\runs\` y haz commit.

**Ejecuciones 2 y 3** (variabilidad del modelo): al terminar la sesión, sin cronometrar ni revisar, ejecuta `preparar.ps1` con `-Ejecucion 2` y `-Ejecucion 3`, lanza `generate` y `mvn test`, y registra solo las métricas automáticas (`ejecucion` = 2 o 3; columnas de tiempo vacías).

## Convención de redacción de los estados del proveedor (ambas condiciones)

El arnés de verificación de piggymetrics simula los errores que dependen del servicio (cuenta inexistente, usuario duplicado…) a partir del texto del `providerState`. Para que la verificación no dependa de la redacción, en la condición manual escribe los estados con estas expresiones (en español o en inglés):

| Situación | Expresiones |
|---|---|
| El recurso no existe | «no existe», «inexistente», «not found», «does not exist», «no encontrado», «no está registrado», «sin cuenta» |
| El recurso ya existe | «ya existe», «duplicado», «already exists», «existente», «ya está registrado», «en uso» |
| Caso normal | cualquier otra redacción, p. ej. «existe la cuenta demo» |

En el caso normal no uses la palabra «existente», porque el arnés la lee como «ya existe». Escribe, por ejemplo, «existe la cuenta demo» en lugar de «una cuenta existente».

Las expresiones se reconocen sin distinguir mayúsculas y en cualquier parte del estado, también en femenino («no encontrada», «ya está registrada»). Solo se aplican cuando el contrato espera el código de error correspondiente: 400 en account-service y 500 en auth-service. La tabla y el arnés (`validation/provider-verification/piggymetrics/.../ProviderStates.java`) se comprueban entre sí con `ProviderStatesTest`. Las tres últimas expresiones de cada fila se añadieron el 21 nov, porque el arnés ya las reconocía.

La convención se fijó antes de la sesión 1 y coincide con la que reconoce el arnés para los contratos generados. Si en un bloque se usa otra redacción, se anota en `observaciones` y la interacción se verifica tal cual, sin reescribir el estado.

## Reglas que no se rompen

- Nada de la herramienta cambia entre la sesión 1 y la 2: ni el código, ni el prompt (v1.1), ni el filtro (activo), ni el modelo (`claude-haiku-4-5-20251001`, temperatura 0,2).
- Las pruebas no se modifican después de conocer los cambios incompatibles (mutantes).
- Si un bloque se interrumpe, se anota la pausa; si se pierde, se repite al día siguiente y se documenta.
- Cualquier desviación del protocolo se anota en `observaciones` y se reporta en la memoria.
