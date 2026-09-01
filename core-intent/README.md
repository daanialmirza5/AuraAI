# core-intent

Classifies a raw utterance into one of AURA's 11 intent types. Pure Kotlin/JVM; depends only on
core-ai.

## What lives here

- **`IntentType`** — `OpenApp`, `Reminder`, `Calendar`, `Research`, `Coding`, `Shopping`,
  `Navigation`, `Email`, `Automation`, `Conversation`, `Unknown`.
- **`RecognizedIntent`** — a type, a confidence, extracted `slots` (e.g. `appName` for
  `OpenApp`), and the raw text.
- **`IntentRecognizer`** — the interface. **`KeywordIntentRecognizer`** is the only
  implementation this phase ships: local, offline, zero-latency keyword/phrase matching.

## This is not a placeholder

`KeywordIntentRecognizer` isn't a stand-in that gets deleted once a provider is connected — it's
the permanent *fast path*. Production assistants triage with something cheap and local before
ever spending a model call on a request that's obviously "open Spotify." A future
`LlmIntentRecognizer`, backed by `AIProviderManager`, would sit behind the same `IntentRecognizer`
interface for the utterances the keyword pass is genuinely unsure about — not replace it outright.

## Status

Fully implemented and real. Confidence scores are a heuristic ("how many rules fired"), not a
calibrated probability — documented on `RecognizedIntent.confidence` itself.
