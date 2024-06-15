/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.docs

import tornadofx.*
import tri.ai.text.chunks.process.GuessedMetadataObject

/**
 * UI that shows a series of key-value pairs discovered by a "metadata guesser" and user can accept/reject them.
 */
class GuessedMetadataValidatorUi: UIComponent() {

    private val model: GuessedMetadataValidatorModel by inject()

    override val root = vbox {

    }

}

/** Model for the metadata validator view. */
class GuessedMetadataValidatorModel: ScopedInstance, Component() {

    val initialProps = observableListOf<GmvProp>()
    val userSelectedProps = observableListOf<GmvProp>()

}

/** A single GMV property, with three possible value types. */
data class GmvProp(
    val key: String,
    val value: String? = null,
    val listValue: List<String>? = null,
    val mapValue: Map<String, String>? = null
) {
    fun isNullAndEmpty() = value == null && listValue.isNullOrEmpty() && mapValue.isNullOrEmpty()
}

/** Convert GMO to a list of GMVs. */
fun GuessedMetadataObject.asGmvPropList() = mutableListOf<GmvProp>().apply {
    add(GmvProp("title", title))
    add(GmvProp("subtitle", subtitle))
    add(GmvProp("authors", null, authors))
    add(GmvProp("date", date))
    add(GmvProp("keywords", null, keywords))
    add(GmvProp("abstract", abstract))
    add(GmvProp("executiveSummary", executiveSummary))
    add(GmvProp("sections", null, sections))
    add(GmvProp("captions", null, captions))
    add(GmvProp("references", null, references))
    other.forEach { (k, v) ->
        add(GmvProp(k, v as? String, v as? List<String>, v as? Map<String, String>))
    }
    removeIf { it.isNullAndEmpty() }
}
