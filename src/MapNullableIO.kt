import kotlin.serialization.KSerializer
import kotlin.serialization.NamedValueInput
import kotlin.serialization.NamedValueOutput

class MapNullableOutput(val map: MutableMap<String, Any?>) : NamedValueOutput() {
    override fun writeNamedNull(name: String) {
        map.remove(name)
    }

    override fun writeNamed(name: String, value: Any) {
        map[name] = value
    }
}

class MapNullableInput(val map: Map<String, Any?>) : NamedValueInput() {
    override fun readNamedNotNullMark(name: String): Boolean {
        return map.containsKey(name)
    }

    override fun readNamed(name: String): Any {
        return map[name]!!
    }
}

fun testMapNullableIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val out = MapNullableOutput(mutableMapOf())
    out.write(serializer, obj)
    // load
    val inp = MapNullableInput(out.map)
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${out.map.size} items ${out.map}")
}

fun main(args: Array<String>) {
//    testCase(CityData, CityData(1, "New York"), ::testMapNullableIO)
//    testCase(StreetData, StreetData(2, "Broadway", CityData(1, "New York")), ::testMapNullableIO)
//    testCase(StreetData2, StreetData2(2, "Broadway", CityData(1, "New York")), ::testMapNullableIO)
//    testCase(StreetData2, StreetData2(2, "Broadway", null), ::testMapNullableIO)
//    testCase(CountyData, CountyData("US", listOf(CityData(1, "New York"), CityData(2, "Chicago"))), ::testMapNullableIO)
//    testCase(Zoo, zoo, ::testMapNullableIO)
    testMethod(::testMapNullableIO)
}
