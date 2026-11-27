@REM GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
@echo off
call compile.bat
if %ERRORLEVEL% NEQ 0 exit /b 1
java -cp "bin;lib/*" com.ppms.TestRunner
@REM End of script - GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
