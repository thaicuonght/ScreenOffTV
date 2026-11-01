# 在 Android TV / Google TV 上安装 Screen Off TV

截图仅用于说明界面，并非当前版本的测试证明。


[English](INSTALL.en.md) · [Tiếng Việt](INSTALL.md) · **简体中文** · [AI 安装指南（越南语）](AGENT-INSTALL.md) · [README](../README.zh-CN.md)

按以下顺序操作：**安装 Shizuku → 连接 Shizuku 与电视 → 开启 Shizuku 自启动 → 安装 Screen Off TV → 授权 → 检查功能**。本指南使用英语界面的电视截图，按钮、菜单和状态名称保留英文，方便与截图对照，操作说明使用中文。

## 1. 准备并下载两个 APK

准备已连接 Wi-Fi 的电视、遥控器，以及电脑或 USB 盘，USB 安装需要电视文件管理器。Screen Off TV 要求 Android TV 8.0 或更高版本，兼容性取决于电视固件。通过无线调试启动 Shizuku 需要 Android 11 或更高版本，且固件提供 Wireless debugging 菜单。

| 应用 | 项目 | 下载 |
|---|---|---|
| 适合电视遥控操作的 Shizuku fork 13.7.0-thedjchi | [发布页](https://github.com/thedjchi/Shizuku/releases/tag/v13.7.0-thedjchi) | [APK](https://github.com/thedjchi/Shizuku/releases/download/v13.7.0-thedjchi/shizuku-v13.7.0-thedjchi.apk) |
| Screen Off TV 0.1.5 | [仓库 APK 页面](https://github.com/thaicuonght/ScreenOffTV/blob/main/dist/Screen-Off-TV-0.1.5.apk) | [APK](https://github.com/thaicuonght/ScreenOffTV/raw/refs/heads/main/dist/Screen-Off-TV-0.1.5.apk) |

先下载两个文件。GitHub 文件页面选择 **Download raw file**，不要把 HTML 网页保存为 APK。在 Linux 用 `sha256sum 文件.apk`，在 PowerShell 用 `Get-FileHash 文件.apk -Algorithm SHA256`，对照：

```text
shizuku-v13.7.0-thedjchi.apk
6ea6dee65d5ddc626b6b75b2c2f67f8cc547fa47d7b437e6892639c37eaffe43
Screen-Off-TV-0.1.5.apk
5683f1cfa28e4d7571cea1a4efb1dce2a7fedc1d576c97884ebb2502d88ab5d3
```

## 2. 开启开发者选项与调试

1. **Settings → System / Device preferences → About → Android TV OS build**，按 OK 约 7 次，直到提示已成为开发者，如要求，输入电视锁屏码。
2. 返回 **System / Device preferences → Developer options**，保持总开关开启。
3. 在调试部分开启 **USB debugging**。
4. 进入 **Wireless debugging**，选择 **Enabled**（启用），如有网络授权提示，仅允许自己的可信网络。不要开启 OEM 解锁或 root 电视。

![开发者选项中的 USB 调试和无线调试入口](images/install/debugging_menu_en.png)

截图中已开启 USB 调试和无线调试。

## 3. 先安装 Shizuku

**USB 方法：**把 Shizuku APK 复制到 USB 盘，插入电视，用电视文件管理器打开 Shizuku APK。如被阻止，进入 **Settings → Apps → Special app access → Install unknown apps**，仅允许当前文件管理器，再返回选择 **Install → Open**。安装完成后可收回该文件管理器的安装权限。不要为绕过原因不明的警告关闭系统保护。

**电脑方法：**安装 [Google Platform Tools](https://developer.android.com/tools/releases/platform-tools)。使用电视当前显示的配对端口和连接端口，两者不同，在电脑提示时输入当前配对码，并在电视允许自己的电脑：

```bash
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:CONNECTION_PORT
adb devices -l
adb -s TV_SERIAL install shizuku-v13.7.0-thedjchi.apk
```

将大写占位符替换为实际值。若固件提供已授权的旧式网络 ADB，可使用该途径，不要假定所有电视都使用 5555 端口。电脑已配对不代表 Shizuku 自身已配对。

从应用列表打开 Shizuku，若找不到图标，可用 **Settings → Apps → Shizuku → Open**。

![Shizuku 已安装，尚未运行](images/install/shizuku_not_run_en.png)

**Shizuku is not running**表示服务尚未启动。启动成功后会显示 **Shizuku is running**。

## 4. 连接 Shizuku 与电视

1. 在 Shizuku 选择 **Start via Wireless debugging → Start**。
2. 如果电视显示 **Allow USB debugging**，确认是自己的连接后，勾选 **Always allow from this computer** 并选择 **Allow**。等待启动完成，命令窗口会自动关闭。
3. 点击 Start 并处理授权弹窗后，读取实际结果。如果 Shizuku 已运行，无需再次配对。仅在提示需要配对或错误明确表示尚未配对时，选择 **Pairing → Developer options → Wireless debugging → Pair device with pairing code**。按当前界面让 Shizuku 识别配对码或输入配对码，确认配对成功，再返回选择 **Start**，如出现调试授权弹窗则确认。电脑 ADB 配对不能代替 Shizuku 自身的配对。其他错误应按错误信息处理。
4. 仅在显示 **Shizuku is running** 且没有待确认弹窗时继续。使用 agent 时，还须确认 `pidof shizuku_server` 返回 PID。仅配对成功或命令窗口关闭，并不能证明服务已运行。

![Shizuku 已成功启动](images/install/shizuku_runing_en.png)

如果电视没有无线调试菜单，在 Shizuku 选择 **Start by connecting to a computer → View command**，通过 ADB 执行显示的命令。使用此方式时，需要另行检查重启后的自动启动。

fork 应用版本 13.7.0 的主页可能显示 server 13.6，请在关于页面或系统应用信息中核对应用版本 `13.7.0-thedjchi`。

## 5. 设置 Shizuku 开机自启动

进入 Shizuku **齿轮图标 → Settings → Start on boot**，开启。如果安装流程要求 WRITE_SECURE_SETTINGS，通过已授权电脑运行：

```bash
adb -s TV_SERIAL shell pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS
```

重启后保持 Wi-Fi 可用，若固件有真实的后台运行/自启动管理菜单，为 Shizuku 设置相应权限。

![Shizuku 的开机启动已开启](images/install/shizuku_setting_en.png)

图中 TCP 和 Watchdog 均已开启。并非所有电视都需要开启 TCP。Watchdog 可在 Shizuku 服务停止后尝试恢复，但不能代替开机自启动。

**注意：**开机自启动需要完成步骤 4 的连接、开启自启动选项，并在重启后保持 Wi-Fi 可用。启动时间取决于电视和 Wi-Fi 连接。Screen Off TV 会持续等待 Shizuku，即使超过 30 秒也会在其就绪后自动连接，服务连接失败时会自动重试。自动启动的检查见步骤 9。

## 6. 安装 Screen Off TV 并授权

按步骤 3 的 USB 方法安装，或运行：

```bash
adb -s TV_SERIAL install Screen-Off-TV-0.1.5.apk
```

打开 Screen Off TV，选择 **Start service**。应用可能提示需要 Shizuku 权限：

![应用尚未授权](images/install/screenoffftv_not_run_en.png)

在 Shizuku 弹窗选择 **Allow all the time**。若电视要求该应用的通知权限，接受相应提示。

![Screen Off TV 已获授权并就绪](images/install/screenofftv_run_en.png)

等待显示 **Ready to control** 和 **Shizuku: running · authorized**。否则先检查 Shizuku，再返回选择 **Check Shizuku connection** 或重新启动服务。

## 7. 确认默认设置

打开 Screen Off TV **Settings**，检查：

| 设置 | 默认值 |
|---|---|
| Remote key（遥控按键） | Mute |
| Press action（按键操作） | Double press（双击） |
| Start when TV boots（随电视启动） | On（开启） |
| Screen-off mode（息屏模式） | Screen Off / Picture Off |
| Language（语言） | Follow system（跟随系统） |

![服务就绪、Mute 双击和开机启动开启](images/install/screenoffftv_setting_en.png)

Screen Off TV 有独立的开机启动开关，同时需要开启步骤 5 中 Shizuku 的 **Start on boot**。修改息屏模式前先选择 **Stop service**。可在 **Settings → Language** 中更改语言，

## 8. 可选：更换为 RED 红色按键

默认快捷操作为 **Mute，双击**。如需使用 RED 红色按键：

1. 选择 **Start service**，打开 **Settings → Remote key → Set custom remote key**。
2. 在 **Learn a remote key** 窗口中按一次遥控器上的 RED。不要使用电源、OK、Back 或方向键。
3. 显示 **Key detected** 后，选择 **Confirm this key** 保存，或选择 **Try another key** 重新设置。确认后会自动关闭窗口。
4. 如需保留默认操作，将 **Press action** 设为 **Double press**。打开 **Test remote key**，按一次 RED 检查是否收到信号。

![显示 Key detected 后，选择 Confirm this key 保存](images/install/set-red_key_en.png)

## 9. 验证画面、声音及开机恢复

**检查画面和声音：**播放音乐或视频并确认有声音。使用已设置的快捷操作（默认快速按两下 Mute）关闭画面，检查声音是否继续，再执行相同操作恢复画面。按键仍保留电视原有功能，请先检查该功能。如果画面无法恢复，可使用电视的重启或电源操作恢复。

**检查自动启动：**通过系统菜单重启电视，等待 Wi-Fi 和 Shizuku。等待时间取决于电视和 Wi-Fi 连接。**打开 Shizuku 或 Screen Off TV 之前，不要点击 **Start service**，也不要运行电脑启动命令。**先播放音乐或视频并确认有声音，再用快捷操作关闭和恢复画面。如果画面响应且声音继续播放，说明本次自动启动正常。

如失败，请检查两个应用的自启动选项及 Shizuku 是否正在运行。如果必须打开应用或手动启动服务才能使用，则本次自动启动未通过。报告问题时，请提供电视型号、Android 版本、两个应用的版本和显示状态。重启后保持 Wi-Fi 可用。



## 10. 常见问题

| 问题 | 处理 |
|---|---|
| Screen Off TV 重启后未自动启动 | 检查授权、Screen Off TV 的 **Start when TV boots**，以及 Shizuku 的 **Start on boot** 和运行状态。 |
| Shizuku 无法连接 | 检查 Wi-Fi、**USB debugging** 和 **Wireless debugging**。 |
| 尚未授权 | 选择 **Start service**，在 Shizuku 弹窗中选择 **Allow all the time**，曾拒绝时查看 Shizuku 的应用授权列表。 |
| Shizuku 就绪但客户端超时 | 等待自动重新连接，如果持续报错，请记录提示并检查 Shizuku 授权。 |
| 播放停止 | 恢复画面并检查播放器，能否继续播放取决于播放器。 |

参考：[fork](https://github.com/thedjchi/Shizuku)、[Setup](https://github.com/thedjchi/Shizuku/wiki/Setup)、[Features](https://github.com/thedjchi/Shizuku/wiki/Features)、[Google ADB](https://developer.android.com/tools/adb)。另见 [AI 指南](AGENT-INSTALL.md) 与 [测试记录](TESTING.md)。
