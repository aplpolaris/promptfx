import tri.ai.core.TextPlugin;
import tri.promptfx.apps.*;
import tri.promptfx.integration.WeatherViewPlugin;
import tri.promptfx.integration.WikipediaViewPlugin;
import tri.util.ui.NavigableWorkspaceView;

// command line settings:
// --add-reads kotlin.stdlib=kotlinx.coroutines.core.jvm
// --add-opens javafx.controls/javafx.scene.control.skin=tornadofx
// --add-opens javafx.graphics/javafx.scene=tornadofx
module tri.promptfx {

    requires transitive tri.promptkt;

    requires java.desktop;
    requires java.logging;

    requires kotlinx.coroutines.core;

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

    requires tornadofx;
    requires de.jensd.fx.glyphs.commons;
    requires de.jensd.fx.glyphs.fontawesome;
    requires org.controlsfx.controls;
    requires org.jsoup;

    opens tri.promptfx to com.fasterxml.jackson.databind;
    opens tri.promptfx.api to com.fasterxml.jackson.databind;
    opens tri.promptfx.apps.resources to tornadofx;
    opens tri.promptfx.fun to com.fasterxml.jackson.databind;
    opens tri.promptfx.integration to com.fasterxml.jackson.databind;

    exports tri.promptfx;
    exports tri.promptfx.api;
    exports tri.promptfx.apps;
    exports tri.promptfx.fun;
    exports tri.promptfx.integration;
    exports tri.util.ui;

    // services (service loader API)
    uses TextPlugin;
    uses NavigableWorkspaceView;

    provides NavigableWorkspaceView with
            DocumentQaPlugin,
            EntityExtractionPlugin,
            QuestionAnsweringPlugin,
            SentimentAnalysisPlugin,
            SummarizationPlugin,
            TextSimilarityPlugin,
            TextToJsonPlugin,
            TranslationPlugin,
            WeatherViewPlugin,
            WikipediaViewPlugin
    ;
}