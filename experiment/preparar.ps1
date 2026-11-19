<#
.SYNOPSIS
  Prepara el proyecto consumidor de un bloque del experimento y muestra los comandos a ejecutar.
.EXAMPLE
  .\experiment\preparar.ps1 -Sesion 1 -Proveedor P3 -Condicion manual
  .\experiment\preparar.ps1 -Sesion 1 -Proveedor P4 -Condicion pactgen
  Ejecutar desde la raíz del repositorio (pactgen-ai).
#>
param(
  [Parameter(Mandatory)][ValidateSet(1,2)][int]$Sesion,
  [Parameter(Mandatory)][ValidateSet('P1','P2','P3','P4','P5')][string]$Proveedor,
  [Parameter(Mandatory)][ValidateSet('manual','pactgen')][string]$Condicion,
  [ValidateSet(1,2,3)][int]$Ejecucion = 1
)
$corpus = @{
  P1 = @{ provider = 'customers-service';  consumer = 'api-gateway' }
  P2 = @{ provider = 'visits-service';     consumer = 'api-gateway' }
  P3 = @{ provider = 'account-service';    consumer = 'notification-service' }
  P4 = @{ provider = 'statistics-service'; consumer = 'account-service' }
  P5 = @{ provider = 'auth-service';       consumer = 'account-service' }
}
$c = $corpus[$Proveedor]
$suffix = if ($Condicion -eq 'pactgen') { "pactgen-run$Ejecucion" } else { 'manual' }
$dir = "experiment\workspace\s$Sesion\$Proveedor-$suffix"
$openapi = "validation\microservices\$($c.provider)\openapi.yaml"

if (-not (Test-Path $openapi)) { Write-Error "No existe $openapi"; exit 2 }
if (Test-Path "$dir\src") { Write-Error "$dir ya contiene pruebas; no se sobrescribe."; exit 2 }

New-Item -ItemType Directory -Force "$dir\src\test\java" | Out-Null
(Get-Content experiment\plantilla-consumidor\pom.xml -Raw) -replace '__ARTIFACT__', "$Proveedor-$suffix".ToLower() |
  Set-Content -Encoding utf8 "$dir\pom.xml"

Write-Host "Proyecto listo: $dir" -ForegroundColor Green
Write-Host "Proveedor: $($c.provider) | Consumidor: $($c.consumer) | OpenAPI: $openapi"
if ($Condicion -eq 'pactgen') {
  Write-Host "`nComando de generación (anota la hora de inicio ANTES de ejecutarlo):" -ForegroundColor Cyan
  Write-Host "java -jar pactgen-cli\target\pactgen.jar generate --openapi $openapi --consumer $($c.consumer) --provider $($c.provider) --out $dir --archive experiment\datos\runs"
} else {
  Write-Host "`nEscribe las pruebas en $dir\src\test\java (paquete manual.$($Proveedor.ToLower()))." -ForegroundColor Cyan
}
Write-Host "Ejecutar pruebas: mvn -q -f $dir\pom.xml test"
