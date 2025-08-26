# PromptFx 0.12.0 Release Notes

## Major Release Highlights

PromptFx 0.12.0 represents a significant architectural overhaul with enhanced modularity, new agent-based AI capabilities, and a completely redesigned user interface. This release introduces a flexible JSON-configurable prompting pipeline system and extensive improvements to document management and text processing workflows.

---

## 🏗️ **Architecture & Build System**

### **New Modular Structure**
* **#406** Created meta project to build all promptkt/promptfx modules at once
* **#409** Separated pipelining code into new library (`promptkt-pips`)
* **#410** Separated document management into new module (`promptkt-docs`)

---

## 🤖 **Agent & Prompt System Enhancements**

### **New Agent Capabilities**
* **#460** Created an "AgentChat" API with contextual/reasoning chat capability
* **#467** Built a "pips core" executable API for universal tool interfaces

### **Advanced Prompting Features**
* **#451** JSON-configurable prompting pipeline with universal tool interface
* **#455** Research and report-writing prompts with question generation, outlines, and draft review
* **#429** Added MCP (Model Context Protocol) prompt interface and server to promptkt-cli
* **#462** Standard web search tool integration

### **Prompt Library Improvements**
* **#412** Refactored prompt library code for flexible prompt definition files
  - Multiple prompt file support
  - Enhanced metadata with MCP compatibility
  - Categories, tags, and versioning support
  - Grouping prompts by category within IDs
* **#414** Builder pattern for constructing and executing prompt tasks

---

## 🎨 **User Interface & Experience**

### **Major UI Restructuring**
* **#421** Updated default prompt views to reflect layered GAI (Generative AI) application perspective:
  - **API** - Low-level FM service APIs (OpenAI, Gemini, etc.)
  - **Prompts** - Basic prompt engineering activities
  - **Text/Fun** - Configured views for basic prompt engineering
  - **Documents** - RAG and document analysis with semantic search
  - **Tools** - Tool and MCP server registration
  - **Agents** - Agent registration and tool utilization
  - **Multimodal** - Audio/vision models, TTS, STT

### **New Views & Features**
* **#417** Created comprehensive PromptFx Settings view with navigable tree structure
* **#426** Added "About PromptFx" view
* **#449** Runtime-customized views organized by category in "Custom" tab
* **#444** Session configuration saves/restores last viewed tab

---

## 📄 **Document & Text Management**

### **Enhanced Text Library System**
* **#441** Text library creator wizard includes third step for library location
  - Folder chooser with smart defaults
  - File name validation with conflict detection
  - Metadata extraction options
  - Embedding generation controls
* **#440** Load document metadata from ".meta.json" files when loading text libraries
* **#249** Web scraping generates metadata files with document text

### **Improved Document Views**
* **#446** Streamlined "Images from Document" view with single-row scrolling
* **#321** Document Insights tab uses shared library toolbar and views
* **#320** Added image cache for handling large PDF images in TextManager view

---

## 🔧 **Model & API System**

### **Model Selection & Management**
* **#418** Changed main toolbar to feature chat model selection instead of completion model
* **#419** Default model for most views is now the chat model (completion model only for Completions API view)
* **#458** Fixed models view timeout issues that caused UI delays with unresponsive plugins
* **#368** Enabled filtering by input and output types in Models API view

### **Enhanced Configuration**
* **#420** Configuration-based views now use updated `PromptDef` configurations
* **#413** Updated Document QA pipeline to use TextChat instead of TextCompletion
* **#411** Separated EmbeddingService into chunking service and model service

---

## 🐛 **Bug Fixes & Stability**

* **#319** Fixed embeddings file corruption with special characters in file paths
  - Resolved multiple internal representations of same document
  - Fixed URL encoding/decoding inconsistencies
  - Improved chunk text extraction reliability

---

## 🎯 **Developer Experience**

### **Improved APIs & Tools**
* Enhanced JSON input/output format enforcement
* Universal tool interface for future-proof extensibility
* Lightweight validation with clear error reporting
* Support for JSON schema inputs and outputs (planned)

### **Command Line Tools**
* New MCP-compatible command-line interface
* Improved prompt execution with retry and delay support
* Enhanced batch processing capabilities

---

## 📊 **Technical Improvements**

* **Streaming & Performance**: Better handling of long-running operations
* **Memory Management**: Improved caching for large document sets
* **Error Handling**: Enhanced exception management and user feedback
* **Configuration**: Runtime YAML configurations for greater flexibility

---

## 🔮 **Future Compatibility**

This release establishes groundwork for:
- Full JSON schema validation
- Enhanced MCP compatibility
- Expanded agent workflows
- Additional multimodal capabilities

---

**Full Changelog**: [Compare promptfx-0.11.1.1...promptfx-0.12.0](https://github.com/aplpolaris/promptfx/compare/promptfx-0.11.1.1...promptfx-0.12.0)

**Total Issues Resolved**: 34 milestone items completed

---

*Note: This release includes breaking changes in the modular structure. Please review the migration guide for upgrading existing configurations.*