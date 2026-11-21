# ZTE Mesh Manager 0.4.0 – Native Router API

Base project: `ZTEMeshManager-v0.3.9-session-persistence.zip`.

## What changed
- Replaced the Router-admin WebView login screen with a native Android login form.
- Replaced hidden-WebView/DOM scraping with direct HTTPS API calls from Kotlin.
- Initializes the router root page first to obtain the HTTPS session cookie before login AJAX calls.
- Uses the observed ZTE endpoints:
  - `GET /?_type=loginData&_tag=login_entry`
  - `GET /?_type=loginData&_tag=login_token`
  - `POST /?_type=loginData&_tag=login_entry`
  - `POST /?_type=loginData&_tag=logout_entry`
  - `GET /?_type=menuData&_tag=topo_lua.lua`
  - `GET /?_type=menuData&_tag=topo_lua.lua&ParentID=<MAC>&Action=GetPopData`
- Parses `controllerData`, `agentlay1..3`, `master`, `wlan2G`, `wlan5G`, and `lan` JSON objects directly.
- Dashboard presentation remains the bundled local HTML asset; it no longer navigates to or scrapes the router website.
- Refreshes live data every 30 seconds while the app is foregrounded.
- Optional auto-login credentials are encrypted with an AES-GCM key held in Android Keystore. The router session cookie itself is held in the in-memory native CookieManager; after process death, the app obtains a fresh session by logging in again.
- Reboot controls remain disabled because no safe per-node reboot API has been verified.

## Important verification items on the user's firmware
The request structure is based on the supplied DevTools captures. The browser first loads `/`, then requests the login JSON and XML challenge. The app now follows that bootstrap order and reports a readable error if an endpoint returns the wrong content type. Two protocol details are still inferred and must be tested against the user's HV3601P firmware:
1. `Password` is sent as a 64-character hexadecimal string. This implementation currently uses SHA-256 of the entered password. The captures establish the format, not the exact hashing/salting algorithm. If login fails, capture the browser-side password transformation (never share the password) and adjust `sha256()` accordingly.
2. The observed `login_token` GET returns an XML numeric value. The POST payload supplied by the user contains `_sessionTOKEN` matching the 24-digit `sess_token` from GET `login_entry`; therefore this implementation uses that value for `_sessionTOKEN` and calls `login_token` as part of the observed flow. The numeric `login_token` response is not currently included in the POST because the captured payload did not show a corresponding field.

The client accepts the router's self-signed HTTPS certificate because the device is a private-LAN appliance. The login UI restricts the configured host to RFC1918 private IPv4 ranges. Do not reuse this trust-all TLS pattern for public Internet endpoints.

## Build
- Gradle wrapper: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- compileSdk: 35
- minSdk: 23
- Java/Kotlin target: 17

Open the project in Android Studio and build `app`. This environment did not have an Android SDK/Gradle executable available, so an APK build could not be verified here.


## v0.4.1 login challenge correction
- Login challenge endpoint uses a cache-busting timestamp.
- Password digest follows the reference client: SHA-256(password + login_token).
