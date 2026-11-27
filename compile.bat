@REM GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
@echo off
echo =====================================================================
echo  Compiling Product Price Management System (PPMS)
echo =====================================================================

if not exist bin mkdir bin

powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = (Get-ChildItem -Path 'src' -Recurse -Filter '*.java').FullName; javac -cp 'lib/*' -d bin -encoding UTF-8 $files; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }; if (Test-Path 'src\main\resources') { Copy-Item -Path 'src\main\resources\*' -Destination 'bin' -Recurse -Force }"

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully into bin/
) else (
    echo [ERROR] Compilation failed.
    exit /b 1
)
@REM End of script - GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
