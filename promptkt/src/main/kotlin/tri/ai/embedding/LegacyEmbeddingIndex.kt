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
package tri.ai.embedding

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.openai.jsonMapper
import java.io.File

/**
 * An embedding index that loads the documents from the local file system.
 * This should be used only for importing legacy formats.
 */
class LegacyEmbeddingIndex {
    var info = mapOf<String, LegacyEmbeddingInfo>()
    companion object {
        fun loadFrom(file: File): LegacyEmbeddingIndex {
            val index = LegacyEmbeddingIndex()
            index.info = jsonMapper.readValue(file)
            return index
        }
    }
}

//region WORKING WITH LEGACY DATA

/** Legacy format for embedding information. */
class LegacyEmbeddingInfo {
    var path: String = ""
    var sections: List<LegacyEmbeddingSectionInfo> = listOf()
}

/** Legacy format for embedding section information. */
class LegacyEmbeddingSectionInfo {
    var embedding: List<Double> = listOf()
    var start: Int = 0
    var end: Int = 0
}

//endregion
