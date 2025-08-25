package tri.ai.pips.core

/** Registry so plan names resolve to code. */
interface ExecutableRegistry {
    fun get(name: String): Executable?
    fun list(): List<Executable>

    companion object {

        /** Creates a registry from a list of executables. */
        fun create(listOf: List<Executable>) = object : ExecutableRegistry {
            val index = listOf.associateBy { it.name }
            override fun get(name: String) = index[name]
            override fun list() = index.values.toList()
        }

    }
}

/**
 * Registry merging multiple other registries.
 * Naive implementation allows executables in later registries to override earlier ones.
 */
class MergedExecutableRegistry(registries: List<ExecutableRegistry>) : ExecutableRegistry {
    private val mergedIndex = registries
        .flatMap { it.list() }
        .associateBy { it.name }
    override fun get(name: String) = mergedIndex[name]
    override fun list() = mergedIndex.values.toList()
}