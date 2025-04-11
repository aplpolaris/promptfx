package tri.ai.openai.api

import com.aallam.openai.api.logging.LogLevel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Test

class OpenAiApiConfigTest {

    @Test
    fun testConfig() {
        val config = OpenAiApiConfig()
        config.endpoints.add(OpenAiApiEndpointConfig().apply {
            source = "Test Source"
            settings = OpenAiApiSettingsGeneric().apply {
                baseUrl = "https://api.openai.com/v1"
                apiKeyFile = "test-api-key.txt"
                apiKeyRegistry = "OPENAI_API_KEY"
                logLevel = LogLevel.None
                timeoutSeconds = 60
            }
            modelFileName = "test-models.yaml"
        })
        val str = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }.writerWithDefaultPrettyPrinter().writeValueAsString(config)
        println(str)
    }

}