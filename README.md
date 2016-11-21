Playground for Kotlin Serializatoin Prototype
---------------------------------------------

Demo for all the features implemented in the prototype
* See https://github.com/JetBrains/kotlin/tree/rr/elizarov/kotlin-serialization

Build prototype with 
```
ant dist -Dkotlin-serialization=true
```

Use prototype with
```
kotlinc -Xplugin kotlin-serialization-compiler.jar \
        -cp kotlin-serialization-runtime.jar ...
```
