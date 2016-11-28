import kotlin.reflect.KFunction
import kotlin.reflect.companionObjectInstance
import kotlin.serialization.KSerializer
import kotlin.serialization.Serializable

// -------------------------------------------
// simple data objects

@Serializable
data class CityData(
        val id: Int,
        val name: String
)

@Serializable
data class StreetData(
        val id: Int,
        val name: String,
        val city: CityData
)

@Serializable
data class StreetData2(
        val id: Int,
        val name: String,
        val city: CityData?
)

@Serializable
data class CountyData(
        val name: String,
        val cities: List<CityData>
)

// -------------------------------------------
// testing framework

data class Result(
        val obj: Any, // original object
        val res: Any, // resulting object
        val ext: Any  // serialized (external) representation
)

class Case<T: Any>(
        val obj: T,
        val name: String = obj.javaClass.simpleName
) {
    @Suppress("UNCHECKED_CAST")
    val serializer: KSerializer<T> = obj::class.companionObjectInstance as KSerializer<T>
}

val testCases: List<Case<*>> = listOf(
        Case(CityData(1, "New York")),
        Case(StreetData(2, "Broadway", CityData(1, "New York"))),
        Case(StreetData2(2, "Broadway", CityData(1, "New York"))),
        Case(StreetData2(2, "Broadway", null)),
        Case(CountyData("US", listOf(CityData(1, "New York"), CityData(2, "Chicago")))),
        Case(zoo),
        Case(shop)
)

@Suppress("UNCHECKED_CAST")
fun <T: Any> testCase(serializer: KSerializer<T>, obj: T, method: (KSerializer<Any>, Any) -> Result): Boolean {
    println("Start with $obj")
    val result = try { method(serializer as KSerializer<Any>, obj) } catch (e: Throwable) {
        println("Failed with $e")
        return false
    }
    println("Loaded obj ${result.res}")
    println("    equals=${obj == result.res}, sameRef=${obj === result.res}")
    println("Saved form ${result.ext}")
    return obj == result.res
}

fun testCase(case: Case<Any>, method: (KSerializer<Any>, Any) -> Result): Boolean {
    println("Test case ${case.name}")
    return testCase(case.serializer, case.obj, method)
}

@Suppress("UNCHECKED_CAST")
fun testMethod(method: (KSerializer<Any>, Any) -> Result) {
    println("==============================================")
    println("Running with ${(method as KFunction<*>).name}")
    var totalCount = 0
    var failCount = 0
    testCases.forEach { case ->
        println()
        if (!testCase(case as Case<Any>, method))
            failCount++
        totalCount++
    }
    println("==============================================")
    println("Done with ${(method as KFunction<*>).name}")
    if (failCount > 0)
        println("!!! FAILED $failCount TEST CASES OUT OF $totalCount TEST CASES !!!")
    else
        println("Passed $totalCount test cases")
}

