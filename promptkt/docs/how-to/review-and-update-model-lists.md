# Review and Update Provider Model Lists

This guide describes a repeatable process for reviewing provider model catalogs and updating local model-list YAML files.

## Scope

Primary model index files:

- `promptkt-provider-openai/src/main/resources/tri/ai/openai/resources/openai-models.yaml`
- `promptkt-provider-openai-sdk/src/main/resources/tri/ai/openaisdk/resources/openai-models.yaml`
- `promptkt-provider-gemini/src/main/resources/tri/ai/gemini/resources/gemini-models.yaml`
- `promptkt-provider-gemini-sdk/src/main/resources/tri/ai/geminisdk/resources/gemini-sdk-models.yaml`
- `promptkt-provider-anthropic-sdk/src/main/resources/tri/ai/anthropicsdk/resources/anthropic-sdk-models.yaml`

## Review Workflow

1. Pull current provider model docs and list APIs.
2. Compare provider models vs local YAML model IDs.
3. Classify each difference:
   - new model to add
   - renamed/alias model
   - deprecated/legacy/discontinued model
   - provider-only operational model to ignore in tests
4. Update top-level category lists (`chat`, `responses`, `tts`, etc.).
5. Update or add full `models:` entries.
6. Verify lifecycle and token limits from provider docs.
7. Sync mirrored provider files (for example OpenAI and OpenAI SDK copies).
8. Run model-index tests/smoke checks.

## What to Update in Each Model Entry

For each model card under `models:`:

- `id`
- `type`
- `source`
- `metadata.name`
- `metadata.description`
- `metadata.lifecycle`
- `metadata.modelInfoUrl` (when available)
- `capabilities.inputs`
- `capabilities.outputs`
- `params.totalTokenLimit`
- `params.outputTokenLimit`
- any provider-specific params (for example `outputDimension`)

## Scraping and Data Collection Tips

- Prefer official model pages over summary lists for token limits.
- Capture both "context window" and "max output tokens" for each model.
- Normalize values before editing YAML:
  - `1,050,000` -> `1050000`
  - `128,000` -> `128000`
- Save source URLs while working so values are auditable.
- Watch for dynamic docs pages; inspect rendered page content, not only static HTML fragments.
- Treat `-latest` and other rolling aliases carefully; they can change without notice.

## Diff and Validation Tips

- Keep edits minimal and scoped to changed IDs.
- Preserve YAML order and formatting to reduce noisy diffs.
- Verify no accidental category regressions (for example model removed from `responses`).
- Validate by running provider model index tests.

Example command (from `promptkt/`):

```powershell
mvn -pl promptkt-provider-openai -Dtest=OpenAiModelIndexTest#testModels test
```

## Common Pitfalls

- Updating one mirrored YAML but forgetting the sibling file.
- Copying token limits from older docs snapshots.
- Using broad ignore patterns in tests that hide real drift.
- Marking models `DEPRECATED` without confirming lifecycle label from provider docs.

## Suggested PR Checklist

- [ ] Model IDs added/removed match provider catalog.
- [ ] Token limits verified from model pages.
- [ ] Lifecycle values reviewed.
- [ ] Mirror files updated.
- [ ] Tests/smoke checks run.
- [ ] Notes included for any unresolved or ambiguous models.

