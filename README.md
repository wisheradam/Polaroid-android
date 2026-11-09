# Polaroid-android

**Instant Room** is a native Android instant-film camera powered by check2go. It uses a real CameraX preview and capture, ten-shot manually replaced cartridges, physical shake development, a persistent archive, gallery export, and a corkboard with movable/pinned photographs.

Open [`android/`](android/) in Android Studio. See the [setup guide](android/README.md), [architecture](android/docs/ARCHITECTURE.md), [validation report](android/docs/VALIDATION.md), and [AWS build infrastructure](infra/aws/README.md).

The APK has no mandatory network dependency or account system. AWS CodeBuild, private S3 artifacts, and CloudWatch provide the on-demand build pipeline.
