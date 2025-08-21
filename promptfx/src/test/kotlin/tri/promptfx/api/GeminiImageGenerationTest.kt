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

import org.junit.jupiter.api.Test
import tri.ai.gemini.GeminiModelIndex

/** Test to verify Gemini image models configuration. */
class GeminiImageGenerationTest {

    @Test
    fun `test Gemini image model configuration`() {
        println("=== Test Gemini Image Model Configuration ===")
        
        // Check that image generator models are configured in the model index
        val imageGeneratorModels = GeminiModelIndex.imageGeneratorModels()
        println("Configured Gemini image generators: $imageGeneratorModels")
        
        // Should contain our new models
        assert(imageGeneratorModels.contains("imagen-3.0-generate-001")) { 
            "Should contain imagen-3.0-generate-001" 
        }
        assert(imageGeneratorModels.contains("imagen-3.0-fast-generate-001")) { 
            "Should contain imagen-3.0-fast-generate-001" 
        }
        
        println("✓ Gemini image models are configured correctly")
    }
}
