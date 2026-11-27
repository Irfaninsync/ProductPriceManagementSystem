# GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
# PPMS PowerShell Build Script
Write-Host "Compiling Product Price Management System (PPMS)..." -ForegroundColor Cyan
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

$srcFiles = (Get-ChildItem -Path "src" -Recurse -Filter "*.java").FullName
$cp = "lib/*"
javac -cp $cp -d bin -encoding UTF-8 $srcFiles

if ($LASTEXITCODE -eq 0) {
    if (Test-Path "src/main/resources") {
        Copy-Item -Path "src/main/resources/*" -Destination "bin" -Recurse -Force
    }
    Write-Host "[SUCCESS] Compilation and resource sync successful into bin/" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    exit 1
}
# End of script — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
