/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.ui.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.text.docs.WebCrawler
import tri.promptfx.promptFxDirectoryChooser
import tri.util.ui.graphic
import tri.util.ui.slider
import tri.util.ui.sliderwitheditablelabel
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Files

/** Form for crawling web data. */
class WebScrapeFragment: Fragment("Web Scraper Settings") {

    val model: WebScrapeViewModel by param()
    val isShowLocalFolder = SimpleBooleanProperty(false)

    override val root = form {
        fieldset("Crawl Settings") {
            field("Starting URL") {
                textfield(model.webUrl) {
                    tooltip("Enter URL starting with http:// or https://")
                    prefColumnCount = 80
                }
                button("", FontAwesomeIcon.GLOBE.graphic) {
                    disableWhen(model.webUrl.isEmpty)
                    action { hostServices.showDocument(model.webUrl.value) }
                }
            }
            field("Crawl Depth") {
                tooltip("How many links deep to follow when scraping the website.")
                slider(1..3, model.webUrlDepth)
                label(model.webUrlDepth)
            }
            field("Domains") {
                checkbox("Limit to URLs on same domain", model.webUrlLimitDomain)
            }
            field("Max # Links to Crawl", forceLabelIndent = true) {
                tooltip("Maximum number of links to crawl from each page.")
                sliderwitheditablelabel(1..1000, model.webUrlMaxLinks)
            }
            field("Target Folder") {
                visibleWhen(isShowLocalFolder)
                managedWhen(isShowLocalFolder)
                hyperlink(model.webTargetFolder.stringBinding {
                    val path = it?.absolutePath ?: ""
                    if (path.length > 25) {
                        "..." + path.substring(path.length - 24)
                    } else {
                        path
                    }
                }) {
                    action {
                        Files.createDirectories(model.webTargetFolder.value.toPath())
                        Desktop.getDesktop().open(model.webTargetFolder.value)
                    }
                }
                button("", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                    action { promptFxDirectoryChooser { model.webTargetFolder.set(it) } }
                }
            }
        }
    }
}

/** Model for crawling web data. */
class WebScrapeViewModel : Component() {
    /** URL to scrape. */
    val webUrl = SimpleStringProperty("http://")
    /** Max depth to crawl when scraping a website. */
    val webUrlDepth = SimpleIntegerProperty(1)
    /** Max number of links to crawl when scraping a website. This is per identified note, not in total. */
    val webUrlMaxLinks = SimpleIntegerProperty(10)
    /** Whether to only crawl links on the same domain. */
    val webUrlLimitDomain = SimpleBooleanProperty(true)
    /** Target folder for saving scraped files. */
    val webTargetFolder = SimpleObjectProperty<File>()

    /** Get text of main URL. */
    fun mainUrlText() = WebCrawler.scrapeText(webUrl.value).text
    /** Scrape the website, with the given crawl settings. */
    fun scrapeWebsite(progressUpdate: (String) -> Unit) =
        WebCrawler.crawlWebsite(
            link = webUrl.value,
            depth = webUrlDepth.value,
            maxLinks = webUrlMaxLinks.value,
            requireSameDomain = webUrlLimitDomain.value,
            scraped = mutableSetOf(),
            progressUpdate = progressUpdate
        ).map { (url, content) ->
            URI.create(url) to content.title
        }.toMap()
}
