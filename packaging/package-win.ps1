# GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
# Packages PPMS into a standalone Windows application image (includes bundled JRE; no Java required on target PC)
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
Set-Location $rootDir

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host " Building Standalone Windows Distribution (jpackage app-image)       " -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan

# 1. Compile main sources + resources cleanly (excluding test classes)
$buildMainDir = Join-Path $rootDir "dist\classes"
$inputDir = Join-Path $rootDir "dist\input"
$appOutDir = Join-Path $rootDir "dist\PPMS"

if (Test-Path $buildMainDir) { Remove-Item -Recurse -Force $buildMainDir }
if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
if (Test-Path $appOutDir) { Remove-Item -Recurse -Force $appOutDir }

New-Item -ItemType Directory -Force -Path $buildMainDir, $inputDir | Out-Null

$mainSources = (Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java").FullName
javac -cp "lib/*" -d $buildMainDir -encoding UTF-8 $mainSources
if ($LASTEXITCODE -ne 0) { throw "Compilation of main sources failed." }

if (Test-Path "src\main\resources") {
    Copy-Item -Path "src\main\resources\*" -Destination $buildMainDir -Recurse -Force
}

# 2. Build executable ppms-app.jar and stage dependencies in dist\input\
Copy-Item -Path "lib\*.jar" -Destination $inputDir -Force
$jarPath = Join-Path $inputDir "ppms-app.jar"
jar --create --file $jarPath --main-class com.ppms.app.Main -C $buildMainDir .
if ($LASTEXITCODE -ne 0) { throw "JAR creation failed." }

# 3. Invoke jpackage to bundle minimal JRE + native PPMS.exe launcher
$jpackageArgs = @(
    "--type", "app-image",
    "--name", "PPMS",
    "--app-version", "2.0.0",
    "--vendor", "PPMS",
    "--description", "Product Price Management System (Local Business Application)",
    "--input", $inputDir,
    "--main-jar", "ppms-app.jar",
    "--main-class", "com.ppms.app.Main",
    "--dest", "dist",
    "--add-modules", "java.base,java.sql,java.desktop,jdk.httpserver,java.logging"
)

$iconPath = Join-Path $scriptDir "ppms-icon.ico"
if (Test-Path $iconPath) {
    $jpackageArgs += @("--icon", $iconPath)
}

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

# Clean intermediate staging
Remove-Item -Recurse -Force $buildMainDir, $inputDir

Write-Host ""
Write-Host "[SUCCESS] Standalone Windows bundle ready at: $appOutDir\PPMS.exe" -ForegroundColor Green
Write-Host "You can copy the entire dist\PPMS\ folder to any Windows 10/11 PC and run PPMS.exe without installing Java." -ForegroundColor Gray
