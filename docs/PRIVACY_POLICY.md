# AURA AI — Privacy Policy

**Draft — for review before publication.** This document describes what the app's code actually
does today, verified directly against the implementation (see [SECURITY.md](SECURITY.md) for the
full review it's built from), not aspirational or template language. It is not a substitute for
review by the app's publisher and, where required by the jurisdictions AURA is distributed in,
legal counsel — see §7. Wherever this document would need a real business name, contact address,
or jurisdiction that only the app's publisher can supply, that is called out explicitly rather than
invented.

Last reviewed against the codebase: 2026-07-29.

---

## 1. What AURA stores, and where

AURA is a local-first assistant. Conversation history, memory entries (facts, preferences, goals,
projects, and the other categories described in [KNOWLEDGE_GRAPH.md](KNOWLEDGE_GRAPH.md)),
workflows, and automations are stored **only on your device**, in AURA's own local database. None
of this is uploaded to any AURA-operated server, because AURA does not operate a server —
there is no backend this app talks to on its own behalf.

AI provider API keys you enter (for Claude, OpenAI, Gemini, or a self-hosted Ollama instance) are
stored using Android's `EncryptedSharedPreferences`, encrypted with a key held in your device's
Android Keystore. That encrypted file is excluded from Android's backup and device-transfer
mechanisms — see [SECURITY.md §3](SECURITY.md#3-backup-excluding-the-encrypted-credential-store).

## 2. What leaves your device, and to whom

**Only what you send to an AI provider you configure.** When a request needs a connected AI
provider (for example, generating a written response), your message and whatever memory/context
AURA includes with it are sent directly to that provider's own API — Anthropic (Claude), OpenAI,
Google (Gemini), or your own Ollama server — using the API key you provided. That is a direct
connection from your device to the provider you chose; AURA does not proxy, log, or retain a copy
of that traffic anywhere else. Each provider's own privacy policy and data-handling terms govern
what they do with a request once AURA sends it — AURA does not control that.

**Nothing else.** AURA has no analytics SDK, no crash-reporting SDK, and no advertising SDK
integrated — verified directly in [SECURITY.md §9](SECURITY.md#9-third-party-data-sharing-review).
No data is sold, shared, or used for advertising, because no such pipeline exists in the app at
all.

## 3. Permissions AURA requests, and why

Every permission below is used by real, specific functionality — none is requested speculatively
(verified in [SECURITY.md §1](SECURITY.md#1-declared-permissions-vs-actual-usage)):

| Permission | Why AURA asks for it |
|---|---|
| Internet / network state | To reach the AI provider you configure, and to know whether the device is online before attempting to. |
| Microphone (Record Audio) | Push-to-talk voice input — only active while you're actively speaking to AURA. |
| Notifications | To show reminders and automation results you've asked AURA to notify you about. |
| Calendar (read) | To create or reference calendar events when you ask AURA to (e.g. "schedule a meeting"). |
| Location (approximate/precise) | Only used when a request needs it — e.g. navigation or location-aware automation. |
| Contacts (read) | To address a message or reminder to a specific contact when you ask AURA to. |
| Camera | For the Vision Runtime's document/receipt/barcode scanning and photo capture, initiated only when you tap a camera action. |
| Storage (read, legacy Android only) | To search your Downloads folder by filename when you ask AURA to find a file — only requested pre-Android 10, where this is required. |
| Biometric | To let you optionally lock the app with your device's fingerprint/face unlock. |

You can deny or revoke any of these in Android's system Settings at any time; AURA reports, per
feature, when a permission it needs isn't granted rather than failing silently.

## 4. Your control over your data

- **Delete stored memory**: Profile → Memory Manager → "Clear all memory" permanently deletes
  every stored memory entry, after an explicit confirmation. This is real, verified deletion, not
  a placeholder — see [SECURITY.md §4](SECURITY.md#4-data-deletion-clear-all-memory-now-actually-clears-memory).
- **Uninstalling the app** removes its local database and encrypted credential store entirely,
  since nothing is stored outside the app's own private storage.
- **Export**: not yet available — a real gap, tracked openly in
  [ROADMAP.md](../ROADMAP.md) under "Conversation/memory export & backup" rather than silently
  omitted from this policy.

## 5. Security

API keys are encrypted at rest (Android Keystore-backed AES-256). Network requests to AI providers
use HTTPS; cleartext traffic is blocked globally except for a narrowly-scoped `localhost`/
`10.0.2.2` allowance needed only for a self-hosted Ollama instance on the same device or an
emulator's host machine. Full detail: [SECURITY.md](SECURITY.md).

## 6. Children's privacy

AURA is not directed at children and has no age-gating or children-specific handling implemented.
If the app's publisher intends to distribute it in a way that could reach children, that requires
its own dedicated review (e.g. COPPA, if distributed in the US) — out of scope for what this
document can assert on the codebase's behalf.

## 7. Before this is published

This document describes the app's actual current behavior, verified against its code. It does not
include: a real business name or legal entity, a contact address or support email, the specific
jurisdiction(s) whose law applies, or sign-off from legal counsel — none of these can be inferred
from the codebase, and each is a decision only the app's publisher can make (matching this
project's own stated exception for blockers requiring a human decision: "legal requirements").
Fill these in, and have this reviewed by counsel appropriate to wherever AURA is distributed,
before treating it as a real, published policy.
