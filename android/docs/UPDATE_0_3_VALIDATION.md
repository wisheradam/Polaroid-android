# Instant Room 0.3.0 implementation and validation

10 November 2026. Package `com.instantroom`; version **0.3.0**, code **3**. Based on working 0.2.0 commit `e737fbe58fb2594745a4047fdd61523234166ce8`. Dedicated branch: `feature/android-branding-captions-haptics`. No merge or release deployment.

## Implemented

- Replaced the camera's upper-left title block with the supplied Check2GO logo. Transparent outer padding is cropped and the bitmap scaled without altering its proportions or colors. A compact warm paper plate keeps it legible in both themes and leaves room for the camera switch.
- Added event-driven shake-development haptics: 12 ms soft pulses, at most one per 260 ms, only after accepted sensor input actually commits development progress during the active foreground session. Motors without amplitude control receive an 8 ms pulse. No periodic vibration or timer-driven development. Pause, completion and screen disposal cancel the session; late events cannot request pulses. The global haptics setting is respected; manual accessibility steps do not request shake feedback.
- Added **Write on photo / Edit caption** to completed-photo detail, with live preview, add/edit/remove, and a three-font picker: Sol Kol, Denistina and Historic. Draft text/font survive configuration recreation. Up to 80 Unicode code points and two short lines; invalid input shows an explanation and disables Save.
- Captions are fitted and clipped to the lower white paper margin for Classic, Go, Spectra and Round Frame. Glyph-bound fitting compensates for differing font metrics. Normal Android fallback applies to characters absent from a selected font.
- Added caption metadata and a retained uncaptioned-print path with Room migration **1→2**. Each render is written and finalized before the database reference changes. Original and uncaptioned files remain intact; removal restores the exact base file. Editing never consumes exposures, changes development progress, or moves pins.
- Archive, detail, corkboard and thumbnails load the updated processed path. Save/share resolve the latest committed record, exporting the complete captioned framed JPEG with the existing secure URI grants. Prior render paths remain readable for already-open share recipients.

## Modified components

| Component | Changes |
|---|---|
| `core` | Caption validation/font options; haptic throttle; feedback-port methods |
| `app/data` | Schema 2, explicit migration, atomic caption update, serialized edits, actual-progress result |
| `app/imaging` | Shared native Canvas/StaticLayout caption renderer using supplied fonts |
| `app/device` | Restrained development vibration and explicit cancellation |
| `app/ui` | Logo header, caption dialog/live preview/font picker, development lifecycle session, latest-record export/share |
| Assets | Supplied logo and three unmodified font files; Sol Kol OFL license |
| Build/docs/tests | Version bump; legacy schema fixture resources; migration, rendering, persistence, UI and haptic tests; asset and architecture notes |

No dependency, Android permission, camera, flash, theme, film-treatment, sensor-threshold, cartridge-capacity or corkboard model change was needed. No photo uploads or cloud dependency were added to the application.

## Automated validation

```bash
cd android
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
cd ..
python3 scripts/test_upgrade_helper.py
```

| Suite | Passed | Failed | Skipped |
|---|---:|---:|---:|
| Existing core film/shake/ejection/camera/theme/brightness rules | 44 | 0 | 0 |
| Caption validation and haptic throttle rules | 10 | 0 | 0 |
| Existing app Room/camera/lighting/theme/sharing/Compose regression tests | 45 | 0 | 0 |
| Version-1 database migration and reopen | 1 | 0 | 0 |
| Actual font rendering, margin containment and fitting | 4 | 0 | 0 |
| Caption add/edit/remove, persistence, export/share, concurrency and regeneration | 7 | 0 | 0 |
| Logo and caption Compose interactions | 4 | 0 | 0 |
| Development/haptic session integration | 4 | 0 | 0 |
| Development vibration ownership preserves shutter/ejection feedback | 1 | 0 | 0 |
| Existing Python backup-safety tests | 6 | 0 | 0 |
| **Total** | **126** | **0** | **0** |

Android/JVM total: **120** (54 core + 66 app). The original 89 tests remain present. Robolectric API 35 runs Android/Compose tests; caption raster and logo screenshot tests use native graphics with the actual bundled fonts and logo. They are not connected emulator or physical-device tests. Visual proofs for all twelve format/font combinations and both logo themes were inspected during development; they are build intermediates, not user photo uploads.

The migration test constructs a real version-1 SQLite database from the committed Room schema, populates legacy photo paths/progress, a seven-exposure cartridge and pinned placement, then opens it through Room's 1→2 migration and checks data preservation and reopening. Renderer tests check actual ink pixels remain inside the writing margin. Sharing and MediaStore tests compare delivered bytes to the captioned JPEG for every film format. Regeneration produces identical JPEG bytes after processing preferences change.

Local Gradle validation completed successfully (exit 0), including app assembly, instrumentation APK compilation, all **120 Android/JVM tests** and lint. Lint reports **no issues**. Final results include the vibration-ownership regression check: development cleanup cancels its own pulse while preserving shutter and ejection waveforms. Python safety checks passed (6/6, exit 0). Native raster proofs and both logo themes were inspected. Legacy AGP/Kotlin and Compose test-helper deprecation warnings remain from the existing toolchain; no dependency upgrade was introduced. Final diff whitespace validation passed.

## Verified delivered APK

The supplied APK is the locally built and validated debug artifact from application source **`62f2fff08621bc174333123eb2766baf64526034`**. A follow-up commit records packaging evidence only. The downloadable APK is not substituted with a differently hashed cloud artifact.

| Property | Verified value |
|---|---|
| File | `deliverables/InstantRoom-0.3.0-debug.apk` |
| Size | 17,482,251 bytes |
| SHA-256 | `4887d3b303908c908541ce31cf1ab43d69b0ccc67de84d3b45a179e205ff1da7` |
| Certificate SHA-256 | `57e0508a35dcb3f3ae4f648fcff78bf71d006fa370c821aa69bfb44245c234ab` |
| Package / version | `com.instantroom` / 0.3.0 / code 3 |
| Android API range | Minimum 26; target/compile 37 |
| Signature validation | `apksigner verify --print-certs`, exit 0; same retained certificate as canonical 0.2.0 |
| Bundled resources | Check2GO logo, all three TTF files and Sol Kol OFL license confirmed inside APK |

AWS CodeBuild **`instant-room-build-android:4d8616e7-0936-4219-bea0-0de743cde7ce` SUCCEEDED** on the same source revision. The private bundle was downloaded and checked independently: `SOURCE_COMMIT` matches `62f2fff08621bc174333123eb2766baf64526034`; packaged JUnit XML reports **120 passed, 0 failures, 0 errors, 0 skipped**; the cloud APK hash matches `APK-SHA256.txt`; `apksigner` verification succeeds with the same retained certificate as the delivered local APK and canonical 0.2.0.

Cloud verification artifact (separate from the supplied local APK): 17,261,468 bytes; SHA-256 `871d9395d7b84a4c5ef507774fbb21aaae540ffe1081ca50b2be5653a79849ea`. Bundle SHA-256: `c14d78265ee32d1133f792992526f27a657d31543440e223b14fe9c9735d366f`; private object `builds/4d8616e7-0936-4219-bea0-0de743cde7ce/instant-room.zip`. Both builds assemble the app/instrumentation APKs and run tests/lint; neither performs connected-device tests. Local and cloud binaries have distinct hashes; the delivered file remains the explicitly identified local artifact above.

[Draft PR #3](https://github.com/wisheradam/Polaroid-android/pull/3) remains unmerged and targets the working 0.2.0 feature branch. All tracked source changes are committed and published; supplied attachment files remain outside the source snapshot.

## Upgrade and remaining validation

Canonical 0.2.0 and 0.3.0 builds use the same retained debug signing key. A normal same-certificate Android update preserves data and runs the migration; no uninstall or reset is required. The originally supplied 0.1.0 key differs; its special backup-first guidance in `UPGRADE_0_2.md` still applies. Do not manually uninstall a populated installation.

Still requires a physical Android phone: vibration strength and motor/sensor interaction, actual pause/lock/call behavior, comfortable shake calibration, camera/mirroring/exposure, selfie lighting, ejection audio/haptics, third-party Instagram/Facebook composer behavior, and OEM upgrade/install behavior. Connected tests compile but have not been executed for this update. No physical-device results are claimed.

Caption editing requires a completed photograph. It handles up to two short lines rather than arbitrary typography or freehand drawing. Earlier rendered versions are retained to avoid breaking in-flight share URIs and can consume additional storage after many edits. Missing/corrupt base images produce an editor error; they cannot be reconstructed faithfully without the retained base. Two supplied font attachments contain no standalone redistribution license; see `ASSETS.md`. No new Stories-only integration was added.
