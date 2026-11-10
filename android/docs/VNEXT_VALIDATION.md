# Instant Room 0.2.0 implementation and validation

Date: 10 November 2026. Application ID `com.instantroom`; version **0.2.0**, version code **2**. Based on the working v1 branch at `a1faaba3e9ae82595aba78a69309b988f45d5632`. Work is isolated on `feature/android-vnext-selfie-theme-sharing`; nothing is merged into `main` or published to an app store.

## Implemented features

| Feature | Implementation |
|---|---|
| Main-screen camera switching | Original camera/rotation icon, single-tap front/rear selection, capability checks, disabled during charging/capture/processing/ejection, and a repository-lock guard. Fallback to the available lens on single-camera devices. No inventory/database writes during switching. |
| Selfie orientation | CameraX manages target rotation. Front capture sets horizontal-reversal metadata deliberately to match the mirrored preview; existing EXIF processing bakes it into the final framed JPEG. Rear images remain unmirrored. |
| Screen flash | CameraX `FLASH_MODE_SCREEN` plus its `ScreenFlashView` perform the animated full-display white/warm-white illumination, window brightness boost, UI-ready callback and exposure synchronization. It captures camera sensor pixels, not a screenshot. |
| Flash cleanup | Idempotent session wrapper fences late callbacks. Camera pause/unbind, capture completion/failure, root disposal and leaving the camera clear illumination. The native view sits behind controls while inactive; a Compose touch test verifies the layer does not block controls. |
| Edge light | Actual bright screen pixels, not a shadow/glow effect. White/warm-white frame, 8–32 dp width, adjustable window brightness, rounded-corner awareness and safe-drawing/cutout insets. The preview stays visible. Screen-only, edge-only and combined modes are available from the selfie control. |
| Brightness ownership | Window-only override, no WRITE_SETTINGS permission. Edge mode saves the original brightness including the system sentinel `-1`; screen flash temporarily overrides that edge level, then returns to it; leaving the camera restores the original. |
| Themes | Light, Dark and Follow System across Material controls, all screens, menus/dialogs and system-bar icons. Warm neutral light surfaces and deep neutral dark surfaces. Paper borders, image pixels, cork and pin materials remain physical colors. |
| Theme upgrade | New installations default to Follow System. Existing v1 settings or database retain the prior dark appearance. Theme, lens and lighting preferences persist without a Room migration. |
| Social sharing | Instagram and Facebook menu actions in archive cards and completed-photo detail. Narrow package queries, image-handler resolution including Lite variants, ACTION_SEND with the existing full processed JPEG, MIME image/jpeg, FileProvider content URI, ClipData and temporary read-only permission. Missing/unsupported targets and launch failures fall back to the Sharesheet. No Meta SDK, account, OAuth or new permissions. |
| Upgrade handling | Database/schema/file layout remain unchanged. A retained encrypted debug-signing key makes subsequent canonical builds consistent. The first supplied 0.1.0 key is unavailable; a documented opt-in ADB backup/restore helper is provided for that signature transition. It never uninstalls on a certificate mismatch without an explicit flag and verified local backup. |

## Modified components

- `core`: camera switch policy, theme/lighting/share modes and scoped brightness ownership.
- `app/data`: backward-compatible SharedPreferences defaults and persistence. Room entities, DAO, repository and schema are unchanged.
- `app/device`: CameraX capability/mirroring/screen-flash handling and the idempotent lighting session.
- `app/ui`: existing camera, theme, root navigation, settings, archive and development screens; small camera-control, lighting-overlay and share-menu components.
- `app/imaging/PhotoExport`: secure target-aware image intents and fallback. Film processing itself is unchanged.
- Manifest: four narrow package-visibility queries only. No new runtime/system permission.
- Build: version bump, optional debug keystore path and JVM Compose test dependency. Working Kotlin/Compose/CameraX/Room/Gradle versions are preserved.
- `infra/aws`: scoped read access to one encrypted signing parameter; runtime-only key injection into CodeBuild. Signing material is excluded from source/artifacts.
- `scripts`: explicit data-preserving debug upgrade helper and archive-safety tests; setup/upgrade/validation documentation.

## Executed local checks

Command:

```bash
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
python3 scripts/test_upgrade_helper.py
```

| Suite | Passed | Failed | Skipped |
|---|---:|---:|---:|
| Existing film/shake/ejection/pin rules | 30 | 0 | 0 |
| New core camera/theme/lighting/brightness rules | 14 | 0 | 0 |
| Existing Room/processing/archive/export regression tests | 11 | 0 | 0 |
| Camera switch/inventory/archive integration | 3 | 0 | 0 |
| Lighting session/cancellation/failure cleanup | 8 | 0 | 0 |
| Actual CameraX ScreenFlashView + simulated Window | 2 | 0 | 0 |
| Sharing intents, read grants, format/file preservation and fallback | 8 | 0 | 0 |
| Theme/lighting preference migration and restart persistence | 7 | 0 | 0 |
| Compose switch/menu/theme/touch checks on Robolectric | 6 | 0 | 0 |
| Python backup path/link/ownership/hash safety | 6 | 0 | 0 |
| **Total** | **95** | **0** | **0** |

The Android/JVM total is **89 tests**; the additional **6** tests exercise Python migration safety functions. The 41 original JVM/Room tests remain present and passing. Compose tests executed under Robolectric API 35, not a connected emulator. The actual CameraX view tests verify the Window brightness property and ramp cancellation against Android simulation, not physical illumination or camera exposure.

The Gradle build and lint/APK tasks complete successfully; lint reports no issues. Existing device-side test sources also compile into the instrumentation APK. Legacy AGP/external-Kotlin and test-helper deprecation warnings remain, as in the base toolchain; they are not lint errors.

Infrastructure template validation: cfn-lint zero findings, both project-specific cfn-guard rules pass, CloudFormation validation passes. The build-role/project update adds only encrypted parameter injection and scoped parameter read access; no release deployment is involved.

## Sharing contract and Stories limitation

Supported sharing is selected from the installed app's current ACTION_SEND image/jpeg handler at runtime. The application hands the complete framed JPEG to its composer; it does not publish automatically or promise a particular Feed/Story/album destination. The receiving application may still offer its own crop or resolution controls.

A distinct Instagram Stories-only intent was **not** added. The current Meta Stories integration could not be verified from accessible official documentation in this environment, and no registered Meta app integration is configured. A package-name/undocumented-action workaround is deliberately avoided. Instagram's supported ordinary image-sharing flow remains available where its installed app advertises one.

References checked:

- Android binary sharing: https://developer.android.com/training/sharing/send
- CameraX ImageCapture screen-flash mode: https://developer.android.com/reference/androidx/camera/core/ImageCapture
- CameraX ScreenFlashView animation/brightness contract: https://developer.android.com/reference/androidx/camera/view/ScreenFlashView
- CameraX metadata: https://developer.android.com/reference/androidx/camera/core/ImageCapture.Metadata
- Meta Stories reference (not retrievable during verification): https://developers.facebook.com/docs/instagram/sharing-to-stories/

## Physical-device checks still required

- Real front/rear preview rebinding, missing-lens fallback, saved selfie mirroring/rotation on OEM camera drivers.
- Actual face illumination, display brightness, AE/white-balance response and flash timing on OLED and LCD phones.
- Pause/lock/call/rotation interruption during a flash and both lighting modes; verify global brightness is unaffected and window brightness returns to its prior value.
- Rounded displays/cutouts, landscape, large fonts and the maximum border width on small screens.
- Instagram/Facebook/Lite availability, composer handoff, URI consumption and fallback on current installed app versions. No third-party apps were launched or posted to by these tests.
- Original real camera flash, vibration, motor/ejection sound alignment and shake calibration remain hardware checks; the existing mechanics were not rewritten.
- The opt-in USB signature-migration workflow has not run on an actual phone. Read `UPGRADE_0_2.md`, retain a verified backup and do not manually uninstall a populated 0.1.0 installation.

No connected emulator/device tests or physical-device validation are claimed for vNext. No destructive database migration or data reset was introduced.

## APK and AWS build

AWS CodeBuild `instant-room-build-android:c2e93d02-5da8-4cb9-b13e-2a24a6cad07f` **SUCCEEDED** on 10 November 2026. Built source revision: `e85d3b383203187b8d5d4a7dc5a215d163c3dd8a`. The final follow-up commit changes this report only; application source is identical to the built revision.

The private artifact bundle was downloaded and independently checked: its `SOURCE_COMMIT` matches the built revision, all packaged JUnit XML totals **89 passed, 0 failures, 0 errors, 0 skipped**, and the APK checksum matches `APK-SHA256.txt`. The six Python safety tests also passed locally and ran as a required AWS build step. The local Gradle build was rechecked successfully after resuming the task (exit 0); Python safety tests returned exit 0. Instrumentation APK compilation passed; no connected test execution is claimed.

| Artifact | Verified value |
|---|---|
| Delivered APK | `deliverables/InstantRoom-0.2.0-debug.apk` |
| APK size | 16,930,377 bytes |
| APK SHA-256 | `9bba8129e9b2fa5d3ce6eb6048a7cf8b84efcf8aa5bfc2babc6ebfe2baf954f3` |
| APK certificate SHA-256 | `57e0508a35dcb3f3ae4f648fcff78bf71d006fa370c821aa69bfb44245c234ab` |
| Package metadata | `com.instantroom`, version 0.2.0, code 2, min SDK 26, target SDK 37 |
| AWS artifact bundle SHA-256 | `01e905bb00e33e2c8205ba74dfac189e77fff9ce866aaad4ae77e9fbf45b1f9c` |
| AWS artifact object | `builds/c2e93d02-5da8-4cb9-b13e-2a24a6cad07f/instant-room.zip` |

`apksigner verify --print-certs` completed successfully (exit 0). Final diff whitespace validation passed. [Draft PR #2](https://github.com/wisheradam/Polaroid-android/pull/2) targets the working v1 feature branch, remains a draft, and has not been merged. The detailed initial-build report remains in `VALIDATION.md` as historical evidence for version 0.1.0.
