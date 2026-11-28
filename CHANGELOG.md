# Changelog

## v0.8.2
- HV3601P Dashboard header displays the exact Work Mode label from Router Information, e.g. `Controller(Bridge)`, without translating it.
- F6601P Dashboard header displays the `Software Version` value from Router Information instead of a generic controller-mode label.
- AMOLED Black theme uses absolute black (`#000000`) for the app background and major surfaces, with dark controls and subtle borders.
- Other Router/API behavior remains unchanged.

## v0.8.1
- Dashboard header: for HV3601P, show the actual Work Mode from Router Information (for example, `Controller(Bridge)`) instead of the generic translated controller label.
- Remove the duplicate Refresh button from the Router Information card; use the existing Tools > Refresh now action to synchronize Router data.
- Keep F6601P header behavior and all other features unchanged.

## v0.8.0
- Fix HV3601P System Information retrieval by reading CPU, memory and uptime before opening the Work Mode page, preserving Status Manager page context.
- Keep the Work Mode value in Router Information as the original English mode label (for example, `Controller(Bridge)`) in all app languages.
- Preserve the existing device-info and reboot behavior.

# Changelog

## v0.8.0
- Fix HV3601P Router Information request order: initialize Status Manager and read `OBJ_DEVINFO_ID` before opening the Work Mode page.
- Keep Work Mode retrieval on `mpworkmode` + `multiap_work_mode_lua.lua`.
- Preserve reboot implementation from v0.7.9.8-beta.

# Changelog

## v0.8.0
- Fix Kotlin compilation: restore `initializeStatusManagerPage()` required by the F6601P Router Information path.
- Keep the dedicated HV3601P Work Mode API flow (`mpworkmode` + `multiap_work_mode_lua.lua`) unchanged.

# Changelog

## v0.8.0
- Fix HV3601P Work Mode detection using the same two-step API sequence as the Web UI: `menuView` page `mpworkmode`, followed by `menuData` API `multiap_work_mode_lua.lua`.
- Parse `CurrentWorkMode` from `OBJ_MAWORKMODE_ID`, with `ShowWorkMode` as fallback.
- Keep router authentication/session flow and F6601P Router Information behavior unchanged.

## 0.7.9.6-beta
- HV3601P Router Information: read Work Mode from the Status Manager HTML/JavaScript page because this firmware does not include it in `OBJ_DEVINFO_ID`.
- Parse the mode code and display its Web UI label (for example, `controller_bridge` → `Controller(Bridge)`). Unknown mode codes remain visible rather than being silently discarded.
- Add localized Work Mode row labels in Vietnamese, English and Simplified Chinese.
- F6601P Router Information, login/session, Mesh parsing and reboot behavior are unchanged.
- versionCode 76 / versionName `0.7.9.6-beta`.
- Source package only; APK compilation and physical-router verification have not been performed in this environment.

# Changelog

## 0.7.9.5-beta
- Fix Reboot HTTP 400: include the mandatory ZTE `Check` header, calculated as Base64(RSA-PKCS1v1.5(SHA-256(exact URL-encoded form body))).
- Keep the one-time `_sessionTmpToken` dynamic and send the exact reboot form fields (`IF_ACTION`, `Btn_restart`, `_sessionTOKEN`).
- Apply the shared ZTE dark rounded dialog style to the Reboot confirmation, matching Router selection dialogs.

# Changelog

## v0.7.9.5-beta
- Add automatic recovery after a confirmed Router Reboot on HV3601P v9.3 and F6601P VT9.3.10P2N7.
- Pause scheduled sync while rebooting and preserve stored credentials.
- Poll Router reachability every 10 seconds for up to 5 minutes.
- After the Router responds, create a new API client, auto-login with saved credentials, and refresh Topology/Clients/Router Information.
- Retry login if the Router HTTP service is back before its login endpoint is ready.
- On timeout, show a retry-needed status; do not clear credentials.
- versionCode 74 / versionName `0.7.9.5-beta`.
- Source package only; APK compilation and physical Router reboot tests have not been performed in this environment.

## v0.7.9.3-beta — Topology arrow fix + Router reboot
- Topology SVG flow arrows are now created hidden and remain hidden until their first valid position and rotation are assigned. This prevents the brief flash at the SVG origin/top-left corner after an animation/layout restart.
- Preserve existing connector geometry, red path styling, arrow direction, animation duration, layout observer and resize scheduling.
- Add `Reboot Router` to Tools for both supported profiles: HV3601P v9.3 FPT and F6601P VT9.3.10P2N7.
- Show a confirmation dialog before sending the command. It warns about temporary Wi-Fi/Internet interruption and an estimated reboot period of about 5 minutes.
- Reboot page is initialized first; the current `_sessionTmpToken` is extracted dynamically from its HTML. The app then posts `IF_ACTION=Restart` to `devmgr_restartmgr_lua.lua` using the existing authenticated native client/cookies.
- Validate the XML success fields `IF_ERRORPARAM=SUCC` and `IF_ERRORTYPE=SUCC`; report failure without claiming the router rebooted.
- No Factory Reset control is added. Credentials and Mesh snapshot are not cleared by the reboot command.
- versionCode 73 / versionName `0.7.9.3-beta`.
- Source package only. APK build and physical-router reboot testing have not been performed in this environment.


## v0.7.9.2 — Patch Release
- Dashboard overview: wireless Agent Backhaul now displays `Wi-Fi 5GHz` / `Wi-Fi 2.4GHz` instead of a bare band value.
- All AP list: wireless Backhaul now displays with the `Wi-Fi` prefix; wired remains `Cáp LAN`.
- Node detail popup: keep the Backhaul field as `Wi-Fi 5GHz` / `Wi-Fi 2.4GHz` (inherited from v0.7.9).
- Node detail popup Client list and Clients tree: client band now displays `Wi-Fi 2.4GHz` / `Wi-Fi 5GHz`; 802.11 mode labels remain unchanged.
- Display-only changes. No API, parsing, authentication, session, Topology connections, or router control behavior changed.
- versionCode 72 / versionName `0.7.9.2`.
- Source package only; APK build and physical-device verification have not been performed in this environment.

## v0.7.9.1 — Patch Release
- Vietnamese Node detail popup heading changed from `THÔNG TIN NÚT MẠNG` to `THÔNG TIN NODE`.
- Preserve the English technical terms `Agent` and `Node`.
- Only the Vietnamese translation value for `NODE INFORMATION` is changed. Other translations and all other UI locations remain unchanged.
- No changes to API requests, data parsing, Topology, login, authentication or session handling.
- versionCode 71 / versionName `0.7.9.1`.
- Source package only; APK build and physical-device verification have not been performed in this environment.

## v0.7.9 — Release
- Node detail popup: display wireless backhaul as `Wi-Fi 5GHz` / `Wi-Fi 2.4GHz` in the **Loại kết nối** field.
- Node detail popup: display wired backhaul as `Cáp LAN` in that same field.
- Scope is deliberately limited to the Node detail popup's connection-type value. Other UI locations, underlying data, parsers, API calls, login/session behavior and Topology layout/animation are unchanged.
- Promote the v0.7.8-beta source baseline to release version v0.7.9.
- versionCode 70 / versionName `0.7.9`.
- Source package only; APK build and physical-device verification have not been performed in this environment.

## v0.7.8-beta
- Topology connector layout: defer SVG connector geometry and animated-arrow startup until the Mesh tab is visible and has a measurable layout.
- Wait for two animation frames after rendering/tab activation before measuring node card positions, avoiding zero/stale coordinates while the Mesh page is hidden.
- Recalculate connector geometry when the mesh tree or individual node cards change size via `ResizeObserver`; retain a debounced window-resize fallback.
- Stop arrow animation while leaving the Mesh tab and restart it after returning, preventing animation against hidden/stale geometry.
- Node detail popup: reverse swipe-to-dismiss direction. A downward swipe while the popup is already scrolled to its top now closes it; upward swipes no longer dismiss it. The × button remains available.
- Preserve Router Information, login/session, topology parsing, Clients and Router API behavior.
- versionCode 69 / versionName `0.7.8-beta`.
- Source package only. APK build and on-device regression testing are not verified in this environment.

## v0.7.7-beta
- Router Information: initialize the ZTE Status Manager page before reading XML data on both HV3601P and F6601P.
- Request XML endpoints with an explicit XML Accept header.
- Add safe diagnostics for parsed XML object instance counts, missing-object response shape, and stage error details; do not log router secrets or response payloads.
- Node detail popup in Topology can be dismissed by swiping upward while already at the top of the popup; the close (×) button remains available.
- Preserve login/session, topology, clients, and existing router API flows.

\n## v0.6.3-beta6
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
