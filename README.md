# Playground for Kotlin Serializatoin Prototype

Demo project for all the features implemented in the Kotlin serialization prototype.

## Serialization plugin

You need to obtain compiler plugin to work with Kotlin serialization prototype. 

### IDEA Plugin

IDEA Kotlin plugin for Kotlin 1.1.2 with serialization:

* Make sure you have IntelliJ IDEA 2017.1 installed
* Download [`plugin/kotlin-plugin-1.1.2-serialization.zip`](plugin/kotlin-plugin-1.1.2-serialization.zip).
* In IDEA go to Settings > Plugins > Install plugin from disk ... and select this file.

### Command-line compiler plugin

The command-line compiler plugin for Kotlin 1.1.2 is available in the [`lib`](#lib) subdirectory.

Use serialization plugin with kotlin compiler using the following command-line options:

```sh
kotlinc -Xplugin kotlin-serialization-compiler.jar \
        -cp kotlin-serialization-runtime.jar ...
```

### Gradle compiler plugin

The plugin for building Gradle project with is is available in the [`lib`](#lib) subdirectory.
See [`build.gradle`](build.gradle) as example on how to use it. You need the following configuration
in your build file:

```groovy
compileKotlin {
    kotlinOptions.freeCompilerArgs += ["-Xplugin", "lib/kotlin-serialization-gradle.jar"]
}
```

## Serialization overview

Define serializable class with `@Serializable` annotation. For example:

```kotlin
@Serializable
data class Product(val name: String, val price: Double)
```

A few rules to follow:
* It does not have to be data-class.
* It must have `val`/`var` constructor parameters (simple constructor parameters are not supported yet)
* All properties in class body must be `var` (`val` properties are not supported yet)
* Generic classes cannot be serializable yet, but all standard collections are supported.

Now, you can convert the instance of this class to JSON with:

```kotlin
val str = JSON.stringify(Product("IntelliJ IDEA Ultimate", 199.0))
```

and parse the JSON string back into an instance with:

```kotlin
val obj = JSON.parse<Product>(str)
```

You can define your own serialization formats with a bit of library code:

* See [MapIO](src/MapIO.kt) for a sample on how to convert an object to/from map.
* See [MapNullableIO](src/MapNullableIO.kt) for the same as above with support for nullable types.
* See [DataBinaryNullableIO](src/DataBinaryNullableIO.kt) for a simple binary format implementation.
* See [KeyValueIO](src/KeyValueIO.kt) for a simple JSON-like format implementation.

## Test 

This project contains a test with serialization of various complex data structures with different formats. 
Run it using the following command:

```sh
gradlew test
```

You should get "BUILD SUCCESSFUL" line at the end:

## Plugin sources

See https://github.com/JetBrains/kotlin/tree/rr/elizarov/kotlin-serialization

Build prototype with 

```sh
ant dist -Dkotlin-serialization=true
```
