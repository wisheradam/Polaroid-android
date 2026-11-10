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

Local Gradle validation covers app assembly, instrumentation APK compilation, Android/JVM tests and lint. Final results include the vibration-ownership regression check. Python safety checks passed (6/6, exit 0). Native raster proofs and both logo themes were inspected. Legacy AGP/Kotlin and Compose test-helper deprecation warnings remain from the existing toolchain; no dependency upgrade was introduced.

Canonical AWS build, immutable source revision and delivered APK details are appended after packaging verification.

## Upgrade and remaining validation

Canonical 0.2.0 and 0.3.0 builds use the same retained debug signing key. A normal same-certificate Android update preserves data and runs the migration; no uninstall or reset is required. The originally supplied 0.1.0 key differs; its special backup-first guidance in `UPGRADE_0_2.md` still applies. Do not manually uninstall a populated installation.

Still requires a physical Android phone: vibration strength and motor/sensor interaction, actual pause/lock/call behavior, comfortable shake calibration, camera/mirroring/exposure, selfie lighting, ejection audio/haptics, third-party Instagram/Facebook composer behavior, and OEM upgrade/install behavior. Connected tests compile but have not been executed for this update. No physical-device results are claimed.

Caption editing requires a completed photograph. It handles up to two short lines rather than arbitrary typography or freehand drawing. Earlier rendered versions are retained to avoid breaking in-flight share URIs and can consume additional storage after many edits. Missing/corrupt base images produce an editor error; they cannot be reconstructed faithfully without the retained base. Two supplied font attachments contain no standalone redistribution license; see `ASSETS.md`. No new Stories-only integration was added.
