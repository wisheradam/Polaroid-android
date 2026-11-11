> Repository excerpt: specifications and selected review assets are tracked here. The complete reference and motion boards remain in the linked native Figma file. The delivery-inventory paragraph below describes the broader review package.

# InstantRoom — Old Fashion Design System 1.0

**Old Fashion Design System 1.0 — Pending Design Approval**

Review proposal ·11 November2026 · Check2GO · Android and iOS.

[Editable Figma](https://www.figma.com/design/NxO84Y9jjY9AyBlFMeFvpC) · [Review prototype](https://www.figma.com/proto/NxO84Y9jjY9AyBlFMeFvpC/InstantRoom-Old-Fashion?node-id=16-4660&starting-point-node-id=16-4660)

The main screen reconstructs the supplied black-camera concept. Structure, controls, text, optical recesses and perforated mesh are native editable nodes. Only the authentic logo, illustrative preview and shared microtexture are image fills. The original conceptual screen appears only on reference/comparison boards. This is a design deliverable; production Android sources are unchanged.

## Reference decisions

Retained: raised flash bar, folding support, gold mesh on left, rectangular center preview, right optical element, edge shutter, counter10, timer/switch/flash row, lower InstantRoom plate, ejection slot, three mechanical navigation buttons. Original Check2GO PNG is blue/teal, so the proposal uses those authentic colors rather than reproducing the concept's cream brand lettering. An original INSTANT10 badge replaces the model number and unsupported autofocus claim. The shutter's48×80 target is inset inside the screen; its visible red cap remains narrow and mechanical. Gold mesh is circular and decorative. Native grain is finer and lighting less photographic than the reference, to preserve editability and practical native rendering. The ivory rear insert is a fixed physical finish detail. Extra device height expands the lower housing and safe areas.

## Editable system

11 organized Figma pages.106 component masters in total,13 variant sets.72 camera interaction variants;6 print formats;4 reusable application recovery states. Labels on app buttons/rows/chips use native component text properties. Semantic colors and physical body/trim values are variable-bound; bevel and optical gradients are decorative fixed paint recipes. Geometry/motion tokens and text/effect styles are reusable. Not every numerical decorative vertex is a variable.

18 secondary screen specimens cover manual loading phases, replacement warning, development/finished photo, Photo Booth, archive, corkboard, caption editing/keyboard, settings, style selection and four physical finishes. A separate Light settings specimen validates semantic appearance. Four camera root layouts:360×800,393×852,412×915 and402×874. Review prototype has73 navigation edges; capture, sharing, timer choices and file persistence are illustrative rather than native device functionality.

## Baseline and persistence

Reference InstantRoom0.5.0/code5, commit7cc939015071ecf266ad39478d6c1cce8a5f3423. Display label **Old Fashion** maps to saved enum **OLD_FASION** and previous label **Old Fasion**. Keep cameraStyle/cameraColor preferences unchanged. Four Old Fashion finishes are authoritative; source lists13 valid style/color combinations across3families and11 unique color enum values. Do not invent13 Old Fashion colors.

|Stored color|Existing display name|Body|Trim|Existing shutter|
|---|---|---|---|---|
|BLACK|Black|#252925|#C6A56C|#B34D36|
|IVORY|Ivory|#E9DFCA|#735628|#953F30|
|OLIVE|Dark Olive|#3B4638|#D7B97B|#BF5E43|
|BRASS|Brown / Brass|#6A4936|#E3BF7D|#A64335|

Proposed reference-led shutter red #E63725 is an explicit design override, not a silent rewrite of stored finish data. Application Light/Dark/System, physical finish, film geometry and photographic processing remain separate state domains. Existing film/caption/export settings survive style changes. Other style families retain their existing implementations; no unrelated future designs are introduced.

## Colors and materials

Color JSON carries every primitive in HEX and0–255 RGBA, role aliases, modes and native variable IDs.

|Primitive|HEX|RGBA|
|---|---|---|
|black|#252925|[37, 41, 37, 1]|
|charcoal|#333630|[51, 54, 48, 1]|
|optical|#070907|[7, 9, 7, 1]|
|gray|#71766B|[113, 118, 107, 1]|
|gold|#C6A56C|[198, 165, 108, 1]|
|champagne|#D8C9A5|[216, 201, 165, 1]|
|ivory|#F2E8CF|[242, 232, 207, 1]|
|paper|#FFF9EF|[255, 249, 239, 1]|
|red|#E63725|[230, 55, 37, 1]|
|flash|#FFE0A0|[255, 224, 160, 1]|
|green|#A9DFAD|[169, 223, 173, 1]|
|focus|#8ADDEB|[138, 221, 235, 1]|
|error|#FFB4AB|[255, 180, 171, 1]|
|warning|#FFD48A|[255, 212, 138, 1]|
|ink|#242526|[36, 37, 38, 1]|
|muted|#B7B9AD|[183, 185, 173, 1]|
|lightMuted|#5D5A55|[93, 90, 85, 1]|
|blue|#0064BA|[0, 100, 186, 1]|
|teal|#00A89D|[0, 168, 157, 1]|
|cyan|#159EC0|[21, 158, 192, 1]|
|leaf|#55953F|[85, 149, 63, 1]|
|yellow|#F1C837|[241, 200, 55, 1]|
|orange|#F39A26|[243, 154, 38, 1]|
|rose|#D93757|[217, 55, 87, 1]|
|white|#FFFFFF|[255, 255, 255, 1]|
|olive|#3B4638|[59, 70, 56, 1]|
|brass|#6A4936|[106, 73, 54, 1]|
|oldIvory|#E9DFCA|[233, 223, 202, 1]|
|ivoryTrim|#735628|[115, 86, 40, 1]|
|oliveTrim|#D7B97B|[215, 185, 123, 1]|
|brassTrim|#E3BF7D|[227, 191, 125, 1]|
|lightFocus|#006B7D|[0, 107, 125, 1]|
|lightError|#B3261E|[179, 38, 30, 1]|
|lightWarning|#805400|[128, 84, 0, 1]|

Native material recipes are in materials.json and Figma page02: matte ABS, low-sheen mechanical plastic, ribbed plastic, perforated gold, champagne trim, optical glass, Fresnel flash lens, cartridge compartment, paper, rubberized buttons and cork. Fixed light direction upper-left. Shared shadow recipes: Raised y3/blur5/black60%; Pressed y1/blur2/black40%; Housing y6/blur12/black70%; Inset y2/blur3/black80%. Render128px microtexture at42% Soft Light, cache mesh paths; do not bake the camera screen into a texture. Native gold holes/ribs must align to pixels and remain decorative.

## Typography and assets

|Role|Font|Size/line dp or pt|
|---|---|---|
|Display|Inter Bold|36/42|
|Title|Inter Bold|24/30|
|Body|Inter Regular|14/20|
|Label|Inter Bold|12/16|
|Micro|Inter Medium|10/14|
|Counter|Inter Bold|26/30|
|Wordmark|Inter Bold Italic|24/30|

Inter was available in Figma; its official project license is SIL OFL1.1: https://github.com/rsms/inter/blob/master/LICENSE.txt . Inter font binaries are not bundled. Exact Sol Kol outlined caption proof was generated from the supplied font, with its companion OFL retained; no font binaries are redistributed. Denistina and Historic remain named existing runtime choices, but their redistribution rights are unverified. Obtain the original licensed binaries before adding them to a new platform package. Preserve existing caption-font IDs.

Logo: original1200×209 transparent PNG, shown160×27.87 at standard width, FIT mode, no recolor/distortion. Six original24×24 vector icons: gallery, booth, settings, timer, switch, flash. Tint with appropriate semantic foreground; physical camera controls use warm ivory. App back/navigation uses app/text so Light mode stays readable. Park image is illustrative Figma content, never a fake live camera or release asset.

## Layout contract

Units are logical dp(Android) / points(iOS).393×852 canonical: safeTop44/safeBottom34; flash12,52,369×116; main front20,228,353×250; viewfinder124,256,145×158; aperture137,269,119×132; counter28,236,80×50; shutter2,448,48×80; controls56×48 at y426; compartment20,478,353×190; slot33,564,326×64; navigation28,714,337×80 with8gap and107wide buttons. Exact component sizes are in component-inventory.json.

Responsive scale s=(w−24)/369 applies to decorative geometry only. Flash top=actualSafeTop+8; bodyY=flashTop+176s; compartmentY=bodyY+250s. Viewfinder centered145s×158s. Counter80×50, shutter48×80 and controls56×48 remain fixed. Navigation y=h−safeBottom−104; height80; buttons=(w−72)/3; gap8. Extra height goes to lower housing. Use current system insets, not the example's hardcoded status bars. Keep every action at least48×48. At widths below360dp or textScale>1.4, scroll content and expose a full-width essential-controls tray with readable labels rather than shrinking touch targets. This alternative is specified, not native-device validated. Caption Save stays above the system keyboard; use IME-aware scrolling.

Layer order: housing0 → supports10 → panel20 → preview/optics30 → controls40 → mouth50 → moving paper60 → foreground roller lip70 → countdown/dialog80 → native system UI90. Paper is clipped at the mouth, moves downward with uniform geometry, and sits behind the front roller lip. Native film-slot variants indicate phase at the mouth; full paper compositions and six-format motion live on page07.

## Live viewfinder

After sensor rotation, scale uniformly by max(apertureW/imageW, apertureH/imageH), center overflow and clip the inner rounded aperture. Never scale width/height independently. Front live preview mirrors exactly once. Saved output orientation/mirroring follows the existing capture preference. The physical viewfinder is an optical crop and cannot show every final film edge; disclose this in framing help or offer a framing preview, pending product decision. Focus markers lie above live content; interactive focus points must map back through the same camera transform. Right lens and gold mesh are decorative, never announce them as functioning autofocus hardware.

## Capture and loading state rules

CapturePhase IDLE→COUNTDOWN(if enabled)→CHARGING→CAPTURING→PROCESSING→EJECTING→IDLE. Shutter disabled while busy or film0; indicate why and offer manual loading. Only an accepted capture spends film. Timer OFF/FIVE/FIFTEEN/THIRTY maps to0/5/15/30 seconds; cancellation spends nothing. Photo Booth requires4exposures and4accepted captures at5-second intervals. Serialize hardware capture; slow callbacks postpone the next capture rather than overlap it. Grid/Strip use exact existing geometry.

Cartridge CLOSED→OPEN→REMOVED→INSERTED→successful CLOSED resets10. Warn about unused exposures before opening. Format/treatment selected for the inserted cartridge; opening alone never refills. Back while incomplete must preserve loading phase; prototype dims that back action. Development progresses only from accepted shake input: p=min(1,p+clamp(strength,.35,1.4)×.018). Visual chemical veil=(1−p)^1.35 updates directly on events and holds immediately when input stops. Do not run an autonomous reveal timer or spring toward future progress. Existing explicit optional manual assist remains opt-in; it generates its own accepted user input.

## Motion specification

Every animation includes trigger, initial/final states, properties, layer behavior, audio, haptic and reduced-motion contract in motion.json. Existing domain durations override decorative animation. Easing curves: ease-out(0,0,.2,1), ease-in-out(.4,0,.2,1); linear(0,0,1,1). All sound/haptic cues honor current independent preferences and fire once per accepted event. Cancellation restores the physical button and never duplicates an exposure. Reduced motion changes presentation only.

|Animation|Trigger|Duration ms|Easing|Audio|Haptic|Reduced motion|
|---|---|---|---|---|---|---|
|Shutter depression|pointer down while capture allowed|70|ease-out cubic(0,0,0.2,1)|None until accepted capture|existing enabled press cue|Immediate shadow; no translation|
|Shutter release|pointer up/cancel|110|ease-out cubic(0,0,0.2,1)|Accepted capture dispatches existing shutter/charge sound once|Existing capture feedback once|Immediate release; same capture semantics|
|Flash toggle|flash control selection|300|ease-in-out cubic(0.4,0,0.2,1)|Silent|light selection if enabled|Instant state|
|Flash charging|capture CHARGING event|{'default': 650, 'settingsRange': [400, 1200]}|linear visual indicator|Existing flash-charge audio|Existing charge haptic via feedback port|Static Charging/Ready labels|
|Flash firing|hardware exposure callback / screen flash callback|hardware callback bound|step/callback|Existing capture sound only; avoid duplicate cue|Existing capture haptic|Disable decorative bloom; actual requested light follows flash preference|
|Film ejection|EJECTING domain event|1800|linear|Existing motor cue starts0, ends with domain event|Existing pulses0/300/620/960/1300/1600/1800ms|Static emerging and result states; domain1800ms unchanged|
|Shake development|accepted shake input only|input driven; no elapsed-time progress|direct input mapping|Existing shake cue if supported/enabled; no autonomous loop|Existing shake feedback per accepted event|Numeric progress + immediate image update; optional explicit manual-assist input only|
|Development completion|p reaches1|120|ease-out|Optional existing completion cue|light success if enabled|Static Complete label|
|Open cartridge door|confirmed replacement after warning|260|ease-out|Mechanical click when confirmed|light click if enabled|Instant OPEN; preserve cartridge|
|Remove cartridge|user Remove action|240|linear|Mechanical handling cue if available|light selection|Instant REMOVED; no refill|
|Insert cartridge|user selects format/treatment and inserts|360|ease-out|Mechanical seat click|light impact|Instant INSERTED; counter not reset yet|
|Close cartridge door|successful close/load confirmation|260|ease-in-out|Mechanical latch|light success|Instant CLOSED/10 after same domain success|
|Timer selection|select Off/5/15/30|150|ease-out|Silent|selection if enabled|Instant choice|
|Countdown|monotonic timer start|5/15/30s;1s integer ticks|discrete monotonic|tick if enabled|light tick if enabled|Static digits; identical timing|
|Photo Booth sequence|accepted session with≥4 exposures|four accepted captures;5s intervals|monotonic schedule|existing capture/charge/eject events; no duplicates|existing per-event patterns|Static count; identical capture scheduling|
|Camera switching|switch tap|180ms cover; readiness callback gates preview|ease-out|Silent|selection if enabled|Loading label until ready|
|Theme and finish|selected setting|220|ease-in-out|Silent|selection if enabled|Instant colors|
|Screen navigation|destination tap/back|200|ease-out|Silent|native selection only where expected|Instant or100ms opacity|
|Bottom sheet|open timer/film action|240|ease-out|Silent|None|Instant sheet|
|Pushpin insert/remove|explicit Pin/Unpin action|{'insert': 160, 'remove': 120}|ease-out insert/ease-in remove|subtle click if enabled|light impact|Instant pinned badge|

The native Figma timeline demonstrates six-format1800ms ejection. It was rendered to MP4 and inspected at start, transport and exit. Prototype uses an explicitly labeled button to simulate accepted shake input, not elapsed time. Figma does not capture real photographs or validate sensors.

## Android handoff

Compose Box/Canvas/native shapes form the physical shell and component hierarchy. Use density-aware dimensions and cached path/mesh drawing; keep screen text and controls native rather than rasterizing the UI. Material colorScheme can supply semantic app surfaces, content, focus and errors; scope physical camera roles separately to preserve analog identity. CameraX PreviewView FILL_CENTER or an equivalent verified transform fills the rounded aperture. Keep analysis, capture, orientation and touch-focus transforms consistent. Measure with WindowInsets and imePadding/scroll for keyboard; consume insets exactly once. Drive animation from existing capture/cartridge/film/shake domain states, not a second UI state machine. Use existing feedback ports and pulse scheduling; avoid duplicate system sounds. Export small icons/vector layers selectively; rasterize only optical microtexture where needed.

## iOS handoff

SwiftUI dynamic layout and actual safeAreaInsets reproduce the same logical measurements. Draw cached shapes/Canvas for materials and native Buttons with≥48pt interaction areas. AVFoundation preview gravity resizeAspectFill plus rounded mask preserves proportions; coordinate video orientation and mirroring once. Convert tap locations using the preview-layer capture-device conversion. Core Animation keyframes can use the specified cubic curves/linear ejection; SwiftUI state changes must follow the same domain callbacks. Core Haptics maps enabled cues to available device feedback; gracefully skip unavailable haptics. Native sheets/back/swipe behavior preserve loading/capture states. Safe area and keyboard belong to iOS, not the mock status/gesture layers.

## QA and remaining decisions

Design inspection: direct reference comparison; main screen; all18screen specimens via review sheets; compact and iPhone/large layouts; states; foundations/materials; Light settings; native motion start/mid/exit.40 main-control instances across main+four device frames meet≥48dp and remain inside screen bounds. Main contains only6 image-filled nodes:4material tiles, logo, illustrative preview; no flattened UI.106native component masters/13sets were inventoried. This is design QA, not runtime/device acceptance.

|Contrast role|Foreground|Background|Ratio|
|---|---|---|---|
|Dark text|#F2E8CF|#252925|12.10:1|
|Dark secondary|#B7B9AD|#252925|7.42:1|
|Light text|#242526|#FFF9EF|14.66:1|
|Light secondary|#5D5A55|#FFF9EF|6.55:1|
|Light accent|#735628|#FFF9EF|6.49:1|
|CTA ink|#242526|#C6A56C|6.58:1|
|Nav ink|#242526|#D8C9A5|9.37:1|
|Focus dark|#8ADDEB|#252925|9.58:1|
|Focus light|#006B7D|#FFF9EF|5.90:1|
|Error dark|#FFB4AB|#252925|8.69:1|
|Error light|#B3261E|#FFF9EF|6.24:1|

Semantic text pairs pass4.5:1 in flat base paints. Decorative gradients/glass, disabled colors and brand art require contextual device review. Source logo color is preserved; give the brand a readable accessibility label. Focus cannot rely on color alone: add outline and semantic selected/focused values. Counter values, timer and flash states need screen-reader labels. Gold mesh and optical decoration are hidden from accessibility. Reading order: counter/status, viewfinder, shutter, timer, switch, flash, destinations. Dialog closes return focus to trigger. Corkboard offers menu actions and rotation/reset alternatives to dragging.

Open product decisions: approve physical realism/relative scale; authentic colored logo; INSTANT10 replacement badge; fixed ivory rear inserts; red shutter visual override; framing-help treatment; visual-flash preference. Open implementation checks: hardware flash/selfie flash and orientation; real shake acceptance; slow4-shot scheduling; state persistence/cancellation; effect performance and texture seams; TalkBack/VoiceOver; large text, cutouts and IME extremes. Open asset work: Denistina/Historic licensed binaries. Supplied Sol Kol font embedded license metadata declares SIL OFL; preserve its companion license if distributing that original font. Do not treat this review package as an approved production release.

## Delivery inventory

Figma is the editable source of truth. specs/ includes tokens, finishes, components, states, motion, geometry, responsive values, contrast evidence and asset hashes. assets/ includes six SVG icons, original logo, small matte tile and exact outlined caption proof with companion OFL. visuals/ includes main, comparison, all-screen sheets, responsive layouts, component states, materials, foundations, handoff and motion export. references/ contains the supplied camera concept and physical references for review only. No third-party model logos appear in the proposed production UI. No production application code, APK, cloud infrastructure or repository changes are included.
