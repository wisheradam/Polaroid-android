# Android integration verification — 11 November 2026

Status: implementation review / native-device acceptance pending. These are Compose host renders, not device captures. Their green circle is an explicit preview test double. The production component receives the live CameraX PreviewView; no sample photograph is bundled in the app.

## Executed checks

| Check | Result | Scope |
| --- | --- | --- |
| `:core:test` | 61 passed, zero failed/skipped | Includes adaptive geometry plus the existing cartridge, shake, timing and capture rules |
| `:app:testDebugUnitTest` | 133 passed, zero failed/skipped | Full regression, including repositories, recovery, migration, booth, captions, sharing, styles, settings and the new camera/loading UI tests |
| Python upgrade-helper tests | 6 passed | `python -m unittest discover -s scripts -p 'test_*.py'` |
| Final affected UI rerun | 14 passed, zero failed/skipped | AnalogCameraUiTest, CameraStyleUiTest, BoothUiTest after the final permission-copy, control-alignment and label-metric edits |
| `:app:lintDebug` | No issues found | Repeated after the final edits |
| `git diff --check` | Clean | Source and handoff files |

The full 200-test regression preceded the final small camera layout/copy edits. The final 14-test rerun and lint cover that final source. Do not sum the rerun into the number of unique tests. There is no GitHub Actions workflow in the baseline repository; these are local verification results, not CI claims.

Gradle checks compiled application sources and test resources. No assemble/APK task, AWS build, merge or deployment was run. Version name/code remain 0.5.0/5.

Reproduce with a full JDK21 and the repository's Android SDK37/build-tools37 requirements:

```sh
cd android
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug
```

Final focused rerun:

```sh
./gradlew :app:testDebugUnitTest \
  --tests com.instantroom.AnalogCameraUiTest \
  --tests com.instantroom.CameraStyleUiTest \
  --tests com.instantroom.BoothUiTest \
  :app:lintDebug
```

## Visual review

The five renders below were inspected. Test dimensions are the component viewport in logical dp at mdpi; they omit OS bars. On a device, Scaffold supplies actual status/navigation/cutout insets before the layout calculation. A Figma phone frame includes illustrated system bars and therefore has a different screen-coordinate origin.

| Render | Observation |
| --- | --- |
| [360 × 800](screenshots/camera-360x800-1.0.png) | Circular aperture, all three navigation controls and film status fit at normal text size |
| [393 × 852](screenshots/camera-393x852-1.0.png) | Main composition keeps brand/flash above, shutter left, controls right, spectrum and lower slot |
| [412 × 915](screenshots/camera-412x915-1.0.png) | Lens grows uniformly; touch controls retain their dimensions |
| [360 × 800 / 200% text](screenshots/camera-360x800-2.0.png) | Counter uses a short LEFT/OUT label; side controls remain separated; content scrolls |
| [200% text / navigation](screenshots/camera-large-text-navigation.png) | Full-width vertical navigation exposes whole labels and full touch targets |

Reference: [Figma One and Only / White](https://www.figma.com/design/gONY1ZYQbqtWYaf7S5X5HR?node-id=18-685). The render fixture uses the source-authoritative Cream / Spectrum body. The hierarchy and relative control arrangement match the approved composition. Deliberate native adaptations: actual safe areas, dynamic layout, 48 dp or larger targets, 12 sp counter labels, original SVG icon exports, live preview injection, native gradients and state-driven motion. A warm logo plate is retained only on the Black physical finish. The photographic sample and illustrated OS chrome in Figma are not implementation assets.

## Native-device gates

Review actual camera output and circular preview crop, front mirroring and rotation, texture-view masking, flash/charge timing, selfie light at maximum edge width with large text, exposure accounting through lifecycle interruptions, physical shake acceptance and stopped progress, audio/haptics, TalkBack focus order, hardware Back during loading, process-death recovery, display cutouts/gesture areas, all finish/theme combinations and caption/keyboard layouts. Host tests cannot substitute for these checks.

Denistina and Historic redistribution rights remain unverified in the inherited assets. Confirm them before public release. Inter and Sol Kol retain their supplied OFL licenses. Old Fasion and Insta Style geometry remains the existing implementation; new appearance design for those families is deferred.
