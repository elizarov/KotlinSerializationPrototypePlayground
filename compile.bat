@echo off
SET KOTLIN_HOME=..\kotlin\dist\kotlinc
SET OUT=out\compiled
%KOTLIN_HOME%\bin\kotlinc -Xplugin %KOTLIN_HOME%/lib/kotlin-serialization-compiler.jar -d %OUT% src\*.kt
