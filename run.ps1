# GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
# PPMS PowerShell Launch Script
if (!(Test-Path "bin/com/ppms/app/Main.class")) {
    & ./build.ps1
}
java -cp "bin;lib/*" com.ppms.app.Main @args
# End of script — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
