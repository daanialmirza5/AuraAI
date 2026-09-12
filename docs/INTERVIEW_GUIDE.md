# AuraAI — Interview Guide & Technical Defense

## 1. Pitches
- **30-Second Pitch**: "AuraAI is an on-device multi-agent assistant platform for Android built across 18 modular Gradle subprojects. It combines screen perception, accessibility automation, and an 8-question constraint reasoning engine to automate mobile tasks securely."
- **2-Minute Pitch**: "Mobile AI assistants often compromise user privacy by streaming personal screen data to third-party cloud servers. AuraAI addresses this through a modular, on-device agent platform engineered in Kotlin. Structured across 18 Gradle modules, our runtime uses an Android Accessibility perception layer to parse the UI hierarchy into semantic nodes. When executing complex user goals, our orchestrator decomposes tasks into sequential steps, each verified against an on-device 8-point constraint safety engine. Critical actions like payments trigger confirmation gates, while routine interactions execute via non-invasive gestures with sub-100ms latency."

## 2. Key Technical Q&A
- **Q: Why divide an Android app into 18 Gradle modules?**
  - **A**: Multi-module architecture enforces strict separation of concerns, accelerates Gradle parallel build times, prevents circular dependencies between core orchestration and feature plugins, and allows dynamic feature delivery where plugins are downloaded only on demand.
- **Q: How do you prevent the agent from executing destructive actions?**
  - **A**: The `:core:constraint` engine acts as a deterministic barrier. Every action proposed by the reasoning layer is classified against risk vectors (financial impact, data destruction, privacy exposure). Any high-risk action halts execution and presents an explicit system-level confirmation dialog to the user.
