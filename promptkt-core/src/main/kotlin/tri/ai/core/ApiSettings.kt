package tri.ai.core

/** Base class for API settings. */
interface ApiSettings {
    val baseUrl: String?
    val apiKey: String?

    /** Hook for checking if API has a valid configuration. */
    fun isConfigured(): Boolean

    /** Hook for checking validity of an API key. */
    @Throws(UnsupportedOperationException::class)
    fun checkApiKey()
}