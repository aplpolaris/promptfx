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
package tri.promptfx.docs

import javafx.geometry.Insets
import tornadofx.*
import tri.promptfx.ui.docs.WebScrapeFragment
import tri.promptfx.ui.docs.WebScrapeViewModel
import tri.util.io.WebCrawler

/** Dialog for choosing web scraper settings. */
class TextCrawlDialog: Fragment("Web Crawler Settings") {

    private lateinit var fragment: WebScrapeFragment

    val model
        get() = fragment.model

    override val root = vbox {
        prefWidth = 400.0
        fragment = find<WebScrapeFragment>("model" to find<WebScrapeViewModel>())
        add(fragment)
        buttonbar {
            padding = Insets(10.0)
            spacing = 10.0
            button("Crawl") {
                disableWhen(model.webUrl.isEmpty or model.webUrl.isEqualTo("http://"))
                action {
                    with (fragment.model) {
                        WebCrawler.crawlWebsite(
                            webUrl.value,
                            webUrlDepth.value,
                            webUrlMaxLinks.value,
                            webUrlLimitDomain.value,
                            webTargetFolder.value,
                            progressUpdate = { println(it) }
                        )
                    }
                    close()
                }
            }
            button("Cancel") {
                action { close() }
            }
        }
    }
}

