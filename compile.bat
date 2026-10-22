@echo off
echo =====================================================================
echo  Compiling Product Price Management System (PPMS)
echo =====================================================================

if not exist bin mkdir bin

powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = (Get-ChildItem -Path 'src' -Recurse -Filter '*.java').FullName; javac -d bin -encoding UTF-8 $files"

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully into bin/
) else (
    echo [ERROR] Compilation failed.
)
