package tri.util.ui

import javafx.scene.image.Image
import kotlinx.coroutines.runBlocking
import tornadofx.runLater
import tri.ai.core.TextCompletion
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPrompt.Companion.INPUT
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo
import tri.ai.prompt.trace.batch.AiPromptRunConfig
import tri.promptfx.PromptFxModels
import tri.promptfx.docs.FormattedText

/** Pipeline execution for [StarshipUi]. */
object StarshipPipeline {

    /** Execute a pipeline, adding interim results to the [results] object. */
    fun exec(config: StarshipPipelineConfig, results: StarshipPipelineResults) {
        results.started.set(true)

        val input = config.generator()
        results.input.set(input)

        val runConfig = AiPromptRunConfig(
            AiPromptInfo(config.primaryPrompt.prompt.template, mapOf(INPUT to input) + config.primaryPrompt.params),
            AiPromptModelInfo(config.completion.modelId)
        )
        results.runConfig.set(runConfig)

        val firstResponse = runBlocking { config.promptExec.exec(config.primaryPrompt, runConfig.promptInfo.filled()) }
        results.output.set(firstResponse)

        config.secondaryPrompts.forEach {
            val secondInput = results.output.value.rawText
            val secondRunConfig = AiPromptRunConfig(
                AiPromptInfo(it.prompt.template, mapOf(INPUT to secondInput) + it.params),
                AiPromptModelInfo(config.completion.modelId)
            )
            results.secondaryRunConfigs.add(secondRunConfig)
        }

        results.secondaryRunConfigs.forEachIndexed { i, cfg ->
            val prompt = config.secondaryPrompts[i]
            val secondResponse = runBlocking { config.secondaryPromptExec.exec(prompt, cfg.promptInfo.filled()) }
            runLater { results.secondaryOutputs.add(secondResponse) }
        }

        results.completed.set(true)
    }

}

/** Executes a prompt, returning a string and optional image. */
interface AiPromptExecutor {
    suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult
}

/** Result object for [StarshipPipeline]. */
class StarshipInterimResult(val label: String, val text: FormattedText, val image: Image?, val docs: List<DocumentThumbnail>) {
    val rawText
        get() = text.toString()
}

/** Pipeline config for [StarshipUi]. */
class StarshipPipelineConfig(val completion: TextCompletion) {
    /** Input generator. */
    val generator: () -> String = { runBlocking { testRandomQuestion() } }
    /** Primary prompt template, with {{input}} and other parameters. */
    val primaryPrompt = PromptWithParams("document-map-summarize")
    /** Executor for primary prompt. */
    var promptExec: AiPromptExecutor = object : AiPromptExecutor {
        override suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult {
            val filledPrompt = prompt.fill(input)
            val response = completion.complete(filledPrompt)
            return StarshipInterimResult(prompt.prompt.templateName, FormattedText(response.value!!), null, listOf())
        }
    }
    /** Secondary prompt executors. */
    val secondaryPrompts: List<PromptWithParams> = listOf(
        PromptWithParams("document-map-simplify"),
        PromptWithParams("document-reduce-summarize"),
        PromptWithParams("translate-text", mapOf("instruct" to "a random language"))
    )
    /** Executor for secondary prompts. */
    var secondaryPromptExec: AiPromptExecutor = promptExec
}

/** Groups a prompt with associated parameters. */
class PromptWithParams(val prompt: AiPrompt, val params: Map<String, Any> = mapOf()) {
    constructor(prompt: String, params: Map<String, Any> = mapOf()) :
            this(AiPromptLibrary.lookupPrompt(prompt), params)

    fun fill(input: String) = prompt.fill(mapOf(INPUT to input) + params)
}

private suspend fun testRandomQuestion(): String {
    val prompt = """
        Generate a random question about LLMs. The question should be 10-20 words.
        Sample topics include LLMs, LSTMs, NLP, GPT3, memory, hallucination, transformers, summarization, and retrieval-augmented generation.
        Example: "What is the difference between LLM and GPT-3?"
    """.trimIndent()
    return PromptFxModels.textCompletionModelDefault().complete(prompt).value!!
}