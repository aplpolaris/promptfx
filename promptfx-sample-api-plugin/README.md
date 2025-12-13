# PromptFx Sample TextPlugin

Sample plugin demonstrating how to implement a custom `TextPlugin` to provide AI models.

## Implement

Create a plugin class implementing `TextPlugin`:

```kotlin
class SampleTextPlugin : TextPlugin {
    override fun modelSource() = "SampleText"
    override fun chatModels() = listOf(SampleChatModel())
    override fun textCompletionModels() = listOf(SampleTextCompletionModel())
    override fun embeddingModels() = emptyList<EmbeddingModel>()
    // ... other model types
}
```

Register in `module-info.java`:
```java
provides TextPlugin with SampleTextPlugin;
```

## Deploy

Build and copy to config directory:
```bash
mvn package -pl promptfx-sample-api-plugin
cp promptfx-sample-api-plugin/target/promptfx-sample-api-plugin-*.jar config/modules/
```

## Run/Test

Start PromptFx - models appear under "SampleText" source in model selection dropdowns.

Run tests:
```bash
mvn test -pl promptfx-sample-api-plugin
```
