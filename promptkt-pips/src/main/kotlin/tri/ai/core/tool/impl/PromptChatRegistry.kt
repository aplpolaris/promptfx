package tri.ai.core.tool.impl

import tri.ai.core.TextChat
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.prompt.PromptLibrary

/** Creates an executable registry from a prompt library file, with executables returning LLM chat responses. */
class PromptChatRegistry(private val lib: PromptLibrary, chat: TextChat): ExecutableRegistry {

    private val chatExecutables by lazy {
        lib.list().associate { def ->
            val exec = PromptChatExecutable(def, chat)
            exec.name to exec
        }
    }

    override fun get(name: String) = chatExecutables[name]

    override fun list() = chatExecutables.values.toList()

}