import org.junit.Assert.fail
import org.junit.Test
import kotlin.serialization.KSerializer

class SelfTest {
    @Test
    fun testAll() {
        println("============= Running self-test =============")
        val methods = listOf<(KSerializer<Any>, Any) -> Result>(
            ::testCloneIO,
            ::testDataBinaryNullableIO,
            ::testMapNullableIO,
            ::testJsonIO,
            ::testJsonUnquotedIO,
            ::testJsonIndentedIO
        )
        var sumFailCount = 0
        var sumTotalCount = 0
        for (method in methods) {
            val (failCount, totalCount) = testMethod(method, verbose = false)
            sumFailCount += failCount
            sumTotalCount += totalCount
        }
        println("============= Done self-test =============")
        if (sumFailCount > 0)
            fail("!!! FAILED $sumFailCount TEST CASES OUT OF $sumTotalCount TEST CASES !!!")
        else
            println("Passed $sumTotalCount test cases")
    }
}