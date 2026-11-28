# ZTE Mesh Manager v0.7.3

Ứng dụng Android quản lý Router/Mesh ZTE qua Native API HTTPS nội bộ.

## Trạng thái phiên bản

- **v0.7.3**: sửa bộ đếm tiếng Việt thành `0 Agent`; thêm mục Dự án trên GitHub trong Tools với liên kết mở repository.

- **v0.7.2**: trì hoãn đồng bộ khi quay lại ứng dụng đến đúng hạn chu kỳ; đổi nhãn tiếng Việt Topology thành “Sơ đồ kết nối”.
- **v0.7.1**: dịch Backhaul/Link Speed/ETH; sửa bản dịch tiếng Anh trong popup chi tiết Node; dịch nhãn Topology ở Dashboard; thêm giao diện Sáng/Tối/Đen AMOLED và lưu lựa chọn.

- **v0.7.0**: ẩn nút đăng nhập khi đã đăng nhập; nút Đăng xuất chỉ hiện khi có phiên; sửa mô tả ngôn ngữ tiếng Việt; tinh gọn nhãn chọn Router.
- **v0.6.13**: đồng bộ giao diện hộp thoại native; chu kỳ tự động đồng bộ mặc định 180 giây; phân loại lỗi xác thực F6601P và giữ thông tin đăng nhập khi lỗi sau xác thực.
- **v0.6.12**: sửa hiển thị Node không có Client; cập nhật trạng thái hỗ trợ F6601P và thông tin firmware trên màn hình chọn Router.
- **F6601P: ĐÃ HỖ TRỢ** hồ sơ `F6601P_VT9310P2N7` / firmware `VT9.3.10P2N7`.
- **HV3601P v9.3: ĐÃ HỖ TRỢ** hồ sơ `HV3601P_V93_FPT` / firmware FPT.
- Chưa có xác nhận kiểm thử đầy đủ tất cả chức năng trên mọi firmware ZTE. Không nên coi hai hồ sơ này tương thích với mọi firmware/model cùng hãng.

## Router model và địa chỉ mặc định

| Model đã chọn | Tên hiển thị | HTTPS mặc định |
|---|---|---|
| F6601P | `F6601P` · firmware `VT9.3.10P2N7` | `https://192.168.1.1/` |
| HV3601P v9.3 | `HV3601P v9.3` · firmware FPT | `https://192.168.1.11/` |

- Khi đổi model, ứng dụng chỉ tự đổi địa chỉ nếu địa chỉ đang lưu là địa chỉ mặc định của model cũ.
- Địa chỉ do người dùng tự nhập được giữ nguyên.
- Dashboard hiển thị tên theo model đã chọn, không gắn cứng HV3601P.
- Nhãn `Controller Bridge` được đổi thành `Controller mode`; giao diện tiếng Việt hiển thị `Chế độ bộ điều khiển`.
- F6601P: hỗ trợ đọc danh sách phiên quản trị từ trang xung đột, hiển thị xác nhận trước khi gửi `login_preempt` và đăng nhập lại sau khi Router xác nhận.
- Mục chọn loại Router trong Tools có bản dịch Tiếng Việt, English và 简体中文.

## Tính năng chính

- Đăng nhập Router bằng Native API HTTPS.
- Đọc dữ liệu Mesh/Topology theo API và cấu trúc phản hồi của từng hồ sơ firmware.
- Hiển thị Controller, Agent, thông tin backhaul/link và Clients khi Router cung cấp dữ liệu tương ứng.
- Sơ đồ Topology và danh sách Clients.
- Tìm kiếm/lọc thiết bị.
- Tự động đồng bộ theo chu kỳ cấu hình (mặc định 180 giây; tùy chỉnh 10–3600 giây). Khi quay lại ứng dụng từ nền, app chỉ đồng bộ nếu chu kỳ đã đến hạn; không tự gọi Router chỉ vì Activity được resume.
- Giao diện Tiếng Việt, English và 简体中文.

## Hồ sơ API

### HV3601P v9.3 FPT
Giữ nhánh xác thực và parser riêng hiện có. Không dùng parser F6601P để thay thế hồ sơ này.

### F6601P VT9.3.10P2N7
Dùng luồng đăng nhập và parser riêng. Parser topology xử lý `controllerData` cùng các lớp `agentlay1`–`agentlay8`; quan hệ cha-con dựa trên trường định danh parent của firmware. Cấu trúc JSON của F6601P có thể khác giữa các firmware, vì vậy cần lấy response thực tế từ Router và bổ sung parser tương ứng khi gặp biến thể mới.

F6601P được đánh dấu Đã hỗ trợ cho hồ sơ firmware `VT9.3.10P2N7`. Khả năng tương thích với firmware F6601P khác hoặc biến thể API chưa được xác nhận; cần kiểm thử trên Router thực tế, đặc biệt với hệ thống nhiều Agent/Client.

## Bảo toàn đăng nhập và phiên

- Giữ luồng xác thực/session hiện có của từng model; không gộp hai parser hoặc hai quy trình xác thực.
- Tự đăng nhập khi mở ứng dụng nếu thông tin đăng nhập đã được lưu và người dùng chưa chủ động đăng xuất.
- Khi đổi Router, chỉ thay phiên và snapshot đang hoạt động sau khi Router mới xác thực và đọc dữ liệu thành công.
- Không hard-code session token; token phải lấy từ phản hồi đăng nhập hiện tại.

## Thiết lập lần đầu

- Lần đầu cài mới, ứng dụng hiển thị chung một màn hình để chọn ngôn ngữ (mặc định chọn sẵn Tiếng Việt) và loại Router.
- Chọn HV3601P v9.3 sẽ đặt IP mặc định `https://192.168.1.11/`; chọn F6601P sẽ đặt `https://192.168.1.1/`.
- Nhấn Tiếp tục để lưu đồng thời ngôn ngữ và model. Các lần mở sau vào Dashboard; có thể đổi ngôn ngữ trong Tools và đổi Router qua chức năng chọn model.
- Cài đặt hiện có đã lưu model sẽ không bị ép chạy lại màn hình thiết lập ban đầu.

## Xung đột phiên quản trị F6601P

- Đã bổ sung hàm native `preemptLoginSession(preemptSessionId)` theo request capture: POST `?_type=loginData&_tag=login_preempt`, payload `_type=loginData`, `_tag=login_preempt`, `preempt_sessid` và `action=preempt`; response mong đợi có `login_need_refresh=true`.
- Khi phát hiện trang xung đột, ứng dụng đọc `preempt_sessid` động và hiển thị danh sách phiên để người dùng chọn; chỉ gửi `login_preempt` sau khi người dùng xác nhận. Hủy thao tác sẽ không thu hồi phiên. Sau khi Router xác nhận preempt, ứng dụng thử đăng nhập lại. Không dùng giá trị session mẫu đã capture làm hằng số.

## Build APK bằng Android Studio

1. Giải nén ZIP.
2. Trong Android Studio, chọn **Open** và mở thư mục gốc có `settings.gradle.kts`.
3. Chờ Gradle Sync hoàn tất.
4. Chọn **Build → Build APK(s)**.
5. Nếu build thất bại, mở cửa sổ **Build Output**, lấy toàn bộ lỗi đầu tiên (không chỉ dòng `Generate APKs`) để kiểm tra.

Yêu cầu build hiện tại: Android Gradle Plugin `8.7.3`, Kotlin `2.0.21`, compile SDK `35`, Java/JVM target `17`. Dự án không kèm `gradle-wrapper.jar`; Android Studio cần có Gradle phù hợp hoặc người build cần cấu hình Gradle cục bộ.

## Ghi chú kiểm thử
- v0.6.12: đã sửa logic phân biệt Node có 0 Client (API trả về thành công, số lượng 0) với lỗi đọc dữ liệu hoặc chưa có bản ghi chi tiết.
- v0.6.12: màn hình chọn Router hiển thị “Đã hỗ trợ” cho cả HV3601P v9.3 firmware FPT và F6601P firmware VT9.3.10P2N7.

- v0.6.8 sửa lỗi Kotlin compile do `modelKey` được tham chiếu trước khi khai báo trong `AdminActivity.doLogin()`.
- v0.6.9 thêm màn hình thiết lập ban đầu kết hợp ngôn ngữ + Router và API helper `login_preempt` (chưa tự động hóa do chưa xác định nguồn session ID động).
- Đã rà soát mã nguồn và cấu trúc gói; chưa thể xác nhận APK build thành công trong môi trường đóng gói này vì không có Android SDK/Gradle đầy đủ.
- Sau khi build, cần kiểm thử riêng: đăng nhập F6601P, đăng nhập HV3601P, đổi model và IP mặc định, giữ IP tùy chỉnh, đổi ngôn ngữ, Topology có/không có Agent và danh sách Clients.


### v0.6.11 note
F6601P conflict-session handling reuses the same HTTP client/cookie jar from the failed login attempt so the Router conflict page can be read with its active session context. The app still requires explicit user confirmation before preempting another admin session.

### v0.6.12 note
Node detail now distinguishes a successful response with zero connected Clients from a missing detail record or an API/parser error. The Router selection screen marks both supported profiles and shows their target firmware.

## Giao diện hộp thoại và đồng bộ

- Các hộp thoại native dùng chung phong cách tối màu, bo góc, viền xanh xám và nút nhấn xanh ngọc để đồng bộ với Initial Setup.
- Chu kỳ đồng bộ mặc định cho cài đặt mới là 180 giây. Giá trị người dùng đã lưu từ phiên bản trước không bị ghi đè; có thể chỉnh trong Dashboard.
- Với F6601P, thông báo sai tài khoản/mật khẩu chỉ dùng khi phản hồi Router thể hiện dấu hiệu từ chối xác thực. Lỗi mạng, phiên hoặc đọc Topology được hiển thị riêng, không tự động xóa thông tin đăng nhập đã lưu.

## Ghi chú v0.7.0

- Khi Router đã đăng nhập, nút kết nối/đăng nhập được ẩn ở Dashboard và Tools. Đăng xuất trong Tools sẽ làm các nút đăng nhập xuất hiện trở lại.
- Phần mô tả ngôn ngữ tiếng Việt không còn chứa ký tự/câu tiếng Trung; các thuật ngữ kỹ thuật vẫn giữ bằng tiếng Anh.
- Bộ chọn Router chỉ hiển thị model và firmware, không kèm nhãn “đang hỗ trợ” hoặc “API riêng”.

## Repository

- GitHub: https://github.com/baolich2/zte-mesh-manager
