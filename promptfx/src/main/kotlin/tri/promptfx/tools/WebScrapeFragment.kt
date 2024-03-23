package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.promptfx.docs.WebCrawler
import tri.util.ui.chooseFolder
import tri.util.ui.graphic
import tri.util.ui.slider
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
                slider(1..3, model.webUrlDepth) {
                    valueProperty().bindBidirectional(model.webUrlDepth)
                }
                label(model.webUrlDepth)
            }
            field("Domains") {
                checkbox("Limit to URLs on same domain", model.webUrlDomain)
            }
            field("Max # Links to Crawl", forceLabelIndent = true) {
                tooltip("Maximum number of links to crawl from each page.")
                slider(1..1000, model.webUrlLimit) {
                    valueProperty().bindBidirectional(model.webUrlLimit)
                }
                label(model.webUrlLimit)
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
                    action { model.webTargetFolder.chooseFolder(currentStage) }
                }
            }
        }
    }
}

/** Model for crawling web data. */
class WebScrapeViewModel {
    /** URL to scrape. */
    val webUrl = SimpleStringProperty("http://")
    /** Max depth to crawl when scraping a website. */
    val webUrlDepth = SimpleIntegerProperty(1)
    /** Max number of links to crawl when scraping a website. This is per identified note, not in total. */
    val webUrlLimit = SimpleIntegerProperty(10)
    /** Whether to only crawl links on the same domain. */
    val webUrlDomain = SimpleBooleanProperty(true)
    /** Target folder for saving scraped files. */
    val webTargetFolder = SimpleObjectProperty<File>()

    /** Get text of main URL. */
    fun mainUrlText() = WebCrawler.scrapeText(webUrl.value).third
    /** Scrape the website, with the given crawl settings. */
    fun scrapeWebsite() = WebCrawler.crawlWebsite(webUrl.value, webUrlDepth.value, webUrlLimit.value)
        .map { (url, titleText) -> URI.create(url) to titleText.second }.toMap()

}