/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.text.chunks

import java.io.File
import java.net.URI

/**
 * Provides information needed to browse to a source.
 * Examples: a snippet of text in a PDF, a file, a website, etc.
 */
data class BrowsableSource(val uri: URI) {
    /** The path to the source file. */
    val path: String = uri.path
    /** Get the source as a file, if it exists. */
    val file = try {
        File(uri).let { if (it.exists()) it else null }
    } catch (x: IllegalArgumentException) {
        null
    }
    /** The short name of the source file. */
    val shortName = path.substringAfterLast('/')
    /** The short name of the source file, without extension. */
    val shortNameWithoutExtension = shortName.substringBeforeLast('.')
}
