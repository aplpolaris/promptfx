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
import tri.ai.core.TextPlugin;
import tri.ai.gemini.GeminiAiPlugin;

module tri.promptkt.gemini {
    requires transitive tri.promptkt.core;
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;

    requires io.ktor.client.core;
    requires io.ktor.client.content.negotiation;
    requires io.ktor.client.auth;
    requires io.ktor.client.logging;
    requires io.ktor.client.okhttp;
    requires io.ktor.http;
    requires io.ktor.serialization;
    requires io.ktor.serialization.kotlinx.json;
    requires io.ktor.utils;

    opens tri.ai.gemini to io.ktor.serialization;

    exports tri.ai.gemini;

    // services (service loader API)
    uses TextPlugin;

    provides TextPlugin with GeminiAiPlugin;
}
