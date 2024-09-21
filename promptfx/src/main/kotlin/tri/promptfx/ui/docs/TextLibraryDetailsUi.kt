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
package tri.promptfx.ui.docs

import tornadofx.*

/** View for collection details. */
class TextLibraryDetailsUi : Fragment() {

    val model by inject<TextLibraryViewModel>()

    private val librarySelection = model.librarySelection

    override val root = form {
        val changeProperty = model.libraryContentChange
        val libraryId = librarySelection.stringBinding(changeProperty) { it?.library?.metadata?.id }
        val file = librarySelection.stringBinding(changeProperty) { it?.file?.name ?: "No file" }
        val libraryInfo = librarySelection.stringBinding(changeProperty) { "${it?.library?.docs?.size ?: 0} documents" }

        fieldset("") {
            visibleWhen { librarySelection.isNotNull }
            managedWhen { librarySelection.isNotNull }
            field("Id") { text(libraryId) }
            field("File") { text(file) }
            field("Info") { text(libraryInfo) }
        }
    }

}
