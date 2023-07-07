package tri.promptfx.integration

import javafx.beans.property.Property
import tri.ai.pips.AiPlanner
import tri.ai.core.TextCompletion
import tri.ai.pips.aitask
import tri.ai.openai.mapper
import tri.ai.openai.promptTask
import tri.ai.openai.instructTask
import java.net.URL
import java.net.URLEncoder

class WikipediaAiTaskPlanner(val completionEngine: TextCompletion, val pageTitle: Property<String>? = null, val input: String): AiPlanner {

    override fun plan() =
        aitask("wikipedia-page-guess") {
            completionEngine.promptTask("wikipedia-page-guess", input, tokenLimit = 500)
        }.task("wikipedia-page-search") {
            firstMatchingPage(it).also {
                pageTitle?.value = it
            }
        }.task("retrieve-page-text") {
            getWikipediaPage(it).also {
                pageTitle?.apply { value = "$value\n\n$it" }
            }
        }.aitask("question-answer") {
            completionEngine.instructTask("question-answer", input, it, tokenLimit = 500)
        }.plan

    private fun firstMatchingPage(query: String): String {
        val url = "https://en.wikipedia.org/w/api.php?action=query" +
                "&format=json&list=search&srsearch=${query.trim().replace(" ", "%20")}"
        val jsonText = URL(url).readText()
        return mapper.readTree(jsonText).at("/query/search/0/title").asText()
    }

    private fun getWikipediaPage(request: String): String {
        val title = request.trim()
        val url = "https://en.wikipedia.org/w/api.php?action=query" +
                "&format=json&prop=extracts&exintro=&explaintext=" +
                "&titles=${URLEncoder.encode(title, "UTF-8")}"
        println(url)
        val jsonText = URL(url).readText()
        val elements = mapper.readTree(jsonText).at("/query/pages").elements()
        return if (elements.hasNext())
            elements.next()["extract"].asText()
        else
            "No content returned"
    }

}