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

module tri.promptkt.mcp {
    requires transitive tri.promptkt.pips;
    requires transitive kotlin.stdlib;

    requires com.fasterxml.jackson.databind;
    requires com.google.common;
    requires io.ktor.client.core;
    requires io.ktor.server.core;
    requires io.ktor.server.netty;
    requires io.ktor.server.host.common;

    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;

    exports tri.ai.mcp;
    exports tri.ai.mcp.http;
    exports tri.ai.mcp.registry;
    exports tri.ai.mcp.stdio;
    exports tri.ai.mcp.tool;

    opens tri.ai.mcp to com.fasterxml.jackson.databind;
    opens tri.ai.mcp.registry to com.fasterxml.jackson.databind;
    opens tri.ai.mcp.tool to com.fasterxml.jackson.databind, kotlin.reflect;
}
