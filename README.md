# Screen Off TV — Turn off the TV screen while audio keeps playing

[**Installation guide with screenshots**](docs/INSTALL.en.md) · [**AI agent installation guide**](docs/AGENT-INSTALL.md)

**English** · [Tiếng Việt](README.vi.md) · [简体中文](README.zh-CN.md)

Screen Off TV lets you **turn off the Android TV screen with your remote while music or video keeps playing**. Listen to music, podcasts, lectures or movie audio without keeping the screen on. The app uses **Shizuku** and supports English, Vietnamese and Simplified Chinese. Screen control and continued audio playback depend on the TV and player.

![Screen Off TV interface in English](fastlane/metadata/android/en-US/images/tvScreenshots/1.png)

## Features

- Turn the screen off and back on with a single press, double press or 0.6-second hold on the remote.
- Choose Mute, Volume Up, Volume Down or learn another remote key. New keys are saved after confirmation and retain their original TV function.
- Choose **Screen Off / Picture Off** or **Backlight off**.
- Start automatically with the TV. You can disable this in Settings.
- Navigate with the remote's arrow keys and OK.
- Choose English, Vietnamese, Simplified Chinese or **Follow system**.

## Requirements

- Android TV 8.0 or later.
- Shizuku running, with Screen Off TV authorized to use it.
- Android 11 or later with **Wireless debugging** to start Shizuku over Wi-Fi.

Screen Off TV controls the display. It does not play music or video or add background playback to other apps.

## Download and install

[**Download APK 0.1.6**](https://github.com/thaicuonght/ScreenOffTV/raw/refs/heads/main/dist/Screen-Off-TV-0.1.6.apk) · [SHA-256](dist/SHA256SUMS)

The APK is signed with a dedicated release key.

### Install it yourself

Install and start the apps using the [illustrated installation guide](docs/INSTALL.en.md).

### Ask an AI agent to install

Use an agent with ADB access and TV UI controls. Copy this prompt, then provide the TV address/port or approve its connection when needed:

```text
Read this guide and the APK profile it links to:
https://raw.githubusercontent.com/thaicuonght/ScreenOffTV/main/docs/AGENT-INSTALL.md

Fully install and configure thedjchi/Shizuku and Screen Off TV on my TV using the published APKs. Connect to the TV, verify the correct fork, start Shizuku, grant authorization, configure both apps and read back their settings as instructed. Report evidence for every required_installation_evidence item in the JSON profile. Report completion only when Shizuku is running and Screen Off TV is authorized and ready to control. If blocked, identify the unfinished step and ask only for the missing information or action you need from me.
```

Installation only needs the APKs. The build section below is for source development.

## How to use

The default shortcut is **Mute, pressed twice quickly**. The app responds as soon as you press the key a second time, without waiting for release.

1. Start the Screen Off TV service and check for **Ready to control**.
2. Start music or a video in your usual player and confirm that sound is playing.
3. Press Mute twice quickly to turn off the screen.
4. Press Mute twice quickly again to restore the picture.

If you changed the key or action, use your selected shortcut instead. The key retains its original TV function, so check it before use. Do not assign Power, OK, Back or arrow keys as shortcuts.

If audio stops, restore the picture and check the player. You can try the other screen-off mode after stopping the service. If the shortcut cannot restore the picture, use the TV's restart or power controls to recover.

### Change the remote key

Open **Settings → Remote key**:

- **Preset key:** select a key to save it immediately.
- **Another remote key:** start the service first, select **Set custom remote key**, press the desired key and select **Confirm this key**. Confirmation closes the dialog.

See the [RED button walkthrough](docs/INSTALL.en.md#8-optional-red-button) for a step-by-step example.

## Screen-off modes

| Mode | How it works |
|---|---|
| **Screen Off / Picture Off** — default | Switches the display to its off state and restores its normal state to bring the picture back. The TV keeps running. |
| **Backlight off / Brightness = -1** | Uses the special brightness value `-1` to request backlight off and restores the saved brightness when turned back on. This is different from setting brightness to `0`. |

Both modes use Shizuku and require TV support. Stop the service before changing modes. The app does not send a power-off or standby command. Continued audio playback depends on the TV and player.

## Start automatically with the TV

**Start when TV boots** is enabled by default. Also enable **Start on boot** in Shizuku and keep Wi-Fi available after the TV boots.

If Shizuku starts late or disconnects, Screen Off TV keeps waiting and reconnects automatically. The app reports ready only when it can control the screen and receive remote key presses. If you disabled automatic startup, the app preserves that choice when reopened.

See [Shizuku startup setup](docs/INSTALL.en.md#5-enable-shizuku-start-on-boot) and [how to check after a reboot](docs/INSTALL.en.md#9-test-pictureaudio-and-boot-recovery).

## Device compatibility

This release has not yet been tested on a TV. Screen control and continuing audio depend on the TV firmware and player. See [verification](docs/TESTING.md).

## Languages

The default is **Follow system**. Open **Settings → Language** to choose **Follow system**, **Tiếng Việt**, **English** or **简体中文**.

If the TV language is unsupported, the app uses English. Your selection is saved. Changing language does not change remote settings or restart the running service.

## Build and test

Version **0.1.6**, versionCode **4**, application ID `com.thaicuonght.screenofftv`.
Requires JDK 17 and Android SDK platform 35. Set `JAVA_HOME` and `ANDROID_HOME`.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :ipc:testDebugUnitTest :shizuku-service:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :app:assembleRelease
```

Debug output: `app/build/outputs/apk/debug/app-debug.apk`.
Release signing: copy `signing.properties.example` to `signing.properties` and supply your private key settings. With signing configured, release output is `app/build/outputs/apk/release/app-release.apk`; otherwise it is an unsigned APK. Never commit the signing key or passwords. Keep the release key backed up privately and use the same key for future updates.

UI tests require a connected device: `./gradlew :app:connectedDebugAndroidTest`.

## Project structure

- `app/`: TV interface, languages, remote settings, background service and tests.
- `hidden-api/`: Android display API access.
- `ipc/`: data exchanged between processes.
- `shizuku-service/`: display control and remote input through Shizuku.
- `design/`: original icon artwork.
- `fastlane/`: English, Vietnamese and Simplified Chinese distribution metadata.

## Privacy and license

Preferences are stored on the device. The app has no ads, behavioral analytics or user-data upload service. Android may back up preferences according to the device settings.

This project uses and modifies GPL v3 source code. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for source attribution and dependency notices.
