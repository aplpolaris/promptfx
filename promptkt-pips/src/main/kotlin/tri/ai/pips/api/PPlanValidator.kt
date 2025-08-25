package tri.ai.pips.api

/** Validates a [PPlan] object. */
object PPlanValidator {

    /** Checks there are no duplicate saveAs names. */
    fun validateNames(plan: PPlan) {
        val dups = plan.steps.mapNotNull { it.saveAs }.groupBy { it }.filter { it.value.size > 1 }
        require(dups.isEmpty()) { "Duplicate saveAs vars: ${dups.keys}" }
    }

    /** Checks that all tools exist within the registry */
    fun validateToolsExist(plan: PPlan, registry: PExecutableRegistry) {
        val missing = plan.steps.map { it.tool }.filter { registry.get(it) == null }
        require(missing.isEmpty()) {
            "Unknown tools: $missing" +
            "    Valid tools: ${registry.list().map { it.name }}"
        }
    }

}