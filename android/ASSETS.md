# Original assets

All five PCM WAV files under `app/src/main/res/raw` were synthesized for this project using the deterministic source in `scripts/generate_audio.py`:

- `charge.wav`: 650 ms rising capacitor chirp; playback rate tracks the selected charging duration.
- `shutter.wav`: 140 ms percussive shutter mechanism.
- `eject.wav`: 1880 ms motor/roller sound, including a final click beginning at 1800 ms.
- `pin.wav`: 80 ms pin insertion transient.
- `countdown.wav`: 70 ms restrained 660 Hz tick; the last three seconds use a higher playback pitch. Charging/shutter/countdown sounds share exclusive playback to prevent overlap.

To the extent copyright applies, these generated sounds and the synthesis script are dedicated to the public domain under CC0 1.0: https://creativecommons.org/publicdomain/zero/1.0/

Camera styling, icon, paper, cork noise, film grain, pin shapes, and chemical-development masks are original code-generated visuals. Android/Jetpack/Kotlin and other dependencies retain their upstream licenses.

## User-supplied branding and handwriting (0.3.0)

The Check2GO logo was supplied for this application. `check2go_logo.png` preserves its colors and proportions; transparent outer padding is trimmed and the bitmap is resized for the header. It is displayed on a small warm paper plate in both themes. The logo is not covered by the CC0 audio dedication above.

All three caption fonts are supplied by the user and embedded unmodified: **Sol Kol**, **Denistina**, and **HISTORIC**. Sol Kol includes the SIL Open Font License 1.1, retained in `app/src/main/assets/licenses/Sol-Kol-OFL.txt`. HISTORIC's embedded copyright reads “Copyright (c) 2024, JGG ARTS”; its attachment and the Denistina attachment contain no separate license. They are used as requested in this project; redistribution rights beyond that instruction have not been independently verified. These fonts are not original project assets or CC0.
