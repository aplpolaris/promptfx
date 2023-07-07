package tri.ai.pips

/** Tracks status of tasks. */
interface AiTaskMonitor {
    fun taskStarted(task: AiTask<*>)
    fun taskUpdate(task: AiTask<*>, progress: Double)
    fun taskCompleted(task: AiTask<*>, result: Any?)
    fun taskFailed(task: AiTask<*>, error: Throwable)
}

class IgnoreMonitor : AiTaskMonitor {
    override fun taskStarted(task: AiTask<*>) {}
    override fun taskUpdate(task: AiTask<*>, progress: Double) {}
    override fun taskCompleted(task: AiTask<*>, result: Any?) {}
    override fun taskFailed(task: AiTask<*>, error: Throwable) {}
}
