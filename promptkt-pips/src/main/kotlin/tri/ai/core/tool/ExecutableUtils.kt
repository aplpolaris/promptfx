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

import kotlinx.serialization.SerializationException
import tri.ai.core.MTool
import tri.ai.core.agent.MAPPER
import tri.ai.core.agent.impl.JsonToolExecutor
import tri.util.warning

/** Creates an [MTool] from a JSON schema. */
fun Executable.createTool(): MTool? {
    val schema = try {
        inputSchema?.let { MAPPER.writeValueAsString(it) }
    } catch (x: SerializationException) {
        warning<JsonToolExecutor>("Invalid JSON schema: $inputSchema", x)
        null
    }
    return schema?.let { MTool(name, description, it) }
}
