# Updating to 0.2.0 without losing the archive

The Room database remains **schema version 1**, with the same application ID and file locations. No destructive migration, archive reset, or automatic uninstall was added. On a normal same-certificate Android update, cartridges, remaining film, originals, processed photos, development progress, board pins and settings stay in place. The old always-dark appearance is preserved unless you choose another theme.

## Important signing constraint for the first supplied APK

The supplied 0.1.0 APK was signed with a temporary CodeBuild-generated debug key. That private key was not retained and cannot be reconstructed from its public APK. Its certificate SHA-256 is `2dbe8d2ebcbb668367337138b721147732c2e85874d48a8ce9799974e1f2cc44`.

The 0.2.0 build uses a retained debug key, stored encrypted in AWS Systems Manager Parameter Store and injected into CodeBuild at runtime. Future canonical debug APKs reuse that key. It is never committed or included in downloadable artifacts. The certificate SHA-256 is `57e0508a35dcb3f3ae4f648fcff78bf71d006fa370c821aa69bfb44245c234ab`.

Because the certificates differ, Android will reject an ordinary in-place update from the originally supplied 0.1.0 APK. **Do not uninstall that app manually if it contains photographs.** If you possess the original signing keystore from a different local build, build with that keystore for a normal update instead.

## Back up first, then migrate the old debug install

Use a computer with Python 3, Android Platform Tools (`adb`), JDK and `apksigner` from Android Build Tools. Enable USB debugging on the Android phone, unlock it and authorize that computer. The helper is intended only for the debuggable Instant Room APK; it refuses non-debuggable installs. No data leaves your computer/phone for this operation.

From the repository root, run **without the migration flag first**:

```bash
python3 scripts/upgrade-debug-preserve-data.py --apk /path/to/InstantRoom-0.2.0-debug.apk --backup-dir /path/to/safe-backups --apksigner /path/to/Android/Sdk/build-tools/37.0.0/apksigner
```

This copies the old APK, force-stops the application for a consistent database/WAL snapshot, and creates and validates a local TAR backup of `files`, `databases` and `shared_prefs`, with SHA-256 hashes of every regular file. If certificates differ, it exits **before uninstalling anything**. Keep that backup somewhere durable. Use `--serial DEVICE_ID` when multiple devices are connected.

After verifying that backup exists and reviewing the helper, run the same command with **`--allow-signature-migration`**. That explicitly permits uninstalling the old signature, installing the new APK, restoring all selected private data using `run-as` under its new UID, and comparing every restored file with the verified backup before you open the app. It retains both backup and prior APK. On failure after uninstall it attempts to reinstall the prior APK and restore the saved data; if the device disconnects or recovery fails, the local backup remains available for manual recovery.

The helper's archive/path/ownership safety functions are covered by automated Python tests. **The USB migration procedure has not been executed on a physical phone in this environment.** Do not treat it as a substitute for keeping a verified backup. You may need to grant camera permission again after a signature migration. A fresh installation needs no migration.
