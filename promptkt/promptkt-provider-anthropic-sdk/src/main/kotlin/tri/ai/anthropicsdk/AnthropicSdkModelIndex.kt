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
package tri.ai.anthropicsdk

import tri.ai.core.ModelIndex

/** Models available via the Anthropic SDK. */
object AnthropicSdkModelIndex : ModelIndex("anthropic-sdk-models.yaml") {

    /** Model source identifier for the Anthropic SDK. */
    const val MODEL_SOURCE = "Anthropic-SDK"

    //region MODEL ID's

    const val CLAUDE_3_5_HAIKU = "claude-3-5-haiku-latest"
    const val CLAUDE_3_5_SONNET = "claude-3-5-sonnet-latest"
    const val CLAUDE_3_7_SONNET = "claude-3-7-sonnet-latest"
    const val CLAUDE_OPUS_4_5 = "claude-opus-4-5"
    const val CLAUDE_SONNET_4_5 = "claude-sonnet-4-5"
    const val CLAUDE_HAIKU_4_5 = "claude-haiku-4-5"
    const val CLAUDE_OPUS_4_0 = "claude-opus-4-0"
    const val CLAUDE_SONNET_4_0 = "claude-sonnet-4-0"

    //endregion

}
