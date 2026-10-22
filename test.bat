@REM GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
@echo off
if not exist bin\com\ppms\TestRunner.class (
    echo Binaries not found. Running compilation first...
    call compile.bat
)
java -cp bin com.ppms.TestRunner
@REM End of script - GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
