#!/usr/bin/env bash
# GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
if [ ! -f "bin/com/ppms/app/Main.class" ]; then
    bash compile.sh
fi
java -cp bin com.ppms.app.Main
# End of script — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

