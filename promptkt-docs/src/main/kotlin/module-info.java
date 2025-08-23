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

module tri.promptkt.docs {
    requires transitive tri.promptkt.pips;

    requires transitive java.desktop;
    requires transitive org.apache.poi.ooxml;
    requires transitive org.apache.poi.scratchpad;

    requires com.google.common;
    requires org.apache.pdfbox;
    requires org.jsoup;

    opens tri.ai.embedding to com.fasterxml.jackson.databind;
    opens tri.ai.text.chunks to com.fasterxml.jackson.databind, com.github.mustachejava;
    opens tri.ai.text.docs to com.github.mustachejava;

    exports tri.ai.embedding;
    exports tri.ai.process.pdf;
    exports tri.ai.text.chunks;
    exports tri.ai.text.docs;
    exports tri.util.io;
    exports tri.util.io.pdf;
    exports tri.util.io.poi;
}
