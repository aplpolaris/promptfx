package tri.util

val ANSI_RESET = "\u001B[0m"
val ANSI_RED = "\u001B[31m"
val ANSI_YELLOW = "\u001B[33m"
val ANSI_GREEN = "\u001B[32m"
val ANSI_CYAN = "\u001B[36m"
val ANSI_GRAY = "\u001B[37m"

fun <X> String?.ifNotBlank(op: (String) -> X): X? =
    if (isNullOrBlank()) {
        null
    } else {
        op(this)
    }