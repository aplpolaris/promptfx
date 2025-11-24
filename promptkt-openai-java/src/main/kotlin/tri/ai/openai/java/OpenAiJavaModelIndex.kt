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

import tri.ai.core.ModelIndex

/** Index of OpenAI models available via the official Java SDK. */
object OpenAiJavaModelIndex : ModelIndex("openai-models.yaml") {

    //region MODEL ID'S

    const val EMBEDDING_ADA = "text-embedding-ada-002"
    const val EMBEDDING_3_SMALL = "text-embedding-3-small"
    const val EMBEDDING_3_LARGE = "text-embedding-3-large"
    
    const val GPT_35_TURBO = "gpt-3.5-turbo"
    const val GPT_4_TURBO = "gpt-4-turbo"
    const val GPT_4O = "gpt-4o"
    const val GPT_4O_MINI = "gpt-4o-mini"
    
    const val DALLE2 = "dall-e-2"
    const val DALLE3 = "dall-e-3"

    //endregion

}
