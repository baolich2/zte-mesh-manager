package com.example.ztemeshmanager

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Native HTTP client for the locally administered ZTE router. */
class ZteApiClient(private val baseUrl: String) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private var sessionToken: String = ""

    init { configureTlsForPrivateRouter() }

    @Suppress("DEPRECATION")
    private fun configureTlsForPrivateRouter() {
        // Many ISP routers use a self-signed certificate. Traffic remains HTTPS and is
        // restricted by the caller to the configured private router address.
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers() = arrayOf<java.security.cert.X509Certificate>()
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
        })
        val context = SSLContext.getInstance("TLS")
        context.init(null, trustAll, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier(HostnameVerifier { _, _ -> true })
    }

    fun login(username: String, password: String): JSONObject {
        // The browser first opens the router root page. This initializes the HTTPS
        // session cookie (SID_HTTPS_) before the loginData AJAX endpoints are called.
        // Without this bootstrap, some ZTE firmware responds with an XML challenge/error
        // where the client expects the login_entry JSON object.
        val landingPage = getText("/")
        if (landingPage.isBlank()) throw IllegalStateException("Router không trả về trang khởi tạo phiên HTTPS")

        val initial = getJson("/?_type=loginData&_tag=login_entry", "khởi tạo login_entry")
        val initialToken = initial.optString("sess_token")
        if (initialToken.isBlank()) throw IllegalStateException("Router không cấp sess_token ở bước khởi tạo")
        val tokenResponse = getText("/?_type=loginData&_tag=login_token&_=${System.currentTimeMillis()}")
        val tokenValue = extractXmlValue(tokenResponse)
        if (tokenValue.isBlank()) throw IllegalStateException("Không lấy được login_token dạng XML từ Router")

        // Match the reference ZTE client flow: SHA-256(password + login_token).
        // login_token is the numeric challenge returned as XML by login_token endpoint.
        val passwordHash = sha256(password + tokenValue)
        val body = linkedMapOf(
            "Password" to passwordHash,
            "Username" to username,
            "_sessionTOKEN" to initialToken,
            "action" to "login"
        )
        val result = postJson("/?_type=loginData&_tag=login_entry", body)
        sessionToken = result.optString("sess_token", initialToken)
        if (result.optBoolean("login_need_refresh", false)) {
            // The observed web UI reloads the root document after login_need_refresh.
            getText("/")
        }
        val topology = getJson("/?_type=menuData&_tag=topo_lua.lua&_=${System.currentTimeMillis()}")
        if (!topology.has("controllerData") && !topology.has("agentlay1")) {
            val err = result.optString("loginErrMsg").ifBlank { "Router chưa xác nhận đăng nhập qua API Topology" }
            throw IllegalStateException(err)
        }
        return result
    }

    fun logout() {
        val token = sessionToken
        if (token.isNotBlank()) {
            postJson("/?_type=loginData&_tag=logout_entry", linkedMapOf("IF_LogOff" to "1", "_sessionTOKEN" to token))
        }
        cookies.cookieStore.removeAll()
        sessionToken = ""
    }

    fun readMesh(): JSONObject {
        val topology = getJson("/?_type=menuData&_tag=topo_lua.lua&_=${System.currentTimeMillis()}")
        val aps = JSONArray()
        val clients = JSONArray()
        val perNode = JSONArray()
        val controller = topology.optJSONObject("controllerData")
        if (controller != null) {
            val c = normalizeAp(controller, true)
            aps.put(c)
            val detail = try { readNode(controller.optString("MacAddr")) } catch (e: Exception) { JSONObject().put("readError", e.message ?: "Không đọc được Controller") }
            appendNodeDetail(detail, c, clients, perNode)
        }
        for (key in listOf("agentlay1", "agentlay2", "agentlay3")) {
            val nodes = topology.optJSONArray(key) ?: continue
            for (i in 0 until nodes.length()) {
                val node = nodes.optJSONObject(i) ?: continue
                val ap = normalizeAp(node, false)
                aps.put(ap)
                val detail = try { readNode(node.optString("MacAddr")) } catch (e: Exception) { JSONObject().put("readError", e.message ?: "Không đọc được node") }
                appendNodeDetail(detail, ap, clients, perNode)
            }
        }
        // The reference Linux client supplements topology popup data with independent
        // WLAN/LAN inventories. Those endpoints return ZTE XML Instance/ParaName/ParaValue.
        val inventory = readLegacyInventory()
        val controllerMac = controller?.optString("MacAddr").orEmpty()
        val seen = HashSet<String>()
        for (i in 0 until clients.length()) seen.add(normalizeMac(clients.optJSONObject(i)?.optString("mac").orEmpty()))
        for (i in 0 until inventory.length()) {
            val device = inventory.optJSONObject(i) ?: continue
            val mac = normalizeMac(device.optString("mac"))
            if (mac.isBlank() || !seen.add(mac)) continue
            // Like the Linux reference implementation, use Controller as a clearly
            // marked fallback when the legacy inventory does not expose a parent node.
            device.put("upstream", controller?.optString("DeviceName").orEmpty().ifBlank { "Controller (chưa xác định Node)" })
            device.put("upstreamMac", controllerMac)
            device.put("association", "fallback-controller")
            clients.put(device)
        }
        return JSONObject().put("url", baseUrl).put("capturedAt", System.currentTimeMillis())
            .put("controllerMac", controllerMac).put("aps", aps).put("clients", clients).put("perNode", perNode)
            .put("inventory", JSONObject().put("wlan", inventoryCount(inventory, "Wi-Fi")).put("lan", inventoryCount(inventory, "LAN")))
            .put("topology", topology)
    }

    private fun normalizeAp(o: JSONObject, controller: Boolean): JSONObject {
        val access = o.optString("AccessType")
        return JSONObject()
            .put("name", o.optString("DeviceName", if (controller) "Controller" else "Access Point"))
            .put("ip", o.optString("IpAddr"))
            .put("mac", o.optString("MacAddr"))
            .put("role", if (controller) "Controller" else o.optString("instID", "Agent"))
            .put("backhaul", access)
            .put("linkSpeed", o.optString("LinkSpeed"))
            .put("rssi", o.optString("BhStaRssi"))
            .put("snr", o.optString("BhStaSnr"))
            .put("parent", o.optString("instID_Parent"))
            .put("clientsCount", o.optInt("accdevCount", 0))
            .put("channel24", o.optString("Channel0"))
            .put("channel5", o.optString("Channel1"))
            .put("software", o.optString("SoftwareVer"))
            .put("macKey", normalizeMac(o.optString("MacAddr")))
    }

    private fun readNode(mac: String): JSONObject {
        if (mac.isBlank()) return JSONObject()
        val encoded = URLEncoder.encode(mac, "UTF-8")
        return getJson("/?_type=menuData&_tag=topo_lua.lua&ParentID=$encoded&Action=GetPopData&_=${System.currentTimeMillis()}")
    }

    private fun appendNodeDetail(detail: JSONObject, ap: JSONObject, allClients: JSONArray, perNode: JSONArray) {
        val readError = detail.optString("readError")
        if (readError.isNotBlank()) {
            perNode.put(JSONObject().put("name", ap.optString("name")).put("ip", ap.optString("ip"))
                .put("mac", ap.optString("mac")).put("clients", JSONArray()).put("clientCount", 0).put("readError", readError))
            return
        }
        val master = detail.optJSONObject("master")
        if (master != null) {
            listOf("LinkSpeed", "BhStaRssi", "BhStaSnr", "AccessType", "SoftwareVer", "Channel0", "Channel1", "IpAddr", "MacAddr", "DeviceName").forEach { key ->
                if (master.has(key)) {
                    val mapped = when (key) {
                        "LinkSpeed" -> "linkSpeed"; "BhStaRssi" -> "rssi"; "BhStaSnr" -> "snr"; "AccessType" -> "backhaul"
                        "SoftwareVer" -> "software"; "Channel0" -> "channel24"; "Channel1" -> "channel5"; "IpAddr" -> "ip"; "MacAddr" -> "mac"; else -> "name"
                    }
                    ap.put(mapped, master.optString(key))
                }
            }
        }
        val nodeClients = JSONArray()
        val groups = listOf(Triple("wlan2G", "2.4GHz", "Wi-Fi"), Triple("wlan5G", "5GHz", "Wi-Fi"), Triple("lan", "LAN", "LAN"))
        for ((key, band, kind) in groups) {
            val rows = extractClientRows(detail.opt(key))
            for (raw in rows) {
                val client = JSONObject()
                    .put("name", raw.optString("HostName").ifBlank { "Thiết bị không tên" })
                    .put("ip", raw.optString("IpAddr"))
                    .put("mac", raw.optString("MacAddr"))
                    .put("rssi", if (kind == "LAN") "" else raw.optString("Rssi"))
                    .put("snr", raw.optString("SNR"))
                    .put("band", band)
                    .put("kind", kind)
                    .put("mode", raw.optString("WirelessMode"))
                    .put("channel", raw.optString("Channel"))
                    .put("txRate", raw.optString("TxRateBps"))
                    .put("rxRate", raw.optString("RxRateBps"))
                    .put("time", raw.optString("AssocTime"))
                    .put("down", raw.optString("RxRateBps"))
                    .put("up", raw.optString("TxRateBps"))
                    .put("upstream", ap.optString("name"))
                nodeClients.put(client); allClients.put(client)
            }
        }
        val reportedCount = ap.optInt("clientsCount", 0)
        perNode.put(JSONObject().put("name", ap.optString("name")).put("ip", ap.optString("ip"))
            .put("mac", ap.optString("mac")).put("master", master ?: JSONObject()).put("clients", nodeClients)
            .put("clientCount", maxOf(nodeClients.length(), reportedCount))
            .put("reportedClientCount", reportedCount).put("countSource", if (nodeClients.length() > 0) "popup" else "topology-accdevCount")
            .put("raw", detail).put("readError", ""))
    }

    private fun extractClientRows(value: Any?): List<JSONObject> {
        val found = mutableListOf<JSONObject>()
        fun walk(item: Any?, depth: Int) {
            if (depth > 5 || item == null || item == JSONObject.NULL) return
            when (item) {
                is JSONArray -> for (i in 0 until item.length()) walk(item.opt(i), depth + 1)
                is JSONObject -> {
                    val looksLikeClient = item.has("MacAddr") || item.has("MACAddress") || item.has("IpAddr")
                    if (looksLikeClient) found.add(item)
                    else {
                        val keys = item.keys()
                        while (keys.hasNext()) walk(item.opt(keys.next()), depth + 1)
                    }
                }
            }
        }
        walk(value, 0)
        return found
    }

    private fun readLegacyInventory(): JSONArray {
        val result = JSONArray()
        // The Linux reference uses F6640-compatible legacy inventory endpoints.
        // HV3601P is not explicitly mapped in that project, so these are best-effort
        // probes; failure must not invalidate a successful topology snapshot.
        try { getText("/?_type=menuView&_tag=localNetStatus&_=${System.currentTimeMillis()}") } catch (_: Exception) { }
        val endpoints = listOf(
            Triple("wlan_client_stat_lua.lua", "OBJ_WLAN_AD_ID", "Wi-Fi"),
            Triple("accessdev_landevs_lua.lua", "OBJ_ACCESSDEV_ID", "LAN")
        )
        for ((script, objectName, kind) in endpoints) {
            try {
                val xml = getText("/?_type=menuData&_tag=$script&_=${System.currentTimeMillis()}")
                val rows = parseZteInstances(xml, objectName)
                for (row in rows) {
                    val mac = firstValue(row, "MACAddress", "MacAddr", "MAC")
                    if (mac.isBlank()) continue
                    val active = firstValue(row, "Active", "Status").lowercase()
                    if (active in listOf("0", "false", "no", "offline", "disconnected")) continue
                    result.put(JSONObject()
                        .put("name", firstValue(row, "HostName", "DeviceName", "Host", "Name").ifBlank { "Thiết bị không tên" })
                        .put("ip", firstValue(row, "IPAddress", "IpAddr", "IP"))
                        .put("mac", mac)
                        .put("kind", kind)
                        .put("band", if (kind == "LAN") "LAN" else firstValue(row, "SSID", "SSIDName", "AliasName").ifBlank { "Wi-Fi" })
                        .put("rssi", firstValue(row, "Rssi", "RSSI"))
                        .put("snr", firstValue(row, "SNR"))
                        .put("mode", firstValue(row, "WirelessMode", "Mode"))
                        .put("channel", firstValue(row, "Channel"))
                        .put("txRate", firstValue(row, "TxRateBps"))
                        .put("rxRate", firstValue(row, "RxRateBps"))
                        .put("time", firstValue(row, "AssocTime")))
                }
            } catch (_: Exception) {
                // Keep topology/popup data usable if a firmware does not implement an endpoint.
            }
        }
        return result
    }

    private fun parseZteInstances(xml: String, objectName: String): List<Map<String, String>> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())
        val rows = mutableListOf<Map<String, String>>()
        var inTarget = false
        var inInstance = false
        var key = ""
        var row = linkedMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    objectName -> inTarget = true
                    "Instance" -> if (inTarget) { inInstance = true; row = linkedMapOf() }
                    "ParaName" -> if (inInstance) key = parser.nextText().trim()
                    "ParaValue" -> if (inInstance && key.isNotBlank()) { row[key] = parser.nextText().trim(); key = "" }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "Instance" -> if (inInstance) { rows.add(row.toMap()); inInstance = false }
                    objectName -> inTarget = false
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun firstValue(row: Map<String, String>, vararg keys: String): String =
        keys.firstNotNullOfOrNull { key -> row[key]?.takeIf { it.isNotBlank() } }.orEmpty()

    private fun normalizeMac(value: String): String = value.uppercase().filter { it.isLetterOrDigit() }

    private fun inventoryCount(items: JSONArray, kind: String): Int {
        var count = 0
        for (i in 0 until items.length()) if (items.optJSONObject(i)?.optString("kind") == kind) count++
        return count
    }

    private fun getJson(path: String, stage: String = "API"): JSONObject = parseJson(getText(path), stage)

    private fun parseJson(raw: String, stage: String): JSONObject {
        val trimmed = raw.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (!trimmed.startsWith("{")) {
            val preview = trimmed.take(140).replace('\n', ' ').replace('\r', ' ')
            throw IllegalStateException("Phản hồi $stage không phải JSON (đầu phản hồi: $preview)")
        }
        return try { JSONObject(trimmed) }
        catch (e: Exception) { throw IllegalStateException("JSON $stage không hợp lệ: ${e.message}", e) }
    }

    private fun getText(path: String): String = request("GET", path, null)

    private fun postJson(path: String, values: Map<String, String>): JSONObject {
        val body = values.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        return parseJson(request("POST", path, body), "POST ${path.substringAfter("_tag=", path)}")
    }

    private fun request(method: String, path: String, body: String?): String {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
            connection.setRequestProperty("Referer", baseUrl)
            connection.setRequestProperty("Origin", baseUrl.trimEnd('/'))
            connection.setRequestProperty("Cache-Control", "no-cache")
            val uri = url.toURI()
            cookies.get(uri, emptyMap()).forEach { (k, values) -> values.forEach { connection.addRequestProperty(k, it) } }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            cookies.put(uri, connection.headerFields.filterKeys { it != null })
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Router HTTP $code: ${text.take(250)}")
            return text
        } finally { connection.disconnect() }
    }

    private fun extractXmlValue(xml: String): String = Regex("<ajax_response_xml_root>\\s*(.*?)\\s*</ajax_response_xml_root>", RegexOption.DOT_MATCHES_ALL)
        .find(xml)?.groupValues?.getOrNull(1)?.trim().orEmpty()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
