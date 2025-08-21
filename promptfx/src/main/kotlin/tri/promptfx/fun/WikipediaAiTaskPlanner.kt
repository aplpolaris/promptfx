package tri.promptfx.`fun`

import javafx.beans.property.Property
import tri.ai.core.TextChat
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.openai.jsonMapper
import tri.ai.prompt.PromptTemplate
import tri.promptfx.ModelParameters
import tri.promptfx.PromptFxGlobals.lookupPrompt
import java.net.URL
import java.net.URLEncoder

class WikipediaAiTaskPlanner(val chatEngine: TextChat, val common: ModelParameters, val pageTitle: Property<String>? = null, val input: String): AiPlanner {

    override fun plan() =
        aitask("wikipedia-page-guess") {
            common.completionBuilder()
                .prompt(lookupPrompt("examples-api/wikipedia-page-guess"))
                .tokens(200)
                .params(PromptTemplate.INPUT to input)
                .execute(chatEngine)
        }.task("wikipedia-page-search") {
            firstMatchingPage(it.content!!).also {
                pageTitle?.value = it
            }
        }.task("retrieve-page-text") {
            getWikipediaPage(it).also {
                pageTitle?.apply { value = "$value\n\n$it" }
            }
        }.aitask("question-answer") {
            common.completionBuilder()
                .prompt(lookupPrompt("text-qa/answer"))
                .params(PromptTemplate.INPUT to input, PromptTemplate.INSTRUCT to it)
                .tokens(1000)
                .execute(chatEngine)
        }.plan

    private fun firstMatchingPage(query: String): String {
        val url = "https://en.wikipedia.org/w/api.php?action=query" +
                "&format=json&list=search&srsearch=${query.trim().replace(" ", "%20")}"
        val jsonText = URL(url).readText()
        return jsonMapper.readTree(jsonText).at("/query/search/0/title").asText()
    }

    private fun getWikipediaPage(request: String): String {
        val title = request.trim()
        val url = "https://en.wikipedia.org/w/api.php?action=query" +
                "&format=json&prop=extracts&exintro=&explaintext=" +
                "&titles=${URLEncoder.encode(title, "UTF-8")}"
        println(url)
        val jsonText = URL(url).readText()
        val elements = jsonMapper.readTree(jsonText).at("/query/pages").elements()
        return if (elements.hasNext())
            elements.next()["extract"].asText()
        else
            "No content returned"
    }

}