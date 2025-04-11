package tri.ai.openai.azure

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.fasterxml.jackson.annotation.JsonIgnore
import tri.ai.openai.OpenAiAdapter
import tri.ai.openai.api.OpenAiApiSettings
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * OpenAI-compatible API endpoint. Allows connections with the standard OpenAI API endpoint or
 * other compatible endpoints.
 */
class OpenAiAzureSettings : OpenAiApiSettings {

    companion object {
        private val RESOURCE_NAME = OpenAiAzureSettings::class.java.simpleName+".properties"
        private val INSTANCE_SETTINGS = OpenAiAzureSettings().apply {
            val propsFile = File(RESOURCE_NAME).ifMissing { File("config/$RESOURCE_NAME") }.ifMissing { null }
            val props = Properties().apply {
                propsFile?.inputStream()?.use { load(it) }
            }
            resourceName = props.getProperty("resourceName", "")
            deploymentId = props.getProperty("deploymentId", "")
            apiVersion = props.getProperty("apiVersion", "")
            logLevel = try {
                LogLevel.valueOf(props.getProperty("logLevel", "None"))
            } catch (e: IllegalArgumentException) {
                LogLevel.None
            }
            timeoutSeconds = props.getProperty("timeoutSeconds", "60").toIntOrNull() ?: 60
        }
        val INSTANCE = OpenAiAdapter(INSTANCE_SETTINGS, INSTANCE_SETTINGS.buildClient())

        private fun File?.ifMissing(block: () -> File?) = if (this?.exists() == true) this else block()
    }

    /** Azure resource name. */
    var resourceName: String = ""
    /** Azure deployment ID. */
    var deploymentId: String = ""
    /** Azure API version. */
    var apiVersion: String = ""

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
        // unclear how to do automated checks for Azure
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

    private fun buildHost() =
        OpenAIHost.azure(resourceName, deploymentId, apiVersion)

}