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

module tri.promptkt.core {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;

    requires transitive java.logging;

    requires transitive com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.kotlin;
    requires transitive com.fasterxml.jackson.datatype.jsr310;
    requires transitive com.fasterxml.jackson.dataformat.yaml;
    requires transitive com.fasterxml.jackson.dataformat.csv;

    requires com.github.mustachejava;

    opens tri.ai.core to com.fasterxml.jackson.databind;
    opens tri.ai.prompt to com.fasterxml.jackson.databind;
    opens tri.ai.prompt.trace to com.fasterxml.jackson.databind;
    opens tri.ai.prompt.trace.batch to com.fasterxml.jackson.databind;
    opens tri.util.json to com.fasterxml.jackson.databind;

    exports tri.ai.core;
    exports tri.ai.prompt;
    exports tri.ai.prompt.trace;
    exports tri.ai.prompt.trace.batch;
    exports tri.util;
    exports tri.util.json;

    // services (service loader API)
    uses TextPlugin;
}
