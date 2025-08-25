package tri.ai.pips.core

import tri.ai.prompt.PromptLibrary

/** Creates an executable registry from a prompt library file. */
class PromptLibraryExecutableRegistry(private val lib: PromptLibrary): ExecutableRegistry {

    private val executables by lazy {
        lib.list().associate { def ->
            val exec = PromptExecutable(def)
            exec.name to exec
        }
    }

    override fun get(name: String) = executables[name]

    override fun list() = executables.values.toList()

}