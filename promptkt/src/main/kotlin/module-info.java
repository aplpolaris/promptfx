module tri.promptkt {
    requires transitive kotlin.stdlib;
    requires kotlinx.coroutines.core;

    requires java.logging;

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

    opens tri.ai.core to com.fasterxml.jackson.databind;
    opens tri.ai.embedding to com.fasterxml.jackson.databind;
    opens tri.ai.memory to com.fasterxml.jackson.databind;
    opens tri.ai.openai to com.fasterxml.jackson.databind;
    opens tri.ai.pips to com.fasterxml.jackson.databind;
    opens tri.ai.prompt to com.fasterxml.jackson.databind;

    exports tri.ai.core;
    exports tri.ai.embedding;
    exports tri.ai.memory;
    exports tri.ai.openai;
    exports tri.ai.pips;
    exports tri.ai.prompt;
    exports tri.ai.tool;
}
