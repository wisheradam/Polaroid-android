# Instant Room 0.5.0 — camera identity and flash motion

10 November 2026. Package `com.instantroom`; version **0.5.0**, code **5**. Based on working 0.4.0 revision `bd740490ad4a63ca6bbac14c20966346b6d657d2`. Dedicated branch: `feature/android-camera-styles-flash`. No main merge or production release.

## Implemented and retained

- Settings offers three original camera identities: **Old Fasion**, **Insta Style**, **One and Only**. They use different shell shapes, housing details, textures, shutter surrounds and accents, with live miniature previews and **13** compatible color finishes. Selection updates the current UI and persists across preferences reopening. Invalid or older values use compatible defaults.
- Old Fasion: Black, Ivory, Dark Olive, Brown / Brass. Insta Style: White, Pink, Baby Blue, Mint, Lavender. One and Only: White, Black, Red Accent, Cream / Spectrum. Surrounding app appearance remains separately selectable as Light/Dark/Follow System. Printed paper, photographs, cork and image processing retain their existing physical colors.
- Rear flash uses one persistent Canvas lightning bolt. Its halo, border, color and off-strike animate reversibly over **300 ms**. The bolt does not disappear during transitions or rapid reversals. Accepted toggles emit one restrained haptic click, obeying global haptics independently of sound. Unsupported/busy controls remain disabled; toggling consumes no film. Flash selection now persists.
- Settings now offers a default handwriting font using all three supplied fonts. New notes initialize from that preference; existing captions keep their own font. Add/edit/remove caption rendering, archive, export and sharing remain non-destructive.
- Existing Check2GO logo, physical-shake-only development/haptics, Off/5/15/30-second timer, four-shot booth with grid/strip outputs, source retention, one ejection, camera switching, selfie illumination, archive, corkboard and secure sharing remain in place. See the preceding [0.4.0 report](UPDATE_0_4_VALIDATION.md) for feature details and timing limits.
- Room remains schema **3**. Existing registered 1→2→3 migrations and all user-data tables are unchanged. Style preferences do not mutate cartridge counts, captures, development progress or photo metadata.

## Modified components

| Component/files | Changes |
|---|---|
| `core/CameraStyle.kt`, `Film.kt` | Style/color model, flash gating/duration, control feedback hook |
| `app/data/Preferences.kt` | Persisted normalized style/color, rear flash and default font |
| `app/ui/CameraStyling.kt`, `FlashToggle.kt`, `Design.kt` | Procedural shells/housings/shutters, contrast-aware materials, live picker, reversible bolt animation, app accents/shapes |
| `CameraScreen`, `FilmLoadingScreen`, `SettingsScreen`, `MainActivity` | Existing screen integration and lifecycle-aware style observation |
| `RoomViewModel`, `device/Feedback` | Guarded flash preference changes and settings-aware subtle clicks |
| `CaptionEditor`, `DevelopmentScreen` | Default font for new captions, preserved per-photo fonts |
| Build, tests and docs | Version 0.5.0/5, generated version footer, 27 new automated checks, updated architecture/assets/setup |

## Automated verification

Executed commands:

```bash
cd android
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
cd ..
python3 scripts/test_upgrade_helper.py
```

| Suite | Passed | Failed | Skipped |
|---|---:|---:|---:|
| Core film/development/camera rules, including 4 new style/flash checks | 58 | 0 | 0 |
| App camera/lighting/theme/captions/timer/booth/archive/board/exports/migrations regressions | 105 | 0 | 0 |
| New style preferences/upgrade defaults | 5 | 0 | 0 |
| New color/contrast checks | 3 | 0 | 0 |
| New flash control/haptic integration | 5 | 0 | 0 |
| New native-graphics flash motion UI | 5 | 0 | 0 |
| New style/settings/logo/caption-font UI | 5 | 0 | 0 |
| Python upgrade-backup safety | 6 | 0 | 0 |
| **Total** | **192** | **0** | **0** |

Gradle tests, debug app assembly, instrumentation APK compilation and lint completed successfully (exit 0). Lint reports **no issues**. Python tests passed (exit 0). Final whitespace validation passed. Unchanged AGP/Kotlin/Compose test-helper deprecation warnings remain.

Android/JVM: **186** (58 core + 128 app); **27** tests were added. Counts come from final JUnit XML and the executed Python runner, with no failures/errors/skips. Earlier UI test timing/scrolling harness failures were corrected and the complete app suite rerun. Instrumentation APK assembly only compiles those tests; it does not imply connected-device execution.

## Visual and hardware scope

Native Robolectric graphics checks exercise all 13 finishes in light and dark app modes, inspect intermediate flash frames in both directions, verify rapid reversals retain the bolt and fixed bounds, and exercise actual settings style/color selection. The real camera composable is checked with three styles on a 320×600 dp surface for logo/countdown/Cancel visibility. Contrast tests require at least 4.5:1 for body/shutter ink and adapted text accents. Camera preview frames are simulated/absent in this host environment, not physical CameraX output.

Physical-device validation remains necessary for rear/front camera orientation and preview continuity, timed and booth shutter cadence, screen-flash/edge-light illumination and brightness cleanup, actual motor/shutter audio and haptics, shake calibration, OEM lifecycle behavior, and Instagram/Facebook handoff. Device-size/font-scale and accessibility review should include the settings picker and shutter deck. No connected emulator or physical-phone tests were run for this update.

The product reference images inspired broad materials/shapes/colors only. All new visuals are drawn in Compose; no product photograph or copied camera logo is embedded. There are no new dependencies or permissions. The app retains three provided handwriting fonts and Check2GO branding from 0.3.0.

Canonical debug builds retain the 0.2.0/0.3.0/0.4.0 signing certificate for normal in-place upgrades. The original 0.1.0 signing exception still applies; follow [upgrade guidance](UPGRADE_0_2.md) before replacing a populated original installation.

## Delivered APK

| Property | Verified value |
|---|---|
| Local artifact | `deliverables/InstantRoom-0.5.0-debug.apk` |
| Size | 17,654,162 bytes |
| SHA-256 | `d01b3bbfd4d5c8ff7c3e41075926c3a9065dc5b52be7543520eb04b0f3215927` |
| Package/version | `com.instantroom`, 0.5.0, code 5; minimum API 26, target/compile 37 |
| Certificate SHA-256 | `57e0508a35dcb3f3ae4f648fcff78bf71d006fa370c821aa69bfb44245c234ab` |
| Signature | `apksigner verify --print-certs` exit 0; matches retained 0.2–0.4 certificate |
| Resources | Three supplied fonts, Check2GO logo and countdown sound verified inside APK |

[Download delivered APK](https://chatgpt.com/library?file_id=libfile_b0e9bf74a69881918e6678a7a3f313e5&account_id=personal).

## Source and delivery status

Built application source: **`7aa97026a7503692c0d271e8d5ac43f08ba82131`**. The follow-up report commit changes documentation only. The committed source snapshot was verified to contain this revision and to exclude supplied attachment/reference directories. All 26 changed files were reviewed; tracked implementation changes match the published feature branch.

[Draft PR #5](https://github.com/wisheradam/Polaroid-android/pull/5) targets the working 0.4.0 branch and remains unmerged. No production release was published.

The additional independent AWS build was **not started/verified**. Automatic approval review rejected the source-archive upload to `instant-room-build-artifacts-ig3j4gz8ktsl` because the current request authorized builds and GitHub delivery but did not explicitly authorize sending the source payload to that AWS destination. No cloud result is claimed for 0.5.0; the delivered artifact and all results above are from the verified local build. Explicit approval is needed before retrying that upload/build. This does not block using the delivered APK.
