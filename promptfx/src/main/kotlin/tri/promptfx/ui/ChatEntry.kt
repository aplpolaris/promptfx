/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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
package tri.promptfx.ui

import tri.ai.core.TextChatMessage

/** A message from a user. */
class ChatEntry(val user: String, val message: String, val style: ChatEntryRole = ChatEntryRole.USER) {
    override fun toString() = "$user: $message"
}

/** Convert a [ChatEntry] to a [TextChatMessage]. */
fun ChatEntry.toTextChatMessage() = style.toTextChatRole()?.let {
    TextChatMessage(it, message)
}