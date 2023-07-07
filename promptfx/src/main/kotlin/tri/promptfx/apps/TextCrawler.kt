package tri.promptfx.apps

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import java.io.File
import java.net.URL
import java.nio.file.Files

/** Utility class for crawling/scraping a website. */
object TextCrawler {

    private val REQUIRE_ARTICLE = true
    private val CRAWL_LIMIT_LINKS = 100

    /** Crawls a website and saves the text from each page to a file. */
    fun crawlWebsite(url: String, depth: Int = 0, targetFolder: File, scraped: MutableSet<String> = mutableSetOf()) {
        if (url.isBlank() || url in scraped)
            return
        else if (url.endsWith(".pdf")) {
            println("Downloading PDF from $url...")
            Files.copy(URL(url).openStream(), File(targetFolder, url.substringAfterLast("/")).toPath())
        } else {
            println("Scraping text and links from $url...")
            val doc = Jsoup.connect(url).get()
            val articleNode = doc.select("article").firstOrNull()
            val docNode = when {
                REQUIRE_ARTICLE -> articleNode ?: return
                else -> articleNode ?: doc.body()
            }
            saveTextToFile(docNode, doc.title().ifBlank { url.removeSuffix("/").substringAfterLast("/") }, targetFolder)
            scraped.add(url)
            if (depth > 0)
                scrapeLinks(docNode, depth, targetFolder, scraped)
        }
    }

    private fun saveTextToFile(docNode: Element, title: String, targetFolder: File) {
        val nodeHtml = docNode.apply {
            select("br").before("\\n")
            select("p").before("\\n")
        }.html().replace("\\n", "\n")
        val text = Jsoup.clean(nodeHtml, "", Safelist.none(),
            Document.OutputSettings().apply { prettyPrint(false) }
        ).replace("\n{3,}".toRegex(), "\n\n")
        if (text.isNotEmpty()) {
            val saveTitle = title
                .replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                .replace("_{2,}".toRegex(), "_")
            File(targetFolder, "$saveTitle.txt").writeText(text)
        }
    }

    private fun scrapeLinks(docNode: Element, depth: Int, targetFolder: File, scraped: MutableSet<String>) {
        val links = docNode.select("a[href]")
            .map { it.absUrl("href").substringBeforeLast("#") }
            .toSet()
            .take(CRAWL_LIMIT_LINKS)
        links.forEach { crawlWebsite(it, depth - 1, targetFolder, scraped) }
    }

}