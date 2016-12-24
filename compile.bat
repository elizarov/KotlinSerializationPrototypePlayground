@echo off
SET KOTLIN_HOME=..\kotlin\dist\kotlinc
SET OUT=out\compiled
"%KOTLIN_HOME%\bin\kotlinc" -Xplugin lib/kotlin-serialization-compiler.jar -cp lib/kotlin-serialization-runtime.jar -d %OUT% src\*.kt
