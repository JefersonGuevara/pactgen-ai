<#
.SYNOPSIS
  Verifica contratos Pact contra un proveedor del corpus (línea base o mutante).
.EXAMPLE
  .\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts examples\spike-consumer\target\pacts
  .\validation\provider-verification\verificar.ps1 -Proveedor P1 -Pacts <carpeta> -Rama bc/customers-service/BC1
  .\validation\provider-verification\verificar.ps1 -Proveedor P2 -Pacts <carpeta> -Ciego
  Ejecutar desde la raíz del repositorio pactgen-ai. Los proveedores se clonan en ..\providers.
#>
param(
  [Parameter(Mandatory)][ValidateSet('P1','P2')][string]$Proveedor,
  [Parameter(Mandatory)][string]$Pacts,
  [string]$Rama = '',
  # A ciegas: la salida de Maven va a un archivo y en pantalla solo se ven los totales
  # (para proveedores del grupo A, cuyas pruebas generadas el autor no debe ver antes del experimento)
  [switch]$Ciego
)
$ErrorActionPreference = 'Stop'
$corpus = @{
  P1 = @{ repo = 'https://github.com/spring-petclinic/spring-petclinic-microservices.git'; dir = 'petclinic'; ref = 'v3.4.1'
          module = 'spring-petclinic-customers-service'; pkg = 'org\springframework\samples\petclinic\customers\web'
          test = 'CustomersContractVerificationTest'; harness = 'customers-service' }
  P2 = @{ repo = 'https://github.com/spring-petclinic/spring-petclinic-microservices.git'; dir = 'petclinic'; ref = 'v3.4.1'
          module = 'spring-petclinic-visits-service'; pkg = 'org\springframework\samples\petclinic\visits\web'
          test = 'VisitsContractVerificationTest'; harness = 'visits-service' }
}
$c = $corpus[$Proveedor]
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$providers = Join-Path (Resolve-Path "$here\..\..\..") 'providers'
$clone = Join-Path $providers $c.dir
$module = Join-Path $clone $c.module

# 1. Clon del proveedor en el commit fijado (o en la rama del mutante)
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

# 3. Contratos a verificar
$pactDir = Join-Path $module 'target\pacts-to-verify'
Remove-Item -Recurse -Force $pactDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $pactDir | Out-Null
Copy-Item (Join-Path $Pacts '*.json') $pactDir
Write-Host "Contratos: $((Get-ChildItem $pactDir).Count) archivo(s)"

# 4. Verificación
$log = Join-Path $module 'target\verificacion.log'
if ($Ciego) {
  mvn -q -f $pom test "-Dtest=$($c.test)" "-Dsurefire.failIfNoSpecifiedTests=false" *> $log
} else {
  mvn -q -f $pom test "-Dtest=$($c.test)" "-Dsurefire.failIfNoSpecifiedTests=false" | Tee-Object -FilePath $log | Out-Host
}

# 5. Resumen por interacción (informe de Surefire)
$report = Get-ChildItem (Join-Path $module 'target\surefire-reports') -Filter "TEST-*$($c.test).xml" | Select-Object -First 1
if (-not $report) { Write-Error 'No hay informe de Surefire'; exit 2 }
[xml]$xml = Get-Content $report.FullName
$cases = $xml.testsuite.testcase
$fail = @($cases | Where-Object { $_.failure -or $_.error })
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
