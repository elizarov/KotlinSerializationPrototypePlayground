@echo off
SET KOTLIN_HOME=..\kotlin\dist\kotlinc
SET OUT=out\compiled
java -cp %OUT%;%KOTLIN_HOME%/lib/kotlin-runtime.jar;;%KOTLIN_HOME%/lib/kotlin-reflect.jar;%KOTLIN_HOME%/lib/kotlin-serialization-runtime.jar KeyValueIOKt
