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
import tri.ai.embedding.LocalFolderEmbeddingIndex.Companion.EMBEDDINGS_FILE_NAME_LEGACY
import tri.ai.openai.OpenAiModelIndex.ADA_ID
import tri.ai.openai.jsonMapper
import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.originalFile
import tri.ai.text.chunks.process.LocalFileManager.textCacheFile
import tri.ai.text.chunks.process.LocalTextDocIndex.Companion.createTextDoc
import tri.ai.text.chunks.process.TextDocEmbeddings.putEmbeddingInfo
import tri.util.fine
import tri.util.info
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

//region WORKING WITH LEGACY DATA

/** Upgrades a legacy format embeddings file. Only supports upgrading from OpenAI embeddings file, `embeddings.json`. */
fun LocalFolderEmbeddingIndex.upgradeEmbeddingIndex() {
    val folder = rootDir
    val file = indexFile
    val oldFile = File(folder, EMBEDDINGS_FILE_NAME_LEGACY)
    if (!file.exists() && oldFile.exists()) {
        fine<LegacyEmbeddingIndex>("Checking legacy embeddings file for embedding vectors: $oldFile")
        try {
            var changed = false
            LegacyEmbeddingIndex.loadFrom(oldFile).info.values.map {
                val f = LocalFileManager.fixPath(File(it.path), folder)?.originalFile()
                    ?: throw IllegalArgumentException("File not found: ${it.path}")
                f.createTextDoc().apply {
                    all = TextChunkRaw(f.textCacheFile().readText())
                    chunks.addAll(it.sections.map {
                        TextChunkInDoc(it.start, it.end).apply {
                            if (it.embedding.isNotEmpty())
                                putEmbeddingInfo(ADA_ID, it.embedding, EmbeddingPrecision.FIRST_EIGHT)
                        }
                    })
                }
            }.forEach {
                if (addIfNotPresent(it))
                    changed = true
            }
            if (changed) {
                info<LegacyEmbeddingIndex>("Upgraded legacy embeddings file to new format.")
                saveIndex()
                info<LegacyEmbeddingIndex>("Legacy embeddings file $oldFile can be deleted unless needed for previous versions of PromptFx.")
            } else {
                fine<LegacyEmbeddingIndex>("No new embeddings found in legacy embeddings file.")
            }
        } catch (x: Exception) {
            info<LegacyEmbeddingIndex>("Failed to load legacy embeddings file: ${x.message}")
        }
    }
}

//endregion