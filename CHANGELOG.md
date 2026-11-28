## v0.7.3

- Sửa bộ đếm Agent trên Dashboard: tiếng Việt hiển thị `0 Agent`, không còn dịch thành “0 nút Agent”. Giữ nguyên thuật ngữ Agent.
- Thêm thẻ “Dự án trên GitHub” ngay dưới phần chọn giao diện trong Tools, kèm mô tả và nút mở repository `https://github.com/baolich2/zte-mesh-manager` bằng trình duyệt/ứng dụng GitHub bên ngoài.
- Bổ sung bản dịch cho mục GitHub trong English và 简体中文.
- Giữ nguyên các chức năng và cấu hình từ v0.7.2.
- versionCode 64, versionName 0.7.3.
- Source-level update; chưa xác nhận APK build trong môi trường hiện tại.

## v0.7.1

- Dịch `Backhaul` thành “Loại kết nối”, `Link Speed` thành “Tốc độ kết nối”, và `ETH` thành “Cáp LAN” trong giao diện tiếng Việt.
- Giữ nguyên các thuật ngữ `Mesh`, `Agent` và `Node` trong tiếng Việt; không tự dịch `Agent` thành “Nút phụ”.
- Sửa bản dịch tiếng Anh trong hộp thoại chi tiết Node: số thiết bị đọc từ popup, ghi chú nguồn dữ liệu, trạng thái Node không có Client và thông báo về việc không suy đoán Client đều được bản địa hóa.
- Sửa nhãn Dashboard “Dữ liệu Mesh · từ Topology” để hiển thị “Dữ liệu Mesh · từ Sơ đồ mạng” trong tiếng Việt.
- Thêm lựa chọn giao diện trong Tools: Sáng (Light), Tối (Dark – giao diện xanh đen hiện tại), Đen AMOLED. Lựa chọn được lưu độc lập và áp dụng khi mở lại ứng dụng.
- versionCode 62, versionName 0.7.1.
- Source-level update; chưa xác nhận APK build trong môi trường hiện tại.

## v0.7.0

- Sau khi đăng nhập Router thành công, ẩn nút kết nối/đăng nhập ở Dashboard và Tools; tại Tools chỉ hiện nút Đăng xuất khi đang đăng nhập. Sau khi đăng xuất, nút đăng nhập xuất hiện trở lại ở cả hai vị trí.
- Đồng bộ trạng thái đăng nhập của các nút ngay khi Dashboard cập nhật snapshot, sau khi đăng nhập thành công và sau khi đăng xuất.
- Sửa nội dung mô tả ngôn ngữ tiếng Việt: loại bỏ câu có tiếng Trung, thay bằng “Toàn bộ giao diện được dịch; thuật ngữ kỹ thuật giữ nguyên bằng tiếng Anh.”
- Màn hình chọn Router: bỏ chú thích “đang hỗ trợ” ở HV3601P và “hồ sơ API riêng/API riêng” ở F6601P; chỉ hiển thị tên model và firmware. Áp dụng cho cả bộ chọn trong Tools và màn hình thiết lập ban đầu.
- Giữ nguyên luồng đăng nhập/session HV3601P và F6601P, xử lý Session Conflict, parser Topology, Clients, xử lý Node 0 Client, chu kỳ đồng bộ mặc định 180 giây và giao diện hộp thoại từ v0.6.13.
- versionCode 61, versionName 0.7.0.
- Source-level update; chưa xác nhận APK build trong môi trường hiện tại.

## v0.6.13

- Đồng bộ giao diện các hộp thoại native (chọn Router, xác nhận đổi Router, Session Conflict) theo phong cách tối màu, bo góc và màu sắc của màn hình Initial Setup; giữ nguyên thao tác/API và yêu cầu xác nhận `login_preempt`.
- Tăng chu kỳ tự động đồng bộ mặc định từ 30 giây lên 180 giây; người dùng vẫn có thể chỉnh trong khoảng 10–3600 giây. Thiết lập chu kỳ đã lưu trước đó được giữ nguyên.
- F6601P: chỉ hiển thị “Sai tài khoản hoặc mật khẩu” khi phản hồi `loginErrMsg` của Router có dấu hiệu từ chối thông tin xác thực; lỗi ở bước khác hiển thị thông báo chưa hoàn tất kèm nguyên nhân, không quy chụp là sai mật khẩu.
- Tự đăng nhập: giữ thông tin đăng nhập đã lưu khi gặp lỗi mạng/phiên/Topology; chỉ xóa thông tin đã lưu khi Router trả về dấu hiệu từ chối xác thực rõ ràng.
- Giữ nguyên luồng phát hiện Session Conflict, chọn phiên, xác nhận và preempt đã được người dùng xác nhận hoạt động trực tiếp không cần Web UI.
- Giữ xử lý Node 0 Client từ v0.6.12, parser Topology, quan hệ Node và giao diện Clients.
- Giữ nguyên luồng đăng nhập/session HV3601P và hồ sơ API riêng F6601P.
- versionCode 60, versionName 0.6.13.
- Source-level update; chưa thể xác nhận APK build trong môi trường này nếu thiếu Android SDK/Gradle.

## v0.6.10

- F6601P: khi đăng nhập gặp trang xung đột phiên, đọc HTML Router và trích xuất `preempt_sessid` động cùng nhãn phiên; không lưu hoặc hard-code ID.
- Hiển thị hộp thoại cho người dùng chọn phiên quản trị cần ngắt; chỉ gửi `login_preempt` sau khi nhấn xác nhận. Hủy sẽ không gửi lệnh thu hồi phiên. Sau khi Router xác nhận, app tạo client mới và thử đăng nhập lại theo nguyên luồng beta7.
- Giữ nguyên luồng đăng nhập/session HV3601P và F6601P beta7; không thay đổi challenge/hash.
- Sửa nhãn tiếng Việt `Controller mode` thành `Chế độ bộ điều khiển`; English và 简体中文 vẫn giữ nhãn theo bản dịch tương ứng.
- Source-level update. Môi trường hiện tại không có Android SDK, Gradle executable hoặc `gradle-wrapper.jar`, vì vậy chưa thể tạo APK hoặc xác nhận biên dịch. Cần build và kiểm thử trên Android Studio/Router thật.

## v0.6.9

- Gộp chọn ngôn ngữ và chọn loại Router vào một màn hình thiết lập lần đầu; Tiếng Việt được chọn sẵn, HV3601P v9.3 là model mặc định được chọn sẵn.
- Lưu đồng thời `ui_language`, `router_model` và IP mặc định tương ứng khi nhấn Tiếp tục. Cài đặt hiện có không bị đưa lại vào onboarding.
- Giữ màn hình chọn ngôn ngữ trong Tools để đổi ngôn ngữ sau này; giữ bộ chọn Router hiện có để đổi model sau này.
- Bổ sung `ZteApiClient.preemptLoginSession(preemptSessionId)` theo capture F6601P (`login_preempt`, `action=preempt`), kiểm tra phản hồi `login_need_refresh=true`.
- Chưa tự động gọi `login_preempt`: cần xác định nguồn lấy `preempt_sessid` động và yêu cầu xác nhận trước khi thu hồi phiên quản trị khác. Không hard-code session ID.
- Giữ nguyên quy trình đăng nhập F6601P beta7 và HV3601P; chưa thay đổi challenge/hash/session flow.
- Source-level update; chưa build APK hoặc kiểm thử trên router thật trong môi trường này.

# CHANGELOG — ZTE Mesh Manager

## v0.7.2

- Sửa lịch đồng bộ khi ứng dụng quay lại foreground: không gọi Router ngay chỉ vì `onResume()` được kích hoạt. App tính thời gian còn lại dựa trên `last_sync_epoch`; nếu chưa đủ chu kỳ đã chọn (mặc định 180 giây), giữ nguyên session và snapshot, chờ đến hạn mới đồng bộ.
- Nếu đã quá chu kỳ đồng bộ khi quay lại app, thực hiện đồng bộ ngay. Nút Tải lại/Cập nhật ngay vẫn hoạt động tức thì theo yêu cầu người dùng.
- Giữ nguyên xử lý SessionTimeout: chỉ thử tự đăng nhập lại khi Router thực sự trả về dấu hiệu phiên hết hạn; lỗi mạng thông thường không bị coi là hết phiên.
- Đổi toàn bộ nhãn tiếng Việt `Sơ đồ mạng` thành `Sơ đồ kết nối`, bao gồm tab, nhãn động và phần tóm tắt nguồn dữ liệu.
- versionCode 63, versionName 0.7.2.
- Chưa xác nhận APK build trong môi trường đóng gói; cần build và kiểm tra thực tế trên Android/Router.


## v0.6.8

- **Fix build:** moved `modelKey` lookup to the beginning of `AdminActivity.doLogin()`, before localized URL validation uses it. The same selected model key is then passed to `ZteApiClient`; removed the duplicate inner declaration that caused three `Unresolved reference: modelKey` compiler errors.
- Kept model-specific default HTTPS URLs: F6601P `https://192.168.1.1/`; HV3601P `https://192.168.1.11/`.
- Preserved manually configured custom Router URLs when switching models.
- Dashboard model title now follows the selected Router: `F6601P` or `HV3601P v9.3`.
- Replaced the fixed `Controller Bridge` label with `Controller mode`.
- Added translations for the Router-model selection action in Tools.
- No intended changes to login challenge/hash, session handling, Router API request sequence, or mesh parsing.

## Current support and test status

- HV3601P v9.3 firmware FPT: existing model-specific login/API/parser path retained.
- F6601P VT9.3.10P2N7: user-confirmed successful login on beta7. Topology/Agent/Clients behavior still requires broader real-router testing; do not interpret login success as full feature validation.
- Firmware variants may return different JSON structures. Add parser support only after inspecting the actual response from that firmware.
- Source-level fix only: APK compilation and on-device regression tests have not been performed in this packaging environment.

## Earlier development history\n\n## v0.6.3-beta6
- F6601P: after `closeaction`, follow the captured browser sequence: load `menuView/mmTopology`, fetch `topo_lua.lua`, then fetch `topo_save_lua.lua`.
- Fixes the previous out-of-order topology bootstrap that could produce an XML `SessionTimeout` response despite a successful `closeaction`.
- HV3601P authentication flow remains unchanged.
- APK not built in this environment; Android SDK/Gradle availability is not guaranteed.

# Changelog

## v0.6.3-beta6
- Sửa thứ tự khởi tạo phiên F6601P: không tải lại trang gốc sau login_need_refresh; đi theo luồng browser đã capture: sntp_data rồi closeaction.
- Kiểm tra phản hồi closeaction linh hoạt với khoảng trắng/chữ hoa thường; ghi log phản hồi đã lọc thông tin nhạy cảm nếu Router từ chối.
- Không thay đổi luồng xác thực HV3601P hoặc cơ chế session baseline.
- Chưa build APK/kiểm thử trực tiếp trên router trong môi trường hiện tại.

# Changelog – ZTE Mesh Manager v0.6.3 beta 4

- Sửa lỗi đổi ngôn ngữ liên tục VI → EN → 中文 → VI khiến một số thành phần giữ bản dịch cũ.
- Lưu nội dung nguồn riêng, xử lý các thay đổi DOM đang chờ trước khi đổi ngôn ngữ, tạm dừng observer trong lúc khôi phục và áp dụng locale mới.
- Giữ nhãn thanh điều hướng theo từ điển riêng nhưng luôn dùng Tiếng Việt làm chuỗi nguồn để có thể đổi qua lại nhiều lần.
- Không yêu cầu tắt/mở lại ứng dụng; không thay đổi đăng nhập, session token, API Router hoặc dữ liệu Mesh.
- Đã kiểm tra cú pháp JavaScript bằng Node.js; chưa biên dịch APK do môi trường không có Android SDK/Gradle wrapper đầy đủ.

# Changelog – ZTE Mesh Manager v0.6.3 beta 3

- Hoàn thiện bước F6601P Easy Setup → Advanced Setup bằng POST `wizardData/closeaction`.
- Payload lấy theo capture: `_sessionTOKEN` dùng `sess_token` vừa nhận từ `login_entry`; `IF_CLOSE_REDIRECTPAGE` gửi giá trị rỗng.
- Kiểm tra response `IF_ERRORPARAM=SUCC` và `IF_ERRORID=0` trước khi tiếp tục xác minh Topology.
- Không hard-code session token; giữ nguyên luồng đăng nhập HV3601P.
- Chưa thể xác nhận build APK hoặc kiểm thử router thật trong môi trường đóng gói này.

# Changelog – ZTE Mesh Manager v0.6.3 beta 2

- Thêm nhánh đăng nhập F6601P VT9.3.10P2N7 tách biệt với HV3601P.
- Thêm parser Topology F6601P cho controllerData và agentlay1–agentlay8; giữ quan hệ cha-con từ instID_Parent.
- Thêm truy vấn GetALLAP, GetALLClients và GetPopData; chuẩn hóa AP/client về cấu trúc Dashboard hiện có.
- Giữ nguyên luồng đăng nhập HV3601P.
- F6601P là tích hợp thử nghiệm, cần xác minh trên router thật có Agent và Client.

## v0.6.3 beta 1
- Thêm hộp thoại chọn loại Router ở lần mở ứng dụng đầu tiên.
- Thêm mục đổi loại Router trong Công cụ và màn hình quản lý kết nối; đổi cấu hình sẽ khởi động lại Dashboard.
- Giữ thông tin đăng nhập đã lưu khi đổi cấu hình Router.
- Hồ sơ F6601P hiện chỉ là lựa chọn thử nghiệm; chưa bật đăng nhập/API/parser để tránh đọc sai dữ liệu bằng parser HV3601P.
- Sửa kiểm tra địa chỉ IPv4 riêng dải 172.16.0.0/12 để tránh so sánh kiểu Int? với IntRange.
- Bỏ nhãn return@setSingleChoiceItems trong callback chọn Router để tránh lỗi phân giải nhãn lambda trên Kotlin compiler.
- Không thay đổi thuật toán đăng nhập của HV3601P trong bản này.

# CHANGELOG

## v0.6.2 – Rà soát bản dịch toàn ứng dụng

- Rà soát lại chuỗi giao diện tĩnh, nội dung động từ JavaScript và trạng thái do Kotlin truyền sang cho Tiếng Việt, English và 简体中文.
- Bổ sung dịch đầy đủ cho câu trạng thái “dữ liệu vừa đồng bộ” ở trạng thái tự đăng nhập, đăng nhập thủ công và đồng bộ định kỳ; giữ nguyên thời gian đọc.
- Bổ sung xử lý các trạng thái không có snapshot, đăng xuất, hết hạn phiên và lỗi đồng bộ để không đẩy nguyên văn tiếng Việt vào giao diện English/中文.
- Bổ sung lưu lựa chọn ngôn ngữ sang SharedPreferences để màn hình đăng nhập Router native cũng hiển thị theo ngôn ngữ đã chọn.
- Bổ sung bản dịch cho các nhãn, hướng dẫn, nút và thông báo của màn hình đăng nhập Router native.
- Cập nhật README và dòng phạm vi hỗ trợ: “Tạm thời chỉ hỗ trợ ZTE HV3601P v9.3 – firmware FPT.”
- Không thay đổi thuật toán đăng nhập, quản lý phiên, dữ liệu Mesh, topology connectors hoặc chức năng đồng bộ.
- Không thêm lại Router Information hoặc node ảo.

## v0.6.1 – Sửa bản dịch trạng thái Dashboard

- Bổ sung bản dịch trạng thái Kotlin và nội dung tổng kết đồng bộ.
- Sửa nhận diện chuỗi tổng kết động và cập nhật text node.

## v0.6.0 – Dọn sơ đồ Mesh

- Gỡ giao diện tùy chỉnh node ảo, dữ liệu node ảo và logic tạo node ảo.
- Bổ sung bản dịch giao diện Tiếng Việt, English và 简体中文.
- Sửa metadata IP/vai trò trong Topology, thay `<br>` bằng dấu phân cách ` · `.

## Lịch sử trước v0.6.0

Các phiên bản thử nghiệm 0.5.x: sửa đường nối sơ đồ, thử nghiệm node ảo và chẩn đoán Router Information. Router Information đã được loại bỏ từ v0.6.0.

## Kiểm chứng v0.6.2

- Đã kiểm tra cấu trúc ZIP và rà soát mã nguồn Kotlin/JavaScript/HTML.
- Đã kiểm tra cú pháp JavaScript bằng Node.js.
- Chưa chạy Android Gradle build và chưa kiểm thử trên Router/điện thoại thật trong môi trường tạo ZIP.


## v0.6.11
- F6601P: giữ nguyên ZteApiClient và CookieManager của lần đăng nhập thất bại khi tải trang xung đột, đọc danh sách phiên và lấy preempt_sessid động. Không tạo client mới làm mất SID trước bước xác nhận.
- Chỉ gửi login_preempt sau khi người dùng chọn phiên và xác nhận.
- Giữ nguyên luồng đăng nhập HV3601P.


## v0.6.12
- Clients/Topology: phân biệt Node đã đọc thành công nhưng không có Client (reportedClientCount/clientCount = 0, không có readError) với Node chưa có dữ liệu hoặc gặp lỗi API/parser. Node rỗng hiển thị `0 Client`; cửa sổ chi tiết thông báo “Không có thiết bị nào đang kết nối với Node này.”
- Clients: trạng thái rỗng của từng AP hiển thị “Không có Client đang kết nối.” khi dữ liệu xác nhận số lượng bằng 0; vẫn giữ thông báo lỗi khi không đọc được dữ liệu.
- Màn hình chọn Router: đánh dấu cả hai hồ sơ là “Đã hỗ trợ”; hiển thị rõ HV3601P v9.3 firmware FPT và F6601P firmware VT9.3.10P2N7, đồng bộ cách trình bày.
- README cập nhật F6601P thành hồ sơ đã hỗ trợ (VT9.3.10P2N7), đồng thời ghi rõ chưa khẳng định tương thích với mọi firmware F6601P khác.
- Giữ nguyên luồng đăng nhập/session và parser Topology; không thay đổi xử lý preempt F6601P hoặc xác thực HV3601P.
- Tăng versionCode lên 59, versionName lên 0.6.12.
- Source update. Chưa xác nhận Android APK build trong môi trường này vì không có Android SDK, Gradle executable hoặc gradle-wrapper.jar.
