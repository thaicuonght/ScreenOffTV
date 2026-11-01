# Screen Off TV — 关闭电视画面，继续收听音乐和电影声音

[**图文安装指南**](docs/INSTALL.zh-CN.md) · [**AI 安装指南（越南语）**](docs/AGENT-INSTALL.md)

[English](README.md) · [Tiếng Việt](README.vi.md) · **简体中文**

Screen Off TV 可通过遥控器**关闭 Android TV 屏幕，同时让音乐或视频继续播放**。无需保持屏幕开启，也能收听音乐、播客、课程或电影声音。应用使用 **Shizuku**，支持越南语、英语和简体中文。能否关闭屏幕并继续播放声音，取决于电视和播放器。

![Screen Off TV 简体中文界面](fastlane/metadata/android/zh-CN/images/tvScreenshots/1.png)

## 功能

- 通过遥控器单击、双击或长按 0.6 秒关闭和重新开启屏幕。
- 选择静音键、音量增加键、音量减小键，或识别其他遥控按键。新按键确认后才会保存，并保留电视上的原有功能。
- 选择 **Screen Off / Picture Off** 或关闭背光模式。
- 随电视自动启动，可在设置中关闭。
- 使用遥控器方向键和 OK 操作。
- 选择越南语、英语、简体中文或跟随系统。

## 使用要求

- Android TV 8.0 或更高版本。
- Shizuku 正在运行，Screen Off TV 已获准使用 Shizuku。
- 通过 Wi-Fi 启动 Shizuku 需要 Android 11 或更高版本，并提供 **Wireless debugging**。

Screen Off TV 负责控制屏幕，不播放音乐或视频，也不会为其他应用增加后台播放功能。

## 下载与安装

[**下载 APK 0.1.6**](https://github.com/thaicuonght/ScreenOffTV/raw/refs/heads/main/dist/Screen-Off-TV-0.1.6.apk) · [SHA-256](dist/SHA256SUMS)

APK 使用专用发布密钥签名。

### 自行安装

按照[图文安装指南](docs/INSTALL.zh-CN.md)安装并启动应用。

### 让 AI agent 安装

使用具备 ADB 连接和电视界面操作工具的 agent。复制以下提示词，并在需要时提供电视地址、端口或确认连接：

```text
阅读以下指南及其中链接的 APK 配置文件：
https://raw.githubusercontent.com/thaicuonght/ScreenOffTV/main/docs/AGENT-INSTALL.md

请使用已发布的 APK，在我的电视上完整安装并配置 thedjchi/Shizuku 和 Screen Off TV。连接电视，验证正确的 fork，启动 Shizuku、授予权限，并按指南配置和读回两个应用的设置。逐项报告 JSON 中 required_installation_evidence 的证据。只有 Shizuku 正在运行，且 Screen Off TV 已获授权并就绪时，才能报告完成。如果受阻，请说明未完成的步骤，仅询问仍需我提供的信息或操作。
```

安装只需 APK，下方构建说明仅用于源码开发。

## 使用方法

默认快捷操作为**快速按两下 Mute（静音键）**。第二次按下按键时，应用立即响应，无需等待松开。

1. 启动 Screen Off TV 服务，确认已就绪。
2. 在常用播放器中播放音乐或视频，确认有声音。
3. 快速按两下 Mute 关闭屏幕。
4. 再快速按两下 Mute 恢复画面。

如果已修改按键或操作方式，请使用自己的快捷操作。按键仍保留电视原有功能，请先检查。不要将电源、OK、Back 或方向键设为快捷键。

如果声音停止，先恢复画面并检查播放器。停止服务后，也可尝试另一种息屏模式。如果快捷操作无法恢复画面，可使用电视的重启或电源操作恢复。

### 更改遥控按键

打开 **Settings → Remote key**：

- **预设按键：**选择按键后立即保存。
- **其他遥控按键：**先启动服务，选择 **Set custom remote key**，按下想使用的按键，再选择 **Confirm this key**。确认后窗口会自动关闭。

使用简体中文界面时，对应入口为 **设置 → 遥控按键 → 设置自定义遥控按键**，保存按钮为 **确认使用此按键**。

分步示例见 [RED 红色按键设置](docs/INSTALL.zh-CN.md#8-可选更换为-red-红色按键)。

## 两种息屏模式

| 模式 | 工作方式 |
|---|---|
| **Screen Off / Picture Off** — 默认 | 将屏幕切换为关闭状态，恢复正常状态时重新显示画面。电视继续运行。 |
| **关闭背光 / Brightness = -1** | 使用特殊亮度值 `-1` 请求关闭背光，重新开启时恢复已保存的亮度。这与将亮度设为 `0` 不同。 |

两种模式均通过 Shizuku 操作，并需要电视支持。修改模式前先停止服务。应用不发送关机或待机命令。能否继续播放声音取决于电视和播放器。

## 随电视自动启动

随电视启动选项默认开启。还需要在 Shizuku 中开启 **Start on boot**，并在电视启动后保持 Wi-Fi 可用。Screen Off TV 英语界面中的对应选项为 **Start when TV boots**。

如果 Shizuku 启动较晚或连接中断，Screen Off TV 会继续等待并自动重新连接。只有能够控制屏幕并接收遥控按键时，应用才显示就绪。如果已关闭自动启动，应用下次打开时会保留该选择。

参阅 [Shizuku 自启动设置](docs/INSTALL.zh-CN.md#5-设置-shizuku-开机自启动)和[重启后的检查方法](docs/INSTALL.zh-CN.md#9-验证画面声音及开机恢复)。

## 设备兼容性

此版本尚未在电视上测试。息屏和音频播放取决于电视固件与播放器。请参阅[验证结果](docs/TESTING.md)。

## 语言

默认使用**跟随系统**。在 **设置 → 语言** 中选择 **跟随系统**、**Tiếng Việt**、**English** 或 **简体中文**。英语界面中的入口为 **Settings → Language**，跟随系统选项为 **Follow system**。

如果不支持电视的系统语言，应用使用英语。选择会被保存。切换语言不会更改遥控设置，也不会重启正在运行的服务。

## 构建与测试

版本 **0.1.6**，versionCode **4**，应用 ID `com.thaicuonght.screenofftv`。
需要 JDK 17 和 Android SDK platform 35。设置 `JAVA_HOME` 与 `ANDROID_HOME`。

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :ipc:testDebugUnitTest :shizuku-service:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :app:assembleRelease
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。
复制 `signing.properties.example` 为 `signing.properties` 并配置私有签名密钥。配置后 release APK 位于 `app/build/outputs/apk/release/app-release.apk`；未配置时生成未签名 APK。请勿提交密钥或密码。请私下备份发布密钥，并在更新时继续使用同一密钥。

连接设备后运行界面测试：`./gradlew :app:connectedDebugAndroidTest`。

## 项目结构

- `app/`：电视界面、语言、遥控设置、后台服务和测试。
- `hidden-api/`：访问 Android 屏幕 API。
- `ipc/`：进程间交换的数据。
- `shizuku-service/`：通过 Shizuku 控制屏幕并接收遥控输入。
- `design/`：原始图标素材。
- `fastlane/`：英语、越南语和简体中文的分发信息。

## 隐私与许可证

设置保存在设备本地。应用不含广告、行为分析或将用户数据上传到服务器的服务。Android 可能根据设备配置备份设置。

本项目使用并修改 GPL v3 源代码。源码来源和依赖声明请参阅 [LICENSE](LICENSE) 和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
