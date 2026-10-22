@REM GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
@echo off
if not exist bin\com\ppms\app\Main.class (
    echo Binaries not found. Running compilation first...
    call compile.bat
)
java -cp bin com.ppms.app.Main
@REM End of script - GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
