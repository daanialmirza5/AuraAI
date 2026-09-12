# AuraAI — Engineering Guide & Mastery Document

## 1. What Is AuraAI?
AuraAI is an **autonomous on-device multi-agent assistant platform for Android**. Built across **18 modular Gradle subprojects**, AuraAI executes complex device actions, screen perception, accessibility automation, and goal-oriented workflows while maintaining strict user privacy by executing constraint reasoning on-device.

## 2. Real-World Problem Solved
1. **Cloud Privacy Risks with Personal Device Automation**: Sending personal screen contents, SMS, and app states to cloud LLMs presents severe privacy risks.
2. **App Fragility in Mobile Automation**: UI layouts change across Android OEMs and OS versions, breaking static UI automations.
3. **Unbounded Agent Actions on Mobile**: Autonomous agents could accidentally trigger financial transfers or delete critical user data without confirmation gates.

## 3. High-Level Android Architecture (18 Modular Subprojects)
- **Architecture**: Clean Architecture + Multi-Module Gradle + Kotlin Coroutines & Flow.
- **Key Modules**:
  - `:core:agent`: Orchestrator coordinating task decomposition, memory, and tool execution.
  - `:core:constraint`: 8-question deterministic constraint engine validating safety, user confirmation gates, and device battery/network states.
  - `:core:perception`: Accessibility Node parser and on-device OCR pipeline analyzing screen UI hierarchies.
  - `:core:memory`: Hierarchical short-term session buffer and encrypted SQLite persistent episodic memory.
  - `:core:automation`: Android `AccessibilityService` executor simulating secure touch, gesture, and text entry actions.
  - `:feature:*`: Modularized feature plugins (Messaging, Navigation, Calendar, Settings).

## 4. Algorithmic Formulations & Constraint Safety Engine
- **Constraint Reasoning Engine**: Before any action $A$ is executed, it must pass an 8-point constraint evaluation vector:
  $$C(A) = \bigwedge_{i=1}^{8} c_i(A) \in \{\text{ALLOW}, \text{DENY}, \text{REQUIRE\_USER\_CONFIRMATION}\}$$
  Critical actions (e.g., payment, deletion, permissions) strictly trigger confirmation dialogs.
- **Screen Node Tree Traversal**: BFS indexing over `AccessibilityNodeInfo` with bounding-box spatial intersection for robust element targeting.

## 5. Security & Android Permissions
- Strictly operates within Android Accessibility and Device Admin permission boundaries.
- Biometric authentication gate for sensitive automated workflows.

## 6. Testing Strategy
- Unit test suite covering constraint decision tables, plan decomposition trees, and mock accessibility event streams.
