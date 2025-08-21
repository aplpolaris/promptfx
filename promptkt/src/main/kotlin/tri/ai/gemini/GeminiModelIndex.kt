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
package tri.ai.gemini

import tri.ai.core.ModelIndex

/** Models available in the Gemini API. */
object GeminiModelIndex : ModelIndex("gemini-models.yaml") {

    //region MODEL ID's

    const val EMBED1 = "embedding-001"
    const val EMBED4 = "text-embedding-004"
    const val EMBED5 = "text-embedding-005"

    const val GEMINI_PRO_VISION = "gemini-1.0-pro-vision"
    const val GEMINI_15_FLASH = "gemini-1.5-flash"
    const val GEMINI_15_PRO = "gemini-1.5-prop"

    const val IMAGEN_3_GENERATE = "imagen-3.0-generate-001"
    const val IMAGEN_3_FAST_GENERATE = "imagen-3.0-fast-generate-001"

    //endregion

}
