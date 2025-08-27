/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.ai.rest

import kotlinx.serialization.Serializable

/** Request model for document Q&A endpoint. */
@Serializable
data class DocumentQaRequest(
    val question: String,
    val folder: String = "",
    val numResponses: Int = 1,
    val historySize: Int = 1
)

/** Response model for document Q&A endpoint. */
@Serializable
data class DocumentQaResponse(
    val answer: String,
    val question: String,
    val folder: String,
    val success: Boolean = true,
    val error: String? = null
)

/** Response model for listing available document folders. */
@Serializable
data class FoldersResponse(
    val folders: List<String>,
    val currentFolder: String
)

/** Error response model. */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null
)