# Instant Room 0.4.0 — self-timer and Photo Booth

10 November 2026. Package `com.instantroom`; version **0.4.0**, code **4**. Based on working 0.3.0 revision `9dfaf1f414f570a3dfe804cdd2330bdd7a4d4ce9`. Dedicated branch: `feature/android-timer-photo-booth`. No main merge or production release.

## Implemented

- Persistent Off/5/15/30-second self-timer in a compact camera menu. Large preview countdown, distinct final-three-second tone/digits, cancellation, lifecycle/camera-loss cancellation and single-flight capture locking. Off preserves the original configurable charge sequence with no additional timer; delayed captures include charging inside the selected countdown.
- Photo Booth mode on the existing camera screen: layout/lens/lighting selection before Start, a five-second countdown for each of four shots, current shot label, source thumbnails, and a preview-local Cancel control. Busy controls are hidden/locked and the camera scrolls back to the countdown after Start.
- Four separately journalled source captures, each with its own original JPEG, timestamp, session/index metadata and exactly one transactional exposure deduction. Start requires four remaining exposures. No automatic reload, no exposure for the composite and no refunds for already captured shots.
- Cancellation/errors stop future requests while an already submitted camera result completes its durable archive section. Successful partial sources remain individually accessible. Restart never resumes taking photos automatically. Recovery reconstructs a missing four-source primary composite exactly once and preserves previously developed/captioned composites.
- 2×2 collage and 1×4 vertical strip, rendered from full upright originals with intentional selfie mirroring, full-image fitting rather than cropping, selected film color/mono treatment, consistent paper/frame geometry and a writable lower border. Default outputs: 2400×2748 and 1200×4913. Existing low-memory preference: 1600×1832 and 800×3275.
- One mechanical ejection for a complete composition, then mandatory shake-only development with the existing haptics and persistence. The chemical mask covers all four image regions; time alone does not advance it.
- Existing three-font add/edit/remove captions, archive, pinned corkboard placement/rotation/board zoom, secure exports and Instagram/Facebook/Sharesheet flows work with both layouts. Completed booth detail can reprint either layout from retained originals; each reprint starts undeveloped and uses no film.
- Explicit schema **2→3** migration for booth fields/session table. Both 1→2 and 2→3 migrations are registered, preserving older images, captions, progress, cartridges and pins. Existing branding, themes, selfie flash/edge light, sensor thresholds and cartridge formats remain intact.

## Modified components

| Component | Changes |
|---|---|
| `core` | Clock-injected scheduling, timer/layout options, print/cell geometry, countdown phase and feedback hooks |
| `app/data` | Schema 3/session metadata, source-aware journals/recovery, idempotent composite creation, reprinting, persisted timer/layout |
| `app/device` | Highest available standard CameraX resolution in booth mode, readiness observation, countdown/exposure feedback, exclusive countdown/charge/shutter audio |
| `app/imaging` | Orientation-aware composite rendering and shared caption geometry; original single-photo film pipeline retained |
| `app/ui` | Timer/mode/layout/countdown controls, session coordination, thumbnails, caption/development/archive/board geometry, lifecycle cancellation |
| Assets/build/docs | Original countdown WAV/generator, timer icon, version bump, setup/architecture and validation documentation |

## Executed automated checks

```bash
cd android
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
cd ..
python3 scripts/test_upgrade_helper.py
```

| Suite | Passed | Failed | Skipped |
|---|---:|---:|---:|
| Existing core rules | 54 | 0 | 0 |
| Existing app camera/film/shake/lighting/theme/caption/archive/feedback regressions | 66 | 0 | 0 |
| Clock-driven timer/sequence/layout rules | 8 | 0 | 0 |
| Booth repository/recovery/render/orientation/caption/export/pin/reopen checks | 13 | 0 | 0 |
| Timer/booth coordinator integration with fake camera, clock and feedback | 10 | 0 | 0 |
| Compose timer/layout/countdown/mask/small-screen checks | 5 | 0 | 0 |
| Timer/layout preference persistence and upgrade defaults | 2 | 0 | 0 |
| Existing schema-2 captioned data migration | 1 | 0 | 0 |
| Existing Python backup-safety tests | 6 | 0 | 0 |
| **Total** | **165** | **0** | **0** |

Android/JVM total: **159** (54 core + 105 app). All 120 previous Android/JVM tests remain present. The earlier schema-1 migration test now exercises the registered 1→2→3 chain. The new schema-2 test retains caption/font/base image path, progress, exposure count and pinned placement through migration.

Gradle build, tests, app assembly, instrumentation APK compilation and lint completed successfully (exit 0). Lint reports **no issues**. Python tests passed (6/6, exit 0). Final diff whitespace validation passed. Existing AGP/Kotlin/Compose test-helper deprecation warnings remain from the unchanged toolchain.

Robolectric API 35 executes Android/Compose integration tests; native graphics verifies pixel order, full-image fitting, EXIF rotation/mirroring, monochrome output and strip proportions. Fake-clock tests request booth photos at 5000/10000/15000/20000 ms despite simulated storage latency. Integration checks cover rear hardware-flash requests and front screen-flash requests, repeated Start, insufficient film, cancellation before any shot and during shots 2/4, and failed shot 2. Completed/partial archive counts and exposure deductions are asserted. Both geometries exercise all three supplied caption fonts, development, pins, sharing and MediaStore exports. Native visual proofs for both compositions and the active countdown on a 320×560 dp camera surface were inspected. These are simulated graphics/camera ports, not physical-camera verification.

## Timing and remaining validation

Five seconds is the target interval between **CameraX capture requests**, not a promise that every phone's shutter/AE/flash finishes at an exact millisecond. Processing is deferred until after the fourth shot. If a request itself exceeds the interval, the next request waits safely and still allows charging; captures never overlap. Highest available standard capture resolution is requested in booth mode; the source files retain the JPEGs supplied by CameraX, without forcing vendor-specific sensor modes.

Full images are fitted into fixed cells, so paper letterboxing can appear for differing portrait/landscape ratios. Reprinting creates another undeveloped print and does not carry an earlier print's handwritten note onto the new layout. Four original source records plus the composition are visible in Archive. Interrupted partial sessions retain sources but do not resume camera capture; start a new session when ready. Existing board zoom preserves proportions; individual print-resize controls were not added.

Physical-phone checks remain: actual five-second shutter cadence and camera latency, front/rear maximum JPEG resolution, orientation/mirroring across OEM drivers, flash/display lighting/exposure, preview smoothness, app pause/lock/call/camera-disconnect handling, countdown audio/vibration/ejection timing, real shake calibration and current Instagram/Facebook composer handoff. Instrumentation tests compile but were not executed on a connected emulator or phone for this update. No real-device results are claimed.

Canonical builds retain the same signing certificate as 0.2.0/0.3.0 and support normal in-place updates with the registered migration. The first supplied 0.1.0 signature exception still applies; see `UPGRADE_0_2.md` before replacing a populated installation. Do not uninstall a populated app manually.

## Source and APK evidence

Exact committed revision, artifact size/hash, certificate verification and independent AWS build evidence are appended after packaging.
