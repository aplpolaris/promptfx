The *Documents* tab has views for working with document sets and text libraries.

# Document Q&A View

The `Document Q&A` view is the default view. This lets you "ask questions" of a folder of local documents on your system, using OpenAI's Ada embedding model to retrieve relevant document chunks, and using context-augmented prompting to answer questions. In the view below, a question is being asked on a collection of research papers.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/f5d6d17c-335f-4074-848a-87d0ea7c2aaa)

The view at the bottom left shows the "document snippets" that are used to find the most relevant matches in the documents in the folder. If you click on a snippet from a PDF document, a PDF viewer will show the page where the snippet comes from, as shown here:

![image](https://github.com/aplpolaris/promptfx/assets/13057929/7c289a7d-661b-4059-a21c-a6c1b4b90303)

To customize the documents used, select the `Open Folder` button and browse to a folder on your local system. Add documents (`.txt`, `.pdf`, `.doc`, or `.docx`) to this folder, and when you ask a question PromptFx will automatically pull text from the PDF/DOC files, chunk the text into sections, calculate embeddings, lookup relevant chunks based on your question, and use OpenAI's API to formulate a suitable response. The prompt is also designed to provide a reference to the source document. The configuration panel at right can be used to adjust the settings for chunking documents, as well as the prompting strategy.

In "full screen mode" (see button on toolbar), PromptFx provides a more elegant interactive mode, additionally rendering PDF thumbnails for source documents.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/a063f5b3-59be-4b87-b0ef-76d5d22a9fa6)

# Document Insights View

This view allows multi-step document processing over a folder on your local machine, and is in early development.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/704f15f2-6adc-4c9c-aff4-d5cdbddd86a9)

# Text Manager View

The `Text Manager` view is for managing collections of documents/text.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/3febee10-159d-4112-b013-a9fd2703a388)

PromptFx manages text collections as a "library", consisting of a set of "documents", each of which contains a set of "chunks" or "snippets". These libraries are created automatically in the document views (see [[Documents]]), and also can be created manually in this view.

## Creating Text Libraries

Click `Create...` to open a wizard for generating a text library.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/b74c0578-d2a6-438c-b1bc-25e09997d1c6)

The first tab `Select Source` has four modes:

* **File** mode, for generating a library with a single document from that file. This will scrape text from some formats (`pdf,doc,docx`) while other formats will be read as-is.
* **Folder** mode, for generating a library from all documents in a single folder.
* **User Input** mode, for generating a library with a single document based on user input in a text area.
* **Web Scraping** mode, for generating a library by scraping a website.

Click `Next` to proceed to the next tab `Configure Chunking`. The application may pause for a second while loading in the first document specified in the previous tab.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/84007b40-ce13-4b65-836d-58f700144b1d)

This tab has four modes:

* **Automatic** mode, for generating chunks automatically from text, using the same algorithm used in [[Documents]].
* **Delimiter** mode, for breaking up chunks by white space or other delimiters.
* **Regex** mode, for breaking up chunks by regex.
* **Field** mode is a planned future feature.

Click `Finish` to load the resulting chunks into the view. The new library will show up under `Document Collections` and be selected. Be sure to use `Save...` if you want to be able to reload the library later.

## Working with Text Libraries

The list view under `Document Collections` shows all currently loaded document collections. Select a collection to see documents in the second list view, and select one or more documents to see all associated chunks in the third list view. Information about selected library, document(s), and chunk(s) will be displayed on the right side of the view.

Libraries are not saved automatically, and changes are not tracked. Use `Save...` to save the library information as a JSON file, and `Load...` to restore it later. However, any libraries in the view that have been saved to a file will be automatically loaded the next time you start `PromptFx`. (This may cause a short delay when you first open the `Text Manager` tab.)

Additional notes:

* You can remove libraries from the view using the context menu (`Remove Selected Library`)
* You can remove documents from a library using the context menu (`Remove Selected Document(s)`)
* You can remove chunks from selected documents using the context menu (`Remove Selected Chunks(s)`)

## Working with Embeddings

Document libraries are designed so embeddings can be saved by chunk.

* Use `Calculate Embeddings` to generate embeddings for the current embedding model for all chunks in the given library. This may take some time and consume a lot of tokens, so use this with caution.
* Once calculated, save the library to file again to ensure the embedding vectors are available for future use.
* You can search through documents semantically using the filter button in the text chunk view. Enter a query, and the third view will update to show the top matching snippets, along with a score for each.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/91d65fcb-64b2-4f30-9414-c9385e2c9359)

# Text Clustering View

The `text clustering` is for generating clusters from collections of text or document sets. Use the wizard to create a new collection of text chunks, or open an existing one. Here is an example pasting in a list of news headlines:

![image](https://github.com/user-attachments/assets/885db421-f5e1-4e86-8a6d-0f8fb6a7d9eb)

![image](https://github.com/user-attachments/assets/383ef3a2-2317-46bc-97dd-3e29714843b6)

With the default settings, hitting `Run` will generate a list of clusters using [Affinity Propagation](https://en.wikipedia.org/wiki/Affinity_propagation) clustering.

![image](https://github.com/user-attachments/assets/66aee3bc-3e61-4852-a44a-60f076d13ec1)

You can enable cluster summarization and recursive clustering using parameters, and also view cluster results as a list and/or tree. Here is a sample result, where two levels of clustering are used to summarize headlines:

![image](https://github.com/user-attachments/assets/422928d1-15ba-4fa9-898d-e299368f659a)

