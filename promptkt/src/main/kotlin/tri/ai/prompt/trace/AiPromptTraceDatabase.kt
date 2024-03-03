package tri.ai.prompt.trace

/**
 * In-memory database of prompt traces, allowing for reuse of prompt, model, exec, and output objects,
 * and storage of these in separate tables.
 *
 * TODO - this is not optimized for large databases, but should work well for up to a few thousand
 */
class AiPromptTraceDatabase {

    var traces = mutableListOf<AiPromptTraceId>()

    var prompts = mutableSetOf<AiPromptInfo>()
    var models = mutableSetOf<AiPromptModelInfo>()
    var execs = mutableSetOf<AiPromptExecInfo>()
    var outputs = mutableSetOf<AiPromptOutputInfo>()

    /** Get all prompt traces as list. */
    fun promptTraces() = traces.map { it.promptTrace() }

    /** Get the prompt trace by index. */
    fun AiPromptTraceId.promptTrace() = AiPromptTrace(
        prompts.elementAt(promptIndex),
        models.elementAt(modelIndex),
        execs.elementAt(execIndex),
        outputs.elementAt(outputIndex)
    )

    /** Add all provided traces to the database. */
    fun addTraces(result: Iterable<AiPromptTrace>) {
        result.forEach { addTrace(it) }
    }

    /** Adds the trace to the database, updating object references as needed and returning the object added. */
    fun addTrace(trace: AiPromptTrace): AiPromptTraceId {
        val prompt = addOrGet(prompts, trace.promptInfo)
        val model = addOrGet(models, trace.modelInfo)
        val exec = addOrGet(execs, trace.execInfo)
        val output = addOrGet(outputs, trace.outputInfo)

        return AiPromptTraceId(
            trace.uuid,
            prompts.indexOf(prompt),
            models.indexOf(model),
            execs.indexOf(exec),
            outputs.indexOf(output)
        ).also {
            traces.add(it)
        }
    }

    private fun <T> addOrGet(set: MutableSet<T>, item: T): T {
        val existing = set.find { it == item }
        return if (existing != null) existing else {
            set.add(item)
            item
        }
    }

}

