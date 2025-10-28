package tri.util.ui.starship

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptChatRegistry
import tri.ai.openai.OpenAiPlugin
import tri.ai.pips.PrintMonitor
import tri.ai.pips.api.PPlanExecutor
import tri.ai.prompt.PromptLibrary

class StarshipConfigTest {

    @Test
    fun testLoad() {
        val config = StarshipConfig.readDefaultYaml()
        println("Loaded plan: ${config.pipeline}")
    }

    @Test
    fun testRandomQuestion() {
        val config = StarshipConfig.readDefaultYaml()
        val questioner = StarshipExecutableQuestionGenerator(config.question, OpenAiPlugin().chatModels().first())
        println(questioner.randomQuestion())
        println(questioner.randomQuestion())
    }

    @Disabled("Need alternative to the current view executor")
    @Test
    fun testExecute() {
        val config = StarshipConfig.readDefaultYaml()
        val chat = OpenAiPlugin().chatModels().first()
        val registry = ExecutableRegistry.Companion.create(
            listOf(StarshipExecutableQuestionGenerator(config.question, chat)) +
                    PromptChatRegistry(PromptLibrary.Companion.INSTANCE, chat).list()
        )

        runBlocking {
            PPlanExecutor(registry).execute(config.pipeline, ExecContext(), PrintMonitor())
        }
    }
}