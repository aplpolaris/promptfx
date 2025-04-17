package tri.ai.cli

import tri.ai.openai.jsonWriter
import tri.ai.openai.yamlWriter
import tri.ai.prompt.trace.AiPromptTraceDatabase
import tri.ai.prompt.trace.AiPromptTraceSupport
import java.io.File

// UTILITIES FOR EXPORTING PROMPT TRACES TO FILES

/** Writes the given [AiPromptTraceDatabase] to the specified file. */
fun writeTraceDatabase(database: AiPromptTraceDatabase, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, database)
}

/** Writes the given [AiPromptTraceDatabase] to the specified file. */
fun writeTraceDatabase(traces: List<AiPromptTraceSupport<*>>, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, AiPromptTraceDatabase(traces))
}

/** Writes the given [AiPromptTraceSupport] to the specified file. */
fun writeTrace(trace: AiPromptTraceSupport<*>, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, trace)
}

/** Writes a list of [AiPromptTraceSupport]s to the specified file. */
fun writeTraces(traces: List<AiPromptTraceSupport<*>>, file: File) {
    val writer = if (file.extension == "json") jsonWriter else yamlWriter
    writer.writeValue(file, traces)
}