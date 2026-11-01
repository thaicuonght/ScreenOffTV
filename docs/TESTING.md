# Screen Off TV 0.1.6 — verification

Application ID: `com.thaicuonght.screenofftv`, versionCode 6.
APK: `dist/Screen-Off-TV-0.1.6.apk`, signed with the existing dedicated release key.
APK identity, hash, certificate and publication status are recorded in [install-profile.json](install-profile.json).

## Host checks — 2026-11-01

33 unit tests passed: 29 app, 1 IPC and 3 Shizuku service tests.

Regression coverage includes queued toggles, readiness rechecking after waiting, repeated explicit screen-off commands, brightness recovery across recreated state, durable-save failures, cancellation and serialized service replacement, permanent display errors, and cleanup failures during reconnecting.

UI-test sources also cover disabling actions while stopping and showing permanent errors with a Stop action. UI-test APK compilation is a host check, not evidence of device test execution.

```bash
./gradlew :app:assembleRelease :app:testDebugUnitTest :ipc:testDebugUnitTest :shizuku-service:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug
```

Signature verification passed with the same release certificate as 0.1.5. aapt confirmed package `com.thaicuonght.screenofftv`, version 0.1.6 / code 6. APK SHA-256 is recorded in the install profile and `dist/SHA256SUMS`. VCS metadata remains disabled.

## Device checks still required

This release has not been installed or tested on a TV. Verify actual panel off/on, standby/wake, rapid remote commands, stop/start, delayed/restarted Shizuku, crash recovery, activity recreation during authorization and audio continuity with a named player. The device recovery journal is excluded from cloud backup and device transfer.

Connected UI tests require a selected test device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Dependencies remain pinned. AGP 8.5.2 reports a compileSdk 35 support warning. Final lint: 0 errors, 23 existing warnings (icons, unused resources, wake-lock timeout, obsolete SDK check and vector-path complexity). Warnings are not suppressed.
