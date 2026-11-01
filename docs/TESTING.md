# Screen Off TV 0.1.5 — verification

Application ID: `com.thaicuonght.screenofftv`, versionCode 5.
Release APK: `dist/Screen-Off-TV-0.1.5.apk`, signed with the dedicated release certificate.
APK hash and certificate fingerprint are recorded in [install-profile.json](install-profile.json).

## Host checks — 2026-11-01

Release build passed. All **22 unit tests passed** (18 app, 1 IPC, 3 Shizuku service). Android UI-test APK compilation passed; connected tests were not run.

Android lint completed with **0 errors and 23 warnings**: launcher icon shape/duplicates, unused resources, wake-lock timeout, an obsolete SDK check and vector-path complexity. The wake lock intentionally remains held while the panel is off and is released when the service restores the picture or stops. These warnings are not suppressed.

The release APK signature and identity were verified using apksigner and aapt. VCS metadata is disabled in build configuration, so APKs do not contain the source repository commit reference.

Dependencies are pinned to the versions in `gradle/libs.versions.toml`; this cleanup does not claim every dependency is the newest upstream release. AGP 8.5.2 reports a compileSdk 35 support warning; this configuration completed the build and checks.

```bash
./gradlew :app:assembleRelease :app:testDebugUnitTest :ipc:testDebugUnitTest :shizuku-service:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug
```

## Brightness restoration regression checks

Six unit tests cover standby without a saved brightness, restoring the original values once, retry after brightness/mode write failures, preventing an overwrite of pending settings, and leaving brightness mode unchanged when manual compatibility is disabled.

## Device checks

This release has not been installed or tested on a TV. UI-test APK compilation does not mean device tests passed.

Screen on/off, physical shortcut capture, audio continuity with a named player, brightness restoration and reboot recovery require device verification. Screenshots in the installation guides illustrate the UI, not verification of this release.

Run connected UI tests only on a selected test device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Record the TV model, Android/firmware, Shizuku version, player, selected screen mode, shortcut and observed results when reporting compatibility.
