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

## Original camera styles (0.5.0)

Old Fasion, Insta Style and One and Only use original Compose geometry, textures, housing details and color palettes. The three user-supplied product reference photographs were inspected for broad material/shape/color inspiration only. They are not bundled, traced or used as UI assets. The Cream / Spectrum variant uses an original four-color detail.

## Design System 1.0 additions

The interface uses the original Inter variable font from the Inter project:
https://github.com/rsms/inter/blob/master/docs/font-files/InterVariable.ttf
Copyright (c) 2016 The Inter Project Authors. SIL Open Font License 1.1 is
retained in `app/src/main/assets/licenses/Inter-OFL.txt`. The font is bundled
unchanged; interface weights select its original variable weight axis. Caption
fonts and their existing provenance are unchanged.

The six `ir_*` icon exports and the procedural One and Only
body/lens/flash/roller drawings are original project assets based on the
approved InstantRoom design. No reference-camera photos or
generated park examples from the Figma review are bundled in the application.

The six One and Only icons are exported from the original design-system SVGs at 4× (96 px for a 24 dp slot) into `drawable-xxxhdpi`. Editable SVG masters live in `docs/design-system/icons`. Runtime tint follows semantic camera ink; no network asset URLs are used.

One and Only uses the Check2GO artwork without a plate on light physical finishes, and retains the warm plate on the Black finish for legibility. Its main camera uses the approved five-band spectrum; the original 0.5.0 generic cartridge-shell detail remains unchanged.

## Old Fashion native implementation

The runtime Check2GO logo is the unchanged supplied artwork. The six `of_*`
VectorDrawables retain the original handoff SVG paths, round 1.7-unit strokes
and native tinting. The original 128 px matte-plastic tile is copied unchanged
to `drawable-nodpi/of_matte_tile.png`; it is loaded once and repeated by a cached
shader. `python3 android/scripts/generate_old_fashion_icons.py --check` verifies
all eight runtime assets against their preserved handoff sources.

Camera surfaces, flash Fresnel lines, gold mesh, optical recesses, shutter, roller
and ejection lip are native Compose drawings. No camera/product photograph or
preview test double is bundled as a production preview. Physical finish colors
are separate from the app's Light/Dark/System appearance.

## Insta Style native review implementation

The seven `is_*` vectors preserve the original supplied SVG icon geometry,
including the 36 dp shutter glyph. Its ivory circle is an even-odd cutout
against the ivory shutter face, allowing the original outline to take each
finish's accent. `scripts/generate_insta_style_assets.py --check` checks the
handoff asset hashes, all native vector reproductions and the unchanged logo.

Shell, satin bevels, circular lens, Fresnel glass, shutter and output roller are
native Compose drawing/layout. No reference photograph, generated selfie,
manufacturer branding or flattened review screen is a runtime camera asset.
No new font is added. Existing Inter and Sol Kol licenses are preserved;
Denistina/Historic redistribution rights remain unresolved for public release.
