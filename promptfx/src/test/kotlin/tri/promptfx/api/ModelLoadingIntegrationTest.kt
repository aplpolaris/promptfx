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
package tri.promptfx.api

import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import tri.ai.core.*
import tri.util.info
import tri.util.warning
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureTimeMillis

/**
 * Integration test to demonstrate the incremental model loading behavior.
 * This test simulates the timeout handling without actually running the UI.
 */
class ModelLoadingIntegrationTest {

    /** Test plugin that simulates different response times. */
    class TestPlugin(private val name: String, private val delayMs: Long, private val shouldFail: Boolean = false) : TextPlugin {
        override fun modelSource(): String = name
        
        override fun modelInfo(): List<ModelInfo> {
            if (shouldFail) throw RuntimeException("Simulated failure for $name")
            Thread.sleep(delayMs)
            return listOf(ModelInfo("$name-model", ModelType.TEXT_CHAT, name))
        }
        
        override fun embeddingModels(): List<EmbeddingModel> = emptyList()
        override fun chatModels(): List<TextChat> = emptyList()
        override fun multimodalModels(): List<MultimodalChat> = emptyList()
        override fun textCompletionModels(): List<TextCompletion> = emptyList()
        override fun visionLanguageModels(): List<VisionLanguageChat> = emptyList()
        override fun imageGeneratorModels(): List<ImageGenerator> = emptyList()
        override fun close() {}
    }

    @Test
    fun testIncrementalModelLoadingBehavior() {
        val plugins = listOf(
            TestPlugin("FastAPI", 100),      // Fast response
            TestPlugin("SlowAPI", 2000),     // Slow but within timeout 
            TestPlugin("TimeoutAPI", 12000), // Will timeout (12s > 10s timeout)
            TestPlugin("FailingAPI", 50, shouldFail = true), // Will fail
            TestPlugin("AnotherFastAPI", 200) // Another fast response
        )
        
        val results = ConcurrentLinkedQueue<Pair<String, List<ModelInfo>>>()
        val startTime = System.currentTimeMillis()
        
        println("=== Testing Incremental Model Loading ===")
        println("Starting at ${startTime}ms")
        
        // Simulate the ModelsView loading pattern
        runBlocking {
            val jobs = plugins.map { plugin ->
                async {
                    simulatePluginLoading(plugin, results, startTime)
                }
            }
            jobs.awaitAll()
        }
        
        println("\n=== Results Summary ===")
        println("Total plugins: ${plugins.size}")
        println("Successful loads: ${results.size}")
        
        // Show results in order they completed
        results.sortedBy { it.second.firstOrNull()?.source }.forEach { (timing, models) ->
            println("$timing -> ${models.firstOrNull()?.source ?: "No models"} (${models.size} models)")
        }
        
        // Verify behavior
        assert(results.size >= 2) { "Should have at least 2 successful plugin loads" }
        val hasFastAPI = results.any { (_, models) -> models.any { it.source == "FastAPI" } }
        assert(hasFastAPI) { "Fast plugin should succeed" }
        
        println("\n✅ Incremental loading behavior verified!")
    }

    private suspend fun simulatePluginLoading(
        plugin: TextPlugin, 
        results: ConcurrentLinkedQueue<Pair<String, List<ModelInfo>>>,
        startTime: Long
    ) {
        try {
            info<ModelLoadingIntegrationTest>("Loading models from ${plugin.modelSource()}...")
            val models = withTimeout(10000) { // 10 second timeout
                plugin.modelInfo()
            }
            val elapsed = System.currentTimeMillis() - startTime
            val timing = "t+${elapsed}ms"
            info<ModelLoadingIntegrationTest>("✅ Loaded ${models.size} models from ${plugin.modelSource()} at $timing")
            results.add(timing to models)
        } catch (e: TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - startTime
            warning<ModelLoadingIntegrationTest>("⏰ Timeout loading models from ${plugin.modelSource()} at t+${elapsed}ms")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            warning<ModelLoadingIntegrationTest>("❌ Error loading models from ${plugin.modelSource()} at t+${elapsed}ms: ${e.message}")
        }
    }
}