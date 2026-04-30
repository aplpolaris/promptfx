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
package tri.ai.openaisdk

import tri.ai.core.ModelIndex

/** Index of OpenAI models (reuses the same YAML as promptkt-provider-openai). */
object OpenAiSdkModelIndex : ModelIndex("openai-models.yaml") {

    /** Model source identifier for the OpenAI SDK. */
    const val MODEL_SOURCE = "OpenAI SDK"

    //region MODEL ID'S

    private const val WHISPER_ID = "whisper-1"
    const val ADA_ID = "text-embedding-ada-002"
    const val DALLE2_ID = "dall-e-2"
    const val GPT4O_ID = "gpt-4o"
    const val GPT4O_MINI_ID = "gpt-4o-mini"
    const val TTS_1_ID = "tts-1"

    //endregion

    val AUDIO_WHISPER = modelInfoIndex[WHISPER_ID]?.id ?: WHISPER_ID
    val EMBEDDING_ADA = modelInfoIndex[ADA_ID]?.id ?: ADA_ID
    val IMAGE_DALLE2 = modelInfoIndex[DALLE2_ID]?.id ?: DALLE2_ID
    val GPT4O = modelInfoIndex[GPT4O_ID]?.id ?: GPT4O_ID
    val GPT4O_MINI = modelInfoIndex[GPT4O_MINI_ID]?.id ?: GPT4O_MINI_ID
    val TTS_1 = modelInfoIndex[TTS_1_ID]?.id ?: TTS_1_ID

}
