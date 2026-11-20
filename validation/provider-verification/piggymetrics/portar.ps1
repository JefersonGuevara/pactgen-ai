<#
.SYNOPSIS
  Regenera src/main/java a partir del clon de piggymetrics (commit 6bb2cf9) aplicando solo las adaptaciones
  documentadas en NOTICE. Hace reproducible el portado: el resto del código se copia sin cambios.
.EXAMPLE
  .\validation\provider-verification\piggymetrics\portar.ps1
  .\validation\provider-verification\piggymetrics\portar.ps1 -Origen F:\otra\ruta\piggymetrics
#>
param(
  [string]$Origen = ''
)
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Origen) { $Origen = Join-Path (Resolve-Path "$here\..\..\..\..") 'providers\piggymetrics' }
$commit = '6bb2cf9'
if (-not (Test-Path $Origen)) {
  git clone -q https://github.com/sqshq/piggymetrics.git $Origen
}
git -C $Origen checkout -q --force $commit
Write-Host "Origen: $Origen ($(git -C $Origen rev-parse --short HEAD))" -ForegroundColor Cyan

# Archivos portados: lo que un contrato verifica (controladores, manejo de errores, dominio y la interfaz del servicio)
$archivos = @{
  'account-service'    = @('account/controller/AccountController.java', 'account/controller/ErrorHandler.java',
                           'account/domain/Account.java', 'account/domain/Currency.java', 'account/domain/Item.java',
                           'account/domain/Saving.java', 'account/domain/TimePeriod.java', 'account/domain/User.java',
                           'account/service/AccountService.java')
  'statistics-service' = @('statistics/controller/StatisticsController.java',
                           'statistics/domain/Account.java', 'statistics/domain/Currency.java',
                           'statistics/domain/ExchangeRatesContainer.java', 'statistics/domain/Item.java',
                           'statistics/domain/Saving.java', 'statistics/domain/TimePeriod.java',
                           'statistics/domain/timeseries/DataPoint.java', 'statistics/domain/timeseries/DataPointId.java',
                           'statistics/domain/timeseries/ItemMetric.java', 'statistics/domain/timeseries/StatisticMetric.java',
                           'statistics/service/StatisticsService.java')
  'auth-service'       = @('auth/controller/UserController.java', 'auth/domain/User.java', 'auth/service/UserService.java')
}

$destino = Join-Path $here 'src\main\java\com\piggymetrics'
Remove-Item -Recurse -Force $destino -ErrorAction SilentlyContinue
$utf8 = New-Object System.Text.UTF8Encoding($false)
$n = 0
foreach ($servicio in $archivos.Keys) {
  foreach ($rel in $archivos[$servicio]) {
    $src = Join-Path $Origen "$servicio\src\main\java\com\piggymetrics\$rel"
    $text = [System.IO.File]::ReadAllText($src)
    $nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
    $lineas = $text -split '\r?\n'
    $out = New-Object System.Collections.Generic.List[string]
    foreach ($l in $lineas) {
      # 1. Seguridad: @PreAuthorize (OAuth2) se resuelve fuera del contrato
      if ($l -match '^\s*@PreAuthorize\(' -or $l -match '^import org\.springframework\.security\.access\.prepost\.PreAuthorize;') { continue }
      # 2. Persistencia: anotaciones de Spring Data MongoDB, sin efecto en el JSON
      if ($l -match '^\s*@Document\(' -or $l -match '^\s*@Id\s*$' -or
          $l -match '^import org\.springframework\.data\.(annotation\.Id|mongodb\.core\.mapping\.Document);') { continue }
      # 3. Anotación de Jackson 1 (org.codehaus), que Jackson 2 ignora: se elimina para no arrastrar Jackson 1
      if ($l -match '^import org\.codehaus\.jackson\.annotate\.JsonIgnoreProperties;' -or
          ($l -match '^@JsonIgnoreProperties\(ignoreUnknown = true\)\s*$' -and $text.Contains('org.codehaus.jackson'))) { continue }
      # 4. Jakarta EE 9+: javax.validation -> jakarta.validation
      $out.Add(($l -replace '^import javax\.validation\.', 'import jakarta.validation.'))
    }
    $cabecera = "// Portado de piggymetrics ($commit) $servicio/src/main/java/com/piggymetrics/$rel. Adaptaciones: ver NOTICE."
    $dst = Join-Path $destino ($rel -replace '/', '\')
    New-Item -ItemType Directory -Force (Split-Path -Parent $dst) | Out-Null
    [System.IO.File]::WriteAllText($dst, $cabecera + $nl + ($out -join $nl), $utf8)
    $n++
  }
}
Write-Host "Portados $n archivos en $destino"
