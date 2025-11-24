/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.openai.java

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAiJavaModelIndexTest {

    @Test
    fun testModels() {
        assertTrue(OpenAiJavaModelIndex.javaClass.getResourceAsStream("resources/openai-models.yaml") != null)

        println(OpenAiJavaModelIndex.audioModels())
        println(OpenAiJavaModelIndex.chatModels())
        println(OpenAiJavaModelIndex.completionModels())
        println(OpenAiJavaModelIndex.embeddingModels())
        println(OpenAiJavaModelIndex.multimodalModels())
        println(OpenAiJavaModelIndex.moderationModels())
        println(OpenAiJavaModelIndex.ttsModels())
        println(OpenAiJavaModelIndex.imageGeneratorModels())
        println(OpenAiJavaModelIndex.visionLanguageModels())

        assertTrue(OpenAiJavaModelIndex.chatModels().isNotEmpty())
        assertTrue(OpenAiJavaModelIndex.embeddingModels().isNotEmpty())
        assertTrue(OpenAiJavaModelIndex.multimodalModels().isNotEmpty())
    }

}
