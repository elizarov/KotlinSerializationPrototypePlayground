import kotlin.serialization.KSerializer
import kotlin.serialization.NamedValueInput
import kotlin.serialization.NamedValueOutput

class MapOutput(val map: MutableMap<String, Any> = mutableMapOf()) : NamedValueOutput() {
    override fun writeNamed(name: String, value: Any) {
        map[name] = value
    }
}

class MapInput(val map: Map<String, Any>) : NamedValueInput() {
    override fun readNamed(name: String): Any {
        return map[name]!!
    }
}

fun testMapIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val out = MapOutput()
    out.write(serializer, obj)
    // load
    val inp = MapInput(out.map)
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${out.map.size} items ${out.map}")
}

fun main(args: Array<String>) {
//    testCase(CityData, CityData(1, "New York"), ::testMapIO)
//    testCase(StreetData, StreetData(2, "Broadway", CityData(1, "New York")), ::testMapIO)
//    testCase(StreetData2, StreetData2(2, "Broadway", CityData(1, "New York")), ::testMapIO)
//    testCase(StreetData2, StreetData2(2, "Broadway", null), ::testMapIO)
//    testCase(CountyData, CountyData("US", listOf(CityData(1, "New York"), CityData(2, "Chicago"))), ::testMapIO)
    testMethod(::testMapIO)
}
