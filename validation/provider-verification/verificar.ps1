<#
.SYNOPSIS
  Verifica contratos Pact contra un proveedor del corpus (línea base o mutante).
.EXAMPLE
  .\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts examples\spike-consumer\target\pacts
  .\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts <carpeta> -Parche validation\breaking-changes\customers-service\BC1.patch
  .\validation\provider-verification\verificar.ps1 -Proveedor P2 -Pacts <carpeta> -Ciego
  .\validation\provider-verification\verificar.ps1 -Proveedor P4 -Pacts <carpeta>
  .\validation\provider-verification\verificar.ps1 -Proveedor P3 -Pacts <carpeta> -Ciego
  Ejecutar desde la raíz del repositorio pactgen-ai. P1 y P2 se clonan en ..\providers; P3–P5 usan el portado
  de piggymetrics de validation\provider-verification\piggymetrics (sin clonar ni cambiar de rama).
  -Parche aplica un mutante (validation\breaking-changes) solo durante la verificación y lo revierte siempre.
  -Resultado escribe un CSV con el resultado por interacción (indice,ok), en el orden del contrato.
#>
param(
  [Parameter(Mandatory)][ValidateSet('P1','P2','P3','P4','P5')][string]$Proveedor,
  [Parameter(Mandatory)][string]$Pacts,
  [string]$Rama = '',
  # A ciegas: la salida de Maven va a un archivo y en pantalla solo se ven los totales
  # (para proveedores del grupo A, cuyas pruebas generadas el autor no debe ver antes del experimento)
  [switch]$Ciego,
  # Mutante: parche relativo a la raíz del clon (P1, P2) o del portado de piggymetrics (P3–P5)
  [string]$Parche = '',
  [string]$Resultado = ''
)
$ErrorActionPreference = 'Stop'
$corpus = @{
  P1 = @{ repo = 'https://github.com/spring-petclinic/spring-petclinic-microservices.git'; dir = 'petclinic'; ref = 'v3.4.1'
          module = 'spring-petclinic-customers-service'; pkg = 'org\springframework\samples\petclinic\customers\web'
          test = 'CustomersContractVerificationTest'; harness = 'customers-service' }
  P2 = @{ repo = 'https://github.com/spring-petclinic/spring-petclinic-microservices.git'; dir = 'petclinic'; ref = 'v3.4.1'
          module = 'spring-petclinic-visits-service'; pkg = 'org\springframework\samples\petclinic\visits\web'
          test = 'VisitsContractVerificationTest'; harness = 'visits-service' }
  # piggymetrics (Spring Boot 2.0 / Java 8): portado a Spring Boot 3.4.1 en piggymetrics\ (ver su NOTICE)
  P3 = @{ local = 'piggymetrics'; test = 'AccountContractVerificationTest' }
  P4 = @{ local = 'piggymetrics'; test = 'StatisticsContractVerificationTest' }
  P5 = @{ local = 'piggymetrics'; test = 'AuthContractVerificationTest' }
}
$c = $corpus[$Proveedor]
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path "$here\..\..").Path
$portado = 'validation/provider-verification/piggymetrics'
if ($Parche) { $Parche = (Resolve-Path $Parche).Path }
if ($Resultado -and -not [IO.Path]::IsPathRooted($Resultado)) { $Resultado = Join-Path (Get-Location) $Resultado }

if ($c.local) {
  # P3–P5: proyecto Maven propio con el portado; no hay clon ni rama
  if ($Rama) { Write-Error "-Rama no se aplica a $Proveedor (portado local de piggymetrics)"; exit 2 }
  $module = Join-Path $here $c.local
  # El parche se aplica sobre el árbol de trabajo del repositorio: el portado debe estar limpio
  git -C $repoRoot diff --quiet -- "$portado/src"
  if ($LASTEXITCODE -ne 0) { Write-Error "El portado tiene cambios sin confirmar en $portado/src; confírmalos o descártalos antes de verificar"; exit 2 }
  Write-Host "Proveedor $Proveedor en el portado de piggymetrics (6bb2cf9): $module" -ForegroundColor Cyan
} else {
  $providers = Join-Path (Resolve-Path "$here\..\..\..") 'providers'
  $clone = Join-Path $providers $c.dir
  $module = Join-Path $clone $c.module

  # 1. Clon del proveedor en el commit fijado (o en la rama indicada); --force descarta cualquier cambio previo
  if (-not (Test-Path $clone)) {
    New-Item -ItemType Directory -Force $providers | Out-Null
    git clone -q $c.repo $clone
  }
  $ref = if ($Rama) { $Rama } else { $c.ref }
  git -C $clone checkout -q --force $ref
  Write-Host "Proveedor $Proveedor en $ref ($(git -C $clone rev-parse --short HEAD))" -ForegroundColor Cyan

  # 2. Arnés de verificación (archivos de PactGen-AI, no del proyecto original)
  $testDir = Join-Path $module "src\test\java\$($c.pkg)"
  $commonDir = Join-Path $module 'src\test\java\dev\pactgen\verification'
  New-Item -ItemType Directory -Force $testDir, $commonDir | Out-Null
  Copy-Item "$here\$($c.harness)\$($c.test).java" $testDir -Force
  Copy-Item "$here\common\ContractFixtures.java" $commonDir -Force
  $pom = Join-Path $module 'pom.xml'
  $pomText = Get-Content $pom -Raw
  if ($pomText -notmatch 'junit5spring') {
    $dep = @"
        <dependency>
            <groupId>au.com.dius.pact.provider</groupId>
            <artifactId>junit5spring</artifactId>
            <version>4.6.17</version>
            <scope>test</scope>
        </dependency>
	</dependencies>
"@
    $idx = $pomText.IndexOf('</dependencies>')
    $pomText = $pomText.Substring(0, $idx) + $dep.TrimStart() + $pomText.Substring($idx + '</dependencies>'.Length)
    Set-Content -Path $pom -Value $pomText -Encoding utf8
  }
}
$pom = Join-Path $module 'pom.xml'

# 3. Mutante (opcional): se aplica después de preparar el proveedor y se revierte siempre (finally)
function Invoke-Parche([switch]$Revertir) {
  $a = @('apply')
  if ($Revertir) { $a += '-R' }
  if ($c.local) { $a += "--directory=$portado"; $root = $repoRoot } else { $root = $clone }
  $ErrorActionPreference = 'Continue'
  git -C $root @a $Parche 2>&1 | ForEach-Object { Write-Host "$_" }
  $ok = ($LASTEXITCODE -eq 0)
  $ErrorActionPreference = 'Stop'
  return $ok
}
if ($Parche) {
  if (-not (Invoke-Parche)) { Write-Error "No se pudo aplicar el parche $Parche"; exit 2 }
  Write-Host "Mutante aplicado: $(Split-Path -Leaf $Parche)" -ForegroundColor Magenta
}

$log = Join-Path $module 'target\verificacion.log'
$reports = Join-Path $module 'target\surefire-reports'
$cases = @()
$report = $null
try {
  # 4. Contratos a verificar
  $pactDir = Join-Path $module 'target\pacts-to-verify'
  Remove-Item -Recurse -Force $pactDir -ErrorAction SilentlyContinue
  New-Item -ItemType Directory -Force $pactDir | Out-Null
  Copy-Item (Join-Path $Pacts '*.json') $pactDir
  Write-Host "Contratos: $((Get-ChildItem $pactDir).Count) archivo(s)"

  # 5. Verificación. Maven escribe avisos en stderr (p. ej., el agente de Mockito): en PowerShell 5.1, con
  #    ErrorActionPreference = Stop, cualquier línea de stderr de un ejecutable detendría el script.
  Get-ChildItem $reports -Filter "TEST-*$($c.test).xml" -ErrorAction SilentlyContinue | Remove-Item -Force
  $ErrorActionPreference = 'Continue'
  if ($Ciego) {
    mvn -q -f $pom test "-Dtest=$($c.test)" "-Dsurefire.failIfNoSpecifiedTests=false" *> $log
  } else {
    mvn -q -f $pom test "-Dtest=$($c.test)" "-Dsurefire.failIfNoSpecifiedTests=false" 2>&1 |
      ForEach-Object { "$_" } | Tee-Object -FilePath $log | Out-Host
  }
  $ErrorActionPreference = 'Stop'

  # 6. Informe de Surefire (se lee antes de revertir el mutante)
  $report = Get-ChildItem $reports -Filter "TEST-*$($c.test).xml" -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($report) {
    [xml]$xml = Get-Content $report.FullName
    $cases = @($xml.testsuite.testcase)
  }
} finally {
  if ($Parche) {
    if (Invoke-Parche -Revertir) { Write-Host "Mutante revertido" -ForegroundColor Magenta }
    else { Write-Host "ATENCIÓN: no se pudo revertir $Parche; restaura el proveedor a mano" -ForegroundColor Red }
  }
}

# 7. Resumen por interacción
if (-not $report) { Write-Error "No hay informe de Surefire (¿no compila? ver $log)"; exit 2 }
$fail = @($cases | Where-Object { $_.failure -or $_.error })
$skipped = @($cases | Where-Object { $_.SelectSingleNode("skipped") })
if ($cases.Count -eq 0 -or $skipped.Count -eq $cases.Count) {
  Write-Host "Ninguna interacción verificada: no hay contratos de este proveedor en $Pacts" -ForegroundColor Yellow
  exit 2
}
if ($Resultado) {
  # Cada caso se llama verify(PactVerificationContext)[n]: n es la posición de la interacción en el contrato
  $filas = foreach ($t in $cases) {
    $n = [int]([regex]::Match($t.name, '\[(\d+)\]$').Groups[1].Value)
    [pscustomobject]@{ indice = $n; ok = [int](-not ($t.failure -or $t.error)) }
  }
  $filas | Sort-Object indice | Export-Csv -Path $Resultado -NoTypeInformation -Encoding UTF8
}
if ($Ciego) {
  Write-Host ("`nInteracciones: {0} | verificadas: {1} | fallidas: {2}" -f $cases.Count, ($cases.Count - $fail.Count), $fail.Count)
  Write-Host "Registro completo (no abrir si el proveedor es del grupo A): $log" -ForegroundColor Yellow
  exit 0
}
foreach ($t in $cases) {
  $ok = -not ($t.failure -or $t.error)
  Write-Host ("{0}  {1}" -f ($(if ($ok) { 'OK  ' } else { 'FALLA' })), $t.name) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
}
Write-Host ("`nInteracciones: {0} | verificadas: {1} | fallidas: {2}" -f $cases.Count, ($cases.Count - $fail.Count), $fail.Count)
