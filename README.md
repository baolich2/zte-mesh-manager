# ZTE Mesh Manager — v0.8.2

Android application for managing supported ZTE Router/Mesh systems through their native HTTPS APIs. The app reads Router, Topology, Access Point and Client information without using the router's administration WebView for the normal dashboard workflow.

## Current release

**v0.8.2 · versionCode 82**

### Changes in v0.8.2
- HV3601P Dashboard header shows the firmware Work Mode label exactly as supplied, e.g. `Controller(Bridge)`; automatic Vietnamese translation is bypassed for this value.
- AMOLED Black now uses pure black (`#000000`) for the screen background and major UI surfaces to reduce OLED pixel illumination.
- F6601P behavior and existing router functions remain unchanged.
- Source package only; build and verify the APK in Android Studio.

### Changes in v0.7.9.5-beta
- Fix Topology animated-arrow flicker at the upper-left corner: newly created SVG arrow polygons start hidden and are shown only after a valid position/rotation transform has been calculated.
- Keep the existing red connector paths, arrow direction, animation timing, Topology layout and observer scheduling.
- Add a guarded `Reboot Router` action to Tools for both supported profiles (HV3601P v9.3 FPT and F6601P VT9.3.10P2N7).
- Reboot requires an explicit confirmation dialog warning that Wi-Fi/Internet will be interrupted and that startup may take about 5 minutes.
- Reboot reads the one-time `_sessionTmpToken` dynamically, then signs the exact URL-encoded form body with the ZTE RSA `Check` header (`Base64(RSA-PKCS1v1.5(SHA-256(body)))`).
- Sends `POST /?_type=menuData&_tag=devmgr_restartmgr_lua.lua` with `IF_ACTION=Restart`, `Btn_restart` and `_sessionTOKEN`, then validates the Router's XML success response.
- Reboot confirmation uses the same dark rounded ZTE dialog style as Router selection.
- No factory-reset action is exposed. Reboot does not clear saved credentials or the existing Mesh snapshot.
- Source package only; Android APK build and physical-router reboot testing are not verified in this environment.

### Changes in v0.7.9.2
- Add the `Wi-Fi` prefix to wireless Backhaul labels in Dashboard overview and All AP.
- Add the `Wi-Fi` prefix to client band labels in the Node detail popup and Clients tree: `Wi-Fi 2.4GHz` / `Wi-Fi 5GHz`.
- Wired Backhaul continues to display as `Cáp LAN`.
- Existing Node detail popup connection type remains `Wi-Fi 5GHz` / `Wi-Fi 2.4GHz`.
- Display-only changes; no changes to source API values, parsing, authentication or session handling.

### Changes in v0.7.9.1
- Correct the Vietnamese Node detail popup heading from `THÔNG TIN NÚT MẠNG` to `THÔNG TIN NODE`.
- Keep `Agent` and `Node` as English technical terms, as requested.
- This is a translation/display-only correction. No API, parsing, topology, login/session, or other labels are changed.

### Changes in v0.7.9
- Node detail popup: clarify the connection-type value. Wireless backhaul is displayed as `Wi-Fi 5GHz` or `Wi-Fi 2.4GHz`; wired backhaul is displayed as `Cáp LAN`.
- This is a display-only change limited to the **Loại kết nối** field in the Node detail popup. Other Topology cards, All AP, Dashboard, Clients, filters, parsing and API values remain unchanged.
- Promoted the v0.7.8-beta source baseline to the v0.7.9 release version without changing its Topology connector scheduling/observer logic or Node popup gesture behavior.

## Supported Router profiles

| Selected profile | Firmware/API profile | Default HTTPS address |
|---|---|---|
| HV3601P v9.3 | FPT · `V9.0.10P1_VN2` family | `https://192.168.1.11/` |
| F6601P | `VT9.3.10P2N7` | `https://192.168.1.1/` |

Support is confirmed for these tested profiles, not every firmware revision carrying the same model name. When changing Router model, the app changes the address only when the saved address matches the previous model's default; a user-customized address is retained.

## Main features

- Native HTTPS login and session handling for each supported Router profile.
- Dashboard summary for Controller, Agents, Access Points and Clients.
- Topology diagram with Controller/Agent hierarchy, backhaul type, Link Speed, RSSI where supplied, client counts and animated directional arrows.
- Node detail popup with AP information and Client details read from that Node's detail response.
- Clients view with AP-parent/client-child tree and device search/filter.
- Router Information in Tools: model, serial, hardware/software/boot versions, manufacturer, CPU, memory and uptime where supplied by firmware.\n- Tools includes a confirmed Reboot Router action for both tested firmware profiles; Factory Reset is intentionally not included.
- Vietnamese, English and Simplified Chinese interface; technical terms such as Dashboard, Topology, Mesh, Agent, Node, Backhaul and Link Speed are retained where appropriate.
- Light, Dark and AMOLED themes.
- Configurable automatic refresh (new installations default to 180 seconds; supported saved preferences are retained).

## Node detail popup — connection type display

In the Node detail popup, the field translated as **Loại kết nối** now shows:
- Wireless backhaul: `Wi-Fi 5GHz` or `Wi-Fi 2.4GHz`.
- Wired backhaul: `Cáp LAN`.

The change is intentionally scoped to that one popup field. It does not alter the underlying backhaul value or the text shown in other parts of the app.

## Router Information API profiles

### HV3601P v9.3 FPT
- Device information: `menuData/devmgr_statusmgr_lua.lua` → `OBJ_DEVINFO_ID`.
- CPU, memory and uptime: `menuData/SystemInfo_lua.lua` → `OBJ_CPUINFO_ID`, `OBJ_MEMINFO_ID`, `OBJ_UPTIME_ID`.
- The Status Manager page is initialized before the XML data requests.

### F6601P VT9.3.10P2N7
- One data endpoint: `menuData/devmgr_statusmgr_lua.lua`.
- Device information: `OBJ_DEVINFO_ID`.
- CPU/memory: `OBJ_CPUMEMUSAGE_ID`.
- Uptime: `OBJ_POWERONTIME_ID`.
- The Status Manager page is initialized before reading the XML response.

The two Router profiles use separate parsing paths. A Router Information read failure is best-effort and must not invalidate a successful login or Mesh snapshot. Firmware fields absent from the response are not fabricated as zero values.

## Topology connector behavior (inherited from v0.7.8-beta)

- SVG paths are measured only when the Mesh page is active, visible and has a non-zero layout width.
- Geometry is deferred by two `requestAnimationFrame` callbacks after a render or tab activation so node cards have final positions before connector calculation.
- `ResizeObserver` watches the rendered tree and node cards; viewport resize remains a debounced fallback.
- Arrow animation is paused outside the Mesh tab and restarted after returning.
- The implementation uses actual rendered card positions rather than fixed coordinates or an assumed number of nodes.

## Node popup gestures (inherited from v0.7.8-beta)

- Tap a Node card to open its details.
- Tap the × button to close the popup.
- Alternatively, when the popup is already scrolled to its top, swipe downward to dismiss it.
- Swiping upward does not dismiss the popup; normal vertical scrolling within the popup remains available.

## Login/session safeguards

- Preserve separate authentication and session flows for HV3601P and F6601P.
- The app auto-logs in on launch when credentials are saved and the user has not explicitly logged out.
- F6601P session-conflict handling reads the active session identifier dynamically and requires user confirmation before preempting another administrator session.
- When switching routers, retain the previous router's authenticated session/snapshot until the new router authenticates successfully.
- Do not hard-code passwords, cookies, session IDs or tokens. Sensitive values must not be written to diagnostic logs.
- Network/API errors after authentication must not be misclassified as invalid credentials or erase a valid snapshot.

## Build with Android Studio

1. Extract the source ZIP.
2. In Android Studio choose **Open** and select the project root containing `settings.gradle.kts`.
3. Allow Gradle Sync to complete.
4. Choose **Build → Build APK(s)**.
5. If the build fails, inspect the first meaningful error in **Build Output** and share that output for diagnosis.

Build configuration: Android Gradle Plugin `8.7.3`, Kotlin `2.0.21`, compile SDK `35`, Java/JVM target `17`. The source package does not include `gradle-wrapper.jar`; Android Studio must have a compatible Gradle installation/configuration.

## Verification status

- Router Information: user-confirmed working on HV3601P v9.3 and F6601P `VT9.3.10P2N7` with v0.7.7-beta.
- v0.7.8-beta Topology connector and popup gesture changes are inherited in this release; the v0.7.9 display-only change has been source-checked.
- The v0.7.9.2 source has not been built into an APK or tested on a physical device in this environment.
- Regression-test the **Loại kết nối** field in the Node detail popup on both Router profiles. Also recheck Topology on first entry, tab switching, multi-level Agent layouts, and popup scrolling/dismissal.

## Repository

https://github.com/baolich2/zte-mesh-manager

## Router reboot API profiles

Both captured firmware profiles use the same reboot data endpoint:
- Initialize the reboot page with `GET /?_type=menuView&_tag=rebootAndReset&Menu3Location=0`.
- Read the latest dynamically generated `_sessionTmpToken` from that page.
- Build the exact form body `IF_ACTION=Restart&Btn_restart=&_sessionTOKEN=<dynamic token>`.
- Calculate `Check` as Base64(RSA PKCS#1 v1.5 encryption of the lowercase hexadecimal SHA-256 digest of that exact body).
- Submit the body to `POST /?_type=menuData&_tag=devmgr_restartmgr_lua.lua` with the `Check` header.
- Treat the operation as accepted only when the XML response reports `IF_ERRORPARAM=SUCC` and `IF_ERRORTYPE=SUCC`.

The captured request's cookie, SID and session token are examples from one browser session only. The app must use its own authenticated CookieManager and the current page's dynamic token. A successful response means the Router accepted the command; it does not mean the reboot has completed. The device may be unreachable for several minutes.


## Post-Reboot recovery (v0.7.9.5-beta)
After a confirmed Reboot command, the app enters a dedicated recovery state. It pauses normal periodic Mesh refresh, retains saved credentials, marks the previous snapshot stale, and checks the Router every 10 seconds for up to five minutes. When the HTTP service responds, the app creates a fresh ZteApiClient and runs the existing saved-credential login flow, then reads Mesh and Router Information again. Login is retried if the Router's web service is reachable before its login service is fully ready. If recovery times out, the app reports that manual retry is needed and does not erase credentials.
\n\n### HV3601P Work Mode\nThe app opens the Work Mode page (`menuView`, `mpworkmode`) and reads `CurrentWorkMode` from `OBJ_MAWORKMODE_ID` returned by `multiap_work_mode_lua.lua`. `ShowWorkMode` is used only as a fallback.\n