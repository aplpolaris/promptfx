package tri.promptfx.docs

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import tornadofx.runAsync
import java.io.File
import java.io.IOException

/**
 * Recursive web crawler for extracting text from web pages, using [Jsoup].
 */
object WebCrawler {

    /** Crawls a given URL, extracting text and optionally following links. */
    fun crawlWebsite(url: String, depth: Int = 0, maxLinks: Int = 100, requireSameDomain: Boolean, targetFolder: File, progressUpdate: (String) -> Unit) {
        val crawlResult = crawlWebsite(url, depth, maxLinks, requireSameDomain, mutableSetOf(), progressUpdate)
        crawlResult.forEach { (_, content) ->
            File(targetFolder, content.fileName())
                .writeText(content.text)
        }
    }

    /** Crawls a given URL, extracting text and optionally following links. */
    fun crawlWebsite(link: String, depth: Int = 0, maxLinks: Int = 100, requireSameDomain: Boolean, scraped: MutableSet<String> = mutableSetOf(), progressUpdate: (String) -> Unit): Map<String, WebCrawlContent> {
        // revise url to prevent duplicate result
        val url = link.substringBeforeLast('#')

        if (url.isBlank() || url in scraped)
            return mapOf()
        val task = runAsync {
            val resultMap = mutableMapOf<String, WebCrawlContent>()
            val domain = domain(url)
            try {
                val content = if (url in CACHE.keys)
                    CACHE[url]!!
                else {
                    progressUpdate("Scraping text and links from $url, domain $domain...")
                    scrapeText(url)
                }
                resultMap[url] = content
                scraped.add(url)
                if (content.text.isNotEmpty() && content.docNode.title().length > 2) {
                    if (depth > 0) {
                        content.links.take(maxLinks).filter {
                            !requireSameDomain || it.contains("//$domain")
                        }.forEach {
                            resultMap.putAll(crawlWebsite(it, depth - 1, maxLinks, requireSameDomain, scraped, progressUpdate))
                        }
                    }
                }
            } catch (x: IOException) {
                println("  ... failed to retrieve URL due to $x")
            }
            resultMap
        }
        return task.get()
    }

    /** Get domain from URL. */
    private fun domain(url: String): String {
        return url.substringAfter("//").substringBefore("/")
    }

    /** Get text from URL. */
    fun scrapeText(url: String) =
        CACHE.getOrPut(url) { scrapeTextUncached(url) }

    private fun scrapeTextUncached(url: String): WebCrawlContent {
        val doc = Jsoup.connect(url).get()
        val article = doc.select("article").firstOrNull()
        val textElement = article ?: doc.body()
        val nodeHtml = textElement.apply {
            select("br").before("\\n")
            select("p").before("\\n")
        }.html().replace("\\n", "\n")
        val text = Jsoup.clean(nodeHtml, "", Safelist.none(),
            Document.OutputSettings().apply { prettyPrint(false) }
        )
        val textWithWhiteSpaceCleaned = text
            .replace(Regex("\\n\\s*\\n\\s*\\n"), "\n\n")
            .replace(Regex("^\\n{1,2}"), "")
            .replace(Regex("\\n{1,2}$"), "")
        return WebCrawlContent(url, doc, doc.title(), textElement, article != null, textWithWhiteSpaceCleaned, textElement.links())
    }

    /** Get list of links from a web element. */
    private fun Element.links() =
        select("a[href]").map { it.absUrl("href") }.toSet()
            .filter { it.startsWith("http") }

    /** Replace non-alphanumeric characters with underscores. */
    private fun WebCrawlContent.fileName() =
        docNode.title().replace("[^a-zA-Z0-9.-]".toRegex(), "_") + ".txt"

    private val CACHE = mutableMapOf<String, WebCrawlContent>()
}

/** Object with content for a website. */
class WebCrawlContent(
    val url: String,
    val docNode: Document,
    val title: String,
    val textElement: Element,
    val textArticle: Boolean,
    val text: String,
    val links: List<String>
)