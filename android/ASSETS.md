# Original assets

All four PCM WAV files under `app/src/main/res/raw` were synthesized for this project using the deterministic source in `scripts/generate_audio.py`:

- `charge.wav`: 650 ms rising capacitor chirp; playback rate tracks the selected charging duration.
- `shutter.wav`: 140 ms percussive shutter mechanism.
- `eject.wav`: 1880 ms motor/roller sound, including a final click beginning at 1800 ms.
- `pin.wav`: 80 ms pin insertion transient.

To the extent copyright applies, these generated sounds and the synthesis script are dedicated to the public domain under CC0 1.0: https://creativecommons.org/publicdomain/zero/1.0/

Camera styling, icon, paper, cork noise, film grain, pin shapes, and chemical-development masks are original code-generated visuals. No proprietary logos or camera photographs are included. Android/Jetpack/Kotlin and other dependencies retain their upstream licenses.
