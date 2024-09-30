/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.promptfx.api.AudioApiPlugin;
import tri.promptfx.api.AudioSpeechApiPlugin;
import tri.promptfx.api.ImagesApiPlugin;
import tri.promptfx.apps.*;
import tri.promptfx.docs.DocumentInsightPlugin;
import tri.promptfx.docs.DocumentQaPlugin;
import tri.promptfx.fun.ChatBackPlugin;
import tri.promptfx.fun.ColorPlugin;
import tri.promptfx.fun.EmojiPlugin;
import tri.promptfx.integration.WeatherViewPlugin;
import tri.promptfx.integration.WikipediaViewPlugin;
import tri.promptfx.library.TextClusterPlugin;
import tri.promptfx.library.TextManagerPlugin;
import tri.promptfx.tools.*;
import tri.util.ui.NavigableWorkspaceView;

// command line settings:
// --add-reads kotlin.stdlib=kotlinx.coroutines.core.jvm (debugging)
// --add-opens javafx.controls/javafx.scene.control.skin=tornadofx
// --add-opens javafx.graphics/javafx.scene=tornadofx
module tri.promptfx {

    requires transitive tri.promptkt;

    requires java.desktop;
    requires java.logging;

    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires kotlinx.serialization.core;

    requires openai.core.jvm;
    requires openai.client.jvm;

    requires okhttp3;
    requires okio;

    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.scratchpad;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.kotlin;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;

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
    requires org.jsoup;
    requires com.github.mustachejava;
    requires org.apache.commons.logging;

    // clustering tools
    requires commons.math3;
    requires clust4j;

    opens tri.promptfx to com.fasterxml.jackson.databind;
    opens tri.promptfx.api to com.fasterxml.jackson.databind;
    opens tri.promptfx.apps to com.fasterxml.jackson.databind;
    opens tri.promptfx.docs to tornadofx, com.github.mustachejava;
    opens tri.promptfx.fun to com.fasterxml.jackson.databind;
    opens tri.promptfx.integration to com.fasterxml.jackson.databind;
    opens tri.promptfx.library to com.fasterxml.jackson.databind;
    opens tri.promptfx.tools to com.fasterxml.jackson.databind;
    opens tri.promptfx.ui to com.fasterxml.jackson.databind;
    opens tri.promptfx.ui.docs to com.fasterxml.jackson.databind;
    opens tri.util.ui.starship to com.fasterxml.jackson.databind;

    exports tri.promptfx;
    exports tri.promptfx.api;
    exports tri.promptfx.apps;
    exports tri.promptfx.docs;
    exports tri.promptfx.fun;
    exports tri.promptfx.integration;
    exports tri.promptfx.library;
    exports tri.promptfx.tools;
    exports tri.promptfx.ui;
    exports tri.promptfx.ui.chunk;
    exports tri.promptfx.ui.docs;
    exports tri.promptfx.ui.trace;
    exports tri.util.ui;
    exports tri.util.ui.pdf;
    exports tri.util.ui.starship;

    // services (service loader API)
    uses TextPlugin;
    uses NavigableWorkspaceView;

    provides NavigableWorkspaceView with
            AudioApiPlugin,
            AudioSpeechApiPlugin,
            ImagesApiPlugin,
            ImageDescribePlugin,
            PromptLibraryPlugin,
            PromptScriptPlugin,
            PromptTemplatePlugin,
            PromptValidatorPlugin,
            PromptTraceHistoryPlugin,
            DocumentQaPlugin,
            DocumentInsightPlugin,
            TextManagerPlugin,
            TextClusterPlugin,
            EntityExtractionPlugin,
            ListGeneratorPlugin,
            QuestionAnsweringPlugin,
            SentimentAnalysisPlugin,
            StructuredDataPlugin,
            SummarizationPlugin,
            TextSimilarityPlugin,
            TranslationPlugin,
            ChatBackPlugin,
            ColorPlugin,
            EmojiPlugin,
            WeatherViewPlugin,
            WikipediaViewPlugin
    ;
}
