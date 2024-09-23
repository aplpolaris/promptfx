package tri.util.ml

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AffinityClusterServiceTest {

    @Test
    fun testCluster() {
        val service = AffinityClusterService()
        val items = listOf(
            listOf(1.0, 2.0, 3.0),
            listOf(4.0, 5.0, 6.0),
            listOf(7.0, 8.0, 9.0),
            listOf(10.0, 11.0, 12.0),
            listOf(1.0, 2.0, 3.5),
            listOf(4.0, 5.0, 6.5)
        )
        val clusters = service.cluster(items) { it }
        assertEquals(3, clusters.size)
        assertEquals(listOf(items[0], items[4]), clusters[0])
        assertEquals(listOf(items[1], items[2], items[5]), clusters[1])
        assertEquals(listOf(items[3]), clusters[2])
    }

}