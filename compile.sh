#!/usr/bin/env bash
mkdir -p bin
find src -name "*.java" > sources.txt
javac -d bin -encoding UTF-8 @sources.txt
rm sources.txt
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Compilation successful into bin/"
else
    echo "[ERROR] Compilation failed."
fi
