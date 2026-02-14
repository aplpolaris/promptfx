/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
