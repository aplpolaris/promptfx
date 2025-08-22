/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.core

import kotlinx.serialization.json.JsonElement
import tri.util.info

/**
 * Configuration for JSON validation and retry attempts.
 */
data class JsonRetryConfig(
    /** Maximum number of attempts to generate valid JSON */
    val maxAttempts: Int = 3,
    /** Whether to validate that extracted JSON is a specific type (object or array) */
    val requireType: JsonType? = null,
    /** Whether to require the entire response to be valid JSON (not just contain JSON) */
    val requireCompleteResponse: Boolean = false
) {
    enum class JsonType { OBJECT, ARRAY }
}

/**
 * Result of a JSON generation attempt.
 */
data class JsonGenerationResult(
    /** The original response text */
    val responseText: String,
    /** The extracted JSON element, if any */
    val jsonElement: JsonElement?,
    /** Whether valid JSON was found */
    val hasValidJson: Boolean,
    /** The attempt number (1-indexed) */
    val attemptNumber: Int,
    /** The raw JSON text that was extracted */
    val extractedJsonText: String?
) {
    /** Whether this result meets the requirements */
    fun isValid(config: JsonRetryConfig): Boolean {
        if (!hasValidJson) return false
        
        if (config.requireCompleteResponse) {
            // Check if the entire response is valid JSON
            return try {
                kotlinx.serialization.json.Json.parseToJsonElement(responseText.trim())
                true
            } catch (_: Exception) {
                false
            }
        }

        if (config.requireType != null && jsonElement != null) {
            return when (config.requireType) {
                JsonRetryConfig.JsonType.OBJECT -> jsonElement is kotlinx.serialization.json.JsonObject
                JsonRetryConfig.JsonType.ARRAY -> jsonElement is kotlinx.serialization.json.JsonArray
            }
        }

        return true
    }
}

/**
 * Utility for generating multiple attempts and returning the first with valid JSON.
 * Implements issue #190: Multiple attempt option to generate N outputs and return the first with valid JSON.
 */
object JsonRetryProcessor {

    /**
     * Execute a generation function multiple times until valid JSON is found.
     * 
     * @param config Configuration for retry attempts and validation
     * @param generator Suspend function that generates a text response
     * @return The first result that contains valid JSON, or the last attempt if none are valid
     */
    suspend fun generateWithJsonRetry(
        config: JsonRetryConfig = JsonRetryConfig(),
        generator: suspend (attemptNumber: Int) -> String
    ): JsonGenerationResult {
        var lastResult: JsonGenerationResult? = null
        
        for (attempt in 1..config.maxAttempts) {
            info<JsonRetryProcessor>("JSON generation attempt $attempt of ${config.maxAttempts}")
            
            try {
                val responseText = generator(attempt)
                val jsonElement = JsonResponseProcessor.extractFirstValidJson(responseText)
                val extractedJsonText = JsonResponseProcessor.extractFirstJsonText(responseText)
                
                val result = JsonGenerationResult(
                    responseText = responseText,
                    jsonElement = jsonElement,
                    hasValidJson = jsonElement != null,
                    attemptNumber = attempt,
                    extractedJsonText = extractedJsonText
                )
                
                lastResult = result
                
                if (result.isValid(config)) {
                    info<JsonRetryProcessor>("Successfully found valid JSON on attempt $attempt")
                    return result
                }
                
                info<JsonRetryProcessor>("Attempt $attempt did not produce valid JSON, trying again...")
                
            } catch (e: Exception) {
                info<JsonRetryProcessor>("Attempt $attempt failed with error: ${e.message}")
                
                if (lastResult == null) {
                    lastResult = JsonGenerationResult(
                        responseText = "",
                        jsonElement = null,
                        hasValidJson = false,
                        attemptNumber = attempt,
                        extractedJsonText = null
                    )
                }
            }
        }
        
        info<JsonRetryProcessor>("All attempts exhausted. Returning last result.")
        return lastResult ?: JsonGenerationResult(
            responseText = "",
            jsonElement = null,
            hasValidJson = false,
            attemptNumber = config.maxAttempts,
            extractedJsonText = null
        )
    }

    /**
     * Generate multiple responses and return all results with their JSON status.
     * Useful for analysis and comparison of different attempts.
     */
    suspend fun generateMultipleWithJson(
        attempts: Int,
        generator: suspend (attemptNumber: Int) -> String
    ): List<JsonGenerationResult> {
        return (1..attempts).map { attempt ->
            try {
                val responseText = generator(attempt)
                val jsonElement = JsonResponseProcessor.extractFirstValidJson(responseText)
                val extractedJsonText = JsonResponseProcessor.extractFirstJsonText(responseText)
                
                JsonGenerationResult(
                    responseText = responseText,
                    jsonElement = jsonElement,
                    hasValidJson = jsonElement != null,
                    attemptNumber = attempt,
                    extractedJsonText = extractedJsonText
                )
            } catch (e: Exception) {
                JsonGenerationResult(
                    responseText = "Error: ${e.message}",
                    jsonElement = null,
                    hasValidJson = false,
                    attemptNumber = attempt,
                    extractedJsonText = null
                )
            }
        }
    }

    /**
     * Get the best result from a list of attempts.
     * Prioritizes results with valid JSON, then by attempt number.
     */
    fun getBestResult(results: List<JsonGenerationResult>, config: JsonRetryConfig = JsonRetryConfig()): JsonGenerationResult? {
        // First, try to find results that meet the configuration requirements
        val validResults = results.filter { it.isValid(config) }
        if (validResults.isNotEmpty()) {
            return validResults.first() // Return first valid result
        }
        
        // If no results meet requirements, return the first one with any JSON
        val withJson = results.filter { it.hasValidJson }
        if (withJson.isNotEmpty()) {
            return withJson.first()
        }
        
        // Otherwise, return the first result
        return results.firstOrNull()
    }
}