# Instant Room 0.3.0 for Android

Native Kotlin/Jetpack Compose instant-film camera. The Android project is self-contained in this directory; no iOS files are involved. Android 8.0 (API 26) or later. All camera, film, development, archive, and corkboard features work locally without an account or internet permission.

## Build and install

Use Android Studio with JDK 21. Install Android SDK Platform **37.0**, Build Tools **37.0.0**, and Platform Tools through SDK Manager. The minimum device API is 26; target SDK is 37. Compile SDK 37 is needed by current stable Compose artifacts. Set `ANDROID_HOME` or create an untracked `local.properties` containing `sdk.dir=/your/Android/Sdk`.

```bash
./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat` and JDK 21. Internet is needed for the initial dependency downloads, never for normal app use. Do not copy environment-specific proxy settings into the project.

For connected emulator/device tests:

```bash
./gradlew :app:connectedDebugAndroidTest
```

The debug APK is locally signed for development. It is not a production/Play Store release. Build an unsigned release with `./gradlew :app:assembleRelease` and supply your own secure signing setup for distribution.

## Update notes

Version **0.3.0 (code 3)** adds the supplied Check2GO header logo, soft haptics on accepted shake-development events, and a **Write on photo** / **Edit caption** entry in completed-photo detail. Enter up to 80 characters on two short lines, choose Sol Kol, Denistina or Historic, preview, and save. **Remove caption** restores the retained uncaptioned print. Caption editing consumes no film and changes neither originals nor development progress. The archive, corkboard, save and share flows use the captioned JPEG.

Room schema **1 → 2** adds caption metadata and a retained base-print reference without resetting any tables. Canonical 0.2.0 → 0.3.0 debug builds use the same retained signing key and support normal `adb install -r` upgrades. The original 0.1.0 certificate constraint described below still applies. Read [0.3.0 validation](docs/UPDATE_0_3_VALIDATION.md) for tests and device checks.

Version 0.2.0 (code 2) adds a main-screen lens switch, screen/edge selfie lighting, Light/Dark/Follow System appearance and Instagram/Facebook image-sharing shortcuts. The database remains schema 1. Read [upgrade instructions](docs/UPGRADE_0_2.md) before replacing a populated 0.1.0 debug installation: the first build's temporary signing certificate differs from the retained 0.2.x debug certificate. Never manually uninstall it without a backup.

Canonical AWS builds reuse an encrypted retained signing key. For a local same-certificate debug build, point `INSTANT_ROOM_DEBUG_KEYSTORE` to the matching keystore before invoking Gradle. If that variable is absent, Android Studio/Gradle uses the developer machine's own debug key.

See the [vNext implementation report](docs/VNEXT_VALIDATION.md) and [physical-device checklist](docs/VNEXT_HARDWARE_CHECKLIST.md). A Stories-only Instagram integration is not configured; supported image-sharing flows and the Sharesheet are available without Meta SDK/OAuth.

## First photograph

1. Open **Camera**, grant camera permission, and tap **Load film**.
2. Open the film door, remove the old/placeholder cartridge, select format and appearance, insert a cartridge, and close the door. Each new cartridge contains exactly ten exposures.
3. Press the large shutter. Charging sound and subtle feedback start here, followed by capture, processing, shutter feedback, and roller ejection.
4. On the development screen, shake with deliberate back-and-forth motions. Resting pauses development. Return through Archive whenever you want to continue.
5. Once developed, save to Gallery, share through Android, or place the photograph on the corkboard.
6. On the corkboard, drag an unpinned photograph or use two fingers to rotate it. Choose a pin color, then tap the print to insert a pin. Remove the pin before moving it. Pinch/pan the board to explore.

Sound and haptics are independently switchable. Settings also include front/rear camera, charging duration, shake threshold, smaller processed output, and an explicitly enabled accessibility development button. A missing sensor never silently completes development. Real instant film should not be shaken; this is a game mechanic.

## AWS

The repository root's `infra/aws/` contains a deployed on-demand CodeBuild/S3/CloudWatch infrastructure definition. It builds an immutable source snapshot and stores debug APKs privately. The mobile application does not upload personal photographs. See [AWS instructions](../infra/aws/README.md).

## Structure

- `core/`: Android-independent film, development, gesture rules, and state definitions, with JVM unit tests.
- `app/.../data/`: Room entities, transactional repository, capture recovery journal, preferences.
- `app/.../device/`: CameraX adapter, motion sensor adapter, SoundPool and VibrationEffect.
- `app/.../imaging/`: orientation correction, center crop, film treatment, frame rendering, MediaStore and Android sharing.
- `app/.../ui/`: MVVM state, six screens, tactile design system, development drawing and board gestures.
- `docs/`: architecture, format references, validation report, and hardware acceptance checklist.
- `scripts/generate_audio.py`: deterministic original PCM sound synthesis.

## Assets and privacy

All sounds are originally synthesized and dedicated to the public domain under CC0; see `ASSETS.md`. The cork texture, pushpins, film frames, camera body, and chemical masks are drawn procedurally. No Polaroid logos, third-party recordings, or borrowed product photographs are bundled. Product names identify format references only.

Room metadata and JPEG files are private internal app data. Gallery saves and shares require a fully developed photo. Uninstalling the app deletes its private archive; export keeps a separate gallery copy. Android backup is disabled to avoid silently sending photographs to device backup services. There are no analytics, subscriptions, ads, or cloud SDK credentials in the APK.
