# promptrt REPL Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace four single-purpose chat CLIs with one unified REPL (`promptrt`) that supports modes, feature toggles, and in-session model/config switching.

**Architecture:** A JLine3-backed interactive REPL sits on top of a `SessionState` object that holds all live config (model, mode, toggles). A `CommandParser` maps `/cmd` input to sealed `ReplCommand` types; a dispatcher mutates state or delegates to the appropriate AI backend (plain chat, BotMemory, LocalDocumentQaDriver, or agent executor). The existing backend classes are reused unchanged; only the CLI entry layer is replaced.

**Tech Stack:** Kotlin, JLine3 (REPL/completion), Clikt (top-level arg parsing + non-interactive subcommands), Jackson YAML (config file), JUnit 5, existing promptkt/promptex backends.

**Design doc:** `docs/design/promptrt-cli-design.md` (the agreed design document — refer to it for command contracts, mode inheritance rules, and priority tiers).

**Build command:** `mvn -B test -f promptrt/promptrt-cli/pom.xml`

---

## Settled Decisions

- **JLine3** for in-REPL tab completion, readline history, Ctrl+R — required dependency.
- **`/model` does not clear context; `/mode` does.** Model is an inference swap; mode is a full reset.
- **`plain` defaults are hardcoded in binary.** User config only overrides — `plain` ships as `gpt-4o-mini / openai / all features off / stream: true`.
- **Transition:** old CLIs (`SimpleChatCli`, `MemoryChatCli`, `AgentChatCli`) are deprecated when P1 is complete and deleted after user soak testing (Phase 15 — requires explicit user approval).

---

## Phase 1: Dependencies and Package Skeleton

### Task 1: Add JLine3 to pom.xml

**Files:**
- Modify: `promptrt/promptrt-cli/pom.xml`

**Step 1: Add JLine3 dependency**

Add inside `<dependencies>`, after the clikt-jvm entry:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.27.1</version>
</dependency>
```

**Step 2: Verify build still compiles**

```bash
mvn -B compile -f promptrt/promptrt-cli/pom.xml
```
Expected: `BUILD SUCCESS`

**Step 3: Commit**

```bash
git add promptrt/promptrt-cli/pom.xml
git commit -m "build: add JLine3 dependency for REPL tab completion"
```

---

## Phase 2: Config Types and Built-in Modes

### Task 2: ModePreset data class

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/config/ModePreset.kt`
- Create: `promptrt/promptrt-cli/src/test/kotlin/tri/ai/cli/config/ModePresetTest.kt`

**Step 1: Write the failing test**

```kotlin
package tri.ai.cli.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ModePresetTest {

    @Test
    fun `plain built-in has expected defaults`() {
        val plain = BuiltInModes.PLAIN
        assertEquals("gpt-4o-mini", plain.model)
        assertEquals("openai", plain.provider)
        assertFalse(plain.memory)
        assertFalse(plain.rag)
        assertFalse(plain.tools)
        assert(plain.stream)
    }

    @Test
    fun `mode merges onto plain, unspecified fields inherit`() {
        val partial = ModePreset(name = "custom", model = "gpt-4o")
        val resolved = partial.mergedOnto(BuiltInModes.PLAIN)
        assertEquals("gpt-4o", resolved.model)
        assertEquals("openai", resolved.provider)   // inherited
        assertFalse(resolved.memory)                // inherited
    }
}
```

**Step 2: Run to verify it fails**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=ModePresetTest
```
Expected: FAIL — `ModePreset` not defined.

**Step 3: Implement ModePreset and BuiltInModes**

```kotlin
package tri.ai.cli.config

data class ModePreset(
    val name: String = "plain",
    val model: String? = null,
    val provider: String? = null,
    val memory: Boolean? = null,
    val rag: Boolean? = null,
    val ragPath: String? = null,
    val tools: Boolean? = null,
    val stream: Boolean? = null,
    val system: String? = null
) {
    fun mergedOnto(base: ModePreset): ModePreset = ModePreset(
        name = name,
        model = model ?: base.model,
        provider = provider ?: base.provider,
        memory = memory ?: base.memory,
        rag = rag ?: base.rag,
        ragPath = ragPath ?: base.ragPath,
        tools = tools ?: base.tools,
        stream = stream ?: base.stream,
        system = system ?: base.system
    )

    // Resolved (non-null) accessors — only valid after mergedOnto(PLAIN)
    val resolvedModel get() = model ?: BuiltInModes.PLAIN.model!!
    val resolvedProvider get() = provider ?: BuiltInModes.PLAIN.provider!!
    val memoryOn get() = memory ?: false
    val ragOn get() = rag ?: false
    val toolsOn get() = tools ?: false
    val streamOn get() = stream ?: true
}

object BuiltInModes {
    val PLAIN = ModePreset(
        name = "plain",
        model = "gpt-4o-mini",
        provider = "openai",
        memory = false,
        rag = false,
        tools = false,
        stream = true,
        system = null
    )
    val MEMORY = ModePreset(name = "memory", model = "gpt-4o-mini", memory = true)
    val RAG    = ModePreset(name = "rag",    model = "gpt-4o",      rag = true)
    val AGENT  = ModePreset(name = "agent",  model = "gpt-4o",      memory = true, tools = true,
                             system = "You are a helpful assistant with access to tools.")

    val all = mapOf("plain" to PLAIN, "memory" to MEMORY, "rag" to RAG, "agent" to AGENT)
        .mapValues { (_, v) -> v.mergedOnto(PLAIN) }
}
```

**Step 4: Run tests**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=ModePresetTest
```
Expected: PASS

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add ModePreset data class and built-in mode definitions"
```

---

### Task 3: PromptRtConfig and ConfigLoader

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/config/PromptRtConfig.kt`
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/config/ConfigLoader.kt`
- Create: `promptrt/promptrt-cli/src/test/kotlin/tri/ai/cli/config/ConfigLoaderTest.kt`

**Step 1: Write failing tests**

```kotlin
package tri.ai.cli.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigLoaderTest {

    private val minimalYaml = """
        default_mode: plain
    """.trimIndent()

    private val fullYaml = """
        default_mode: rag
        modes:
          custom:
            model: claude-3-5-sonnet
            provider: anthropic
            memory: true
        providers:
          anthropic:
            api_key_env: ANTHROPIC_API_KEY
    """.trimIndent()

    @Test
    fun `minimal config loads with defaults`() {
        val config = ConfigLoader.fromYaml(minimalYaml)
        assertEquals("plain", config.defaultMode)
        assert(config.modes.isEmpty())
    }

    @Test
    fun `full config loads user modes and providers`() {
        val config = ConfigLoader.fromYaml(fullYaml)
        assertEquals("rag", config.defaultMode)
        assertNotNull(config.modes["custom"])
        assertEquals("claude-3-5-sonnet", config.modes["custom"]!!.model)
        assertEquals("ANTHROPIC_API_KEY", config.providers["anthropic"]?.apiKeyEnv)
    }

    @Test
    fun `resolveMode merges user mode onto plain`() {
        val config = ConfigLoader.fromYaml(fullYaml)
        val resolved = config.resolveMode("custom")
        assertEquals("claude-3-5-sonnet", resolved.model)
        assertEquals("anthropic", resolved.provider)
        assert(resolved.memoryOn)
        assert(resolved.streamOn)  // inherited from plain
    }

    @Test
    fun `resolveMode returns built-in for unknown name`() {
        val config = ConfigLoader.fromYaml(minimalYaml)
        val resolved = config.resolveMode("agent")
        assertEquals("gpt-4o", resolved.model)
        assert(resolved.toolsOn)
    }

    @Test
    fun `load returns empty config when file missing`() {
        val config = ConfigLoader.load(java.io.File("/nonexistent/path/config.yaml"))
        assertEquals("plain", config.defaultMode)
    }
}
```

**Step 2: Run to verify fails**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=ConfigLoaderTest
```
Expected: FAIL — types not defined.

**Step 3: Implement PromptRtConfig and ConfigLoader**

```kotlin
// PromptRtConfig.kt
package tri.ai.cli.config

data class ProviderConfig(val apiKeyEnv: String? = null)

data class PromptRtConfig(
    val defaultMode: String = "plain",
    val modes: Map<String, ModePreset> = emptyMap(),
    val providers: Map<String, ProviderConfig> = emptyMap()
) {
    fun resolveMode(name: String): ModePreset {
        val base = BuiltInModes.all["plain"]!!
        val preset = modes[name] ?: BuiltInModes.all[name] ?: return base
        return preset.mergedOnto(base)
    }

    val allModeNames: List<String>
        get() = (BuiltInModes.all.keys + modes.keys).distinct().sorted()
}
```

```kotlin
// ConfigLoader.kt
package tri.ai.cli.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

object ConfigLoader {
    private val mapper = YAMLMapper().apply {
        registerModule(kotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun fromYaml(yaml: String): PromptRtConfig = mapper.readValue(yaml)

    fun load(file: File): PromptRtConfig =
        if (file.exists()) fromYaml(file.readText()) else PromptRtConfig()

    fun loadDefault(): PromptRtConfig =
        load(File(System.getProperty("user.home"), ".promptrt/config.yaml"))
}
```

**Step 4: Run tests**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=ConfigLoaderTest
```
Expected: PASS

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add PromptRtConfig YAML loader with mode resolution"
```

---

## Phase 3: Command Parser

### Task 4: ReplCommand sealed class

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/ReplCommand.kt`

No test needed — this is a pure data structure. Commit it alongside the parser.

```kotlin
package tri.ai.cli.repl

sealed class ReplCommand {
    // Navigation
    data class Mode(val name: String) : ReplCommand()
    data class Model(val id: String) : ReplCommand()
    data class Provider(val name: String) : ReplCommand()

    // Feature toggles
    data class Memory(val on: Boolean) : ReplCommand()
    data class Rag(val on: Boolean, val path: String? = null) : ReplCommand()
    data class Tools(val on: Boolean) : ReplCommand()
    data class Stream(val on: Boolean) : ReplCommand()
    data class JsonMode(val on: Boolean) : ReplCommand()

    // Sampling
    data class Temp(val value: Double) : ReplCommand()
    data class TopP(val value: Double) : ReplCommand()
    data class Seed(val value: Int) : ReplCommand()

    // Prompt
    data class SystemPrompt(val text: String) : ReplCommand()

    // Session
    data class Batch(val path: String) : ReplCommand()
    object Status : ReplCommand()
    object Reset : ReplCommand()
    object Help : ReplCommand()
    object Quit : ReplCommand()

    // Chat input (not a slash command)
    data class Chat(val text: String) : ReplCommand()

    // Error
    data class Unknown(val input: String) : ReplCommand()
}
```

---

### Task 5: CommandParser

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/CommandParser.kt`
- Create: `promptrt/promptrt-cli/src/test/kotlin/tri/ai/cli/repl/CommandParserTest.kt`

**Step 1: Write failing tests**

```kotlin
package tri.ai.cli.repl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommandParserTest {

    @Test fun `plain text becomes Chat`() =
        assertIs<ReplCommand.Chat>(CommandParser.parse("hello world"))

    @Test fun `slash mode parses name`() =
        assertEquals(ReplCommand.Mode("rag"), CommandParser.parse("/mode rag"))

    @Test fun `slash model parses id`() =
        assertEquals(ReplCommand.Model("gpt-4o"), CommandParser.parse("/model gpt-4o"))

    @Test fun `slash memory on`() =
        assertEquals(ReplCommand.Memory(true), CommandParser.parse("/memory on"))

    @Test fun `slash memory off`() =
        assertEquals(ReplCommand.Memory(false), CommandParser.parse("/memory off"))

    @Test fun `slash rag with path`() {
        val cmd = CommandParser.parse("/rag ~/docs/kb")
        assertIs<ReplCommand.Rag>(cmd)
        assertTrue((cmd as ReplCommand.Rag).on)
        assertEquals("~/docs/kb", cmd.path)
    }

    @Test fun `slash rag off`() =
        assertEquals(ReplCommand.Rag(false), CommandParser.parse("/rag off"))

    @Test fun `slash tools on`() =
        assertEquals(ReplCommand.Tools(true), CommandParser.parse("/tools on"))

    @Test fun `slash temp parses double`() =
        assertEquals(ReplCommand.Temp(0.8), CommandParser.parse("/temp 0.8"))

    @Test fun `slash seed parses int`() =
        assertEquals(ReplCommand.Seed(42), CommandParser.parse("/seed 42"))

    @Test fun `slash status`() =
        assertIs<ReplCommand.Status>(CommandParser.parse("/status"))

    @Test fun `slash quit`() =
        assertIs<ReplCommand.Quit>(CommandParser.parse("/quit"))

    @Test fun `unknown command returns Unknown`() {
        val cmd = CommandParser.parse("/frobnicate")
        assertIs<ReplCommand.Unknown>(cmd)
    }

    @Test fun `missing argument returns Unknown with message`() {
        val cmd = CommandParser.parse("/mode")
        assertIs<ReplCommand.Unknown>(cmd)
    }

    @Test fun `slash system captures full text`() =
        assertEquals(ReplCommand.SystemPrompt("You are a pirate."),
            CommandParser.parse("/system You are a pirate."))
}
```

**Step 2: Run to verify fails**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=CommandParserTest
```
Expected: FAIL.

**Step 3: Implement CommandParser**

```kotlin
package tri.ai.cli.repl

object CommandParser {
    fun parse(input: String): ReplCommand {
        if (!input.startsWith("/")) return ReplCommand.Chat(input)

        val parts = input.trim().split("\\s+".toRegex(), limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim()

        return when (cmd) {
            "/mode"     -> arg?.let { ReplCommand.Mode(it) }
                           ?: ReplCommand.Unknown("/mode requires a mode name")
            "/model"    -> arg?.let { ReplCommand.Model(it) }
                           ?: ReplCommand.Unknown("/model requires a model id")
            "/provider" -> arg?.let { ReplCommand.Provider(it) }
                           ?: ReplCommand.Unknown("/provider requires a provider name")
            "/memory"   -> parseToggle(arg, ::ReplCommand.Memory)
            "/tools"    -> parseToggle(arg, ::ReplCommand.Tools)
            "/stream"   -> parseToggle(arg, ::ReplCommand.Stream)
            "/json"     -> parseToggle(arg, ::ReplCommand.JsonMode)
            "/rag"      -> when (arg) {
                               null, "off" -> ReplCommand.Rag(false)
                               "on"  -> ReplCommand.Rag(true)
                               else  -> ReplCommand.Rag(true, path = arg)
                           }
            "/temp"     -> arg?.toDoubleOrNull()?.let { ReplCommand.Temp(it) }
                           ?: ReplCommand.Unknown("/temp requires a number (e.g. /temp 0.7)")
            "/topp"     -> arg?.toDoubleOrNull()?.let { ReplCommand.TopP(it) }
                           ?: ReplCommand.Unknown("/topp requires a number (e.g. /topp 0.9)")
            "/seed"     -> arg?.toIntOrNull()?.let { ReplCommand.Seed(it) }
                           ?: ReplCommand.Unknown("/seed requires an integer")
            "/system"   -> arg?.let { ReplCommand.SystemPrompt(it) }
                           ?: ReplCommand.Unknown("/system requires prompt text")
            "/batch"    -> arg?.let { ReplCommand.Batch(it) }
                           ?: ReplCommand.Unknown("/batch requires a file path")
            "/status"   -> ReplCommand.Status
            "/reset"    -> ReplCommand.Reset
            "/help"     -> ReplCommand.Help
            "/quit"     -> ReplCommand.Quit
            else        -> ReplCommand.Unknown("Unknown command: $cmd — type /help for commands")
        }
    }

    private fun <T : ReplCommand> parseToggle(arg: String?, ctor: (Boolean) -> T): ReplCommand =
        when (arg?.lowercase()) {
            "on"  -> ctor(true)
            "off" -> ctor(false)
            else  -> ReplCommand.Unknown("Expected 'on' or 'off', got: $arg")
        }
}
```

**Step 4: Run tests**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=CommandParserTest
```
Expected: PASS

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add ReplCommand sealed class and CommandParser"
```

---

## Phase 4: SessionState

### Task 6: SessionState and mutations

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/SessionState.kt`
- Create: `promptrt/promptrt-cli/src/test/kotlin/tri/ai/cli/repl/SessionStateTest.kt`

**Step 1: Write failing tests**

```kotlin
package tri.ai.cli.repl

import org.junit.jupiter.api.Test
import tri.ai.cli.config.BuiltInModes
import tri.ai.cli.config.PromptRtConfig
import tri.ai.core.TextChatMessage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStateTest {

    private fun plainState() = SessionState.fromConfig(PromptRtConfig())

    @Test fun `initial state matches plain mode`() {
        val s = plainState()
        assertEquals("gpt-4o-mini", s.effectiveModel)
        assertFalse(s.memoryEnabled)
        assertFalse(s.ragEnabled)
        assertFalse(s.toolsEnabled)
    }

    @Test fun `model override does not clear history`() {
        val s = plainState()
        s.history.add(TextChatMessage(tri.ai.core.MChatRole.User, "hello"))
        s.applyModelOverride("gpt-4o")
        assertEquals("gpt-4o", s.effectiveModel)
        assertEquals(1, s.history.size)
    }

    @Test fun `mode switch clears history`() {
        val s = plainState()
        s.history.add(TextChatMessage(tri.ai.core.MChatRole.User, "hello"))
        s.switchMode(BuiltInModes.all["rag"]!!)
        assertEquals(0, s.history.size)
        assertTrue(s.ragEnabled)
    }

    @Test fun `reset restores default mode and clears overrides`() {
        val s = plainState()
        s.applyModelOverride("gpt-4o")
        s.memoryEnabled = true
        s.reset(PromptRtConfig())
        assertEquals("gpt-4o-mini", s.effectiveModel)
        assertFalse(s.memoryEnabled)
        assertNull(s.modelOverride)
    }

    @Test fun `effectiveModel prefers override over mode model`() {
        val s = plainState()
        s.applyModelOverride("claude-opus")
        assertEquals("claude-opus", s.effectiveModel)
    }
}
```

**Step 2: Run to verify fails**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=SessionStateTest
```
Expected: FAIL.

**Step 3: Implement SessionState**

```kotlin
package tri.ai.cli.repl

import tri.ai.cli.config.BuiltInModes
import tri.ai.cli.config.ModePreset
import tri.ai.cli.config.PromptRtConfig
import tri.ai.core.TextChatMessage

class SessionState private constructor(
    var activeMode: ModePreset,
    var modelOverride: String?,
    var memoryEnabled: Boolean,
    var ragEnabled: Boolean,
    var ragPath: String?,
    var toolsEnabled: Boolean,
    var streamEnabled: Boolean,
    var jsonMode: Boolean,
    var systemPrompt: String?,
    var temperature: Double,
    var topP: Double?,
    var seed: Int?,
    val history: MutableList<TextChatMessage>
) {
    val effectiveModel: String
        get() = modelOverride ?: activeMode.resolvedModel

    fun applyModelOverride(id: String) {
        modelOverride = id
    }

    fun switchMode(preset: ModePreset) {
        activeMode = preset
        modelOverride = null
        memoryEnabled = preset.memoryOn
        ragEnabled = preset.ragOn
        ragPath = preset.ragPath
        toolsEnabled = preset.toolsOn
        streamEnabled = preset.streamOn
        systemPrompt = preset.system
        history.clear()
    }

    fun reset(config: PromptRtConfig) {
        switchMode(config.resolveMode(config.defaultMode))
    }

    companion object {
        fun fromConfig(config: PromptRtConfig): SessionState {
            val mode = config.resolveMode(config.defaultMode)
            return SessionState(
                activeMode = mode,
                modelOverride = null,
                memoryEnabled = mode.memoryOn,
                ragEnabled = mode.ragOn,
                ragPath = mode.ragPath,
                toolsEnabled = mode.toolsOn,
                streamEnabled = mode.streamOn,
                jsonMode = false,
                systemPrompt = mode.system,
                temperature = 0.7,
                topP = null,
                seed = null,
                history = mutableListOf()
            )
        }
    }
}
```

**Step 4: Run tests**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=SessionStateTest
```
Expected: PASS

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add SessionState with mode switching and model override"
```

---

## Phase 5: REPL Loop and JLine3

### Task 7: ReplCompleter

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/ReplCompleter.kt`

No isolated unit test (requires JLine3 terminal). Verified manually in Task 8.

```kotlin
package tri.ai.cli.repl

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import tri.ai.cli.config.PromptRtConfig
import tri.ai.core.AiModelProvider

private val SLASH_COMMANDS = listOf(
    "/mode", "/model", "/provider", "/memory", "/rag", "/tools",
    "/stream", "/json", "/temp", "/topp", "/seed", "/system",
    "/batch", "/status", "/reset", "/help", "/quit"
)

class ReplCompleter(private val config: PromptRtConfig) : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val word = line.word()
        val words = line.words()

        when {
            words.size <= 1 -> SLASH_COMMANDS
                .filter { it.startsWith(word) }
                .forEach { candidates.add(Candidate(it)) }

            words[0] == "/mode" ->
                config.allModeNames
                    .filter { it.startsWith(word) }
                    .forEach { candidates.add(Candidate(it)) }

            words[0] == "/model" ->
                AiModelProvider.chatModels()
                    .map { it.modelId }
                    .filter { it.contains(word, ignoreCase = true) }
                    .forEach { candidates.add(Candidate(it)) }

            words[0] in listOf("/memory", "/tools", "/stream", "/json") ->
                listOf("on", "off")
                    .filter { it.startsWith(word) }
                    .forEach { candidates.add(Candidate(it)) }
        }
    }
}
```

---

### Task 8: PromptRtRepl — skeleton loop

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`

This task wires JLine3 into a working loop — no AI calls yet, just command dispatch to print stubs. Manual test only.

```kotlin
package tri.ai.cli.repl

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import tri.ai.cli.config.PromptRtConfig
import tri.util.ANSI_GRAY
import tri.util.ANSI_LIGHTBLUE
import tri.util.ANSI_RED
import tri.util.ANSI_RESET
import java.io.File

class PromptRtRepl(private val config: PromptRtConfig) {

    private val state = SessionState.fromConfig(config)

    fun start() {
        val terminal = TerminalBuilder.builder().system(true).build()
        val history = DefaultHistory()
        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(ReplCompleter(config))
            .history(history)
            .variable(org.jline.reader.LineReader.HISTORY_FILE,
                File(System.getProperty("user.home"), ".promptrt/history").absolutePath)
            .build()

        printInfo("promptrt — type /help for commands, /quit to exit")
        printInfo("Mode: ${state.activeMode.name}  Model: ${state.effectiveModel}")

        while (true) {
            val line = try {
                reader.readLine("> ")
            } catch (e: UserInterruptException) {
                continue
            } catch (e: EndOfFileException) {
                break
            }

            if (line.isBlank()) continue
            dispatch(CommandParser.parse(line))
        }
    }

    private fun dispatch(cmd: ReplCommand) {
        when (cmd) {
            is ReplCommand.Quit    -> { printInfo("Goodbye!"); System.exit(0) }
            is ReplCommand.Help    -> printHelp()
            is ReplCommand.Status  -> printStatus()
            is ReplCommand.Reset   -> { state.reset(config); printInfo("Reset to ${state.activeMode.name}") }
            is ReplCommand.Unknown -> printError(cmd.input)
            is ReplCommand.Chat    -> printInfo("[chat stub] ${cmd.text}")  // replaced in Phase 6
            else                   -> printInfo("[stub] $cmd")              // replaced in later phases
        }
    }

    private fun printHelp() {
        printInfo("""
            Commands:
              /mode <name>       switch mode (${config.allModeNames.joinToString(", ")})
              /model <id>        override model for session
              /provider <name>   switch provider
              /memory <on|off>   toggle memory
              /rag <on|off|path> toggle RAG
              /tools <on|off>    toggle tool use
              /stream <on|off>   toggle streaming
              /json <on|off>     toggle JSON output mode
              /system <text>     set system prompt
              /temp <n>          set temperature
              /topp <n>          set top-p
              /seed <n>          set sampling seed
              /batch <file>      run a batch job
              /status            show current session config
              /reset             restore default mode
              /help              show this help
              /quit              exit
        """.trimIndent())
    }

    private fun printStatus() {
        printInfo("""
            Mode:        ${state.activeMode.name}
            Model:       ${state.effectiveModel}
            Memory:      ${state.memoryEnabled}
            RAG:         ${state.ragEnabled}${if (state.ragPath != null) " (${state.ragPath})" else ""}
            Tools:       ${state.toolsEnabled}
            Stream:      ${state.streamEnabled}
            JSON mode:   ${state.jsonMode}
            Temperature: ${state.temperature}
            Top-P:       ${state.topP ?: "default"}
            Seed:        ${state.seed ?: "none"}
            System:      ${state.systemPrompt ?: "none"}
            History:     ${state.history.size} messages
        """.trimIndent())
    }

    private fun printResponse(text: String) = println("$ANSI_LIGHTBLUE$text$ANSI_RESET")
    private fun printInfo(text: String)     = println("$ANSI_GRAY$text$ANSI_RESET")
    private fun printError(text: String)    = println("${ANSI_RED}ERROR: $text$ANSI_RESET")
}
```

**Step 1: Compile**

```bash
mvn -B compile -f promptrt/promptrt-cli/pom.xml
```
Expected: `BUILD SUCCESS`

**Step 2: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add JLine3 REPL loop with tab completion scaffold"
```

---

### Task 9: PromptRt top-level Clikt command

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/PromptRt.kt`

This is the new binary entry point. Subcommands (`chat`, `batch`, `models`, `config`) are stubs for now — filled in later phases.

```kotlin
package tri.ai.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import tri.ai.cli.config.ConfigLoader
import tri.ai.cli.repl.PromptRtRepl
import java.io.File

class PromptRt : CliktCommand(name = "promptrt", invokeWithoutSubcommand = true) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = PromptRt()
            .subcommands(PromptRtChatOnce(), PromptRtBatch(), PromptRtModels(), PromptRtConfig())
            .main(args)
    }

    private val configFile by option("--config", "-c", help = "Config file path (default ~/.promptrt/config.yaml)")
        .file(mustExist = false)
        .default(File(System.getProperty("user.home"), ".promptrt/config.yaml"))

    private val mode by option("--mode", "-m", help = "Launch in a specific mode")

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        val config = ConfigLoader.load(configFile)
        val effectiveConfig = if (mode != null) config.copy(defaultMode = mode!!) else config
        PromptRtRepl(effectiveConfig).start()
    }
}

/** Single-turn non-interactive chat. */
class PromptRtChatOnce : CliktCommand(name = "chat") {
    override fun run() = echo("[stub] single-turn chat not yet implemented")
}

/** Non-interactive batch runner. */
class PromptRtBatch : CliktCommand(name = "batch") {
    override fun run() = echo("[stub] batch not yet implemented")
}

/** List available models. */
class PromptRtModels : CliktCommand(name = "models") {
    override fun run() = echo("[stub] models not yet implemented")
}

/** Show resolved config. */
class PromptRtConfig : CliktCommand(name = "config") {
    override fun run() = echo("[stub] config display not yet implemented")
}
```

**Step 1: Compile**

```bash
mvn -B compile -f promptrt/promptrt-cli/pom.xml
```
Expected: `BUILD SUCCESS`

**Step 2: Manual smoke test** — run the REPL stub and verify JLine3 is working:

```bash
mvn -B package -f promptrt/promptrt-cli/pom.xml -DskipTests
java -cp promptrt/promptrt-cli/target/promptrt-cli-*-jar-with-dependencies.jar tri.ai.cli.PromptRt
```
Expected: REPL prompt appears (`>`), tab completes `/mode`, `/quit` exits cleanly.

**Step 3: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add PromptRt entry point with REPL launch and subcommand stubs"
```

---

## Phase 5b: Terminal Styling

### Task 9b: Input highlighter and output polish

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/ReplHighlighter.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`

No unit test — visual verification only.

**Step 1: Create ReplHighlighter**

JLine3's `Highlighter` interface colorizes the input line as the user types. Slash commands get cyan, arguments get white, unknown commands get red.

```kotlin
package tri.ai.cli.repl

import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

private val KNOWN_COMMANDS = setOf(
    "/mode", "/model", "/provider", "/memory", "/rag", "/tools",
    "/stream", "/json", "/temp", "/topp", "/seed", "/system",
    "/batch", "/status", "/reset", "/help", "/quit"
)

class ReplHighlighter : Highlighter {
    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        val builder = AttributedStringBuilder()
        if (!buffer.startsWith("/")) {
            // Plain chat input — white
            builder.append(buffer, AttributedStyle.DEFAULT)
            return builder.toAttributedString()
        }
        val parts = buffer.split("\\s+".toRegex(), limit = 2)
        val cmd = parts[0].lowercase()
        val cmdStyle = if (cmd in KNOWN_COMMANDS)
            AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)
        else
            AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
        builder.append(parts[0], cmdStyle)
        if (parts.size > 1) {
            builder.append(" ", AttributedStyle.DEFAULT)
            builder.append(parts[1], AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
        }
        return builder.toAttributedString()
    }

    override fun setErrorPattern(errorPattern: java.util.regex.Pattern?) {}
    override fun setErrorIndex(errorIndex: Int) {}
}
```

**Step 2: Wire highlighter into PromptRtRepl**

In `PromptRtRepl.start()`, add `.highlighter(ReplHighlighter())` to the `LineReaderBuilder` chain:

```kotlin
val reader = LineReaderBuilder.builder()
    .terminal(terminal)
    .completer(ReplCompleter(config))
    .highlighter(ReplHighlighter())   // ← add this line
    .history(history)
    ...
    .build()
```

**Step 3: Polish `/status` output with colored labels**

In `PromptRtRepl.printStatus()`, replace the plain string block with colored label/value pairs. Add a helper:

```kotlin
private fun statusLine(label: String, value: String) =
    println("${ANSI_CYAN}  %-12s${ANSI_RESET} $value".format(label))
```

Replace `printStatus()` body:
```kotlin
private fun printStatus() {
    println("${ANSI_CYAN}─── session status ───────────────────${ANSI_RESET}")
    statusLine("mode:",     state.activeMode.name)
    statusLine("model:",    state.effectiveModel)
    statusLine("memory:",   state.memoryEnabled.toString())
    statusLine("rag:",      if (state.ragEnabled) "on${if (state.ragPath != null) " (${state.ragPath})" else ""}" else "off")
    statusLine("tools:",    state.toolsEnabled.toString())
    statusLine("stream:",   state.streamEnabled.toString())
    statusLine("json:",     state.jsonMode.toString())
    statusLine("temp:",     state.temperature.toString())
    statusLine("top-p:",    state.topP?.toString() ?: "default")
    statusLine("seed:",     state.seed?.toString() ?: "none")
    statusLine("system:",   state.systemPrompt ?: "none")
    statusLine("history:",  "${state.history.size} messages")
    println("${ANSI_CYAN}──────────────────────────────────────${ANSI_RESET}")
}
```

**Step 4: Rebuild and visually verify**

```bash
mvn -B package -f promptrt/promptrt-cli/pom.xml -DskipTests
java -cp promptrt/promptrt-cli/target/promptrt-cli-*-jar-with-dependencies.jar tri.ai.cli.PromptRt
```

Check:
- Type `/mode` — command turns cyan as you type
- Type `/frobnicate` — turns red (unknown)
- Type `/mode rag` — `/mode` cyan, `rag` white
- `/status` — bordered block with cyan labels
- Regular chat input stays white

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: add input syntax highlighting and styled /status output"
```

---

## Phase 6: Plain Chat (P0 — first working end-to-end)

### Task 10: Wire plain chat into REPL dispatch

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`

Replace the `ReplCommand.Chat` stub with real model calls. At this point memory/RAG/tools are off — just bare chat with rolling history.

**Step 1: Replace chat stub in `dispatch()`**

Replace:
```kotlin
is ReplCommand.Chat    -> printInfo("[chat stub] ${cmd.text}")
```

With a call to a new `handleChat()` method:
```kotlin
is ReplCommand.Chat    -> handleChat(cmd.text)
```

**Step 2: Add handleChat() to PromptRtRepl**

```kotlin
private fun handleChat(userInput: String) {
    val model = try {
        AiModelProvider.chatModels().first { it.modelId == state.effectiveModel }
    } catch (e: NoSuchElementException) {
        printError("Model '${state.effectiveModel}' not found. Available: ${
            AiModelProvider.chatModels().map { it.modelId }.joinToString()
        }")
        return
    }

    val messages = buildList {
        if (state.systemPrompt != null)
            add(TextChatMessage(MChatRole.System, state.systemPrompt!!))
        addAll(state.history)
        add(TextChatMessage(MChatRole.User, userInput))
    }

    state.history.add(TextChatMessage(MChatRole.User, userInput))

    val response = runBlocking {
        try {
            model.chat(messages)
        } catch (e: Exception) {
            printError("Model error: ${e.message}")
            return@runBlocking null
        }
    } ?: return

    val message = (response.firstValue as AiOutput.ChatMessage).message
    // Print model tag in dim color, then response in light blue
    printInfo("[${state.effectiveModel}]")
    printResponse(message.content ?: "")
    state.history.add(TextChatMessage(MChatRole.Assistant, message.content ?: ""))

    // Trim history to mode's maxContext (default 20)
    while (state.history.size > 20) state.history.removeAt(0)
}
```

Add required imports: `tri.ai.core.*`, `tri.ai.prompt.trace.AiOutput`, `kotlinx.coroutines.runBlocking`.

**Step 3: Also wire /mode, /model, and sampling commands in dispatch()**

Replace `else -> printInfo("[stub] $cmd")` with real handlers:

```kotlin
is ReplCommand.Mode    -> {
    val preset = config.resolveMode(cmd.name)
    if (preset.name == "plain" && cmd.name !in config.allModeNames) {
        printError("Unknown mode '${cmd.name}'. Available: ${config.allModeNames.joinToString()}")
    } else {
        state.switchMode(preset)
        printInfo("mode: ${state.activeMode.name}  model: ${state.effectiveModel}")
    }
}
is ReplCommand.Model   -> { state.applyModelOverride(cmd.id); printInfo("model: ${cmd.id}") }
is ReplCommand.Provider -> printInfo("[stub] /provider not yet wired")
is ReplCommand.Temp    -> { state.temperature = cmd.value; printInfo("temperature: ${cmd.value}") }
is ReplCommand.TopP    -> { state.topP = cmd.value; printInfo("top-p: ${cmd.value}") }
is ReplCommand.Seed    -> { state.seed = cmd.value; printInfo("seed: ${cmd.value}") }
is ReplCommand.SystemPrompt -> { state.systemPrompt = cmd.text; printInfo("system prompt updated") }
is ReplCommand.Stream  -> { state.streamEnabled = cmd.on; printInfo("stream: ${cmd.on}") }
is ReplCommand.JsonMode -> { state.jsonMode = cmd.on; printInfo("json: ${cmd.on}") }
is ReplCommand.Memory  -> printInfo("[stub] /memory not yet wired")
is ReplCommand.Rag     -> printInfo("[stub] /rag not yet wired")
is ReplCommand.Tools   -> printInfo("[stub] /tools not yet wired")
is ReplCommand.Batch   -> printInfo("[stub] /batch not yet wired")
```

**Step 4: Build and manual test**

```bash
mvn -B package -f promptrt/promptrt-cli/pom.xml -DskipTests
java -cp promptrt/promptrt-cli/target/promptrt-cli-*-jar-with-dependencies.jar tri.ai.cli.PromptRt
```

Test: type a message, get a response, `/model gpt-4o`, send another message, `/status` shows updated model, `/mode rag` shows mode switch and clears history.

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: wire plain chat, /mode, /model, and sampling controls into REPL"
```

---

## Phase 6b: Provider Debugging

### Task 10b: Diagnose and fix provider/model availability

**No code changes until root cause is identified.** This is a diagnostic task — follow the steps in order and stop when you find the issue.

**Context:** `AiModelProvider` uses a service-loader plugin pattern. Providers (OpenAI, Gemini, Anthropic) register via `META-INF/services`. If a provider loads but finds no API key, it may register zero models. If it fails to load entirely, it won't appear in `orderedPlugins` at all.

**Step 1: Run `/providers` and `/models` in the REPL**

```bash
java -cp promptrt/promptrt-cli/target/promptrt-cli-*-jar-with-dependencies.jar tri.ai.cli.PromptRt
> /providers
> /models
```

Note exactly what's shown. Expected (working): at least one provider, several chat models. If empty or fewer than expected, proceed.

**Step 2: Check API key environment variables**

The providers look for:
- `OPENAI_API_KEY` or a local `apikey.txt` file
- `ANTHROPIC_API_KEY` or `apikey-anthropic.txt`
- `GEMINI_API_KEY` or `apikey-gemini.txt`

```bash
echo $OPENAI_API_KEY
ls *.txt 2>/dev/null
```

**Step 3: Check service-loader registration in the fat jar**

```bash
jar tf promptrt/promptrt-cli/target/promptrt-cli-*-jar-with-dependencies.jar \
  | grep "META-INF/services"
```

Expected: entries for `tri.ai.core.AiModelProvider` or similar. If missing, the provider JARs aren't being included in the assembly correctly.

**Step 4: Check which provider JARs are on the classpath**

```bash
jar tf promptrt/promptrt-cli/target/promptrt-cli-*-jar-with-dependencies.jar \
  | grep -i "openai\|anthropic\|gemini" | head -20
```

**Step 5: Add startup diagnostic logging to PromptRtRepl**

If steps 1-4 don't reveal the issue, add temporary debug output at the start of `start()` in `PromptRtRepl.kt`:

```kotlin
if (verbose) {  // add --verbose flag to PromptRt if needed
    printInfo("Providers: ${AiModelProvider.orderedPlugins.map { it.javaClass.simpleName }}")
    printInfo("Chat models: ${AiModelProvider.chatModels().size}")
}
```

**Step 6: Fix the root cause**

Based on findings, the fix will be one of:
- Set missing environment variable / API key file
- Fix `pom.xml` to include missing provider dependency
- Fix service-loader file if missing from assembly
- Handle provider init exception that's being swallowed silently

Document what the actual problem was and the fix in a commit message that will help future debugging.

**Commit (after fix):**
```bash
git add <changed files>
git commit -m "fix: <specific description of what was broken and how it was fixed>"
```

---

## Phase 7: Memory Toggle (P1)

### Task 11: Wire /memory into REPL

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/SessionState.kt`

**Step 1: Add memory fields to SessionState**

Add to `SessionState`:
```kotlin
var botMemory: BotMemory? = null

fun getOrCreateMemory(persona: BotPersona = HelperPersona("Assistant")): BotMemory {
    if (botMemory == null) {
        val chatModel = AiModelProvider.chatModels().first { it.modelId == effectiveModel }
        val embeddingModel = AiModelProvider.embeddingModels().first()
        botMemory = BotMemory(persona, chatModel, embeddingModel)
        botMemory!!.initMemory()
    }
    return botMemory!!
}
```

**Step 2: Replace /memory stub in dispatch()**

```kotlin
is ReplCommand.Memory -> {
    state.memoryEnabled = cmd.on
    if (cmd.on) state.getOrCreateMemory()
    printInfo("memory: ${cmd.on}")
}
```

**Step 3: Update handleChat() to use memory when enabled**

Add memory path before the `runBlocking` model call:

```kotlin
if (state.memoryEnabled) {
    handleMemoryChat(userInput)
    return
}
```

Add `handleMemoryChat()`:

```kotlin
private fun handleMemoryChat(userInput: String) {
    val memory = state.getOrCreateMemory()
    val response = runBlocking {
        try {
            val userItem = MemoryItem(MChatRole.User, userInput)
            memory.addChat(userItem)
            val contextHistory = memory.buildContextualConversationHistory(userItem)
                .map { it.toChatMessage() }
            val model = AiModelProvider.chatModels().first { it.modelId == state.effectiveModel }
            val persona = memory.persona
            val sysMsg = listOf(TextChatMessage(MChatRole.System, persona.getSystemMessage()))
            val result = model.chat(sysMsg + contextHistory).firstValue
            val msg = (result as AiOutput.ChatMessage).message
            memory.addChat(MemoryItem(msg))
            memory.saveMemory(interimSave = true)
            msg
        } catch (e: Exception) {
            printError("Memory chat error: ${e.message}")
            null
        }
    } ?: return
    printResponse(response.content ?: "")
}
```

**Step 4: Manual test**

Launch REPL, type `/memory on`, have a short conversation, `/quit`, relaunch, `/memory on` — prior conversation context should be retrieved.

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: wire /memory toggle into REPL using BotMemory"
```

---

## Phase 8: RAG Toggle (P1)

### Task 12: Wire /rag into REPL

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/SessionState.kt`

**Step 1: Add RAG driver field to SessionState**

```kotlin
var ragDriver: LocalDocumentQaDriver? = null

fun getOrCreateRagDriver(): LocalDocumentQaDriver? {
    val path = ragPath ?: return null
    if (ragDriver == null) {
        val root = java.io.File(path).let { if (it.isAbsolute) it else it.absoluteFile }
        ragDriver = createQaDriver(DocumentQaConfig(
            root = root.toPath(),
            folder = "",
            chatModel = effectiveModel,
            embeddingModel = AiModelProvider.embeddingModels().firstOrNull()?.modelId,
            temp = temperature,
            maxTokens = 2000,
            templateId = null
        ))
    }
    return ragDriver
}
```

**Step 2: Replace /rag stub in dispatch()**

```kotlin
is ReplCommand.Rag -> {
    state.ragEnabled = cmd.on
    if (cmd.on && cmd.path != null) state.ragPath = cmd.path
    if (!cmd.on) { state.ragDriver?.close(); state.ragDriver = null }
    printInfo("rag: ${cmd.on}${if (state.ragPath != null) " (${state.ragPath})" else ""}")
}
```

**Step 3: Add RAG path in handleChat()**

```kotlin
if (state.ragEnabled) {
    handleRagChat(userInput)
    return
}
```

Add `handleRagChat()`:

```kotlin
private fun handleRagChat(userInput: String) {
    val driver = try {
        state.getOrCreateRagDriver()
    } catch (e: Exception) {
        printError("RAG init failed: ${e.message}")
        return
    }
    if (driver == null) {
        printError("RAG enabled but no path set — use /rag <path> to set a document folder")
        return
    }
    val response = runBlocking {
        try { driver.answerQuestion(userInput) }
        catch (e: Exception) { printError("RAG error: ${e.message}"); null }
    } ?: return
    printResponse(response.finalResult.toString())
}
```

**Step 4: Manual test**

```
promptrt
> /rag ~/docs/some-folder
> What does this document say about X?
```

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: wire /rag toggle into REPL using LocalDocumentQaDriver"
```

---

## Phase 9: Tool Use Toggle (P1)

### Task 13: Wire /tools into REPL via AgentChat

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/SessionState.kt`

**Step 1: Add agent session to SessionState**

```kotlin
var agentSession: AgentChatSession? = null

fun getOrCreateAgentSession(): AgentChatSession {
    if (agentSession == null) {
        val config = AgentChatConfig(
            modelId = effectiveModel,
            systemMessage = systemPrompt,
            temperature = temperature,
            enableReasoningMode = false,
            enableTools = true
        )
        agentSession = DefaultAgentChatAPI().createSession(config)
    }
    return agentSession!!
}
```

**Step 2: Replace /tools stub in dispatch()**

```kotlin
is ReplCommand.Tools -> {
    state.toolsEnabled = cmd.on
    if (!cmd.on) state.agentSession = null
    printInfo("tools: ${cmd.on}")
}
```

**Step 3: Add agent path in handleChat()**

```kotlin
if (state.toolsEnabled) {
    handleAgentChat(userInput)
    return
}
```

Add `handleAgentChat()`. Tool calls should be visually subordinate to the final response — print them in gray with a `⚙` prefix, tool results indented under them:

```kotlin
private fun handleAgentChat(userInput: String) {
    val session = state.getOrCreateAgentSession()
    val api = DefaultAgentChatAPI()
    runBlocking {
        try {
            val message = MultimodalChatMessage.text(MChatRole.User, userInput)
            val op = api.sendMessage(session, message)
            op.events.collect { event ->
                when (event) {
                    is ExecEvent.UsingTool  -> printInfo("${ANSI_GRAY}⚙ ${event.toolName}(${event.input})${ANSI_RESET}")
                    is ExecEvent.ToolResult -> printInfo("${ANSI_GRAY}  → ${event.result}${ANSI_RESET}")
                    is ExecEvent.Reasoning  -> printInfo("${ANSI_GRAY}  ∴ ${event.text}${ANSI_RESET}")
                    else -> AgentEventPrinter(verbose = false).emit(event)
                }
            }
        } catch (e: Exception) {
            printError("Agent error: ${e.message}")
        }
    }
}
```

**Step 4: Manual test** — `/tools on`, ask a question that exercises a tool.

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: wire /tools toggle into REPL via AgentChatSession"
```

---

## Phase 10: /batch in REPL (P1)

### Task 14: Extract batch logic and wire /batch

**Files:**
- Create: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/batch/BatchRunner.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/PromptBatchRunner.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`

**Step 1: Extract core batch logic into a standalone function**

Create `BatchRunner.kt`:

```kotlin
package tri.ai.cli.batch

import kotlinx.coroutines.runBlocking
import tri.ai.core.AiModelProvider
import tri.ai.pips.AiWorkflowExecutor
import tri.ai.prompt.trace.AiTaskTraceDatabase
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.ai.util.writeTrace
import tri.ai.util.writeTraceDatabase
import java.io.File

object BatchRunner {
    fun execute(inputFile: File, outputFile: File, database: Boolean = false): String {
        val batch = when (inputFile.extension) {
            "json" -> AiPromptBatchCyclic.fromJson(inputFile.readText())
            else   -> AiPromptBatchCyclic.fromYaml(inputFile.readText())
        }
        val result = runBlocking {
            val tasks = batch.plan { AiModelProvider.chatModel(it) }
            AiWorkflowExecutor.execute(tasks.plan)
        }
        if (database)
            writeTraceDatabase(AiTaskTraceDatabase(result.interimResults.values), outputFile)
        else
            writeTrace(result.finalResult, outputFile)
        return outputFile.absolutePath
    }
}
```

**Step 2: Update PromptBatchRunner to delegate to BatchRunner**

In `PromptBatchRunner.run()`, replace the inline logic with:
```kotlin
val path = BatchRunner.execute(inputFile, outputFile, database)
println("${ANSI_CYAN}Output written to $path.$ANSI_RESET")
```

**Step 3: Wire /batch stub in dispatch()**

```kotlin
is ReplCommand.Batch -> {
    val file = java.io.File(cmd.path)
    if (!file.exists()) {
        printError("File not found: ${cmd.path}")
        return
    }
    val outFile = File(file.parent, file.nameWithoutExtension + "-output.json")
    printInfo("Running batch: ${file.name}")
    try {
        val path = BatchRunner.execute(file, outFile)
        printInfo("Done. Output: $path")
    } catch (e: Exception) {
        printError("Batch failed: ${e.message}")
    }
}
```

**Step 4: Compile and verify PromptBatchRunner still works**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml -Dtest=PromptBatchRunnerTest
```

**Step 5: Commit**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: extract BatchRunner, wire /batch into REPL"
```

---

## Phase 11: P2 Features

### Task 15: /models and /providers commands

Exposes provider and model discovery both as in-session REPL commands and as top-level CLI subcommands.

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/ReplCommand.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/PromptRtRepl.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/repl/ReplCompleter.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/PromptRt.kt`

**Step 1: Add ReplCommand entries**

In `ReplCommand.kt`, add to the sealed class:
```kotlin
object Models : ReplCommand()
object Providers : ReplCommand()
```

**Step 2: Add `/models` and `/providers` to CommandParser**

In `CommandParser.kt`, add inside the `when (cmd)` block:
```kotlin
"/models"    -> ReplCommand.Models
"/providers" -> ReplCommand.Providers
```

**Step 3: Add to ReplCompleter SLASH_COMMANDS list**

Add `"/models"` and `"/providers"` to the `SLASH_COMMANDS` list in `ReplCompleter.kt`.

**Step 4: Wire dispatch in PromptRtRepl**

Add to `dispatch()`:
```kotlin
is ReplCommand.Models    -> printModels()
is ReplCommand.Providers -> printProviders()
```

Add methods:
```kotlin
private fun printModels() {
    val chat = AiModelProvider.chatModels()
    val embed = AiModelProvider.embeddingModels()
    println("${ANSI_CYAN}─── chat models (${chat.size}) ───────────────────${ANSI_RESET}")
    chat.forEach { m ->
        val caps = m.info?.capabilities
        val tags = buildList {
            if (caps?.inputs?.contains(tri.ai.core.ModelInput.IMAGE) == true) add("vision")
            if (caps?.inputs?.contains(tri.ai.core.ModelInput.AUDIO) == true) add("audio")
            if (caps?.outputs?.contains(tri.ai.core.ModelOutput.EMBEDDING) == true) add("embed")
        }.joinToString(", ")
        val tagStr = if (tags.isNotEmpty()) "  $ANSI_GRAY[$tags]$ANSI_RESET" else ""
        println("  ${m.modelId}$tagStr")
    }
    if (embed.isNotEmpty()) {
        println("${ANSI_CYAN}─── embedding models (${embed.size}) ──────────────${ANSI_RESET}")
        embed.forEach { println("  ${it.modelId}") }
    }
    println("${ANSI_CYAN}──────────────────────────────────────${ANSI_RESET}")
}

private fun printProviders() {
    val plugins = AiModelProvider.orderedPlugins
    println("${ANSI_CYAN}─── providers (${plugins.size}) ──────────────────────${ANSI_RESET}")
    plugins.forEach { p ->
        val modelCount = AiModelProvider.chatModels()
            .count { it.toString().contains(p.javaClass.simpleName, ignoreCase = true) }
        println("  ${p.javaClass.simpleName}  $ANSI_GRAY(${AiModelProvider.chatModels().size} total models visible)$ANSI_RESET")
    }
    println("${ANSI_CYAN}──────────────────────────────────────${ANSI_RESET}")
}
```

**Step 5: Wire `promptrt models` and `promptrt providers` subcommands**

In `PromptRt.kt`, add `PromptRtProviders()` to the subcommands list and replace the `PromptRtModels` stub:

```kotlin
class PromptRtModels : CliktCommand(name = "models") {
    override fun run() {
        val chat = AiModelProvider.chatModels()
        println("Chat models (${chat.size}):")
        chat.forEach { m ->
            val caps = m.info?.capabilities
            val tags = buildList {
                if (caps?.inputs?.contains(tri.ai.core.ModelInput.IMAGE) == true) add("vision")
                if (caps?.inputs?.contains(tri.ai.core.ModelInput.AUDIO) == true) add("audio")
            }.joinToString(", ").let { if (it.isNotEmpty()) "  [$it]" else "" }
            println("  ${m.modelId}$tags")
        }
        val embed = AiModelProvider.embeddingModels()
        if (embed.isNotEmpty()) {
            println("\nEmbedding models (${embed.size}):")
            embed.forEach { println("  ${it.modelId}") }
        }
    }
}

class PromptRtProviders : CliktCommand(name = "providers") {
    override fun run() {
        val plugins = AiModelProvider.orderedPlugins
        if (plugins.isEmpty()) {
            println("No providers loaded. Check API key environment variables.")
            return
        }
        println("Loaded providers (${plugins.size}):")
        plugins.forEach { p -> println("  ${p.javaClass.simpleName}") }
        println("\nTotal chat models: ${AiModelProvider.chatModels().size}")
        println("Total embedding models: ${AiModelProvider.embeddingModels().size}")
    }
}
```

**Step 6: Run tests and commit**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml
git add promptrt/promptrt-cli/src/
git commit -m "feat: add /models and /providers commands"
```

---

### Task 16: promptrt config and promptrt chat --once subcommands

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/PromptRt.kt`

Replace `PromptRtConfig` stub:

```kotlin
class PromptRtConfig : CliktCommand(name = "config") {
    override fun run() {
        val config = ConfigLoader.loadDefault()
        println("Config file: ${File(System.getProperty("user.home"), ".promptrt/config.yaml").absolutePath}")
        println("Default mode: ${config.defaultMode}")
        println("Modes: ${config.allModeNames.joinToString()}")
    }
}
```

Replace `PromptRtChatOnce` stub:

```kotlin
class PromptRtChatOnce : CliktCommand(name = "chat") {
    private val message by argument(help = "Message to send")
    private val mode by option("--mode").default("plain")
    private val json by option("--json").flag()

    override fun run() {
        val config = ConfigLoader.loadDefault()
        val state = SessionState.fromConfig(config.copy(defaultMode = mode))
        val model = AiModelProvider.chatModels().first { it.modelId == state.effectiveModel }
        val response = runBlocking {
            model.chat(listOf(TextChatMessage(MChatRole.User, message)))
        }
        val text = (response.firstValue as AiOutput.ChatMessage).message.content ?: ""
        if (json) println("""{"response":${com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text)}}""")
        else println(text)
    }
}
```

**Commit:**
```bash
git add promptrt/promptrt-cli/src/
git commit -m "feat: implement config and chat --once subcommands"
```

---

## Phase 12: Deprecate Old CLIs

### Task 17: Mark old CLIs deprecated

**Files:**
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/SimpleChatCli.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/MemoryChatCli.kt`
- Modify: `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/AgentChatCli.kt`

Add to top of each class body:

```kotlin
// DEPRECATED: use `promptrt` instead. Scheduled for deletion after P1 soak period.
```

No functional change. This marks the intent clearly in the codebase.

**Commit:**

```bash
git add promptrt/promptrt-cli/src/
git commit -m "deprecate: mark old chat CLIs for deletion after soak period"
```

---

## Phase 13: Soak Testing and Deletion (Pending User Approval)

### Task 18: Soak testing checklist

**No code changes.** This phase is manual validation. Run through the following scenarios with the live binary and confirm each works:

**P0 checklist:**
- [ ] `promptrt` launches with no flags, prompt appears
- [ ] `promptrt --mode rag` launches in RAG mode
- [ ] `/status` reflects live state at all times
- [ ] `/model gpt-4o` swaps model, history preserved
- [ ] `/mode memory` switches mode, history cleared, notified
- [ ] `/reset` restores plain defaults
- [ ] Tab completion: `/mode <tab>` shows mode names
- [ ] Tab completion: `/model <tab>` shows model IDs
- [ ] `/quit` exits cleanly; Ctrl+C does not crash
- [ ] `~/.promptrt/config.yaml` with custom mode is loaded correctly
- [ ] `--config <path>` override works

**P1 checklist:**
- [ ] `/memory on` → conversation context persists across sessions
- [ ] `/rag ~/docs` → answers grounded in documents
- [ ] `/tools on` → tool calls visible in output
- [ ] `/batch jobs/run.yaml` → runs and writes output file
- [ ] Feature combinations: `/memory on` + `/rag ~/docs` together

**P2 checklist:**
- [ ] `/temp 0.2` → noticeably less varied output
- [ ] `/seed 42` → reproducible output on same prompt
- [ ] `/json on` → model returns JSON
- [ ] `promptrt models` → lists models with tags
- [ ] `promptrt chat "hello"` → single-turn, exits cleanly

---

### Task 19: Delete old CLIs (requires explicit user approval)

**Do not execute this task without confirmation.**

When the soak checklist above is complete and the developer approves deletion, run:

**Files to delete:**
- `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/SimpleChatCli.kt`
- `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/MemoryChatCli.kt`
- `promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/AgentChatCli.kt`
- `promptrt/promptrt-cli/src/test/kotlin/tri/ai/cli/DocumentCliTest.kt` (hardcoded paths, all disabled — replace with real tests if desired)

**Step 1: Verify nothing imports the deleted classes**

```bash
grep -r "SimpleChatCli\|MemoryChatCli\|AgentChatCli" promptrt/promptrt-cli/src/
```
Expected: no results (other than the files themselves).

**Step 2: Delete files**

```bash
rm promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/SimpleChatCli.kt
rm promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/MemoryChatCli.kt
rm promptrt/promptrt-cli/src/main/kotlin/tri/ai/cli/AgentChatCli.kt
```

**Step 3: Build and test**

```bash
mvn -B test -f promptrt/promptrt-cli/pom.xml
```
Expected: `BUILD SUCCESS`

**Step 4: Commit**

```bash
git add -u promptrt/promptrt-cli/src/
git commit -m "remove: delete deprecated chat CLIs replaced by promptrt REPL"
```
