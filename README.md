<p align="center"><img src="assets/icon-512.png" width="128" alt="Paper"></p>

# Paper — کاغذ

Make your whole Android screen look like an e-reader (Kindle, Boox, Fidibook): no colors, paper-grey instead of glaring
white, bold black text, no animations. It works system-wide, inside every app, not just on the home screen.

- **One button** (On / Off) and one slider for the paper tone; an optional warm-light slider for night reading
- Home-screen **widget** and **Quick Settings tile**
- **~40 KB**, no dependencies, no background service, no internet permission
- Turning it off puts every setting back exactly as it was

## How it works

Paper changes the same display settings Android's own *Color correction (monochromacy)*, *Extra dim*, *Night Light*,
*High contrast text*, dark-theme and *Animation scale* use. Nothing runs in the background: the settings are written when
you switch it on or off (and re-applied after a reboot).

## Install

1. Download `Paper-<version>.apk` from [Releases](../../releases) and install it.
2. One-time permission (Android requires adb for this). Enable Developer options and USB debugging
   (on Xiaomi/HyperOS also **USB debugging (Security settings)**), connect the phone and run:

   ```
   adb shell pm grant ir.arashzich.papermode android.permission.WRITE_SECURE_SETTINGS
   ```
3. Open Paper, pick a mode. Add the widget or the *Paper* tile for quick access.

If something ever looks wrong, choose **Off**, or run
`adb shell settings put secure accessibility_display_daltonizer_enabled 0`.

## Build

JDK 17 and the Android SDK are enough.

```
./gradlew assembleDebug     # debug APK
./release.sh                # signed APK + AAB + checksums in dist/ (needs keystore.properties)
```

`keystore.properties` (gitignored):

```
storeFile=keystore/paper-release.jks
storePassword=...
keyAlias=paper
keyPassword=...
```

## License

[MIT](LICENSE) © Arash Zich
