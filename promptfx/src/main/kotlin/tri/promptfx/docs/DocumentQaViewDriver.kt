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
package tri.promptfx.docs

import javafx.application.Platform
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.IgnoreMonitor
import tri.ai.text.docs.DocumentQaDriver
import tri.promptfx.PromptFxModels
import java.io.File
import java.io.FileFilter

/** Document Q&A driver that leverages [DocumentQaView]. */
class DocumentQaViewDriver(val view: DocumentQaView) : DocumentQaDriver {

    override val folders
        get() = view.documentFolder.value.parentFile
            .listFiles(FileFilter { it.isDirectory })!!
            .map { it.name }
    override var folder: String
        get() = view.documentFolder.value.name
        set(value) {
            val folderFile = File(view.documentFolder.value.parentFile, value)
            if (folderFile.exists())
                view.documentFolder.set(folderFile)
        }
    override var completionModel: String
        get() = view.controller.completionEngine.value.modelId
        set(value) {
            view.controller.completionEngine.set(
                PromptFxModels.policy.textCompletionModels().find { it.modelId == value }!!
            )
        }
    override var embeddingModel: String
        get() = view.controller.embeddingService.value.modelId
        set(value) {
            view.controller.embeddingService.set(
                PromptFxModels.policy.embeddingModels().find { it.modelId == value }!!
            )
        }
    override var temp: Double
        get() = view.common.temp.value
        set(value) {
            view.common.temp.set(value)
        }
    override var maxTokens: Int
        get() = view.common.maxTokens.value
        set(value) {
            view.common.maxTokens.set(value)
        }

    override fun initialize() {
        Platform.startup { }
    }

    override fun close() {
        Platform.exit()
    }

    override suspend fun answerQuestion(input: String, numResponses: Int, historySize: Int): AiPipelineResult<String> {
        view.question.set(input)
        return AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor) as AiPipelineResult<String>
    }

}
