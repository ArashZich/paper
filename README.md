<p align="center"><img src="assets/icon-512.png" width="128" alt="Paper"></p>

# Paper — کاغذ

Turn your whole Android screen into an e-reader: grayscale, blue-light filter, extra dim, high-contrast text and no
animations, system-wide (inside every app, not just the home screen). Manual or on an automatic day/night schedule.

- **~45 KB**, no dependencies, no background service, no internet permission
- Day and Night looks, each with its own settings
- Modes: **Off / Day / Night / Auto**
- Home-screen **widget** and **Quick Settings tile**
- Turning it off puts every setting back exactly as it was

## How it works

Paper changes the same display settings Android's own *Color correction (monochromacy)*, *Night Light*,
*Extra dim*, *High contrast text* and *Animation scale* use. Nothing runs in the background: settings are written when you
change mode, and one inexact alarm re-applies them at the next day/night boundary (also after reboot).

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
