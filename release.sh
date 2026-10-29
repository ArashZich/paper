#!/usr/bin/env bash
# Builds every release artifact into dist/: signed APK, AAB, checksums.
# (No native code, so one APK runs on every ABI; per-ABI splits would be identical files.)
set -euo pipefail
cd "$(dirname "$0")"
VER=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
rm -rf dist && mkdir -p dist

./gradlew clean assembleRelease bundleRelease
cp app/build/outputs/apk/release/app-release.apk "dist/Paper-$VER.apk"
cp app/build/outputs/bundle/release/app-release.aab "dist/Paper-$VER.aab"
cp assets/icon-512.png dist/Paper-icon-512.png

(cd dist && sha256sum * > SHA256SUMS.txt)
ls -la dist
