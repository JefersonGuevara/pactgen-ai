<#
.SYNOPSIS
  Verifica un conjunto de contratos contra la línea base y contra cada mutante de un proveedor (Tabla 8).
.DESCRIPTION
  1. Verifica la línea base (proveedor sin mutar) y anota qué interacciones pasan.
  2. Para cada mutante (<servicio>\<ID>.patch): aplica el parche, verifica y lo revierte (siempre, también si falla).
  3. Escribe un CSV: proveedor, mutante, tipo, interacciones, base_ok, fallan_con_mutante, detectado.
     Un mutante se detecta si al menos una interacción que pasa en la línea base falla con él. En un NB,
     detectado = true es un falso positivo. Si un mutante no compila o no se aplica, detectado = error.
  Las interacciones se identifican por su posición en el contrato (orden estable).
  Con -Ciego (P2, P3, P5) en pantalla solo aparece la tabla de totales; sin -Ciego, también qué interacciones fallan.
.EXAMPLE
  .\validation\breaking-changes\mutar.ps1 -Proveedor P1 -Pacts examples\spike-consumer\target\pacts -Salida resultados\P1.csv
  .\validation\breaking-changes\mutar.ps1 -Proveedor P4 -Pacts validation\breaking-changes\referencia\P4 -Mutante BC2
  .\validation\breaking-changes\mutar.ps1 -Proveedor P3 -Pacts $env:TEMP\pactgen-P3\target\pacts -Ciego -Salida $env:TEMP\pactgen-P3\mutantes.csv
  Ejecutar desde la raíz del repositorio pactgen-ai.
#>
param(
  [Parameter(Mandatory)][ValidateSet('P1','P2','P3','P4','P5')][string]$Proveedor,
  [Parameter(Mandatory)][string]$Pacts,
  [string]$Mutante = '',
  [switch]$Ciego,
  [string]$Salida = ''
)
$ErrorActionPreference = 'Stop'
$servicios = @{ P1 = 'customers-service'; P2 = 'visits-service'; P3 = 'account-service'
                P4 = 'statistics-service'; P5 = 'auth-service' }
$servicio = $servicios[$Proveedor]
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$verificar = Join-Path $here '..\provider-verification\verificar.ps1'
$Pacts = (Resolve-Path $Pacts).Path
if (-not $Salida) { $Salida = Join-Path $env:TEMP "pactgen-mutantes-$Proveedor.csv" }
if (-not [IO.Path]::IsPathRooted($Salida)) { $Salida = Join-Path (Get-Location) $Salida }
New-Item -ItemType Directory -Force (Split-Path -Parent $Salida) | Out-Null
$tmp = Join-Path $env:TEMP "pactgen-mutar-$Proveedor"
New-Item -ItemType Directory -Force $tmp | Out-Null

$parches = Get-ChildItem (Join-Path $here $servicio) -Filter '*.patch' | Sort-Object Name
if ($Mutante) { $parches = @($parches | Where-Object { $_.BaseName -eq $Mutante }) }
if (-not $parches) { Write-Error "No hay parches para $Proveedor ($servicio) $Mutante"; exit 2 }

# Verifica en silencio (la salida de Maven queda en el verificacion.log del proveedor) y devuelve ok por posición
function Invoke-Verificacion([string]$Parche, [string]$Csv) {
  Remove-Item $Csv -ErrorAction SilentlyContinue
  $a = @{ Proveedor = $Proveedor; Pacts = $Pacts; Ciego = $true; Resultado = $Csv }
  if ($Parche) { $a.Parche = $Parche }
  try { & $verificar @a *> $null } catch { }
  if (-not (Test-Path $Csv)) { return $null }
  $r = @{}
  Import-Csv $Csv | ForEach-Object { $r[[int]$_.indice] = ($_.ok -eq '1') }
  return $r
}

# Descripción de cada interacción, por posición (solo sin -Ciego)
$descripciones = @{}
if (-not $Ciego) {
  $i = 0
  foreach ($f in Get-ChildItem $Pacts -Filter '*.json') {
    $p = Get-Content $f.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($p.provider.name -ne $servicio) { continue }
    foreach ($it in $p.interactions) { $i++; $descripciones[$i] = $it.description }
  }
}

Write-Host "Línea base de $Proveedor ($servicio)..." -ForegroundColor Cyan
$base = Invoke-Verificacion '' (Join-Path $tmp 'base.csv')
if (-not $base) { Write-Error "La línea base no se pudo verificar (¿no hay contratos de $servicio en ${Pacts}?)"; exit 2 }
$baseOk = @($base.Keys | Where-Object { $base[$_] })
Write-Host ("Línea base: {0} interacciones, {1} verificadas" -f $base.Count, $baseOk.Count)

$filas = @()
foreach ($m in $parches) {
  Write-Host "Mutante $($m.BaseName)..." -ForegroundColor Cyan
  $r = Invoke-Verificacion $m.FullName (Join-Path $tmp "$($m.BaseName).csv")
  $tipo = if ($m.BaseName -like 'BC*') { 'incompatible' } else { 'compatible' }
  if (-not $r) {
    $filas += [pscustomobject]@{ proveedor = $Proveedor; mutante = $m.BaseName; tipo = $tipo; interacciones = $base.Count
                                 base_ok = $baseOk.Count; fallan_con_mutante = 'NA'; detectado = 'error' }
    Write-Host "  ERROR: el mutante no se aplicó o no compila (ver el verificacion.log del proveedor)" -ForegroundColor Red
    continue
  }
  $fallan = @($baseOk | Where-Object { -not $r[$_] } | Sort-Object)
  $filas += [pscustomobject]@{ proveedor = $Proveedor; mutante = $m.BaseName; tipo = $tipo; interacciones = $base.Count
                               base_ok = $baseOk.Count; fallan_con_mutante = $fallan.Count
                               detectado = $(if ($fallan.Count -gt 0) { 'true' } else { 'false' }) }
  if (-not $Ciego) {
    foreach ($n in $fallan) { Write-Host ("  falla [{0}] {1}" -f $n, $descripciones[$n]) }
  }
}

$filas | Export-Csv -Path $Salida -NoTypeInformation -Encoding UTF8
Write-Host ''
$filas | Format-Table -AutoSize | Out-String | Write-Host
$bc = @($filas | Where-Object { $_.tipo -eq 'incompatible' })
$nb = @($filas | Where-Object { $_.tipo -eq 'compatible' })
Write-Host ("Incompatibles detectados: {0}/{1} | falsos positivos (NB): {2}/{3} | errores: {4}" -f `
  @($bc | Where-Object { $_.detectado -eq 'true' }).Count, $bc.Count,
  @($nb | Where-Object { $_.detectado -eq 'true' }).Count, $nb.Count,
  @($filas | Where-Object { $_.detectado -eq 'error' }).Count)
Write-Host "CSV: $Salida"
