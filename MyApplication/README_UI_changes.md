UI changes and how to create a quick before/after comparison

What I changed
- Updated app theme and colors to a dark purple palette.
- Added drawables for rounded cards, input backgrounds, toggle pill and gradient primary button.
- Redesigned `activity_login.xml`, `activity_home.xml`, and `activity_add_habit.xml` to match the screenshots' visual style.

Quick before/after screenshot guide

1) Prerequisites
  - A device or emulator connected with `adb` available in PATH.
  - (Optional) ImageMagick installed if you want an automatic stitched comparison image. On Windows the command is `magick`.

2) Use the helper script
  - The repository includes `scripts\capture_comparison.ps1`. Run it from the project root (PowerShell):

    # from project root
    .\scripts\capture_comparison.ps1 -Package com.firstapp.myapplication

  - The script will capture a `screenshots\before.png` and `screenshots\after.png` and, if ImageMagick is installed, create `screenshots\comparison.png`.

3) Manual capture (if you prefer)
  - Capture a screenshot on device: `adb shell screencap -p /sdcard/screen.png`
  - Pull it locally: `adb pull /sdcard/screen.png ./screenshots/before.png`
  - Make your UI change (navigate to new screen) and repeat to get `after.png`.
  - Use ImageMagick to combine: `magick convert +append before.png after.png comparison.png`

Notes
- I kept some hidden view ids (visibility="gone") to preserve compatibility with the existing Kotlin Activities. If you prefer a clean refactor (remove old ids and update code to match the new layout ids), I can do that next.
- If you want me to produce an actual screenshot comparison, I can: either (A) generate screenshots on your connected device if you allow me to run `adb` here (I can run the commands but need your device/emulator connected), or (B) provide a local mock render (less accurate). Let me know which you prefer.

Next steps I can take (pick one)
- I can extract exact hex colors from your reference images and tune the theme precisely.
- I can add custom fonts and icons to get a 1:1 visual match.
- I can refactor the Kotlin code to use the new ids (cleaner, remove compatibility hidden views).
