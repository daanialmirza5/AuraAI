# AURA AI — Production Release

Critical item 10/10 toward Version 1.0 ([TODO_V1.md §2.10](TODO_V1.md#210-production-release)):
"Release build, App icons, Adaptive icons, Splash assets, Privacy policy, About page, Open-source
licenses, Versioning, Release notes, Play Store readiness."

---

## 1. Summary

| Item | Status |
|---|---|
| App icons / adaptive icons | Already real (pre-existing, verified — see §2) |
| Splash assets | Already real (pre-existing, verified — see §2) |
| Release signing config | **Wired this milestone** — optional, reads `keystore.properties` (see §3) |
| App bundle (`.aab`) build | **Verified this milestone** — `bundleRelease` succeeds |
| Crash-reporting hook | **Added this milestone** — local, dependency-free (see §4) |
| About page | **Added this milestone** — real version info + privacy/licenses summary (see §5) |
| Privacy policy | **Drafted this milestone** — `docs/PRIVACY_POLICY.md`, flagged for legal review (see §5) |
| Open-source licenses | **Documented this milestone** — `docs/THIRD_PARTY_LICENSES.md` (see §5) |
| Versioning | Already disciplined throughout this project — `VERSION.md`/`CHANGELOG.md`/`app/build.gradle.kts` version fields have been kept in sync every milestone |
| Release notes | `CHANGELOG.md` already serves this role — one entry per milestone, all along |
| Play Store readiness | **Genuinely blocked** — see §6 |

---

## 2. App icons, adaptive icons, splash — already real

Verified, not assumed: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` is a real Android
adaptive icon (`<background>`/`<foreground>` layers), backed by hand-drawn vector art in
`ic_launcher_background.xml`/`ic_launcher_foreground.xml` that echoes the app's own orb/glow design
language — not a placeholder or a generated default. `minSdk = 26` means adaptive icons alone are
sufficient; no legacy per-density PNG mipmap set is needed for any supported OS version. Splash
art (`ic_splash_orb.xml`) and a dedicated `Theme.AuraAI.Splash` are already wired into
`AndroidManifest.xml`. Nothing to fix here — this item was correctly built well before this
milestone.

---

## 3. Release signing: wired, optional, and safe in CI

```kotlin
// app/build.gradle.kts
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseSigningConfig = keystorePropertiesFile.exists()
// ...
if (hasReleaseSigningConfig) {
    signingConfig = signingConfigs.getByName("release")
}
```

`keystore.properties` (real values: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) is
already excluded in `.gitignore` alongside `*.jks`/`*.keystore` — a release keystore is a secret
that must never be committed, and this project cannot generate or hold one on its publisher's
behalf (see §6). `keystore.properties.example` documents the exact format and includes the
`keytool` command to generate a real keystore. Its absence — true in this environment, and true in
CI (`docs/CI_CD.md` §3, no keystore secret is configured there either) — means `release` simply has
no `signingConfig` assigned, exactly as before this milestone: `assembleRelease` and `bundleRelease`
both still succeed, producing a real R8-minified, resource-shrunk build — just an unsigned one,
which can't be installed on a device or uploaded to a store until a real keystore exists. Verified
directly: both `./gradlew assembleRelease` and `./gradlew bundleRelease` succeed end-to-end with no
`keystore.properties` present.

---

## 4. Crash reporting: a real local hook, not a third-party SDK

Wiring in a real crash-reporting *service* (Firebase Crashlytics, Sentry, or similar) needs an
account/project only AURA's publisher can create — that's a human decision, not an engineering
gap, matching this project's own stated exception for exactly this class of thing. What's real and
shippable without anyone's credentials: `LocalCrashLogger` (new, `app/src/main/java/com/aura/ai/core/crash/`)
installs a `Thread.setDefaultUncaughtExceptionHandler` in `AuraApplication.onCreate()` that writes
every uncaught exception to a bounded, on-device log (`filesDir/crash-logs/`, capped at 20 entries,
oldest pruned first) *before* delegating to the previous handler — which still runs, so Android's
own crash dialog and process termination behave exactly as they always did. Nothing about a crash
is ever swallowed. A future pass can point a real crash-reporting SDK's own handler at this same
install site without anything else in the app needing to change.

---

## 5. About page, privacy policy, licenses

Profile → About (new `ProfileSub` entry, alongside the screen's existing sub-tabs) shows real
`BuildConfig.VERSION_NAME`/`VERSION_CODE` (requiring `buildFeatures.buildConfig = true`, enabled
during the Security milestone), a condensed, accurate privacy summary, and a pointer to the two
full documents below. The Profile screen's existing "Privacy & Data" link now navigates there
directly instead of falling through to the generic Settings destination it used before.

- **`docs/PRIVACY_POLICY.md`** — a real policy describing AURA's actual, verified data handling
  (local-first storage, encrypted API keys, no analytics/crash SDK, what each of the 9 declared
  permissions is for), built directly from `docs/SECURITY.md`'s own findings rather than generic
  template language. Explicitly marked as a draft: it has no real business name, contact address,
  jurisdiction, or legal sign-off, because none of those can be inferred from the codebase — each
  is called out as the publisher's own decision before this can be treated as a real, published
  policy.
- **`docs/THIRD_PARTY_LICENSES.md`** — every real dependency from the version catalog, grouped by
  actual license (Apache 2.0 for the large majority; EPL 1.0 for JUnit4), with ML Kit correctly
  called out as Google's proprietary terms, not an open-source license — accuracy mattered more
  here than a uniform "everything is Apache 2.0" shortcut would have been convenient.

---

## 6. What's genuinely still blocked — not solved, and shouldn't be guessed

- **A real release keystore.** Generating one is one `keytool` command (documented in
  `keystore.properties.example`) — but it's a secret the app's publisher must generate, own, and
  back up themselves; losing it means losing the ability to ever publish an update to an
  already-published app under the same signing identity, permanently. Not something this project
  can create on anyone's behalf.
- **A Google Play Console developer account** ($25 one-time registration, tied to a real identity)
  and everything that depends on it: the actual store listing, screenshots (needs a real device or
  emulator to capture, neither available in this environment), a content rating questionnaire, and
  the privacy policy's final legal review and hosting at a real, publicly reachable URL.
- **Crash-reporting service credentials**, if the app's publisher later chooses to add
  Crashlytics/Sentry/etc. on top of `LocalCrashLogger` — their own account, their own decision.

Every one of these matches this project's own stated exception for when to stop and flag rather
than guess: "API credentials, legal requirements, Play Store account configuration." Everything
that doesn't depend on one of these three is done.

---

## 7. Verification

`./gradlew ktlintCheck detekt test lintDebug assembleDebug assembleRelease bundleRelease` — all
green: ktlint clean, detekt clean against baselines (two new, real findings from this milestone's
own code — a Compose-naming/length false-positive category and a deliberate broad `catch` in the
crash handler — fixed via the same documented `.editorconfig`-style config adjustments already
established in the CI/CD milestone, not baselined away), all tests passing, Android Lint 0 errors
(85 pre-existing warnings, unchanged), and all four build outputs (debug APK, release APK, release
AAB) succeeding.
