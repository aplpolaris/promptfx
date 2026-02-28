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

/** Library of models, including model information and list of enabled models. */
class ModelLibrary {
    var models = mapOf<String, List<ModelInfo>>()
    var audio = listOf<String>()
    var chat = listOf<String>()
    var multimodal = listOf<String>()
    var completion = listOf<String>()
    var embeddings = listOf<String>()
    var image_generator = listOf<String>()
    var moderation = listOf<String>()
    var responses = listOf<String>()
    var tts = listOf<String>()
    var vision_language = listOf<String>()

    /** Create model index with unique identifiers, including any registered snapshots. */
    fun modelInfoIndex() = models.values.flatten().flatMap { listOf(it) + it.createSnapshots() }.associateBy { it.id }

    /** Get list of all model ids in the library. */
    fun modelIds(): Set<String> = audio.toSet() + chat + multimodal + completion + embeddings + image_generator + moderation + responses + tts + vision_language
}
