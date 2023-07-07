package tri.promptfx.`fun`

import tri.promptfx.AiCompletionView

class EmojiView : AiCompletionView(
    "Emoji",
    "Enter text to turn into Emoji",
    "example-emoji",
    tokenLimit = 50
)
