package tri.ai.pips.api

/** Registry so plan names resolve to code. */
interface PExecutableRegistry {
    fun get(name: String): PExecutable?
    fun list(): List<PExecutable>

    companion object {
        /** Creates a registry from a list of executables. */
        fun create(listOf: List<PExecutable>) = object : PExecutableRegistry {
            val index = listOf.associateBy { it.name }
            override fun get(name: String) = index[name]
            override fun list() = index.values.toList()
        }
    }
}