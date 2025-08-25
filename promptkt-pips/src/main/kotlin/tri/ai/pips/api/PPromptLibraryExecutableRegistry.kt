package tri.ai.pips.api

import tri.ai.prompt.PromptLibrary

/** Creates an executable registry from a prompt library file. */
class PPromptLibraryExecutableRegistry(private val lib: PromptLibrary): PExecutableRegistry {

    private val executables by lazy {
        lib.list().associate { def ->
            val exec = PPromptExecutable(def)
            exec.name to exec
        }
    }

    override fun get(name: String) = executables[name]

    override fun list() = executables.values.toList()

}