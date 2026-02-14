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
package tri.ai.core.agent.impl

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentFlowLogger
import tri.ai.core.tool.JsonToolExecutableTest.Companion.SAMPLE_EXECUTABLES
import tri.ai.openai.OpenAiAdapter
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO

@Tag("openai")
class JsonToolExecutorTest {

    private val MODEL_ID = GPT35_TURBO
    private val CHAT_CONFIG = AgentChatConfig(modelId = MODEL_ID)

    @Test
    fun testExecute() {
        OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None
        val exec = JsonToolExecutor(SAMPLE_EXECUTABLES)

        println()
        listOf(
            "Multiple 21 times 2 and then convert it to Roman numerals.",
            "Convert 5 to a Roman numeral.",
            "What year was Jurassic Park?"
        ).forEach {
            val flow = exec.sendMessage(AgentChatSession(config = CHAT_CONFIG), MultimodalChatMessage.user(it))
            runBlocking {
                flow.events.collect(AgentFlowLogger(verbose = true))
                println()
            }
        }
    }

}
