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
package tri.ai.core

import java.net.URI

/** Interface for image generation. */
interface ImageGenerator : AiModel {
    /** Generate images, returning a URI for each (may be an HTTP URL or a data: URI for base64-encoded images). */
    suspend fun generateImage(
        text: String,
        size: ImageSize,
        numResponses: Int? = null
    ): List<URI>

}

data class ImageSize(val width: Int, val height: Int)
