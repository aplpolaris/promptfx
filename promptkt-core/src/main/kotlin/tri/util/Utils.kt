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
package tri.util

import java.util.IllegalFormatException
import java.util.logging.Level
import java.util.logging.Logger

const val ANSI_RESET = "\u001B[0m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_GRAY = "\u001B[37m"
const val ANSI_BOLD = "\u001B[1m"

fun <X> String?.ifNotBlank(op: (String) -> X): X? =
    if (isNullOrBlank()) {
        null
    } else {
        op(this)
    }

//region LOGGER SHORTCUTS

inline fun <reified T : Any> loggerFor(): Logger = Logger.getLogger(T::class.java.name)

fun Logger.severe(msg: String, x: Throwable? = null) = log(Level.SEVERE, msg, x)
fun Logger.warning(msg: String, x: Throwable? = null) = log(Level.WARNING, msg, x)
fun Logger.info(msg: String, x: Throwable? = null) = log(Level.INFO, msg, x)
fun Logger.fine(msg: String, x: Throwable? = null) = log(Level.FINE, msg, x)

//endregion

//region CLASS LOGGERS

inline fun <reified T : Any> severe(msg: String, x: Throwable? = null) = log<T>(Level.SEVERE, msg, x)
inline fun <reified T : Any> warning(msg: String, x: Throwable? = null) = log<T>(Level.WARNING, msg, x)
inline fun <reified T : Any> info(msg: String, x: Throwable? = null) = log<T>(Level.INFO, msg, x)
inline fun <reified T : Any> fine(msg: String, x: Throwable? = null) = log<T>(Level.FINE, msg, x)

inline fun <reified T : Any> severe(template: String, vararg args: Any?) = log<T>(Level.SEVERE, template, args)
inline fun <reified T : Any> warning(template: String, vararg args: Any?) = log<T>(Level.WARNING, template, args)
inline fun <reified T : Any> info(template: String, vararg args: Any?) = log<T>(Level.INFO, template, args)
inline fun <reified T : Any> config(template: String, vararg args: Any?) = log<T>(Level.CONFIG, template, args)
inline fun <reified T : Any> fine(template: String, vararg args: Any?) = log<T>(Level.FINE, template, args)

const val USE_STDOUT_LOGGER = true
var MIN_LEVEL_TO_LOG = Level.INFO

inline fun <reified T : Any> log(level: Level, msg: String, x: Exception? = null) {
    if (USE_STDOUT_LOGGER) {
        println("$level: $msg")
    } else
        loggerFor<T>().log(level, msg, x)
}
inline fun <reified T : Any> log(level: Level, template: String, vararg args: Any?) {
    if (level.intValue() < MIN_LEVEL_TO_LOG.intValue())
        return
    else if (USE_STDOUT_LOGGER && args.isEmpty()) {
        println("$level: $template")
    } else if (USE_STDOUT_LOGGER) {
        try {
            println("$level: $template".format(*args))
        } catch (e: IllegalFormatException) {
            println("$level: $template")
        }
    } else {
        loggerFor<T>().log(level, template, args)
    }
}

//endregion
