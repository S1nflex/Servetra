param(
    [string]$TomcatHome = $env:CATALINA_HOME,
    [string]$MySqlExe = ""
)

$ErrorActionPreference = "Stop"

function Show-CommandVersion([string]$Name, [string[]]$Arguments) {
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        Write-Host "[MISSING] $Name" -ForegroundColor Red
        return $false
    }
    Write-Host "[FOUND] $Name -> $($command.Source)" -ForegroundColor Green
    & $Name @Arguments 2>&1 | Select-Object -First 3
    return $true
}

Write-Host "SERVETRA Windows 11 environment check" -ForegroundColor Cyan
$javaOk = Show-CommandVersion "java" @("-version")
$javacOk = Show-CommandVersion "javac" @("-version")
$mavenOk = Show-CommandVersion "mvn" @("-version")
$mysqlCommand = Get-Command "mysql.exe" -ErrorAction SilentlyContinue
if (-not $mysqlCommand) {
    foreach ($candidate in @($MySqlExe, "C:\xampp\mysql\bin\mysql.exe", "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe", "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe")) {
        if ($candidate -and (Test-Path $candidate -PathType Leaf)) {
            $mysqlCommand = Get-Item $candidate
            break
        }
    }
}
if ($mysqlCommand) {
    Write-Host "[FOUND] mysql -> $($mysqlCommand.FullName)" -ForegroundColor Green
    & $mysqlCommand.FullName --version
    $mysqlOk = $true
} else {
    Write-Host "[MISSING] mysql.exe" -ForegroundColor Red
    $mysqlOk = $false
}

if (-not $TomcatHome) {
    foreach ($candidate in @("C:\Tools\apache-tomcat-9", "C:\apache-tomcat-9", "C:\Program Files\Apache Software Foundation\Tomcat 9.0")) {
        if (Test-Path (Join-Path $candidate "bin\catalina.bat")) {
            $TomcatHome = $candidate
            break
        }
    }
}

$tomcatOk = $false
if ($TomcatHome -and (Test-Path (Join-Path $TomcatHome "bin\catalina.bat"))) {
    Write-Host "[FOUND] Tomcat -> $TomcatHome" -ForegroundColor Green
    $version = (& (Join-Path $TomcatHome "bin\catalina.bat") version 2>&1 | Out-String)
    $tomcatOk = $version -match "Apache Tomcat/9\."
    if (-not $tomcatOk) { Write-Host "[WRONG VERSION] Tomcat 9 is required." -ForegroundColor Red }
} else {
    Write-Host "[MISSING] Tomcat 9. Pass -TomcatHome or set CATALINA_HOME." -ForegroundColor Red
}

if (-not ($javaOk -and $javacOk -and $mavenOk -and $mysqlOk -and $tomcatOk)) {
    exit 1
}

Write-Host "[READY] Environment can run SERVETRA." -ForegroundColor Green
