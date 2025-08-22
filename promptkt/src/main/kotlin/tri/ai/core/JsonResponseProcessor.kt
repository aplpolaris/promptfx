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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

/**
 * Comprehensive utility for processing JSON output from AI model responses.
 * Provides functionality to extract, validate, and process JSON from various response types.
 */
object JsonResponseProcessor {

    /** 
     * Extract the first valid JSON object or array from text.
     * Handles responses where JSON might be embedded within other text.
     */
    fun extractFirstValidJson(text: String): JsonElement? {
        // Try parsing the entire text as JSON first
        try {
            return Json.parseToJsonElement(text.trim())
        } catch (_: SerializationException) {
            // Continue to search for JSON within the text
        }

        // Look for JSON in markdown code blocks first (more explicit)
        val codeBlockJson = extractJsonFromCodeBlock(text)
        if (codeBlockJson != null) {
            try {
                return Json.parseToJsonElement(codeBlockJson)
            } catch (_: SerializationException) {
                // Continue searching
            }
        }

        // Look for JSON objects {...}
        val objectJson = extractJsonBlock(text, '{', '}')
        if (objectJson != null) {
            try {
                val parsed = Json.parseToJsonElement(objectJson)
                if (parsed is JsonObject) return parsed
            } catch (_: SerializationException) {
                // Continue searching
            }
        }

        // Look for JSON arrays [...]
        val arrayJson = extractJsonBlock(text, '[', ']')
        if (arrayJson != null) {
            try {
                val parsed = Json.parseToJsonElement(arrayJson)
                if (parsed is JsonArray) return parsed
            } catch (_: SerializationException) {
                // Continue searching
            }
        }

        return null
    }

    /**
     * Extract all valid JSON elements from text.
     * Returns a list of all valid JSON objects and arrays found.
     */
    fun extractAllValidJson(text: String): List<JsonElement> {
        val results = mutableListOf<JsonElement>()
        
        // Try parsing the entire text as JSON first
        try {
            val parsed = Json.parseToJsonElement(text.trim())
            results.add(parsed)
            return results
        } catch (_: SerializationException) {
            // Continue to search for JSON within the text
        }

        // Find JSON in code blocks first
        extractAllJsonFromCodeBlocks(text).forEach { jsonText ->
            try {
                val parsed = Json.parseToJsonElement(jsonText)
                results.add(parsed)
            } catch (_: SerializationException) {
                // Skip invalid JSON
            }
        }

        // Find all JSON objects
        findAllJsonBlocks(text, '{', '}').forEach { jsonText ->
            try {
                val parsed = Json.parseToJsonElement(jsonText)
                if (parsed is JsonObject && !results.any { it.toString() == parsed.toString() }) {
                    results.add(parsed)
                }
            } catch (_: SerializationException) {
                // Skip invalid JSON
            }
        }

        // Find all JSON arrays
        findAllJsonBlocks(text, '[', ']').forEach { jsonText ->
            try {
                val parsed = Json.parseToJsonElement(jsonText)
                if (parsed is JsonArray && !results.any { it.toString() == parsed.toString() }) {
                    results.add(parsed)
                }
            } catch (_: SerializationException) {
                // Skip invalid JSON
            }
        }

        return results
    }

    /**
     * Check if the given text contains valid JSON.
     */
    fun containsValidJson(text: String): Boolean = extractFirstValidJson(text) != null

    /**
     * Extract the raw text of the first valid JSON block found.
     * Useful for copying or displaying JSON content.
     */
    fun extractFirstJsonText(text: String): String? {
        // Try parsing the entire text as JSON first
        try {
            Json.parseToJsonElement(text)
            return text.trim()
        } catch (_: SerializationException) {
            // Continue to search for JSON within the text
        }

        // Look for JSON objects
        extractJsonBlock(text, '{', '}')?.let { jsonText ->
            try {
                Json.parseToJsonElement(jsonText)
                return jsonText
            } catch (_: SerializationException) {
                // Continue searching
            }
        }

        // Look for JSON arrays
        extractJsonBlock(text, '[', ']')?.let { jsonText ->
            try {
                Json.parseToJsonElement(jsonText)
                return jsonText
            } catch (_: SerializationException) {
                // Continue searching
            }
        }

        // Look for JSON in code blocks
        extractJsonFromCodeBlock(text)?.let { jsonText ->
            try {
                Json.parseToJsonElement(jsonText)
                return jsonText
            } catch (_: SerializationException) {
                // No valid JSON found
            }
        }

        return null
    }

    /**
     * Format JSON text with proper indentation for display.
     */
    fun formatJson(jsonText: String): String? {
        return try {
            val element = Json.parseToJsonElement(jsonText)
            Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
        } catch (_: SerializationException) {
            null
        }
    }

    /**
     * Format a JsonElement with proper indentation for display.
     */
    fun formatJson(element: JsonElement): String {
        return Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
    }

    // Private helper methods

    private fun extractJsonBlock(text: String, startChar: Char, endChar: Char): String? {
        val startIndex = text.indexOf(startChar)
        if (startIndex == -1) return null

        var depth = 0
        var inString = false
        var escaped = false

        for (i in startIndex until text.length) {
            val char = text[i]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == startChar -> depth++
                !inString && char == endChar -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(startIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun findAllJsonBlocks(text: String, startChar: Char, endChar: Char): List<String> {
        val results = mutableListOf<String>()
        var searchIndex = 0

        while (searchIndex < text.length) {
            val startIndex = text.indexOf(startChar, searchIndex)
            if (startIndex == -1) break

            var depth = 0
            var inString = false
            var escaped = false

            for (i in startIndex until text.length) {
                val char = text[i]
                when {
                    escaped -> escaped = false
                    char == '\\' && inString -> escaped = true
                    char == '"' -> inString = !inString
                    !inString && char == startChar -> depth++
                    !inString && char == endChar -> {
                        depth--
                        if (depth == 0) {
                            results.add(text.substring(startIndex, i + 1))
                            searchIndex = i + 1
                            break
                        }
                    }
                }
            }
            
            if (depth > 0) {
                // Unmatched brackets, move search forward
                searchIndex = startIndex + 1
            }
        }

        return results
    }

    private fun extractJsonFromCodeBlock(text: String): String? {
        // Look for ```json or ``` followed by JSON
        val patterns = listOf(
            Regex("```json\\s*\n([\\s\\S]*?)\n```"),
            Regex("```\\s*\n([\\s\\S]*?)\n```"),
            Regex("```([\\s\\S]*?)```"),
            Regex("`([^`]+)`")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val content = match.groupValues[1].trim()
                try {
                    Json.parseToJsonElement(content)
                    return content
                } catch (_: SerializationException) {
                    // Continue to next pattern
                }
            }
        }
        return null
    }

    private fun extractAllJsonFromCodeBlocks(text: String): List<String> {
        val results = mutableListOf<String>()
        val patterns = listOf(
            Regex("```json\\s*\n([\\s\\S]*?)\n```"),
            Regex("```\\s*\n([\\s\\S]*?)\n```"),
            Regex("```([\\s\\S]*?)```"),
            Regex("`([^`]+)`")
        )

        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                val content = match.groupValues[1].trim()
                try {
                    Json.parseToJsonElement(content)
                    results.add(content)
                } catch (_: SerializationException) {
                    // Skip invalid JSON
                }
            }
        }
        return results
    }
}