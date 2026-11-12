# Polaroid-android

**Instant Room 0.5.0** is a native Android instant-film camera powered by Check2GO. It uses a real CameraX preview and capture, ten-shot manually replaced cartridges, physical shake development, a persistent archive, gallery export, and a corkboard with movable/pinned photographs.

Open [`android/`](android/) in Android Studio. See the [setup guide](android/README.md), [architecture](android/docs/ARCHITECTURE.md), [validation report](android/docs/VALIDATION.md), and [AWS build infrastructure](infra/aws/README.md).

The **Design System 1.0 integration** replaces the One and Only camera surface with the approved analog composition and adds shared tokens, Inter typography, native controls and a dedicated appearance screen. See the [design handoff](android/docs/design-system/README.md) and [verification](android/docs/design-system/verification.md). Native-device visual acceptance is pending; this is not a new APK release.

The latest update adds **Old Fasion**, **Insta Style** and **One and Only** camera bodies with 13 color finishes, live settings previews, a persistent 300 ms flash-toggle animation and a default handwriting preference. See [0.5.0 validation](android/docs/UPDATE_0_5_VALIDATION.md).

The previous update adds a persistent Off/5/15/30-second self-timer and automatic four-shot Photo Booth with 2×2 collage or 1×4 strip output. Booth prints require shaking to develop, support handwritten captions, and integrate with archive, corkboard, export and sharing. Original full-resolution booth JPEGs are retained for reprinting. See [0.4.0 validation](android/docs/UPDATE_0_4_VALIDATION.md).

The supplied Check2GO branding, shake-development haptics and three-font caption editor from 0.3.0 remain available. See [0.3.0 validation](android/docs/UPDATE_0_3_VALIDATION.md).

Front/rear switching, screen/edge selfie lighting, Light/Dark/System themes and Instagram/Facebook sharing from 0.2.0 are preserved. See [earlier validation](android/docs/VNEXT_VALIDATION.md) and [safe upgrade instructions](android/docs/UPGRADE_0_2.md).

The APK has no mandatory network dependency or account system. AWS CodeBuild, private S3 artifacts, and CloudWatch provide the on-demand build pipeline.
