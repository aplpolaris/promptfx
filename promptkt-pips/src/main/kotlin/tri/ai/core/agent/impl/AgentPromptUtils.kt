package tri.ai.core.agent.impl

import tri.ai.prompt.PromptLibrary

val PROMPTS = PromptLibrary.readFromResourceDirectory<AgentPromptUtils>()

object AgentPromptUtils