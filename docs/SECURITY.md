# AURA AI — Security

Milestone 8 of the "AURA FINAL VERSION 1.0 COMPLETION" brief: "Review: Permissions, API Keys,
Encrypted storage, Logging, Privacy, Data deletion, Export/import, Backup."

---

## 1. Summary

| Area | Finding | Outcome |
|---|---|---|
| Permissions | All 9 declared permissions (`AndroidManifest.xml`) are genuinely used by real code — none speculative. | No issue. |
| API keys | `AndroidProviderCredentialStore` uses real `EncryptedSharedPreferences` (`MasterKey` AES256_GCM, `AES256_SIV` key encryption, `AES256_GCM` value encryption) — the Jetpack Security strong default, not a weaker scheme. | No issue. |
| Logging | Provider HTTP request/response bodies and headers are never logged. One low-severity gap: a plugin-logging facade used unconditional `println`, reachable from any future third-party plugin. | **Fixed** — see §2. |
| Secrets in source | No hardcoded API keys/tokens anywhere in the repo. `local.properties`/`keystore.properties`/`.env` correctly `.gitignore`d and untracked. | No issue. |
| Network security config | Cleartext is blocked globally; the only allowlisted cleartext hosts are `localhost`/`127.0.0.1`/`10.0.2.2`, narrowly justified for the local Ollama provider. | No issue. |
| Backup | `android:allowBackup="true"` with extraction rules that excluded the Room database and DataStore prefs — but not the encrypted API-key `SharedPreferences` file. | **Fixed** — see §3. |
| ProGuard/R8 | No blanket `-keep class ** { *; }`; every rule is narrowly scoped (Room entities, Tink's compile-time-only annotations) with a documented reason. | No issue. |
| Data deletion | The Profile screen's "Clear all memory" control was presented as a real, irreversible, dangerous action but had no `onClick` handler at all — a privacy control that lied to the user. | **Fixed** — see §4. |
| Export/import | "Export My Data" / "Export memory graph" are likewise unwired. | **Deliberately not fixed this pass** — see §5. |
| Third-party data sharing | No analytics/crash-reporting SDK integrated; the only outbound network calls anywhere in `app/src/main` or `core-*/src/main` go to the four AI providers the user themselves configured. | No issue. |

Two real, user-facing gaps found and fixed (§2, §3, §4); one real gap found and deliberately
deferred with reasoning, not silently left (§5).

---

## 2. Logging: `PluginContextFactory`'s log sink

`core-plugin/.../PluginContextFactory.kt` built every plugin's `PluginContext.log()` sink as
unconditional `println(...)` — reachable by any plugin (the SDK's own `DiceRollerPlugin`/
`WordCounterPlugin` today; a real third-party plugin once the marketplace has one) and,
unfiltered, written to logcat in release builds exactly as in debug.

Fixed by making the sink an injected dependency rather than something `core-plugin` (a pure-Kotlin
module, correctly free of any Android dependency) hardcodes itself:

```kotlin
// core-plugin — the shape only, no Android dependency
@Qualifier
annotation class PluginLogSink

class PluginContextFactory @Inject constructor(
    ...
    @PluginLogSink private val logSink: @JvmSuppressWildcards (pluginId: String, message: String) -> Unit,
)
```

```kotlin
// :app — the real, environment-aware implementation
@Provides
@PluginLogSink
fun providePluginLogSink(): (String, String) -> Unit =
    if (BuildConfig.DEBUG) { id, msg -> Log.d("Plugin:$id", msg) } else { _, _ -> }
```

In a release build this is a genuine no-op — no plugin-authored string reaches logcat at all, not
just a lower log level. `BuildConfig` generation had to be turned on for `:app`
(`buildFeatures { buildConfig = true }`, previously unset) to make `BuildConfig.DEBUG` available.
`@JvmSuppressWildcards` on the injected function type works around the well-known Dagger/Kotlin
function-type variance mismatch (the injection site's `Function2<? super String, ? super String, ...>`
vs. the provider's unwildcarded return type) — the same category of Dagger+Kotlin gotcha this
project has hit before with default-argument constructors, verified fixed by a clean
`assembleRelease` (R8 included) after the change.

---

## 3. Backup: excluding the encrypted credential store

`data_extraction_rules.xml` (API 31+) and `backup_rules.xml` (legacy) excluded the Room database
and DataStore preferences from both cloud backup and device-transfer, but never excluded
`aura_provider_credentials` — the `EncryptedSharedPreferences` file holding every configured AI
provider's API key.

The `EncryptedSharedPreferences`' encryption key is Android-Keystore-bound and never leaves the
device, so a cloud-restored copy of that file is ciphertext nobody can decrypt on another device —
this materially limits the real exploitability. Device-transfer is a different story: some OEM
transfer implementations *do* migrate Keystore-backed keys alongside app data, which could make a
transferred copy of that file decryptable on the new device. Both files now explicitly exclude
`domain="sharedpref" path="aura_provider_credentials.xml"` from both cloud-backup and
device-transfer — defense-in-depth for the cloud case, a real fix for the device-transfer case, and
removes any ambiguity about whether the original omission was intentional.

---

## 4. Data deletion: "Clear all memory" now actually clears memory

The Profile → Memory Manager screen's "Clear all memory" row (`danger = true`, described as
"Irreversible — starts AURA fresh") had no `onClick` at all — tapping it did nothing, silently.
That's a privacy control lying to the user: someone who taps it and sees no error reasonably
believes their data is gone.

Fixed narrowly, matching exactly what the label promises — the real, persisted `Memory` entries
backing the app's own Memory tab (`MemoryDao`/`MemoryRepository`/`MemoryRepositoryImpl`), not chat
history or provider credentials, which the label never claimed to touch:

- `MemoryDao.deleteAll()` (`DELETE FROM memories`).
- `MemoryRepository.forgetAll()` / `MemoryRepositoryImpl` — a real interface method, not a caller
  looping `forget(id)` over the current list.
- `ProfileViewModel.clearAllMemory()`, called only after an explicit Material3 `AlertDialog`
  confirmation ("This permanently deletes every stored memory. This cannot be undone.") — matching
  the "explicit confirmation before any destructive action" rule already established for
  destructive tools in the Android Automation milestone (`docs/ANDROID_AUTOMATION.md` §3), applied
  here for the first time to a UI-only (non-tool) destructive action.

The rest of the Memory Manager screen ("Review recent memories," the "48 of 500 entries" progress
bar, and the whole Analytics/Device Health/Battery/Storage sub-screens) is pre-existing, clearly
decorative sample/mockup content — hardcoded numbers with no backing subsystem at all (no real
device telemetry integration exists anywhere in this app). That's a UI-completeness gap, not a
security or privacy one: nothing there claims to control real, destructible user data the way
"Clear all memory" did, so it's out of scope for a security review and was left untouched.

---

## 5. Export/import: found, not fixed — a real, documented deferral

"Export My Data" (Profile main screen) and "Export memory graph" (Memory Manager) are both
similarly unwired — tapping either does nothing. Unlike "Clear all memory," this is a missing
capability, not a false claim of control over already-real data being destroyed: nothing is lost or
misrepresented as gone by the button not working, just a feature that doesn't exist yet.

Building a real export (a genuine serialization format for the memory graph plus conversation
history, written to a shareable file, surfaced through a share intent) is a substantive feature in
its own right — this project's `ROADMAP.md` already tracks it, under its own name, as a High
(not Critical) item: "Conversation/memory export & backup." Rather than rushing a half-built
version of that feature into a security-review pass, the honest choice is the one this project has
made consistently elsewhere: leave it where it's already correctly tracked, and say so here rather
than silently leaving the finding unmentioned.

---

## 6. Verification

`assembleDebug`, `assembleRelease` (R8 minification included — the plugin log sink's
`BuildConfig.DEBUG` branch survives shrinking), `lintDebug` (0 errors, 85 pre-existing warnings,
unchanged count), and `test` all green after every change in this document.
