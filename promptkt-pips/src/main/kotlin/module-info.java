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

module tri.promptkt.pips {
    requires transitive tri.promptkt.core;

    requires openai.core.jvm;
    requires openai.client.jvm;

    requires okhttp3;
    requires com.github.mustachejava;

    opens tri.ai.pips.api to com.fasterxml.jackson.databind;
    opens tri.ai.tool.wf to com.fasterxml.jackson.databind;

    exports tri.ai.core.agent;
    exports tri.ai.core.agent.api;
    exports tri.ai.core.agent.impl;
    exports tri.ai.core.tool;
    exports tri.ai.core.tool.impl;
    exports tri.ai.pips;
    exports tri.ai.pips.api;
    exports tri.ai.tool.wf;
}
