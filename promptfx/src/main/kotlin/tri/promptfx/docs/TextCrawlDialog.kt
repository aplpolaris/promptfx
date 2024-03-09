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
package tri.promptfx.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import tornadofx.*
import tri.util.ui.chooseFolder
import tri.util.ui.graphic
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Dialog for choosing web scraper settings. */
class TextCrawlDialog: Fragment("Web Crawler Settings") {

    val folder: SimpleObjectProperty<File> by param()

    private val url = SimpleStringProperty("http://")

    override val root = vbox {
        form {
            fieldset("Crawl Settings") {
                field("URL to Scrape") {
                    textfield(url) {
                        tooltip("Enter URL starting with http:// or https://")
                    }
                    button("", FontAwesomeIcon.GLOBE.graphic) {
                        disableWhen(url.isEmpty)
                        action { hostServices.showDocument(url.get()) }
                    }
                }
                field("Target Folder") {
                    hyperlink(folder.stringBinding {
                        val path = it!!.absolutePath
                        if (path.length > 25) {
                            "..." + path.substring(path.length - 24)
                        } else {
                            path
                        }
                    }) {
                        action {
                            Files.createDirectories(folder.get().toPath())
                            Desktop.getDesktop().open(folder.get())
                        }
                    }
                    button("", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                        action { folder.chooseFolder(currentStage) }
                    }
                }
            }
        }
        buttonbar {
            padding = Insets(10.0)
            spacing = 10.0
            button("Crawl") {
                action {
                    crawlWebsite(url.value, depth = 1, targetFolder = folder.value)
                    close()
                }
            }
        }
    }
}

private fun crawlWebsite(url: String, depth: Int = 0, targetFolder: File, scraped: MutableSet<String> = mutableSetOf()) {
    if (url.isBlank() || url in scraped)
        return
    runAsync {
        println("Scraping text and links from $url...")
        try {
            val doc = Jsoup.connect(url).get()
            val docNode = doc.select("article").firstOrNull() ?: doc.body()
            val nodeHtml = docNode.apply {
                select("br").before("\\n")
                select("p").before("\\n")
            }.html().replace("\\n", "\n")
            val text = Jsoup.clean(nodeHtml, "", Safelist.none(),
                Document.OutputSettings().apply { prettyPrint(false) }
            )
            if (text.isNotEmpty()) {
                val title = doc.title().replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                if (title.length > 2) // require minimum length to avoid saving off blank links
                    File(targetFolder, "$title.txt").writeText(text)
                scraped.add(url)
            }
            if (depth > 0) {
                val links = docNode.select("a[href]").map { it.absUrl("href") }.toSet().take(100)
                links
                    .filter { it.startsWith("http") }
                    .forEach { crawlWebsite(it, depth - 1, targetFolder, scraped) }
            }
        } catch (x: IOException) {
            println("  ... failed to retrieve URL due to $x")
        }
    }
}
