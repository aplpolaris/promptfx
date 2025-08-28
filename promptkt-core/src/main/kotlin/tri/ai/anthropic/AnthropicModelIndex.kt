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
package tri.ai.anthropic

import tri.ai.core.ModelIndex

/** Index of Anthropic models. */
object AnthropicModelIndex : ModelIndex("anthropic-models.yaml") {

    //region MODEL ID'S

    const val CLAUDE_3_HAIKU_ID = "claude-3-haiku-20240307"
    const val CLAUDE_3_SONNET_ID = "claude-3-sonnet-20240229"
    const val CLAUDE_3_OPUS_ID = "claude-3-opus-20240229"
    const val CLAUDE_3_5_SONNET_ID = "claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_HAIKU_ID = "claude-3-5-haiku-20241022"

    //endregion

    val CLAUDE_3_HAIKU = modelInfoIndex[CLAUDE_3_HAIKU_ID]?.id ?: CLAUDE_3_HAIKU_ID
    val CLAUDE_3_SONNET = modelInfoIndex[CLAUDE_3_SONNET_ID]?.id ?: CLAUDE_3_SONNET_ID
    val CLAUDE_3_OPUS = modelInfoIndex[CLAUDE_3_OPUS_ID]?.id ?: CLAUDE_3_OPUS_ID
    val CLAUDE_3_5_SONNET = modelInfoIndex[CLAUDE_3_5_SONNET_ID]?.id ?: CLAUDE_3_5_SONNET_ID
    val CLAUDE_3_5_HAIKU = modelInfoIndex[CLAUDE_3_5_HAIKU_ID]?.id ?: CLAUDE_3_5_HAIKU_ID

}