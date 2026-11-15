# Design to code

| Feature | Figma source | Android source | Status / remaining work |
|---|---|---|---|
| Camera families |17:605 and existing family specs|core/CameraStyle.kt; ui/OneAndOnlyCamera.kt, OldFashionCamera.kt, InstaStyleCamera.kt|13 runtime finishes; Blush/RED conflict unresolved|
| Flash toggle |66:581|ui/FlashToggle.kt; core/CameraStyle.kt|300ms toggle implemented; optical overlay300/400 and independent charge not equivalent to toggle|
| Release slider |89:600|ui/OneAndOnlyCamera.kt; ui/FilmLoadingScreen.kt|24 Figma variants; mechanical drag integration requires engineering|
| Manual loading |43:389;128:955|ui/FilmLoadingScreen.kt; core/Film.kt|CLOSED/OPEN/REMOVED/INSERTED implemented; hinged prototype extension not implemented|
| Unboxing1.2 |137:568;137:569|no dedicated unboxing module found|six products, matching art and rail projection integration pending|
| Film catalog |87:79;109:79|core/Film.kt; imaging/FilmProcessor.kt|four runtime formats ×three appearances;2.0 proposed only|
| Timer/booth |existing motion pages|core/CaptureSequence.kt|Off/5/15/30; four accepted captures, five-second deadlines|
| Ejection/shake |existing motion pages|core/Film.kt|1800ms ejection; sensor-driven development remains authoritative|
| iOS |137:3145;130:13644|not in Android repository|handoff only, no implementation claimed|

Paths above are relative to android/app/src/main/java/com/instantroom or android/core/src/main/kotlin/com/instantroom as appropriate. Existing motion and family JSON remain under android/docs/design-system.

