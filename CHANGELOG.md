## [0.5.2] - 2026-11-24
- Replaced SVG SMIL `animateMotion` arrows with a JavaScript `requestAnimationFrame` animation driven by each branch's own SVG path geometry.
- Each parent-to-child branch now receives an independent arrow animation, including nested branches such as Kho hàng → Phòng ăn.
- Arrow position is sampled along the route and its tangent is transformed to screen coordinates, keeping its orientation correct in the responsive, stretched SVG on Android WebView.
- Kept the continuous red connector paths unchanged; no router API, topology parsing, authentication, session, Dashboard, or Clients behavior changes.
- Preserved the two-line wireless backhaul label (`Wi-Fi 5GHz` / link speed) and separate RSSI badge from v0.5.1.

## Version 0.5.1
- Fixed the Mesh sibling connector geometry: the shared horizontal branch now spans from the first child center to the last child center, while remaining connected to the parent stem and every child drop.
- Replaced the animated Unicode arrow glyph with an SVG polygon arrow that follows each parent-to-child route and rotates to match the path direction.
- Improved arrow visibility and staggered branch animation starts to reduce visual collisions near the shared parent stem.
- Reformatted wireless backhaul badges into two deliberate lines: `Wi-Fi 5GHz` and the link speed (for example, `2401 Mbps`). The separate RSSI badge remains below.
- Preserved router/API parsing, authentication, session behavior, Dashboard, Clients filters, and All AP behavior.

## Version 0.5.0
- Based directly on the supplied v0.4.19 source archive; preserves its continuous SVG topology connectors and all existing router/session behavior.
- Added small animated directional arrows along the red Mesh topology paths to indicate traffic direction from Controller/parent nodes toward child nodes.
- Kept topology links data-driven from live `instID` / `instID_Parent` relationships; the animation follows each rendered parent-to-child route.
- Removed duplicate RSSI text from the Wi-Fi backhaul badge; RSSI remains in its separate signal badge.
- Standardized displayed `5G` labels to `5GHz` in Mesh, Dashboard, All AP and Client band details without changing API values or parsing.
- Added `Mbps` to Link Speed in Node Information and Dashboard node summaries (for example, `1000 Mbps`, `2401 Mbps`).
- No router endpoint, authentication, session, or topology parsing changes.

## Version 0.4.19
- Replaced separately rendered CSS connector segments with one SVG path per parent/child group.
- Parent stems, sibling bars and child drops now share continuous vector paths with rounded joins, eliminating visible hairline seams at zoom levels and on fractional-pixel layouts.
- Preserved responsive node layout, dynamic `instID` / `instID_Parent` hierarchy, login/session behavior, Clients filters, All AP and RSSI display.
- No router API, authentication, or topology parsing changes.

## Version 0.4.18
- Reworked Mesh topology layout to use responsive equal-width columns for sibling nodes, including up to three direct child nodes under the Controller.
- Scaled router icons, card padding and typography down consistently when three nodes share one row, improving fit on phone screens.
- Rebuilt the red parent-to-child connector lane: the parent stem, sibling bar and each child drop now share exact positions so the lines meet without gaps.
- Kept nested parent-child relationships data-driven from `instID` / `instID_Parent`; wireless nodes remain under their actual upstream Agent.
- Preserved the v0.4.17 login/session code, Clients search and filters, All AP inventory, and RSSI display.
- Updated README and version metadata. No router API or authentication behavior changed.

## Version 0.4.17
- Fixed broken red topology connectors in the Mesh diagram.
- Parent trunk, sibling branch bar, and child drops now share a continuous connector zone without visual gaps.
- Single-child branches use one continuous vertical connector and omit the redundant horizontal bar.
- No changes to Kotlin login/session logic, topology parsing, or Clients behavior.

## [0.4.14]
- Redesigned the Mesh page as a mobile-friendly parent/child topology tree.
- Builds node relationships dynamically from each AP's `instID` and `instID_Parent`; nested wireless agents appear beneath their actual upstream node.
- Shows backhaul type and link speed on each node; displays backhaul RSSI only when the router reports a valid negative value.
- Added a Backhaul RSSI column to the Clients tab's All AP inventory for wireless nodes; wired nodes and Controller show `—`.
- Preserved the existing native login/session implementation and the other dashboard tabs.

## [0.4.13]
- Fixed logout empty-state rendering: the All AP inventory is now cleared together with the Client tree and other live router data, including when returning from the login/management screen after logout.
- After a successful Router login and initial topology read, the login screen closes automatically and the app returns to the Dashboard/Overview tab.
- Failed login behavior remains unchanged; the active session is not replaced unless the candidate login and initial data read succeed.

## [0.4.12]
- Removed the redundant RSSI quality summary cards, per-client signal-quality list, and explanatory signal panel from the Clients page.
- Kept the RSSI quality filter; it still filters the always-expanded AP-to-Client tree together with search, band, AP role, and connection type.
- Fixed narrow-screen All AP inventory column sizing and text wrapping to prevent role/backhaul labels from overlapping.
- Preserved the AP-to-Client tree, All AP inventory, and Kotlin login/session code from v0.4.11.

## [0.4.11]
- Restored full Clients filtering: search by name/IP/MAC, band, Controller/Agent, Wi-Fi/LAN connection type and RSSI quality (good/fair/weak/unknown).
- Kept the AP-to-Client tree permanently expanded; filters narrow the visible client rows without adding expand/collapse controls.
- Preserved all Kotlin sources and the v0.4.5 login/session implementation.

## [0.4.10]
- Corrected the Clients page section title to “Clients theo từng AP”; the always-visible AP-to-Client tree remains under this heading.
- Restored a separate, non-interactive “All AP” inventory containing only Controller/Agent details (name, IP, role, MAC and backhaul), with no client rows.
- Preserved all Kotlin sources and the v0.4.5 login/session implementation.

# Changelog

## [0.4.16]
- Redesigned the Mesh tab as a mobile-friendly topology diagram inspired by the router web topology view.
- Shows Controller and Agent cards with client counts, IP, role, backhaul type, link speed and valid wireless RSSI.
- Draws parent-child relationships dynamically from live `instID` / `instID_Parent` data.
- Uses high-contrast red connector lines and avoids dangling connector tails after the final child.
- Added a compact legend for client counts, Ethernet and wireless backhaul.
- Preserved v0.4.14 login/session and Clients behavior.

## [0.4.9]
- Replaced expandable AP details with a permanently expanded AP-to-Client tree; no expand/collapse controls.
- Renamed the tree section to “All AP”.
- Removed the duplicate ALL APs inventory table from the bottom of Clients.
- Search and band/upstream filters remain the way to narrow the visible client list.
- Preserved all Kotlin sources and the v0.4.5 login/session implementation.


## [0.4.8]
- Rebuilt UI directly from the supplied v0.4.5 source archive.
- Merged Signal/RSSI summary and per-client signal list into the Clients page; removed the separate Signal tab.
- Added expandable AP-to-Client tree groups, showing client IP, MAC, RSSI, band, Wi-Fi mode, traffic and association time where supplied.
- Added a non-interactive ALL APs inventory at the bottom, showing AP name, IP, role, MAC and backhaul.
- Replaced AP folder markers with the existing app icon and omitted the “(cây thư mục)” wording.
- Preserved all Kotlin sources and the v0.4.5 login/session implementation.


## [0.4.5]
- Added user-configurable automatic refresh interval in seconds (10–3600; default 30), persisted locally and applied immediately to the scheduler.
- Kept manual Overview reload independent of the configured interval.
- Reused the app launcher icon in Overview node cards.
- Hid Backhaul and Link Speed for the Controller; Agent details remain unchanged.


## [0.4.4]
- Fixed over-escaped RSSI matching in the Signal screen.
- Added per-node client counts to Overview and Mesh.
- Added manual Overview reload using the existing native refresh bridge.
- Documented source-of-count differences (popup rows vs topology-reported count).

## [0.4.3]
- Added session/snapshot state handling and empty dashboard behavior for unauthenticated state.
- Fixed dashboard JavaScript syntax in the follow-up fix build.

## [0.4.2]
- Added per-node client popup parsing and best-effort WLAN/LAN inventory probes.
- Added Controller MAC identity and topology-reported per-node counts.

## [0.4.1]
- Corrected challenge login flow.

## [0.4.0]
- Added native HTTPS API access and topology reads.

## [0.3.9]
- Session persistence foundation.
