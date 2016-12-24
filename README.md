# Playground for Kotlin Serializatoin Prototype

Demo project for all the features implemented in the Kotlin serialization prototype.

## Serialization plugin

You need to obtain compiler plugin to work with Kotlin serialization prototype. 

### IDEA Plugin

IDEA Kotlin plugin for Kotlin 1.1-M04 with serialization:

* Download [`plugin/kotlin-plugin-1.1-M04-serialization.zip`](plugin/kotlin-plugin-1.1-M04-serialization.zip).
* In IDEA go to Settings > Plugins > Install plugin from disk ... and select this file.

### Command-line compiler plugin

The command-line compiler plugin for Kotlin 1.1-M04 is available in the [`lib`](#lib) subdirectory.

Use serialization plugin with kotlin compiler using the following command-line options:

```
kotlinc -Xplugin kotlin-serialization-compiler.jar \
        -cp kotlin-serialization-runtime.jar ...
```

## Serialization overview

Define serializable class with `@Serializable` annotation. For example:

```
@Serializable
data class Product(val name: String, val price: Double)
```

A few rules to follow:
* It does not have to be data-class.
* It must have `val`/`var` constructor parameters (simple constructor parameters are not supported yet)
* All properties in class body must be `var` (`val` properties are not supported yet)
* Generic classes cannot be serializable yet, but all standard collections are supported.

Now, you can convert the instance of this class to JSON with:

```
val str = JSON.stringify(Product("IntelliJ IDEA Ultimate", 199.0))
```

and parse the JSON string back into an instance with:

```
val obj = JSON.parse<Product>(str)
```

You can define your own serialization formats with a bit of library code:

* See [MapIO](src/MapIO.kt) for a sample on how to convert an object to/from map.
* See [MapNullableIO](src/MapNullableIO.kt) for the same as above with support for nullable types.
* See [DataBinaryNullableIO](src/DataBinaryNullableIO.kt) for a simple binary format implementation.
* See [KeyValueIO](src/KeyValueIO.kt) for a simple JSON-like format implementation.

## Plugin sources

See https://github.com/JetBrains/kotlin/tree/rr/elizarov/kotlin-serialization

Build prototype with 
```
ant dist -Dkotlin-serialization=true
```
