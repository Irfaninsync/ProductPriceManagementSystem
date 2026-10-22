# PPMS PowerShell Build Script
Write-Host "Compiling Product Price Management System (PPMS)..." -ForegroundColor Cyan
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

$srcFiles = (Get-ChildItem -Path "src" -Recurse -Filter "*.java").FullName
javac -d bin -encoding UTF-8 $srcFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Compilation successful into bin/" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
}
