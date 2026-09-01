# AURA AI — Third-Party Software

Every dependency AURA ships with, generated from the real, pinned version catalog
([gradle/libs.versions.toml](../gradle/libs.versions.toml)) — not a placeholder list. Referenced
from the in-app About screen (Profile → About).

---

## Apache License 2.0

The large majority of AURA's dependencies — AndroidX/Jetpack, Kotlin/kotlinx, Square, and
JetBrains libraries — are licensed under the
[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0):

- **AndroidX / Jetpack** (Google): `core-ktx`, `lifecycle-*`, `activity-compose`,
  `core-splashscreen`, `compose-bom` and the Compose UI/Material3/Foundation/Animation artifacts,
  `navigation-compose`, `hilt-navigation-compose`, `room-runtime`/`room-ktx`/`room-compiler`,
  `datastore-preferences`, `biometric`, `fragment-ktx`, `security-crypto`,
  `work-runtime-ktx`/`hilt-work`.
- **Kotlin / JetBrains**: `kotlinx-coroutines-android`/`play-services`/`core`/`test`,
  `kotlinx-collections-immutable`, `kotlinx-serialization-json`.
- **Dagger / Hilt** (Google): `hilt-android`, `hilt-android-compiler`.
- **Square**: `okhttp`, `okhttp-sse`, `mockwebserver`.
- **MockK**: `io.mockk:mockk`.
- **Cash App**: `app.cash.turbine:turbine`.
- **javax.inject** (JSR-330 reference API).

## Eclipse Public License 1.0

- **JUnit 4** (`junit:junit`) — [EPL 1.0](https://www.eclipse.org/legal/epl-v10.html).

## Proprietary — Google ML Kit

- **`com.google.mlkit:text-recognition`**, **`com.google.mlkit:barcode-scanning`** — distributed
  under [Google's ML Kit Terms of Service](https://developers.google.com/ml-kit/terms), not an
  open-source license. Both are used strictly on-device (no ML Kit cloud API is used) for the
  Vision Runtime's OCR and barcode-scanning tools — see
  [VISION_RUNTIME.md](VISION_RUNTIME.md).

---

Full license text for every Apache 2.0 and EPL 1.0 dependency above is available at the URLs
linked; none of AURA's own source code is derived from or a modification of any dependency listed
here — every integration is via each library's own public API, used as a normal application
dependency.
