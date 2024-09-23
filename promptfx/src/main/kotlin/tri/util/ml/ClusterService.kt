package tri.util.ml

/** Interface for a clustering service for arbitrary set of items. */
interface ClusterService {
    /** Generate cluster of given items with given embedding. */
    fun <T> cluster(items: List<T>, op: (T) -> List<Double>): List<List<T>>
}