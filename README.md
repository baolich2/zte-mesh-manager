# ZTE Mesh Manager 0.5.2

## 0.5.2 changes
- Reworked Mesh flow-arrow animation: arrows are positioned and rotated by JavaScript using each individual SVG branch path, instead of SVG SMIL `animateMotion`.
- Added independent animation for every parent-to-child link, including nested Agent-to-Agent links.
- The arrow tangent is calculated in screen coordinates so it remains correctly oriented when the SVG scales to different phone widths.
- Kept v0.5.1's continuous red connector lines and the two-line Wi-Fi backhaul label (`Wi-Fi 5GHz` then link speed), with RSSI displayed separately.
- No changes to router APIs, topology parsing, authentication, session handling, or Clients filtering/search.

## 0.5.1 changes
- Kept continuous red Mesh connector paths and refined the connector branch geometry.
- Replaced the old text arrow with an SVG arrow polygon; this version's SMIL animation is superseded in v0.5.2 by JavaScript path-based motion for Android WebView reliability.
- Displayed wireless backhaul information on two lines (`Wi-Fi 5GHz` and link speed), with RSSI in a separate badge.

## 0.4.19 changes
- Replaced CSS connector fragments with continuous SVG paths for each topology parent/child group.
- Rounded vector joins keep the red Controller-to-node and Agent-to-child connections visually continuous when zoomed.
- Keeps responsive layout for up to three direct children and retains live parent-child relationships from Router topology data.
- Preserves v0.4.18 features and session behavior; no router API or authentication changes.

## 0.4.18 changes
- Mesh sibling nodes are laid out in equal responsive columns; up to three direct child nodes can be shown side-by-side under the Controller.
- Node cards, router icons, spacing and text scale down together for a three-node row on mobile screens.
- Red topology connectors use one shared connector lane so the Controller stem, horizontal sibling branch and vertical drops meet continuously.
- Nested topology still follows the live `instID` / `instID_Parent` relationship; this is not a hard-coded layout for the current home network.
- Preserved login/session handling, Clients search and filters, the always-expanded Clients tree, All AP inventory and wireless-node RSSI display from v0.4.17.
- No changes to router endpoints, authentication, or data parsing.

## Version 0.4.17
- Adjusted the Mesh connector rendering to reduce gaps between parent stems, sibling bars and child drops.
- Kept topology relationships and all router/session behavior unchanged.

## Version 0.4.16
- Redesigned Mesh as a responsive topology diagram inspired by the router web UI.
- Draws parent-child links from live `instID` / `instID_Parent` data with red connectors.
- Node cards show client count, IP, role, backhaul, link speed and valid wireless RSSI.
- Preserved login/session and Clients behavior from v0.4.14.

## Version 0.4.14
- Mesh tab now renders a responsive parent/child topology tree using the Router's `instID_Parent` field.
- Node cards show IP, role, client count, backhaul type, link speed, and valid wireless backhaul RSSI.
- Clients > All AP now includes RSSI for wireless mesh nodes; wired nodes and Controller show `—`.
- Login/session Kotlin code and existing Clients filtering/tree behavior are preserved.

## Version 0.4.13
- Removed the redundant signal-quality panel from Clients; RSSI quality filtering remains available.
- Fixed All AP inventory text wrapping on narrow phone screens.
- Preserved the expanded AP-to-Client tree and login/session code.

## Version 0.4.11
- Clients are shown as an always-expanded AP-to-Client tree under “Clients theo từng AP”.
- A separate “All AP” inventory lists AP information only; it does not repeat clients.
- Login/session Kotlin code is unchanged from v0.4.5.

# ZTE Mesh Manager 0.4.5

Android application for reading ZTE Mesh topology and connected-client information from the router's native HTTPS APIs.

## Target environment
- Verified target reported by the project owner: ZTE HV3601P, firmware `V9.0.10P1_VN2`.
- Controller address used in the owner's network: `https://192.168.1.11/` (DHCP reservation).
- Agent IP addresses are DHCP-assigned and may change. Nodes are matched by MAC address where available.
- Android project: Kotlin, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Gradle wrapper 8.9, compileSdk 35, minSdk 23, Java/Kotlin target 17.

## Features carried forward
- Native HTTPS login flow; no router-admin WebView is used for authentication.
- Topology reads the Controller and up to three agent layers.
- Per-node `GetPopData` reads `master`, `wlan2G`, `wlan5G`, and `lan`.
- Client details include hostname, IP, MAC, band, wireless mode, RSSI/SNR, channel, association time and reported traffic rates when the router supplies them.
- Snapshot state is cleared for a logged-out/unauthenticated dashboard.
- Periodic refresh defaults to 30 seconds while the app is foregrounded; the interval can be configured from 10 to 3600 seconds in the Overview screen and is persisted locally.
- Per-node reboot remains disabled; no safe reboot endpoint has been verified.

## 0.4.4 changes
- Fixed the Signal RSSI filter: the previous JavaScript regular expression was over-escaped and did not match negative RSSI values such as `-40`.
- Added per-node connected-client counts to both the Overview node list and the Mesh Access Point list.
- Count uses distinct client MACs from that node's `GetPopData` popup when available. If popup rows are unavailable but Topology provides `accdevCount`, the UI labels the number as coming from Topology rather than presenting it as a parsed client list.
- Added a manual reload button to Overview. It calls the existing native `AndroidApp.refreshNow()` bridge and does not wait for the 30-second timer.
- Preserved the v0.4.3 session/snapshot behavior and v0.4.1 login flow.


## 0.4.5 changes
- Added a numeric refresh-interval field beside the Overview reload button. The value is entered in seconds, defaults to 30, accepts 10–3600, and is saved in app preferences. Changing it reschedules the next foreground refresh.
- Kept manual reload independent of the automatic interval.
- Replaced the generic square glyph in each Overview node card with the app's existing launcher icon asset.
- Removed the Backhaul row from Controller cards in Overview and Mesh, and from the Controller detail panel. Agent backhaul and link-speed information remain visible.

## API notes
Observed endpoints used by the app:
- `GET /?_type=loginData&_tag=login_entry`
- `GET /?_type=loginData&_tag=login_token`
- `POST /?_type=loginData&_tag=login_entry`
- `POST /?_type=loginData&_tag=logout_entry`
- `GET /?_type=menuData&_tag=topo_lua.lua`
- `GET /?_type=menuData&_tag=topo_lua.lua&ParentID=<MAC>&Action=GetPopData`

The `login_token` endpoint returns XML on the observed firmware. The client must not assume every response is JSON; XML challenge values are parsed separately, and XML `SessionTimeout` errors are surfaced as session failures.

## Experimental / firmware-dependent behavior
- Legacy WLAN/LAN inventory probes (`wlan_client_stat_lua.lua`, `accessdev_landevs_lua.lua`) were modeled on a separate Linux reference project. Their compatibility with HV3601P firmware is not guaranteed; failures are non-fatal.
- Inventory rows without a parent-node identifier are not proof of which mesh node a client is attached to. The app marks these as fallback association rather than inventing a node mapping.
- Per-node counts based on `accdevCount` are router-reported counts; they are not a reconstructed list of client records.
- The exact behavior of session expiry and automatic re-authentication should continue to be tested on the target router.

## Version history
### 0.4.5
- Added configurable refresh interval (10–3600 seconds; default 30), persisted locally.
- Reused the app launcher icon for node cards.
- Hid irrelevant Controller backhaul information.

### 0.4.4
- Fixed RSSI Signal filtering.
- Added per-node client counts to Overview and Mesh.
- Added manual Overview reload.
- Updated README and added CHANGELOG.

### 0.4.3
- Introduced session/snapshot state handling, auto-login attempt and clearing displayed snapshot when unauthenticated or logged out.
- Follow-up fix corrected a JavaScript syntax issue that prevented dashboard interactions.

### 0.4.2
- Added parsing of per-node popup client groups and best-effort WLAN/LAN inventory probes.
- Added Controller-MAC-based router identity handling.
- Added fallback use of topology `accdevCount` when popup client rows are unavailable.

### 0.4.1
- Corrected login challenge flow based on observed ZTE browser requests and a reference implementation.
- Uses SHA-256 of password concatenated with the XML numeric `login_token` challenge, as implemented in the client.

### 0.4.0
- Introduced native HTTPS API access and topology reads, replacing router-admin WebView scraping.

### 0.3.9
- Session persistence foundation.

## Build
1. Open the folder containing `settings.gradle.kts` in Android Studio.
2. Wait for Gradle Sync.
3. Select **Build → Build APK(s)**.
4. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The bundled dashboard JavaScript was syntax-checked with Node.js. An Android Gradle build was not run in this environment; build and runtime behavior must be verified in Android Studio and on the target HV3601P.


## Version 0.4.8
UI-only release based on the supplied v0.4.5 source. Signal is merged into Clients; Clients are grouped in an expandable AP tree; a non-interactive ALL APs inventory is shown at the bottom. Authentication/session Kotlin sources remain unchanged.


## Version 0.4.9
UI-only update based on v0.4.8 (which was based on supplied v0.4.5). The Clients page now displays every AP and its clients in a permanently expanded tree with no expand/collapse controls. The tree heading is “All AP”; the duplicate ALL APs table is removed. Search and filters are used to narrow results. Kotlin login/session sources remain unchanged.
