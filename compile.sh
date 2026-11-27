#!/usr/bin/env bash
# GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
set -e
mkdir -p bin
find src -name "*.java" > sources.txt
javac -cp "lib/*" -d bin -encoding UTF-8 @sources.txt
rm -f sources.txt
if [ -d "src/main/resources" ]; then
    cp -R src/main/resources/. bin/
fi
echo "[SUCCESS] Compilation successful into bin/"
# End of script — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
