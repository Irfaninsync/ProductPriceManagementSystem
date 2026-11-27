@REM GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
@echo off
if not exist bin\com\ppms\app\Main.class (
    echo Binaries not found. Running compilation first...
    call compile.bat
    if %ERRORLEVEL% NEQ 0 exit /b 1
)
java -cp "bin;lib/*" com.ppms.app.Main %*
@REM End of script - GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
