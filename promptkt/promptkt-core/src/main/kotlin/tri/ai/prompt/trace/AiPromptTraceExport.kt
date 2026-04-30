/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.prompt.trace

import tri.util.json.jsonWriter
import tri.util.json.yamlWriter
import java.io.File

/** Writes the given [AiTaskTraceDatabase] to the specified file. */
fun writeTraceDatabase(database: AiTaskTraceDatabase, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, database)
}

/** Writes the given list of traces as an [AiTaskTraceDatabase] to the specified file. */
fun writeTraceDatabase(traces: List<AiTaskTrace>, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, AiTaskTraceDatabase(traces))
}

/** Writes the given [AiTaskTrace] to the specified file. */
fun writeTrace(trace: AiTaskTrace, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, trace)
}

/** Writes a list of [AiTaskTrace]s to the specified file. */
fun writeTraces(traces: List<AiTaskTrace>, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, traces)
}

//region LEGACY (AiPromptTraceDatabase)

/** Writes the given legacy [AiPromptTraceDatabase] to the specified file. */
@Suppress("DEPRECATION")
@Deprecated("Use writeTraceDatabase(AiTaskTraceDatabase, File)")
fun writeTraceDatabase(database: AiPromptTraceDatabase, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, database)
}

//endregion
