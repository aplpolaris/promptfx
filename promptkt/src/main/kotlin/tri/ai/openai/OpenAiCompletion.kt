package tri.ai.openai

import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import tri.ai.core.TextCompletion

/** Text completion with OpenAI models. */
class OpenAiCompletion(val modelId: String = TEXT_DAVINCI3, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    TextCompletion {

    override fun toString() = modelId

    override suspend fun complete(text: String, tokens: Int?, stop: String?) =
        client.completion(CompletionRequest(
            ModelId(modelId),
            text,
            maxTokens = tokens,
            stop = stop?.let { listOf(it) }
        ))

}

