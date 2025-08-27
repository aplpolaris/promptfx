# Research View Implementation

## Overview

The Research View provides a guided process for research and report writing, implementing the multi-agent workflow requested in issue #457.

## Features Implemented

### Core Architecture
- **ResearchView.kt**: Main view class extending AiPlanTaskView
- **ResearchPlannerFx.kt**: Orchestrates the multi-agent workflow
- **ResearchModels.kt**: Data classes for intermediate products
- **ResearchViewPlugin.kt**: Plugin registration for UI integration

### Workflow Phases
1. **Planning Phase**: InfoPlannerAgent analyzes request and creates research plan
2. **Research Phase**: ResearchAgent gathers findings (currently mock implementation)
3. **Writing Phase**: WritingAgent creates outline and drafts report
4. **Review Phase**: ReviewAgent finalizes the report

### User Interface Elements
- Input area for information requests
- Progress tree showing workflow status with visual indicators (✓, ⏳)
- Status display showing current phase and progress
- Export functionality for saving final reports
- Integration with existing PromptFx UI framework

### Data Models
- `InfoRequest`: User's research request with timestamp
- `ResearchProjectPlan`: Structured plan with objectives, questions, methodology, tasks
- `ResearchPack`: Compiled research findings from multiple sources
- `WrittenReport`: Final report with sections, outline, and full text
- `ResearchWorkflowState`: Observable state management for UI updates

### Integration with Existing Systems
- Reuses research prompts from issue #455:
  - `research-report/planner@1.0.0`
  - `research-report/questions@1.0.0`
  - `research-report/outline@1.0.0`
  - `research-report/draft@1.0.0`
  - `research-report/review-edit@1.0.0`
- Follows established patterns from DocumentQaView
- Integrates with TornadoFX UI framework
- Registered as NavigableWorkspaceView plugin

## Testing

### Unit Tests (ResearchViewTest.kt)
- Data model validation
- Word counting functionality
- State management
- Plugin configuration

### Integration Tests (ResearchPluginIntegrationTest.kt)
- Service registration verification
- Plugin loading validation
- Confirms "Research View" appears in UI plugin list

## Current Status

✅ **COMPLETED**:
- Core data models and workflow state management
- Basic UI structure with progress visualization
- Plugin registration and service loading
- Mock workflow implementation
- Unit and integration tests
- Export functionality

🔄 **SIMPLIFIED FOR MINIMAL IMPLEMENTATION**:
- Currently uses mock research findings instead of actual research tools
- Simplified agent delegation (ready for future enhancement)
- Basic progress tracking (can be expanded for step-by-step user interaction)

🚀 **READY FOR ENHANCEMENT**:
- Integration with DocumentQA for actual research
- Web search integration
- More sophisticated report templates
- Interactive agent delegation
- Enhanced user confirmation workflows

## Usage

1. The Research View appears in the PromptFx application under the "Research" category
2. User enters an information request in the input area
3. Clicking "Start Research" triggers the multi-agent workflow
4. Progress is shown in the tree view and status area
5. Final report can be exported as text or markdown

## Technical Implementation Notes

- Extends `AiPlanTaskView` for consistency with existing views
- Uses `AiTaskList` for workflow orchestration
- Leverages existing prompt library and execution framework
- Maintains observable state for real-time UI updates
- Follows established error handling and trace management patterns

## Future Enhancements

The current implementation provides a solid foundation that can be extended with:
- Real research agents that query DocumentQA, web APIs, etc.
- Enhanced report templates and formatting options
- Interactive step-by-step workflow with user confirmation
- Integration with external data sources
- Collaborative features for team research projects