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
import tri.ai.core.TextPlugin;
import tri.ai.gemini.GeminiAiPlugin;
import tri.ai.openai.OpenAiPlugin;

module tri.promptkt {
    requires transitive kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;

    requires java.logging;
    requires java.desktop;

    requires openai.core.jvm;
    requires openai.client.jvm;

    requires okhttp3;
    requires okio;

    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.scratchpad;

    requires com.github.mustachejava;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.kotlin;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;

    requires io.ktor.client.core;
    requires io.ktor.client.content.negotiation;
    requires io.ktor.http;
    requires io.ktor.serialization;
    requires io.ktor.serialization.kotlinx.json;
    requires io.ktor.utils;
    requires io.ktor.client.logging;

    opens tri.ai.gemini to io.ktor.serialization;

    opens tri.ai.core to com.fasterxml.jackson.databind;
    opens tri.ai.embedding to com.fasterxml.jackson.databind;
    opens tri.ai.memory to com.fasterxml.jackson.databind;
    opens tri.ai.openai to com.fasterxml.jackson.databind;
    opens tri.ai.pips to com.fasterxml.jackson.databind;
    opens tri.ai.prompt to com.fasterxml.jackson.databind;
    opens tri.ai.prompt.trace to com.fasterxml.jackson.databind;
    opens tri.ai.prompt.trace.batch to com.fasterxml.jackson.databind;
    opens tri.ai.text.chunks to com.fasterxml.jackson.databind;

    exports tri.ai.core;
    exports tri.ai.embedding;
    exports tri.ai.gemini;
    exports tri.ai.memory;
    exports tri.ai.openai;
    exports tri.ai.pips;
    exports tri.ai.prompt;
    exports tri.ai.prompt.trace;
    exports tri.ai.prompt.trace.batch;
    exports tri.ai.text.chunks;
    exports tri.ai.text.chunks.process;
    exports tri.ai.tool;
    exports tri.util;
    exports tri.util.pdf;

    // services (service loader API)
    uses TextPlugin;

    provides TextPlugin with OpenAiPlugin, GeminiAiPlugin;
}
