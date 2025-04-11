package tri.ai.openai.api

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * OpenAI-compatible API endpoint. Allows connections with the standard OpenAI API endpoint or
 * other compatible endpoints.
 */
class OpenAiApiSettingsGeneric : OpenAiApiSettings {

    /** Base URL of the OpenAI-compatible API endpoint, or null for the standard OpenAI API. */
    override var baseUrl: String? = null
    /** API key for authentication. */
    var apiKeyFile: String = ""
    /** API key for authentication. */
    var apiKeyRegistry: String? = ""
    /** Get API key. */
    @get:JsonIgnore
    override val apiKey: String?
        get() = readApiKey()
    /** Logging level for the OpenAI client. */
    override var logLevel: LogLevel = LogLevel.None
    /** Timeout in seconds for the OpenAI client. */
    var timeoutSeconds: Int = 60

    /** API keys not checked by default. */
    override fun checkApiKey() {
        val isOpenAi = baseUrl.let { it == null || it.contains("api.openai.com") }
        val testKey = apiKey
        val isValidOpenAiKey = testKey != null && testKey.startsWith("sk-") && !testKey.trim().contains(" ")
        if (!isValidOpenAiKey && isOpenAi)
            throw UnsupportedOperationException("Invalid OpenAi API key. Please set a valid OpenAI API key. If you are using Azure, please change the baseURL configuration.")
    }

    @Throws(IllegalStateException::class)
    fun buildClient() = OpenAI(
        OpenAIConfig(
            host = buildHost(),
            token = apiKey ?: "",
            headers = apiKey?.let { mapOf("api-key" to it) } ?: mapOf(),
            logging = LoggingConfig(logLevel),
            timeout = Timeout(socket = timeoutSeconds.seconds)
        )
    )

    /** Read API key by first checking for [apiKeyFile], and then checking user environment variable [apiKeyRegistry]. */
    private fun readApiKey(): String? {
        val apiFile = if (apiKeyFile.isNotBlank() && File(apiKeyFile).exists()) File(apiKeyFile).readText() else null
        val apiRegistry = if (apiKeyRegistry.isNullOrBlank()) null else System.getenv(apiKeyRegistry)
        return apiFile ?: apiRegistry
    }

    private fun buildHost() = when (baseUrl) {
        null -> OpenAIHost.OpenAI
        else -> OpenAIHost(baseUrl!!)
    }
}