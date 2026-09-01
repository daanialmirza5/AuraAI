# core-actions

The only Android-aware core module — it's the one place that needs a real `Context` to actually
*do* things on the device. Depends on core-ai and core-tools.

## What lives here

- **`ActionEngine`** — look up a tool by name in the shared `ToolRegistry`, run it. Deliberately
  doesn't know what a `PlanStep`/`ExecutionPlan` is (that's core-planner's model) — the app's
  `PlanExecutor` is what walks a plan and hands this one step at a time.
- **10 real, working `Tool` implementations** (not stubs — see below):

| Tool name | What it does | New permission needed? |
|---|---|---|
| `open_app` | Resolves an installed app by display name, launches it | No |
| `set_reminder` | Schedules a local notification via `AlarmManager.set()` (inexact) | No |
| `create_calendar_event` | `ACTION_INSERT` hand-off to the Calendar app | No |
| `navigate` | `geo:` URI hand-off to a maps app | No |
| `compose_email` | `ACTION_SENDTO` hand-off to an email app | No |
| `web_search` | Opens a Google search in the browser | No |
| `shopping_search` | Opens a Google Shopping search in the browser | No |
| `notify` | Posts an immediate local notification | Uses the app's existing `POST_NOTIFICATIONS` grant |
| `export_pdf` | Renders text into a real, paginated PDF via `android.graphics.pdf.PdfDocument`, written to the app's cache dir | No |
| `save_file` | Copies a file from cache into permanent app storage | No |

Every intent-based tool (open app / calendar / navigation / email / search) needs **zero new
dangerous permissions** by design — each hands off to another app's own UI for the user to see
and confirm, the same pattern any well-behaved Android app uses to integrate with the system
without asking for capabilities it doesn't strictly need.

## Why `Coding` and `Automation` have no tool here

- **Coding**: there's no OS-level "open a code editor and write this function" action —
  the only real capability here is generating code, which needs a connected AI provider. A
  `PlanStep` for a Coding intent has `toolName = null` and is routed to `AIProviderManager`
  instead, same as any provider-dependent step.
- **Automation**: toggling a device or automation needs the *app's own* `DeviceRepository` /
  `AutomationRepository` — this module has no access to (and shouldn't depend on) the app's
  domain layer. The app itself registers `app_automation` as a `Tool` (see `AppAutomationTool` in
  the app's `com.aura.ai.ai` package) into the same shared `ToolRegistry` this module populates.
  This is the intended pattern: core-actions provides the generic, Android-level tools; the app
  contributes its own domain-specific ones alongside them.

## `ReminderBroadcastReceiver`

Declared in this module's own `AndroidManifest.xml` (`android:exported="false"`) — Android
library modules can and should own the manifest entries for components they define; the app
module never has to remember to declare this itself.

## Status

Fully implemented and real for everything an Android system action can cover without a
connected AI provider. `open_app`/`set_reminder`/.../`save_file` all genuinely work today.
