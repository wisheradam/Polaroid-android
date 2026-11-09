# Physical device acceptance (not yet claimed verified)

Use a real Android API 26+ phone with a rear camera, accelerometer, flash and vibrator. Test more than one OEM before release.

- Fresh install: camera permission grant, refusal, permanent refusal/settings return, unavailable camera, missing front camera.
- Open → remove → insert → close a cartridge; assert 10. Cancel partial replacement and assert the old pack is retained. Replace a nonempty pack and verify the warning.
- Ten real captures: exactly ten archive entries, counter reaches 0; the next shutter is blocked until a manual replacement. Verify a failed capture does not reduce the counter.
- Check preview crop against framed export for every format, portrait/landscape device orientations, rear/front cameras, EXIF rotation and mirroring.
- Listen to first shutter press after cold start: charging then capture/shutter then 1.8-second motor/rollers and final click. Confirm no startup charging sound.
- Test flash on/off and unsupported flash. Verify physical haptic intensity, actual motor/animation alignment, terminal click/pulse, independent sound and vibration toggles, silent mode and sound interruptions.
- In development: leave stationary for a minute, walk normally, rotate slowly; progress should not rapidly increase. Shake gently versus firmly; confirm different development speed. Stop abruptly; all progress must stop. Verify final frame matches exported image.
- Partially develop, background, lock, force-stop and restart; resume from the saved state. Disable accessibility and verify there is no manual progress path; explicitly enable and verify each press advances a small amount.
- Background or rotate during charging/capture/processing/ejection and kill/relaunch during processing. Confirm original/photo survives, no duplicate decrement and retry remains available. Abrupt camera JPEG corruption is a documented recovery limitation.
- Pin each film format; verify mini is smaller on the board. Drag/rotate unpinned, insert each colored pin at different positions, try to move pinned photos, unpin and move, overlap, pan/zoom, restart and verify coordinates/z/pin state.
- Save developed photos via MediaStore and open them in the system gallery. Test legacy API 26–28 storage permission, modern scoped storage, full disk, sharing and cancelled share sheet.
- TalkBack: shutter, permission, film replacement, progress instructions, pin controls, settings and accessible development. Large text/small screens/tablets and landscape layout.
- Battery/lifecycle: camera LED off outside Camera, sensor listeners absent when paused/complete. Profile memory and frame pacing during capture/processing/board zoom.
