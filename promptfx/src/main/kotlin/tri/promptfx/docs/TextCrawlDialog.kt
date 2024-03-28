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
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import tornadofx.*
import tri.promptfx.promptFxDirectoryChooser
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
                        action { promptFxDirectoryChooser { folder.set(it) } }
                    }
                }
            }
        }
        buttonbar {
            padding = Insets(10.0)
            spacing = 10.0
            button("Crawl") {
                action {
                    WebCrawler.crawlWebsite(
                        url.value,
                        depth = 1,
                        maxLinks = 100,
                        requireSameDomain = true,
                        targetFolder = folder.value,
                        progressUpdate = { println(it) }
                    )
                    close()
                }
            }
        }
    }
}

object WebCrawler {

    /** Crawls a given URL, extracting text and optionally following links. */
    fun crawlWebsite(url: String, depth: Int = 0, maxLinks: Int = 100, requireSameDomain: Boolean, targetFolder: File, progressUpdate: (String) -> Unit) {
        val urlTitleText = crawlWebsite(url, depth, maxLinks, requireSameDomain, mutableSetOf(), progressUpdate)
        urlTitleText.forEach { (_, titleText) ->
            File(targetFolder, "${titleText.first}.txt")
                .writeText(titleText.second)
        }
    }

    /** Crawls a given URL, extracting text and optionally following links. */
    fun crawlWebsite(url: String, depth: Int = 0, maxLinks: Int = 100, requireSameDomain: Boolean, scraped: MutableSet<String> = mutableSetOf(), progressUpdate: (String) -> Unit): Map<String, Pair<String, String>> {
        if (url.isBlank() || url in scraped)
            return mapOf()
        return runAsync {
            val domain = domain(url)
            progressUpdate("Scraping text and links from $url, domain $domain...")
            val urlTitleText = mutableMapOf<String, Pair<String, String>>()
            try {
                val (doc, docNode, text) = scrapeText(url)
                if (text.isNotEmpty()) {
                    val title = doc.title().replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                    if (title.length > 2) // require minimum length to avoid saving off blank links
                        urlTitleText[url] = title to text
                }
                if (depth > 0) {
                    docNode.links().take(maxLinks).filter {
                        !requireSameDomain || it.contains("//$domain")
                    }.forEach {
                        urlTitleText.putAll(crawlWebsite(it, depth - 1, maxLinks, requireSameDomain, urlTitleText.keys, progressUpdate))
                    }
                }
            } catch (x: IOException) {
                println("  ... failed to retrieve URL due to $x")
            }
            urlTitleText
        }.get()
    }

    /** Get domain from URL. */
    private fun domain(url: String): String {
        return url.substringAfter("//").substringBefore("/")
    }

    /** Get text from URL. */
    fun scrapeText(url: String) =
        CACHE.getOrPut(url) { scrapeTextUncached(url) }

    private fun scrapeTextUncached(url: String): Triple<Document, Element, String> {
        val doc = Jsoup.connect(url).get()
        val docNode = doc.select("article").firstOrNull() ?: doc.body()
        val nodeHtml = docNode.apply {
            select("br").before("\\n")
            select("p").before("\\n")
        }.html().replace("\\n", "\n")
        val text = Jsoup.clean(nodeHtml, "", Safelist.none(),
            Document.OutputSettings().apply { prettyPrint(false) }
        )
        return Triple(doc, docNode, text)
    }

    /** Get list of links from a web element. */
    fun Element.links() =
        select("a[href]").map { it.absUrl("href") }.toSet()
            .filter { it.startsWith("http") }

    private val CACHE = mutableMapOf<String, Triple<Document, Element, String>>()
}
