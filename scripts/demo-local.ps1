<#
.SYNOPSIS
  Plan B de la demo de DentaFlow (DD-12, C-19 design D8): levanta todo en esta máquina, sin Docker.

.DESCRIPTION
  1. Verifica Java 21, Node 22+, el servicio de MySQL 8 y los archivos .env (sin mostrar valores).
  2. Abre una ventana con el backend (perfil dev, con datos de demo) y otra con el frontend (Vite).
  3. Espera a que /actuator/health responda UP y abre el navegador en el frontend.
  Guarda los PID en scripts/.demo-local.pids para poder cortar todo con -Detener.

  Requisitos previos (una sola vez): pasos 1 y 2 del README (base `dentaflow`, usuario y .env).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\demo-local.ps1

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\demo-local.ps1 -Detener
#>
[CmdletBinding()]
param(
    # Corta el backend y el frontend que levantó este script.
    [switch]$Detener,
    # No abre el navegador al terminar.
    [switch]$SinNavegador,
    # Segundos máximos de espera al backend.
    [int]$EsperaBackendSegundos = 240
)

$ErrorActionPreference = 'Stop'
$Raiz = Split-Path -Parent $PSScriptRoot
$ArchivoPids = Join-Path $PSScriptRoot '.demo-local.pids'
$UrlSalud = 'http://localhost:8080/actuator/health'
$UrlFrontend = 'http://localhost:5173'

function Write-Paso([string]$Texto) { Write-Host "==> $Texto" -ForegroundColor Cyan }
function Write-Ok([string]$Texto) { Write-Host "    OK  $Texto" -ForegroundColor Green }
function Stop-ConError([string]$Texto) {
    Write-Host "    ERROR  $Texto" -ForegroundColor Red
    exit 1
}

function Stop-Demo {
    if (-not (Test-Path $ArchivoPids)) {
        Write-Host 'No hay una demo levantada por este script (no existe scripts\.demo-local.pids).'
        return
    }
    foreach ($linea in Get-Content $ArchivoPids) {
        $procesoId = 0
        if ([int]::TryParse($linea, [ref]$procesoId)) {
            # taskkill /T corta también los procesos hijos (java de Maven, node de Vite).
            & taskkill.exe /PID $procesoId /T /F 2>$null | Out-Null
        }
    }
    Remove-Item $ArchivoPids -Force
    Write-Ok 'Backend y frontend detenidos.'
}

function Test-PuertoLibre([int]$Puerto) {
    # Vite (Node 17+) escucha en ::1 y Spring en 0.0.0.0: se prueban IPv4 e IPv6.
    foreach ($direccion in @('127.0.0.1', '::1')) {
        $cliente = $null
        try {
            $familia = ([System.Net.IPAddress]::Parse($direccion)).AddressFamily
            $cliente = New-Object System.Net.Sockets.TcpClient($familia)
            $intento = $cliente.BeginConnect($direccion, $Puerto, $null, $null)
            if ($intento.AsyncWaitHandle.WaitOne(500) -and $cliente.Connected) { return $false }
        } catch {
            # Sin conexión en esa dirección: se prueba la siguiente.
        } finally {
            if ($cliente) { $cliente.Close() }
        }
    }
    return $true
}

function Get-VariablesEnv([string]$Ruta) {
    # Devuelve solo los nombres de las variables con valor no vacío; nunca los valores.
    $definidas = @{}
    foreach ($linea in Get-Content $Ruta -Encoding UTF8) {
        if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            if ($Matches[2].Trim().Length -gt 0) { $definidas[$Matches[1]] = $true }
        }
    }
    return $definidas
}

function Find-Java21 {
    $candidatos = @()
    if ($env:JAVA_HOME) { $candidatos += $env:JAVA_HOME }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path $base) {
            $candidatos += Get-ChildItem $base -Directory -Filter 'jdk-21*' | Sort-Object Name -Descending | ForEach-Object { $_.FullName }
        }
    }
    foreach ($ruta in $candidatos) {
        $releaseFile = Join-Path $ruta 'release'
        if ((Test-Path (Join-Path $ruta 'bin\java.exe')) -and (Test-Path $releaseFile)) {
            if ((Get-Content $releaseFile -Raw) -match 'JAVA_VERSION="21') { return $ruta }
        }
    }
    return $null
}

if ($Detener) { Stop-Demo; exit 0 }

Write-Host ''
Write-Host 'DentaFlow - demo local (plan B)' -ForegroundColor White
Write-Host ''

# 1. Herramientas
Write-Paso 'Verificando herramientas'
$javaHome = Find-Java21
if (-not $javaHome) { Stop-ConError 'No encontré un JDK 21. Instalá Temurin 21 o definí JAVA_HOME apuntando a un JDK 21.' }
Write-Ok "JDK 21 en $javaHome"

$nodo = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodo) { Stop-ConError 'No encontré Node.js en el PATH. Instalá Node 22 o superior.' }
$versionNode = (& node --version).TrimStart('v')
if ([int]($versionNode.Split('.')[0]) -lt 22) { Stop-ConError "Node $versionNode es muy viejo; hace falta 22 o superior." }
Write-Ok "Node $versionNode"

# 2. MySQL
Write-Paso 'Verificando MySQL 8'
$servicio = Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($servicio -and $servicio.Status -ne 'Running') {
    try {
        Start-Service $servicio.Name -ErrorAction Stop
        Write-Ok "Servicio $($servicio.Name) iniciado"
    } catch {
        Stop-ConError "El servicio $($servicio.Name) está detenido y no pude iniciarlo (hace falta una consola como administrador): Start-Service $($servicio.Name)"
    }
}
if (Test-PuertoLibre 3306) { Stop-ConError 'MySQL no responde en el puerto 3306. Revisá que el servicio esté corriendo (README, paso 1).' }
Write-Ok 'MySQL responde en el puerto 3306'

# 3. Configuración (.env) sin mostrar valores
Write-Paso 'Verificando archivos .env'
$envRaiz = Join-Path $Raiz '.env'
if (-not (Test-Path $envRaiz)) { Stop-ConError 'Falta .env en la raíz. Copiá .env.example a .env y completalo (README, paso 2).' }
$definidas = Get-VariablesEnv $envRaiz
$faltantes = @('SPRING_DATASOURCE_PASSWORD', 'AUTH_SECRET', 'APP_ADMIN_EMAIL', 'APP_ADMIN_PASSWORD') | Where-Object { -not $definidas.ContainsKey($_) }
if ($faltantes) { Stop-ConError "En .env faltan valores para: $($faltantes -join ', ')" }
Write-Ok '.env de la raíz completo'

$envFrontend = Join-Path $Raiz 'frontend\.env'
if (-not (Test-Path $envFrontend)) {
    Copy-Item (Join-Path $Raiz 'frontend\.env.example') $envFrontend
    Write-Ok 'frontend\.env creado desde frontend\.env.example'
} else {
    Write-Ok 'frontend\.env presente'
}

# 4. Puertos
Write-Paso 'Verificando puertos'
if (-not (Test-PuertoLibre 8080)) { Stop-ConError 'El puerto 8080 está ocupado. ¿Quedó un backend corriendo? Probá con -Detener o cerralo a mano.' }
if (-not (Test-PuertoLibre 5173)) { Stop-ConError 'El puerto 5173 está ocupado. ¿Quedó un frontend corriendo? Probá con -Detener o cerralo a mano.' }
Write-Ok '8080 y 5173 libres'

# 5. Arranque
Write-Paso 'Levantando backend (perfil dev, con datos de demo)'
$comandoBackend = "`$env:JAVA_HOME = '$javaHome'; `$env:Path = '$javaHome\bin;' + `$env:Path; " +
    "`$Host.UI.RawUI.WindowTitle = 'DentaFlow backend'; Set-Location '$Raiz\backend'; " +
    ".\mvnw.cmd -q spring-boot:run '-Dspring-boot.run.profiles=dev'"
$backend = Start-Process powershell.exe -ArgumentList @('-NoExit', '-NoProfile', '-Command', $comandoBackend) -PassThru

Write-Paso 'Levantando frontend (Vite)'
$instalar = if (Test-Path (Join-Path $Raiz 'frontend\node_modules')) { '' } else { 'npm install; ' }
$comandoFrontend = "`$Host.UI.RawUI.WindowTitle = 'DentaFlow frontend'; Set-Location '$Raiz\frontend'; ${instalar}npm run dev"
$frontend = Start-Process powershell.exe -ArgumentList @('-NoExit', '-NoProfile', '-Command', $comandoFrontend) -PassThru

Set-Content -Path $ArchivoPids -Value @($backend.Id, $frontend.Id) -Encoding ASCII

# 6. Espera al backend
Write-Paso "Esperando al backend (hasta $EsperaBackendSegundos s; la primera vez Maven descarga dependencias)"
$inicio = Get-Date
$listo = $false
while (((Get-Date) - $inicio).TotalSeconds -lt $EsperaBackendSegundos) {
    if ($backend.HasExited) { Stop-ConError 'La ventana del backend se cerró. Revisá el error en esa ventana.' }
    try {
        $salud = Invoke-RestMethod -Uri $UrlSalud -TimeoutSec 3
        if ($salud.status -eq 'UP') { $listo = $true; break }
    } catch {
        # Todavía arrancando: se reintenta.
    }
    Start-Sleep -Seconds 3
}
if (-not $listo) { Stop-ConError "El backend no respondió UP en $EsperaBackendSegundos s. Mirá la ventana 'DentaFlow backend' (base, contraseña de .env, JDK)." }
$segundos = [int]((Get-Date) - $inicio).TotalSeconds
Write-Ok "Backend UP en $segundos s ($UrlSalud)"

$frontendListo = $false
for ($i = 0; $i -lt 40; $i++) {
    if (-not (Test-PuertoLibre 5173)) { $frontendListo = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $frontendListo) { Stop-ConError "El frontend no levantó en el puerto 5173. Mirá la ventana 'DentaFlow frontend'." }
Write-Ok "Frontend en $UrlFrontend"

if (-not $SinNavegador) { Start-Process $UrlFrontend }

Write-Host ''
Write-Host 'Demo lista. Guion: docs\demo.md' -ForegroundColor Green
Write-Host 'Para cortar todo: powershell -ExecutionPolicy Bypass -File scripts\demo-local.ps1 -Detener'
Write-Host ''
