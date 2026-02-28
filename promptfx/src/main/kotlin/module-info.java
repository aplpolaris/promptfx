/*-
 * #%L
 * tri.promptfx:promptfx
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
import tri.promptfx.agents.*;
import tri.promptfx.mcp.McpPromptPlugin;
import tri.promptfx.mcp.McpResourcePlugin;
import tri.promptfx.mcp.McpServerPlugin;
import tri.promptfx.mcp.McpToolPlugin;
import tri.promptfx.settings.AboutPlugin;
import tri.promptfx.settings.PromptFxSettingsPlugin;
import tri.promptfx.text.*;
import tri.promptfx.docs.*;
import tri.promptfx.fun.*;
import tri.promptfx.multimodal.*;
import tri.promptfx.prompts.*;
import tri.util.ui.NavigableWorkspaceView;

// command line settings:
// --add-reads kotlin.stdlib=kotlinx.coroutines.core.jvm (debugging)
// --add-opens javafx.controls/javafx.scene.control.skin=tornadofx
// --add-opens javafx.graphics/javafx.scene=tornadofx
module tri.promptfx {
    requires transitive tri.promptkt.docs;
    requires transitive tri.promptkt.gemini;
    requires transitive tri.promptkt.gemini.sdk;
    requires transitive tri.promptkt.openai;
    requires transitive tri.promptkt.mcp;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.swing;
    requires javafx.web;

    requires tornadofx;
    requires de.jensd.fx.glyphs.commons;
    requires de.jensd.fx.glyphs.fontawesome;
    requires org.controlsfx.controls;
    requires com.google.common;
    requires org.apache.commons.logging;

    // clustering tools
    requires commons.math3;
    requires clust4j;

    opens tri.promptfx to com.fasterxml.jackson.databind;
    opens tri.promptfx.agents to com.fasterxml.jackson.databind;
    opens tri.promptfx.api to com.fasterxml.jackson.databind;
    opens tri.promptfx.docs to tornadofx, com.github.mustachejava;
    opens tri.promptfx.fun to com.fasterxml.jackson.databind;
    opens tri.promptfx.mcp to com.fasterxml.jackson.databind;
    opens tri.promptfx.multimodal to com.fasterxml.jackson.databind;
    opens tri.promptfx.prompts to com.fasterxml.jackson.databind;
    opens tri.promptfx.settings to com.fasterxml.jackson.databind;
    opens tri.promptfx.text to com.fasterxml.jackson.databind;
    opens tri.promptfx.ui to com.fasterxml.jackson.databind;
    opens tri.promptfx.ui.docs to com.fasterxml.jackson.databind;
    opens tri.util.ui.starship to com.fasterxml.jackson.databind;

    exports tri.promptfx;
    exports tri.promptfx.agents;
    exports tri.promptfx.api;
    exports tri.promptfx.docs;
    exports tri.promptfx.fun;
    exports tri.promptfx.mcp;
    exports tri.promptfx.multimodal;
    exports tri.promptfx.prompts;
    exports tri.promptfx.settings;
    exports tri.promptfx.text;
    exports tri.promptfx.ui;
    exports tri.promptfx.ui.chunk;
    exports tri.promptfx.ui.docs;
    exports tri.promptfx.ui.prompt;
    exports tri.util.ui;
    exports tri.util.ui.pdf;
    exports tri.util.ui.starship;

    // services (service loader API)
    uses TextPlugin;
    uses NavigableWorkspaceView;

    provides NavigableWorkspaceView with

            // 1 - prompts
            PromptLibraryPlugin,
            PromptScriptPlugin,
            PromptTemplatePlugin,
            PromptValidatorPlugin,
            PromptTraceHistoryPlugin,

            // 2 - text
            ListGeneratorPlugin,

            // 3 - docs
            DocumentQaPlugin,
            DocumentInsightPlugin,
            TextManagerPlugin,
            TextClusterPlugin,
            TextSimilarityPlugin,

            // 4 - multimodal
            AudioApiPlugin,
            AudioSpeechApiPlugin,
            ImagesApiPlugin,
            ImageDescribePlugin,

            // 5 - mcp
            McpServerPlugin,
            McpPromptPlugin,
            McpToolPlugin,
            McpResourcePlugin,

            // 6 - agents
            AgenticPlugin,

            // 7 - settings
            AboutPlugin,
            PromptFxSettingsPlugin,

            // 8 - custom
            ChatBackPlugin,
            ColorPlugin,
            WeatherViewPlugin,
            WikipediaViewPlugin
    ;
}
