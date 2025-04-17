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

    override suspend fun answerQuestion(input: String): AiPipelineResult<String> {
        view.question.set(input)
        return AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor) as AiPipelineResult<String>
    }

}