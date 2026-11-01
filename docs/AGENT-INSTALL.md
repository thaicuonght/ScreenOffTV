# Cài đặt Screen Off TV lên TV — hướng dẫn cho agent

Hướng dẫn cài mới và cấu hình cả **thedjchi/Shizuku** và **Screen Off TV** để sẵn sàng sử dụng. Thực hiện theo 6 bước bên dưới, chỉ chuyển bước khi đạt điều kiện ghi ở cuối bước đó.

## Chuẩn bị

Đọc [install-profile.json](https://raw.githubusercontent.com/thaicuonght/ScreenOffTV/main/docs/install-profile.json) để lấy package, phiên bản, URL APK, SHA-256, cấu hình mặc định và checklist hoàn thành. Chỉ dùng bằng chứng của lần cài hiện tại để báo hoàn tất.

- Dùng APK đã phát hành hoặc APK người dùng chỉ định. Tải trực tiếp, không cần tải mã nguồn hay bộ công cụ build. Chỉ build khi người dùng yêu cầu rõ.
- Có thể dùng **uiautomator, UI hierarchy/XML, ADB, ảnh màn hình, phím điều hướng** hoặc công cụ tương đương trong toàn bộ quy trình, kể cả ghép nối. Chọn cách phù hợp với công cụ và giao diện thực tế.
- Sau thao tác thay đổi giao diện, đọc lại trạng thái trước khi dùng tọa độ tiếp. Switch không bấm được thì chọn hàng cài đặt chứa nó và đọc lại giá trị. Không bấm theo tọa độ ảnh mẫu.
- “Không cần test” vẫn phải khởi động dịch vụ, cấp quyền và đọc cài đặt, chỉ bỏ qua thử remote thật, tắt/bật màn hình, âm thanh, reboot và bộ kiểm thử.

Tham khảo hướng dẫn: [Việt](INSTALL.md) · [Anh](INSTALL.en.md) · [Trung](INSTALL.zh-CN.md).

## Đọc trạng thái và thao tác giao diện

Áp dụng cho mọi công tắc, checkbox và mục lựa chọn trong cả hai ứng dụng:

1. Đọc cây XML đầy đủ bằng trình phân tích XML hoặc công cụ hierarchy. Tìm đúng nhãn, đi lên hàng cài đặt chứa nhãn rồi tìm control tương ứng trong hàng đó. Nhãn và công tắc có thể là hai node anh em, không nhất thiết công tắc nằm trong node nhãn. Không cắt một khoảng ký tự quanh nhãn hoặc lấy giá trị `checked` gần nhất để suy ra trạng thái.
2. Với Switch/CheckBox, đọc `checked` của đúng control có `checkable="true"`. Ví dụ `class="android.widget.Switch" checkable="true" checked="true"` nghĩa là đang bật. Bỏ qua `checked` trên TextView hoặc layout có `checkable="false"`. Không phụ thuộc thứ tự thuộc tính hoặc tên class duy nhất vì control có thể dùng class khác.
3. Phân biệt các thuộc tính: `checked` là trạng thái chọn/bật của control checkable, `clickable` là khả năng bấm, `enabled` là khả năng tương tác, `focused` là vị trí focus. `clickable="false"` không có nghĩa đang tắt. Chỉ dùng `selected` hay nhãn On/Off khi giao diện thực sự dùng chúng để thể hiện giá trị, không coi focus hoặc tô sáng là bằng chứng đã bật.
4. So sánh với giá trị cần đặt. Đã đúng thì không bấm. Chưa đúng thì thao tác một lần trên control hoặc hàng cài đặt có thể bấm, đọc XML mới rồi xác nhận lại đúng mục. Nếu có hộp thoại, xử lý và đọc lại kết quả trước khi thao tác tiếp.
5. Không tìm thấy control, thiếu thuộc tính trạng thái hoặc không xác định được control thuộc mục nào thì ghi **chưa xác định**, không mặc định là tắt và không bấm tiếp để thử. Đọc lại hierarchy hoặc dùng công cụ khác, chỉ nhờ người dùng khi vẫn bị chặn.

## 1. Kết nối đúng TV

```bash
adb devices -l
```

Nếu TV chưa kết nối, dùng địa chỉ/cổng thực tế:

```bash
adb connect TV_IP:CONNECTION_PORT
```

Nếu kết nối yêu cầu ghép đôi máy tính, dùng `adb pair TV_IP:PAIRING_PORT`, nhập mã TV hiển thị rồi kết nối lại. Hai cổng có thể khác nhau, chỉ dùng 5555 khi đã biết TV dùng cổng đó. Nếu TV hiện **Allow USB debugging / Cho phép gỡ lỗi qua USB**, xác nhận đúng kết nối của mình rồi chọn **Always allow from this computer / Luôn cho phép từ máy tính này → Allow / Cho phép**.

Lưu thiết bị đích vào `tv_serial`, dùng `adb -s "$tv_serial"` cho các lệnh tiếp theo:

```bash
adb -s "$tv_serial" shell getprop ro.product.model
adb -s "$tv_serial" shell getprop ro.build.version.release
adb -s "$tv_serial" shell getprop ro.build.version.sdk
```

Nếu nhiều thiết bị mà chưa xác định được TV, chỉ hỏi thông tin còn thiếu. Nếu `unauthorized`, xử lý hộp thoại trên TV, nếu `offline`, kết nối lại.

**Chuyển bước khi:** đúng TV, ADB ở trạng thái `device` và thực hiện được lệnh. Screen Off TV cần API 26 trở lên.

## 2. Tải và cài hai APK

Tải APK từ `shizuku.url` và `screen_off.download_url` trong profile. Đối chiếu SHA-256 lần lượt với `shizuku.sha256` và `screen_off.sha256`, sau đó cài từng ứng dụng:

```bash
adb -s "$tv_serial" install SHIZUKU_APK_PATH
adb -s "$tv_serial" install SCREEN_OFF_APK_PATH
```

Thay các đường dẫn bằng tệp APK vừa tải. Cả hai lệnh phải trả về `Success`. Đọc lại package và phiên bản sau cài:

```bash
adb -s "$tv_serial" shell dumpsys package moe.shizuku.privileged.api
adb -s "$tv_serial" shell dumpsys package com.thaicuonght.screenofftv
```

Shizuku phải là **thedjchi**, được xác minh bằng SHA-256 của APK trong profile, không chỉ bằng tên hoặc biểu tượng. Với APK người dùng chỉ định, kiểm tra package, phiên bản và nguồn gốc trước khi cài. Nếu tải lỗi, checksum sai hoặc cài lỗi, báo thông báo cụ thể và xử lý tại bước này.

**Chuyển bước khi:** cài thành công cả hai ứng dụng, package và phiên bản đúng với APK đã chọn, xác minh đúng fork Shizuku.

## 3. Khởi động và xác nhận Shizuku

Mở Shizuku từ launcher hoặc **Settings → Apps → Shizuku → Open**.

1. Bấm **Start via Wireless debugging → Start / Bắt đầu**.
2. Nếu xuất hiện **Allow USB debugging / Cho phép gỡ lỗi qua USB**, xác nhận đúng kết nối rồi chọn **Always allow from this computer → Allow / Luôn cho phép → Cho phép**. Đọc kết quả sau khi xác nhận.
3. Xử lý theo kết quả:
   - **Đã chạy:** kiểm tra điều kiện cuối bước.
   - **Yêu cầu ghép nối hoặc lỗi cho biết chưa ghép nối:** chọn **Pairing / Ghép nối → Developer options → Wireless debugging → Pair device with pairing code**. Cho Shizuku nhận mã hoặc nhập mã theo giao diện thực tế. Xác nhận ghép nối thành công, quay lại bấm **Bắt đầu**, xử lý hộp thoại cho phép nếu xuất hiện rồi kiểm tra lại.
   - **Lỗi khác:** đọc thông báo và xử lý nguyên nhân, không mặc định mọi lỗi đều cần ghép nối.

Dùng uiautomator/XML hoặc công cụ sẵn có để đọc nút, mã và trạng thái. Nếu mã không xuất hiện trong hierarchy, thử cách đọc khác mà công cụ hỗ trợ. Nếu thao tác tự nhận mã bị gián đoạn, đọc lại trạng thái và đổi cách thao tác. Chỉ nhờ người dùng nhập mã/thao tác remote khi không thể thực hiện bằng công cụ, nói rõ đang thiếu gì. Không đoán mã hoặc cổng. `adb pair` trên máy tính không thay thế ghép nối riêng của Shizuku.

Nếu cách không dây không được hỗ trợ, mở **Start by connecting to a computer → View command** và chạy đúng lệnh app hiển thị qua ADB. Không đoán đường dẫn starter. Báo giới hạn tự khởi động của cách này.

Mở **Settings → Start on boot** và bật nếu cách khởi động hỗ trợ. Nếu app yêu cầu quyền hỗ trợ, cấp qua ADB rồi đọc lại trạng thái:

```bash
adb -s "$tv_serial" shell pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS
```

TCP/Watchdog là tùy chọn, không cần bật chỉ vì ảnh mẫu. Nếu yêu cầu tự khởi động chưa đáp ứng được, ghi rõ phần còn thiếu. App 13.7.0 có thể hiển thị server 13.6, đối chiếu phiên bản app trong About/thông tin ứng dụng.

**Chỉ chuyển sang bước 4 khi đồng thời:**

- Giao diện báo **Shizuku is running / Shizuku đang chạy**.
- `adb -s "$tv_serial" shell pidof shizuku_server` trả về PID.
- Không còn hộp thoại cấp quyền hoặc ghép nối chờ xử lý.

Bấm Bắt đầu, cho phép gỡ lỗi hay ghép nối thành công riêng lẻ đều chưa đủ. Nếu chưa đạt, xử lý tiếp tại bước 3 hoặc báo bước bị chặn, không chuyển sang cấp quyền Screen Off TV.

## 4. Cấp quyền và bật Screen Off TV

1. Mở Screen Off TV, chọn **Start service / Bật dịch vụ** nếu chưa bật.
2. Khi Shizuku hỏi quyền cho Screen Off TV, chọn **Allow all the time / Cho phép mọi lúc**. Chấp nhận quyền thông báo nếu TV hỏi cho ứng dụng.
3. Nếu đã cấp quyền nhưng dịch vụ chưa chạy, quay lại bật dịch vụ.
4. Nếu chưa sẵn sàng, kiểm tra Shizuku và quyền, dùng **Check Shizuku connection / Kiểm tra kết nối Shizuku** hoặc dừng/bật dịch vụ khi cần. Đọc lỗi trước khi xử lý, không lặp cài APK để chữa lỗi kết nối.

**Chuyển bước khi:** app báo **Shizuku: running · authorized / đang chạy · đã cấp quyền** và **Ready to control / Sẵn sàng điều khiển**.

## 5. Kiểm tra cấu hình

Mở **Settings / Cài đặt** và đọc đủ các mục:

| Ứng dụng | Mục | Giá trị |
|---|---|---|
| Shizuku | Start on boot | Bật nếu được hỗ trợ |
| Screen Off TV | Remote key / Phím điều khiển | Mute |
| Screen Off TV | Press action / Thao tác | Double press / Nhấn 2 lần |
| Screen Off TV | Screen-off mode / Chế độ tắt màn hình | Screen Off / Picture Off |
| Screen Off TV | Start when TV boots / Tự khởi động cùng TV | Bật |
| Screen Off TV | Language / Ngôn ngữ | Follow system / Theo hệ thống |

Dùng các giá trị trên, trừ khi người dùng yêu cầu cấu hình khác. Đọc lựa chọn trong mục Language: giao diện tiếng Việt không có nghĩa đã chọn cố định Tiếng Việt. Nếu đổi chế độ màn hình, dừng dịch vụ trước, đổi rồi bật lại.

**Chuyển bước khi:** đã đọc cấu hình thực tế, đáp ứng thay đổi được yêu cầu và app vẫn **Sẵn sàng điều khiển**. Nếu chưa thiết lập được phần người dùng yêu cầu, ghi là còn thiếu.

## 6. Báo kết quả

Dùng `required_installation_evidence` trong JSON làm checklist kết thúc, không cần đọc checklist thành lời trước khi thao tác. Báo ngắn gọn mỗi ID với kết quả và bằng chứng của lần chạy hiện tại, có thể gộp trong một bảng. Không cần dán toàn bộ XML hoặc log. Ví dụ:

```text
Kết quả: ready / incomplete — lý do
<ID>: passed / failed / blocked — giá trị quan sát, nguồn bằng chứng
Đã làm: phiên bản hai ứng dụng, có ghép nối hay không, quyền và cấu hình đã thiết lập
Chưa kiểm thử: remote thật, màn hình/âm thanh, reboot (nêu những mục chưa làm)
Còn thiếu: bước cụ thể và thông tin/thao tác cần hỗ trợ
```

Chỉ báo **đã cài đặt và cấu hình xong** khi đủ checklist và không còn cấu hình được yêu cầu chưa đáp ứng. `ready` là đã cấu hình, sẵn sàng điều khiển, không đồng nghĩa đã kiểm thử thực tế. Không có kết quả thao tác thì không kể như đã làm.

## Khi gặp vấn đề hoặc có yêu cầu thêm

Chỉ dùng nhánh liên quan, không thực hiện toàn bộ để phòng ngừa:

- **Không mở được app:** resolve activity với `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER PACKAGE`, nếu không có, thử `android.intent.category.LAUNCHER`. Chạy qua `adb -s "$tv_serial" shell`, mở component trả về bằng `am start -W -n COMPONENT`. Không đoán activity hoặc lặp tìm khi đã mở được app.
- **Có nhiều hồ sơ hoặc danh sách cấp quyền mơ hồ:** lấy user bằng `am get-current-user`, liệt kê package/UID bằng `cmd package list packages --user USER_ID -U PACKAGE`. Xác minh đúng package và UID của Screen Off TV, không chọn chỉ theo `new_user (10)` hay cấp cho mọi hồ sơ. Dùng `--user USER_ID` cho các lệnh hỗ trợ chọn user khi cài/mở/cấp quyền cho hồ sơ đó. Không cần kiểm tra UID riêng khi không có vấn đề hồ sơ.
- **Đổi phím theo yêu cầu:** bật dịch vụ → Settings → Remote key → Set custom remote key → nhấn phím thật → Confirm this key. Không đoán mã RED, sự kiện ADB không chứng minh nhận remote thật.
- **Thử màn hình/âm thanh theo yêu cầu:** mở nội dung, xác nhận có tiếng, thử phím tắt tắt/bật hình và quan sát màn hình thật cùng âm thanh. Ảnh ADB không chứng minh panel đã tắt.
- **Thử reboot theo yêu cầu:** kiểm tra `sys.boot_completed`, `pidof shizuku_server` và `dumpsys activity services com.thaicuonght.screenofftv` trước khi mở app hoặc khởi động thủ công, sau đó kiểm tra sẵn sàng. Công tắc bật không chứng minh tự khởi động thành công.

Sau mỗi lần xử lý, kiểm tra kết quả. Nếu cùng lỗi lặp lại mà không có thông tin mới, báo lỗi và bước cần hỗ trợ thay vì lặp cài APK, dump hoặc ghép nối vô hạn. Không tự sửa mã, commit Git hay phát hành APK trong tác vụ chỉ cài lên TV.
