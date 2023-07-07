package tri.ai.pips

class PrintMonitor: AiTaskMonitor {
    override fun taskStarted(task: AiTask<*>) {
        printGray("Started: ${task.id}")
    }
    override fun taskUpdate(task: AiTask<*>, progress: Double) {
        printGray("Update: ${task.id} $progress")
    }
    override fun taskCompleted(task: AiTask<*>, result: Any?) {
        val value = (result as? AiTaskResult<*>)?.value ?: result
        if (value is Iterable<*>) {
            printGray("  result:")
            value.forEach { printGray("\u001B[1m    - $it") }
        } else {
            printGray("  result: \u001B[1m$value")
        }
        printGray("  completed: ${task.id}")
    }
    override fun taskFailed(task: AiTask<*>, error: Throwable) {
        printRed("  failed: ${task.id} with error $error")
    }

    private fun printGray(text: String) {
        println("\u001B[90m$text\u001B[0m")
    }

    private fun printRed(text: String) {
        println("\u001B[91m$text\u001B[0m")
    }
}