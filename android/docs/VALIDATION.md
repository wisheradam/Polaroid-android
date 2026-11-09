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
- App and instrumentation APK compilation succeeded with Gradle 9.5.0, AGP 9.3.1, Kotlin 2.4.21, Compose BOM 2026.09.00, CameraX 1.6.2 and Room 2.8.5. Final build/lint and AWS execution results are recorded below when complete.
- Debug APK's APK Signature Scheme v2 verification passed.
- Manifest inspection verified minimum API 26, compile/target API 37, Camera/Vibrate permissions, legacy write permission limited to API 28, and no INTERNET permission.
- CloudFormation local schema check: zero errors/warnings. Two custom cfn-guard rules passed. AWS ValidateTemplate and change-set validation passed. Infrastructure reached CREATE_COMPLETE in eu-central-1.

The Room tests exercise ten-exposure depletion, replacement without archive deletion, failed capture, duplicate completion, cartridge format, development persistence, actual database close/reopen, journal replay/idempotence, interrupted metadata write with no camera output, image processing and original preservation, export gating, and simulated MediaStore insertion.

Core tests cover shake threshold/reversal/refractory/gap/reset rules, no accepted input/no advancement, strength scaling, clamping, explicit accessibility opt-in, loading transitions, film dimensions, board pin locks, and ejection timing definitions.

## Emulator and hardware distinction

`androidTest` includes the repository tests, two Compose checks, and two fake-camera workflow tests. The workflow tests verify single-flight capture and the sequence/timing of calls to fake feedback ports; they do not claim to hear or feel physical effects. Emulator execution status is recorded below. Robolectric tests are JVM simulation, not emulator or real-device verification.

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
