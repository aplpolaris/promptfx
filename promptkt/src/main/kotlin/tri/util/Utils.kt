/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
