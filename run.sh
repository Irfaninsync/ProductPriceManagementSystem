#!/usr/bin/env bash
if [ ! -f "bin/com/ppms/app/Main.class" ]; then
    bash compile.sh
fi
java -cp bin com.ppms.app.Main
