# InstantRoom — Design System 1.0 / Android integration

Design source: [editable Figma system](https://www.figma.com/design/gONY1ZYQbqtWYaf7S5X5HR).
[Interaction review](https://www.figma.com/proto/gONY1ZYQbqtWYaf7S5X5HR/InstantRoom?node-id=36-3190&starting-point-node-id=36-3190).
Baseline: InstantRoom 0.5.0, `7cc939015071ecf266ad39478d6c1cce8a5f3423`.

The product owner authorized Android integration after reviewing the design. Native visual acceptance and hardware acceptance are pending. This change does not constitute a new release. Version code/name stay at 5/0.5.0.

## Camera families and new design reviews

See the [three-skin switching contract and delivery matrix](camera-skins.md) and the [Old Fashion Design System 1.0 review](old-fashion/README.md). The new Old Fashion surface is Pending Design Approval and is not implemented by that documentation change. Insta Style retains its existing runtime geometry. One and Only's implemented behavior is documented below.

## Implemented surface

One and Only uses a cream physical shell, circular live preview, concentric lens ribs, red mechanical shutter, upper-right fluted flash window, counter and timer on the left, flash and camera switch on the right, five-band spectrum, dark film compartment, roller slot and three physical navigation controls. The supplied Check2GO logo is reused unchanged. No camera-manufacturer logo or reference photograph is shipped.

The circular opening contains the existing CameraX PreviewView in COMPATIBLE/FILL_CENTER mode. It retains CameraX orientation and front-camera handling. The white guide is a visual centering aid, not a promise that the entire saved image equals the circular visible area. Film formats continue to own the saved photograph's geometry. Device verification of crop and mirroring remains required.

Gallery and Settings navigate directly. Photo Booth opens a sheet with 2×2/strip choices and single/booth mode selection. The counter opens manual film loading. Timer opens Off/5/15/30 presets. Front-camera lighting has its own sheet. Archive exposes Corkboard. Settings exposes the dedicated Camera Design screen with immediate physical-finish preview. Existing caption, sharing, corkboard arrangement and image-processing flows remain in place under the shared palette and typography.

## Tokens and mapping

`tokens.json` is the approved design-token export; `finishes.json` records the original repository mapping. Runtime semantics live in `Design.kt`, physical controls in `AnalogTokens.kt`, and finish resolution remains in `CameraStyling.kt`. One and Only uses the approved style-level shutter red `#E32A1C` across its physical finishes, as in Figma. Original per-finish shutter/accent values remain recorded and unchanged for existing controls; body mappings are used exactly. This is an explicit style treatment, not a replacement of stored finish names or enum mappings.

The 13 supported family/finish combinations and existing enum values are preserved, including the product label “Old Fasion”. Future families retain their established layouts.

Resolution order: global primitives → application semantic theme → camera family → physical finish → film presentation. One and Only's physical finish remains identical under Light/Dark/System. A finish change never writes film-format, treatment, image-processing or photo-pixel values. Supporting screens and sheets use semantic Material colors. Runtime camera preview photography always comes from the device.

Inter Variable is bundled unmodified under OFL 1.1. See `ASSETS.md` and the shipped license. Existing caption fonts are retained; Denistina and Historic redistribution rights still need confirmation before a public release. No newly inferred license is assigned to them.

## Geometry and safe areas

All geometry is dp, text is sp. Scaffold supplies status/navigation/cutout insets. The housing uses a 12 dp exterior inset. `AnalogLayout.fit` consumes the available size after those insets, the 24 dp outer padding and a 32 dp allowance for the film-status caption. Body height is at least 620 dp, or 1100 dp above 1.4× font scale; shorter viewports scroll. Upper shell is 62.5% of body height clamped to 360–520 dp. Lens diameter is min(68% of body width, 56% of upper shell height); aperture is 75.2% of lens diameter. No nonuniform image scale is applied.

The lens center is at 50% of body width and 48% of the upper shell height. Brand is x10/y16. Flash window is 76×68, top18/right12. Counter is 66 wide (78 at large text), min48 high, x10/y22% of shell. Timer and flash controls are min48×48 at y36%; flash is inset24 dp (26 for the front-light face), aligned under the flash-window center; above 1.4× font scale they move to y42%. Shutter has an 80×80 target at x8/y62%, moving to y70% for large text. Camera switch is 48×48 at right26/y65%, moving to y73%. Material decorations never receive input.

Navigation uses equal-width columns with min76 dp height. Above 1.4× font scale, the three destinations stack vertically at full width to preserve whole words. Labels grow with font scale. The lower compartment expands through the minimum-body-height fallback; navigation remains reachable by scroll rather than reducing touch targets. Lens/preview are behind side controls; the aperture is circular. The film-ejection layer is above the lower body and begins beneath the slot roller, clipped at the emergence edge. It is constrained by available lower-body height and actual print aspect.

Intentional implementation differences from Figma: native gradients/strokes replace bitmap textures, aggressive reflections and perspective; the shell scales to actual safe space; large text scrolls; original repository timing, sensors, exposure accounting and image generation remain authoritative. Photo Booth is a settings sheet plus the physical main camera rather than a duplicate capture surface.

## Interaction ownership and motion

| Interaction | Native owner and behavior | Visual timing |
| --- | --- | --- |
| Shutter press/release | Compose interaction state, enabled only when capture is accepted | 2 dp depression; 70 ms press, 110 ms release |
| Rear flash toggle | Existing FlashToggle and persisted settings; keeps icon understandable | Existing 300 ms transition |
| Flash charge/fire | Existing capture state, feedback port and actual device flash | Existing settings-controlled charge; hardware firing |
| Timer | Existing capture coordinator; preset changes use no film | Actual 1-second countdown ticks and cancel action |
| Four-shot booth | Existing coordinator; four accepted exposures, five-second intervals | Existing sequence clock |
| Ejection | Existing normalized `CaptureUi.ejection`, motor and haptic sequence | Existing 1800 ms duration; no independent clock |
| Development | Stored photo progress from accepted shake input or explicitly enabled accessibility step | Stops at stored progress; time does not advance development |
| Film loading | Closed → confirmation → Open → Removed → Inserted → successful close/load | Existing 600 ms door/tray animation; exactly10 only after repository success |
| Finish/theme | Existing persistent settings; preview uses actual mapped body colors | Immediate semantic change |
| Navigation/pins | Existing state and feedback actions | Existing behavior; no extra sensor or exposure effects |

Compose animations honor platform animator duration scale. When Android reports animators disabled, ejection shows a static undeveloped print while the original 1800 ms motor sequence completes; the timeline and exposure accounting stay unchanged. Countdown/development state remains functional with animations disabled. Audio/haptics stay governed by the existing settings and feedback port; no additional cue is played by the decorative UI. The flash-window LED indicates selected flash, not an independent measurement of hardware charge readiness.

Manual loading blocks Back after opening until closure. Partial phases use saved instance state. A failed repository load keeps the inserted phase and re-enables the close action for retry. Existing photos and the exposure ledger are not changed by intermediate visual phases.

## Validation and release gates

See `verification.md` for executed checks and rendered evidence. Host rendering cannot validate hardware camera visibility, rotation, mirrored output, flash timing, motor sound, haptic intensity, real shake input, TalkBack traversal, display cutouts or process-death recovery on a device. Verify these on compact Android hardware before approving a release. Test all finish/theme combinations and text sizes, including translated labels. Confirm caption-font distribution rights.

No database schema, camera controller, image-processing algorithm, saved-photo geometry, exposure count or preference enum is changed by this integration. No APK build, merge or deployment is part of this change. Keep the integration PR as a draft until device and product review.
