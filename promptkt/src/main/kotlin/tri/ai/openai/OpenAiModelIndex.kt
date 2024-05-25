/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.openai

import tri.ai.core.ModelIndex

/** Index of OpenAI models. */
object OpenAiModelIndex : ModelIndex("openai-models.yaml") {

    //region MODEL ID'S

    private const val WHISPER_ID = "whisper-1"
    const val ADA_ID = "text-embedding-ada-002"
    private const val DALLE2_ID = "dall-e-2"
    private const val DALLE3_ID = "dall-e-3"
    private const val GPT35_TURBO_ID = "gpt-3.5-turbo"
    private const val GPT35_TURBO_INSTRUCT_ID = "gpt-3.5-turbo-instruct"

    //endregion

    val AUDIO_WHISPER = MODEL_INDEX[WHISPER_ID]!!.id
    val EMBEDDING_ADA = MODEL_INDEX[ADA_ID]!!.id
    val IMAGE_DALLE2 = MODEL_INDEX[DALLE2_ID]!!.id
    val GPT35_TURBO = MODEL_INDEX[GPT35_TURBO_ID]!!.id
    val GPT35_TURBO_INSTRUCT = MODEL_INDEX[GPT35_TURBO_INSTRUCT_ID]!!.id

}