# Architecture

## Boundaries

`:core` is pure Kotlin. `:app` is a native Android application whose data, device, imaging, and UI packages form isolated components. `RoomViewModel` owns screen-independent capture coordination. Compose observes Room Flows through lifecycle-aware collection. Dependencies are constructed once by `InstantRoomApp`, without a dependency injection framework. Camera, motion, and feedback boundaries have hardware-independent interfaces (`CapturePort`, `MotionPort`, `FeedbackPort`).

## Camera state machine

`IDLE → CHARGING → CAPTURING → PROCESSING → EJECTING → IDLE`.

The capture mutex is acquired before launching the coroutine, so repeated shutter taps cannot enqueue duplicate requests. The same mutex guards cartridge replacement. No cartridge or an empty cartridge rejects capture before starting hardware. Charging duration is configurable (400–1200 ms). CameraX's capture-start callback triggers shutter feedback. The UI and ejection position use the same monotonic 1800 ms clock. Synthesized roller audio has a terminal click at 1800 ms; the vibration waveform's last pulse begins at 1800 ms. SoundPool loads assets at startup without playing them.

CameraX binds to the screen lifecycle only while the camera screen is started. It unbinds on navigation/background. A capture already submitted runs its durable completion section under `NonCancellable`; binding loss is handled as camera failure. A completed capture still processes and archives when the UI is backgrounded, but suppresses feedback and skips the ejection presentation. No camera or sensor is intentionally kept running in the background. Hardware/OS camera sounds may be independently enforced by the device.

## Capture durability

Before requesting CameraX, write and fsync a `.pending` journal containing photo UUID, immutable cartridge attributes, and timestamp. CameraX writes the private original JPEG. On a camera error remove that attempted capture's files without consuming film. Once CameraX reports success, use a Room transaction to conditionally decrement the active cartridge and insert the unique photo UUID together. A duplicate completion finds the existing UUID and cannot decrement twice. Delete the journal after commit. Apply image processing after the original and archive record are durable.

On launch, recover pending journals with an original file and replay the same idempotent transaction. An intent with no output is discarded. Reprocess Room records whose `processing` flag remains true. Recovery completes before new exposures/cartridge changes are enabled. Processed JPEGs are written to a temporary file, fsynced and renamed; Room's path is updated last. Processing errors retain the original and expose Retry. No destructive migrations are enabled.

Limit: an abrupt power loss can leave a partially written camera JPEG. Its original is retained, but corrupt source data cannot be reconstructed. There is no promise of recovering bytes the OS/camera never wrote.

## Film state machine

`No cartridge → CLOSED → OPEN → REMOVED → INSERTED → CLOSED/loaded`.

Loading is a user-controlled sequence with a hinged-door and sliding-cartridge animation. Selection is exposed only after removal. Cancelling a loading session preserves the installed cartridge; the replacement is committed only on final closure. Warn before opening with unused exposures. Replacement deactivates the old cartridge and inserts a new UUID with ten exposures in a transaction. Archive records retain their old cartridge IDs and formats. At zero exposures, capture is blocked and FILM EMPTY is displayed. There is no automatic reload.

## Development state machine

`Processing → Undeveloped/partial → Developed` is derived from `processing` and a progress value in `[0, 1]`. There is no duration, deadline, periodic increment, or automatic-completion task.

Prefer Android's linear-acceleration sensor, fall back to a low-pass gravity estimate from the accelerometer. Register only on the development screen while RESUMED; unregister on pause, disposal, or completion. Warm up for 500 ms. A shake requires opposing acceleration peaks above threshold, 65–450 ms apart, plus a 110 ms accepted-event refractory interval. Gaps greater than 600 ms reset the candidate, blocking stale peak combinations. Nonfinite and out-of-order input cannot create progress. Strength controls a bounded per-event increment. Each accepted increment is committed to Room under a mutex, and queries use `MAX` to avoid regressions.

The print itself shows emergence through a tinted chemical mask and deterministic soft patches; progress is not animated or numerically displayed. GPU-backed Compose drawing handles masks and scaling. Final processing uses a bounded CPU Canvas pipeline on a background dispatcher for deterministic exports across API 26+. A GPU image-processing backend is a future optimization, not claimed implemented.

Accessibility manual development is off by default. When deliberately enabled, each button press provides one small step. No hold-to-complete timer is used.

As of 0.3.0, `develop` returns whether a real increment was committed. A view-model development session fences late callbacks after pause/disposal. Only an accepted sensor-driven event that commits progress during the active foreground session can request a haptic pulse. A monotonic 260 ms throttle limits 12 ms low-amplitude pulses (8 ms on motors without amplitude control). There is no continuous/repeating vibration job. Completion, navigation and pause cancel development vibration. Accessibility presses never request shake pulses; the existing global haptics switch is respected.

## Processing and export

Original JPEG → EXIF rotation/reflection → centered format crop → per-stock color matrix → optical softness → vignette → deterministic grain → optional edge chemistry → correctly proportioned white/circular frame → processed JPEG.

Frames are baked into processed images, so exports match archive and board presentation. Physical millimetre ratios define image and exterior frames. The Go print is smaller relative to Classic on the board. Processing preferences apply when processing a new image; caption editing does not rerun the film treatment. Input and output resolutions are bounded; previews use a size-bounded bitmap cache. MediaStore uses pending writes on API 29+, deleting failed insertions. API 26–28 requests legacy write permission only when exporting. Sharing uses a read-granted FileProvider URI. Undeveloped/processing files cannot be saved or shared from the UI or export service.

## Handwritten captions and migration

Room schema 2 adds `caption`, `captionFont`, and `baseProcessed` to photos using an explicit non-destructive 1→2 migration. Old records default to empty text and Sol Kol. On the first caption edit, retain the existing uncaptioned `processed` JPEG as the immutable base. `CaptionRenderer` loads that file and clips handwriting to the lower paper margin derived from each format's physical dimensions. It fits actual glyph bounds into at most two lines; the three supplied font files have distinct metrics. Input is limited to 80 Unicode code points and two explicit lines, with validation rather than silent truncation. Android supplies fallback glyphs where a chosen font lacks a character.

Render each edit on a worker dispatcher into a new temporary JPEG, fsync and rename it, then update the caption, font, base and final path in one SQL statement under the repository mutation mutex. A render failure or interruption before that update leaves the previous committed print intact. No original/base image is overwritten. Empty text points `processed` back to the exact uncaptioned file. The unique rendered path invalidates archive/corkboard bitmap-cache keys, and prior render files remain available for share recipients still reading an older URI. Caption edits do not mutate cartridges, progress or board placements. Save/share resolve the latest repository record under the same mutex before exporting.

The retained base, text and font permit consistent regeneration via `CaptionRenderer.render` even if global film-processing preferences change. Missing/corrupt base files produce a recoverable editor error rather than overwriting the photograph. Previous caption-render files are currently retained; there is no automatic archive cleanup in this iteration.

## Corkboard state machine

`Archive-only → Unpinned placement → Pinned placement → Unpinned placement → Archive-only`.

`PlacementEntity` uses `(boardId, photoId)` as its key, with photo foreign key, board coordinates in dp, rotation, z, pin color, normalized pin position, and pin lock. The MVP board is `main`; multiple boards need only a board selector and board metadata model. Developed prints can be dragged/tapped from the archive-derived tray. Tap color then tap the print to position its pin. Only unpinned prints can move/rotate, enforced again in the repository. Translation/rotation are previewed in Compose and persisted at gesture end. Removing a placement never deletes the photograph. Board navigation uses bounded zoom and pan; photo coordinates are board-local and independent of viewport size.

## Timed capture and Photo Booth (0.4.0)

`CaptureSequence` is a clock-injected monotonic scheduler. For delayed single captures, charging is scheduled inside the countdown before the camera-request deadline; Off retains the original charge duration without an added timer. Booth targets four request starts at +5/+10/+15/+20 seconds. Each next deadline is based on the preceding request start, so JPEG storage latency is not added to the interval. If a camera takes more than the interval, wait for its result and allow charging before the next request: timing slips safely rather than overlapping captures. Camera AE/flash can introduce additional shutter latency beyond request timing.

`RoomViewModel` synchronously takes the existing capture mutex before launching any schedule. This locks lenses, mode/layout/light selection, navigation and cartridge replacement for the sequence. Pause, camera-unavailable state and Cancel cancel the coroutine. A camera request already submitted finishes its journal/archive section under `NonCancellable`; cancellation then prevents all subsequent requests. Progress survives because each successful source is separately journalled and transactionally deducted. Charging/countdown/shutter audio use exclusive playback; successful booth exposures have a short confirmation pulse. Motor ejection happens once for the finished composite, using the existing timeline.

Room schema 3 adds `boothSessionId`, `boothIndex` and `boothLayout` to photographs and a `booth_sessions` table. Source IDs are deterministic per session/index; repeating a callback or recovery cannot deduct twice. Original photo records retain each source path and timestamp. Ordered `sources(sessionId)` gives the four stable source references for composition/reprinting. A composite is another photo record with index -1, layout and session ID, inserted without calling exposure consumption. Its legacy `original` field points to source 1; rendering always uses the full ordered session source set. Sessions store initial layout, cartridge and creation timestamp.

Recovery first replays journals and processes pending prints. Incomplete sessions with fewer than four sources are marked interrupted and never restart capture. A four-source session creates/retries exactly one deterministic primary composite; subsequent recovery preserves its caption and development progress. Partial sources stay individually accessible and require their own development if opened. Reprint creates a distinct undeveloped composition, leaving earlier prints unchanged and consuming no additional film.

`BoothRenderer` decodes bounded upright copies of full CameraX originals, applies the selected film's color/mono treatment and vignette, and fits each complete image into its cell without cropping. Paper letterboxing preserves portrait/landscape proportions. Default grid output is 2400×2748, strip 1200×4913; the existing low-memory preference reduces these to 1600×1832 and 800×3275. Raw JPEGs retain CameraX's highest available standard capture resolution in booth mode; special vendor sensor modes are not requested. Image processing is deferred until after the four timed shots and runs off the UI thread.

`PrintGeometry` describes either the existing single-film dimensions or a booth layout with four image regions. FilmPrint masks those regions using the existing shake-only progress, captions use the lower paper margin, and board widths/heights use the same geometry. This keeps strip proportions during rotation/board zoom. Booth layouts are not cartridge types; the original four film formats and ten-exposure capacity remain unchanged.

## Known implementation limits

The design is an original stylized camera rather than a 3D replica. Film-room steps include accessible buttons and drag gestures rather than a full rigid-body simulation. The release is a first native implementation and needs device UX tuning of shake thresholds, flash latency, viewfinder crop, vibration amplitude, frame timing and audio synchronization. No hardware results should be inferred from unit or emulator checks. Single board only. No account or cloud photo synchronization. AWS is used for build infrastructure, not the photo workflow.
