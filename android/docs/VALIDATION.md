# Implementation and validation report

Date: 9 November 2026. Working name: **Instant Room**, package `com.instantroom`, version `0.1.0` (debug).

## Implementation

| Area | Implemented behavior |
|---|---|
| Foundation | Native Kotlin, Compose/Material 3, MVVM, separate JVM core, Room, six screens, Android Studio project and official Gradle wrapper |
| Camera | CameraX preview and real JPEG capture; camera permission flow and Android Settings recovery; flash capability handling; rear/front option; single-flight capture |
| Film | Exactly ten exposures, transactional decrement, no decrement on failure, FILM EMPTY blocking, manual door/removal/insertion/closure, early-discard warning, persistent cartridges |
| Analog interaction | Original charge/shutter/roller/pin sounds, configurable charging duration, 1800 ms ejection clock, restrained synchronized vibration waveform, independent settings |
| Development | Opposing-peak accelerometer detection, stronger/faster development, stationary pause, Room persistence, no timer completion, explicit accessibility button, sensor fallback and lifecycle cleanup |
| Formats | Classic, Go, Spectra, Round, cartridge-bound crop/frame geometry and appearance; manufacturer references documented |
| Processing | EXIF correction, center crop, three stocks, tonal compression, softness, grain, vignette, optional chemistry; independent original and processed JPEGs; bounded resolutions |
| Archive | All captured photographs, unfinished-photo resume, failed-processing retry, completed-photo viewing, MediaStore export and FileProvider sharing |
| Corkboard | Cork texture, drag/tap print tray, free placement/rotation, stacking, shadows, colored positionable pins, storage-enforced pin locks, unpin and detach, viewport pan/zoom, persisted coordinates |
| AWS | Deployed on-demand CodeBuild project, private S3 source/artifacts, encrypted CloudWatch logs, least-scope service role, CloudFormation and repeatable snapshot packaging |

## Verified locally

- 30 JVM core tests passed, zero failures/skips.
- 11 Room/Robolectric Android JVM tests passed, zero failures/skips (simulated API 35).
- App and instrumentation APK compilation succeeded with Gradle 9.5.0, AGP 9.3.1, Kotlin 2.4.21, Compose BOM 2026.09.00, CameraX 1.6.2 and Room 2.8.5. Local final verification exited 0; lint reported no issues. AWS execution results are recorded below.
- Debug APK's APK Signature Scheme v2 verification passed. All four original PCM audio assets passed format/duration checks.
- Manifest inspection verified minimum API 26, compile/target API 37, Camera/Vibrate permissions, legacy write permission limited to API 28, and no INTERNET permission.
- CloudFormation local schema check: zero errors/warnings. Two custom cfn-guard rules passed. AWS ValidateTemplate and change-set validation passed. Infrastructure reached CREATE_COMPLETE in eu-central-1.

The Room tests exercise ten-exposure depletion, replacement without archive deletion, failed capture, duplicate completion, cartridge format, development persistence, actual database close/reopen, journal replay/idempotence, interrupted metadata write with no camera output, image processing and original preservation, export gating, and simulated MediaStore insertion.

Core tests cover shake threshold/reversal/refractory/gap/reset rules, no accepted input/no advancement, strength scaling, clamping, explicit accessibility opt-in, loading transitions, film dimensions, board pin locks, and ejection timing definitions.

## Emulator and hardware distinction

`androidTest` includes the repository tests, two Compose checks, and two fake-camera workflow tests. The workflow tests verify single-flight capture and the sequence/timing of calls to fake feedback ports; they do not claim to hear or feel physical effects. An API 35 software-emulated device installed both APKs successfully, but its first instrumentation run was killed during application startup before executing tests. Logs show system-wide startup ANRs/timeouts, including phone and media-provider processes, without KVM acceleration. No passing emulator test count is claimed for that attempt. Robolectric tests are JVM simulation, not emulator or real-device verification.

**No physical Android phone has been tested.** Real camera quality/flash latency, physical shake calibration, vibrator intensity, acoustic synchronization, battery use and device-specific frame pacing remain unverified. Follow `HARDWARE_CHECKLIST.md` before treating this as a production release.

## Remaining limitations

- Single corkboard; schema includes a board identifier for future expansion.
- Final export processing uses CPU Canvas off the main thread; display composition and chemical masks use Compose's GPU rendering. No custom GPU image-processing engine is claimed.
- Original stylized camera/door animations, not a photorealistic 3D camera model.
- Border offsets are visual approximations around manufacturer-specified exterior/image dimensions.
- No re-edit screen for applying a different stock to an already processed photograph.
- An interrupted animation is skipped on resume; the archived exposure/development state is the durable recovery target.
- A damaged camera JPEG whose bytes were never fully written cannot be reconstructed; the original remains available for recovery/diagnosis.
- The debug APK is installable development software, not a signed production release or Play Store submission.
- The Gradle project currently uses AGP's supported external-Kotlin/legacy-DSL compatibility switches; migration to built-in Kotlin should happen before AGP 10.
- Archive data is device-private and is removed on uninstall. Gallery exports are separate copies. There is no cloud photo backup.

## Final packaging results

Local command (exit **0**):

```bash
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Results: **30/30** core tests and **11/11** Room/Robolectric tests passed, zero failures/errors/skips; lint **0 errors, 0 warnings**. Application and instrumentation APKs assembled successfully. Official wrapper distribution SHA-256 is pinned. Four original audio format/duration checks passed. The debug signature verifier exited **0**.

AWS CodeBuild execution **SUCCEEDED**, including the same test, lint and assembly tasks. Build ID: `instant-room-build-android:956d909f-ff1b-4776-82c5-8f5a619988fa`. Immutable app source revision: `9ca488a1f36e38d6d36d771f233ae2d452d6a2fa`. Subsequent report/ignore-file cleanup does not alter the application source used for this APK.

The private artifact ZIP was downloaded and independently checked: it contains both APKs, two JUnit XML reports showing 30 + 11 passing tests, the lint HTML report, the exact `SOURCE_COMMIT`, and `APK-SHA256.txt`. The stored artifact uses `aws:kms` encryption. No connected device/emulator tests ran in CodeBuild.

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| Delivered AWS-built `InstantRoom-0.1.0-debug.apk` | 16,896,742 | `0e6a564cf7d141e04f64d9c4e478af990376c840ffe8f734d7121f82d24515ad` |
| AWS `instant-room.zip` | 17,244,441 | `f8e978772bb32847ce4d978afb6056d5a9f75561193bfb14e0eeecdc9d2c884e` |

Artifact location: the stack's private ArtifactsBucket under `builds/956d909f-ff1b-4776-82c5-8f5a619988fa/instant-room.zip`. The independently generated local debug APK uses a different build environment/signing key; the delivered file is the verified AWS-built APK. If switching between differently signed development APKs, Android may require uninstalling the earlier version, which deletes its private archive. Export photographs first.

### Emulator execution outcome

Two instrumentation attempts on an API 35 x86_64 software-emulated device ended with `Process crashed` during startup and **did not produce a passing test suite**. The emulator has no `/dev/kvm` support in this environment. Its logs show startup timeouts/ANRs in the application, phone and media-provider processes, and a captured screen shows **“Process system isn't responding”**. App UI rendering was observed behind that system dialog; working CameraX capture was not verified. Both APK install commands returned Success. These results are an environment-limited check, not a physical-device acceptance pass.

**Release readiness:** installable initial debug implementation with passing JVM/integration simulation checks and a verified AWS build. Real-device acceptance and UX/performance calibration remain open. The pull request remains a draft.
