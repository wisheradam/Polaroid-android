# InstantRoom camera skins — shared contract and delivery status

Design handoff, 11 November 2026. Runtime inspected at `c484c6ff7bfeb0782d84b6922ce9e2bcc5de594f` on `feature/android-camera-styles-flash`.

## Three families, one camera workflow

| Display name | Saved style ID | Valid finish IDs | Current delivery status |
| --- | --- | --- | --- |
| Old Fashion | `OLD_FASION` | `BLACK`, `IVORY`, `OLIVE`, `BRASS` | New native-editable Figma design and handoff pending design approval; runtime still uses the previous generic Old Fasion geometry |
| Insta Style | `INSTA_STYLE` | `WHITE`, `PINK`, `BLUE`, `MINT`, `LAVENDER` | Existing Android geometry and preview; a new detailed high-fidelity design system has not been produced or approved |
| One and Only | `ONE_AND_ONLY` | `WHITE`, `BLACK`, `RED`, `SPECTRUM` | New native Compose surface integrated by merged PR #6 into the development branch; device acceptance still pending |

There are 13 valid family/finish combinations and 11 distinct color enum values. These are not 13 colors per skin. Changing the public spelling to Old Fashion must preserve `OLD_FASION` in saved settings.

## Existing switching path

**Camera → Settings → Camera design → Camera appearance.**

`CameraStyleSettings` presents a live style preview, three style chips and only the selected family's compatible finishes. A style selection copies the current Settings object, changing `cameraStyle` and resolving `cameraColor` with `style.colorOrDefault(currentColor)`. A finish selection changes only `cameraColor`. Preferences persist these existing enum names.

If the selected color belongs to the destination family, retain it. Otherwise use that family's default: Old Fashion Black, Insta Style White, One and Only White. Example: Old Fashion/Olive → Insta Style/White. One and Only/Black → Old Fashion/Black retains Black. The current implementation stores a single current finish; it does not remember a separate last-used finish for each family. Per-family finish memory would be a separate backward-compatible settings enhancement, not an existing feature.

Only the presentation changes. The same camera controller, preview surface, capture coordinator, cartridge, exposure ledger, archive and stored photographs are shared. A new skin must not create an independent capture pipeline or reload a cartridge.

## Independent state domains

| User choice | Owns | Must not change |
| --- | --- | --- |
| Camera family | Shell geometry, control placement, material language | Lens selection, timer, cartridge, development, saved photo pixels |
| Physical finish | Compatible body/trim colors and explicit per-style shutter treatment | App theme, film format, photographic processing |
| Light / Dark / System | App semantic surfaces, typography contrast, native sheets/dialogs | Physical finish |
| Film format and treatment | Print geometry and photographic processing | Camera skin or app theme |
| Caption font | Caption presentation | Style ID, finish, film count, original photo |

Token resolution: primitives → app semantics; camera family → physical material roles → compatible finish. Film presentation is a separate branch. Preserve existing semantic application tokens when adding Old Fashion; proposed Old Fashion decorative colors are scoped to that family. Do not globally replace the approved One and Only palette with its gold/charcoal values.

## Implementation contract for the new Old Fashion surface

Inject the existing live preview and existing callbacks into a native Old Fashion presentation, as the One and Only integration already does. Decorative sensor mesh and right optical module do not introduce new hardware capabilities. Preserve correct preview crop, rotation and front mirroring. The public label correction must update visible labels/accessibility strings and affected assertions while preserving persisted enum names.

Capture, ejection and shake development remain driven by the existing domain state. The proposed UI should prevent committing a style change during capture/charging/ejection or incomplete cartridge loading, and retain the requested choice until idle or offer an explicit cancel/finish route. This is a design requirement for the integration; this documentation PR does not implement or verify that gate. Settings alone must never stop a capture silently or spend film.

The Old Fashion Figma selector uses a confirmation action. The current Android appearance screen applies choices immediately. Integration must resolve this explicitly: retain immediate persistent updates with matching preview, or add a real staged preview/Apply/Cancel model that commits only on Apply and restores the original on Cancel. Do not show a decorative Apply button while mutating preferences before confirmation. The two states are not equivalent.

## Three-skin acceptance before a new APK

Verify all three entries navigate to the correct native surface; all 13 valid family/finish combinations survive restart; unsupported pairs normalize to the destination default; physical colors remain constant in Light/Dark/System; existing photos, cartridge phase/count, timer, caption font and flash preferences remain unchanged. Verify switching while busy/loading follows the selected lifecycle policy. Keep front/rear preview, selfie lighting, Photo Booth, archive/corkboard and sharing available from every skin. Exercise compact layouts, large text, TalkBack, reduced motion and lifecycle recovery.

No native Old Fashion implementation, redesigned Insta Style, APK build or new test pass is claimed by this design-only PR. Existing One and Only verification remains recorded separately in [verification.md](verification.md).

## Design sources

- [One and Only Figma](https://www.figma.com/design/gONY1ZYQbqtWYaf7S5X5HR) and [existing Android handoff](README.md).
- [Old Fashion Figma/main](https://www.figma.com/design/NxO84Y9jjY9AyBlFMeFvpC?node-id=7-4), [review prototype](https://www.figma.com/proto/NxO84Y9jjY9AyBlFMeFvpC/InstantRoom-Old-Fashion?node-id=16-4660&starting-point-node-id=16-4660), and [handoff](old-fashion/README.md).
- Insta Style's existing sources: `android/core/src/main/kotlin/com/instantroom/core/CameraStyle.kt`, `CameraStyling.kt`, `CameraScreen.kt` and `CameraAppearanceScreen.kt` under `android/app/src/main/java/com/instantroom/ui/`.
