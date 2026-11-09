#!/usr/bin/env bash
set -euo pipefail
sdk_path="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_HOME="$sdk_path"
export ANDROID_SDK_ROOT="$sdk_path"
mkdir -p "$sdk_path/cmdline-tools"
if [[ ! -x "$sdk_path/cmdline-tools/latest/bin/sdkmanager" ]]; then
  curl --fail --location --retry 3 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip --output /tmp/android-tools.zip
  unzip -q /tmp/android-tools.zip -d /tmp/instant-room-sdk
  mv /tmp/instant-room-sdk/cmdline-tools "$sdk_path/cmdline-tools/latest"
fi
# yes receives SIGPIPE after sdkmanager finishes reading; retain sdkmanager's exit status.
set +o pipefail
yes | "$sdk_path/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$sdk_path" --licenses >/dev/null
license_status=${PIPESTATUS[1]}
set -o pipefail
test "$license_status" = 0
"$sdk_path/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$sdk_path" 'platforms;android-37.0' 'build-tools;37.0.0' 'platform-tools'
printf 'sdk.dir=%s\n' "$sdk_path" > "$CODEBUILD_SRC_DIR/android/local.properties"
