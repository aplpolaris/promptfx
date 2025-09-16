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
package tri.ai.core.tool

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.agent.MAPPER
import java.util.UUID

/** Runtime context available to every executable. */
data class ExecContext(
    val vars: MutableMap<String, JsonNode> = mutableMapOf(),
    val resources: Map<String, Any?> = emptyMap(),
    val traceId: String = UUID.randomUUID().toString()
) {
    /** Jackson ObjectMapper for JSON operations. */
    val mapper = MAPPER
}
