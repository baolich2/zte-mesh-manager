package com.example.ztemeshmanager

import android.util.Log
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


data class PreemptCandidate(val sessionId: String, val description: String)

/** Native HTTP client for the locally administered ZTE router. */
class ZteApiClient(private val baseUrl: String, private val modelKey: String = "HV3601P_V93_FPT") {
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
        if (modelKey == "F6601P_VT9310P2N7") return loginF6601p(username, password)
        // The router web UI sets this client-side cookie after checking that cookies
        // are supported. Native HTTP clients do not execute that JavaScript, so add
        // the same marker explicitly before opening the landing page. The SID_HTTPS_
        // session cookie is still issued and managed by the router as before.
        logDiag("LOGIN begin: baseHost=${safeHost(baseUrl)}, cookieStore=${cookieStoreSummary()}")
        ensureCookieSupportMarker()
        logDiag("LOGIN marker initialized: cookieStore=${cookieStoreSummary()}")
        // The browser first opens the router root page. This initializes the HTTPS
        // session cookie (SID_HTTPS_) before the loginData AJAX endpoints are called.
        // Without this bootstrap, some ZTE firmware responds with an XML challenge/error
        // where the client expects the login_entry JSON object.
        val landingPage = getText("/")
        logDiag("LOGIN stage=landingPage complete: responseLength=${landingPage.length}, cookieStore=${cookieStoreSummary()}")
        if (landingPage.isBlank()) throw IllegalStateException("Router không trả về trang khởi tạo phiên HTTPS")

        val initial = getJson("/?_type=loginData&_tag=login_entry", "khởi tạo login_entry")
        logDiag("LOGIN stage=login_entry complete: hasSessToken=${initial.optString("sess_token").isNotBlank()}, cookieStore=${cookieStoreSummary()}")
        val initialToken = initial.optString("sess_token")
        if (initialToken.isBlank()) throw IllegalStateException("Router không cấp sess_token ở bước khởi tạo")
        val tokenResponse = getText("/?_type=loginData&_tag=login_token&_=${System.currentTimeMillis()}")
        logDiag("LOGIN stage=login_token complete: responseLength=${tokenResponse.length}, cookieStore=${cookieStoreSummary()}")
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
        logDiag("LOGIN stage=login_submit complete: hasSessToken=${result.optString("sess_token").isNotBlank()}, loginNeedRefresh=${result.optBoolean("login_need_refresh", false)}, hasLoginErrMsg=${result.optString("loginErrMsg").isNotBlank()}, cookieStore=${cookieStoreSummary()}")
        // Preserve the v0.5.3/v0.4.5 native login behavior: use the token returned
        // by login_entry. Only reload the root document when the router explicitly asks
        // for it; do not overwrite a valid login token with a token scraped from an
        // unrelated/stale root-page response.
        sessionToken = result.optString("sess_token", initialToken).ifBlank { initialToken }
        if (result.optBoolean("login_need_refresh", false)) getText("/")
        val topology = getJson("/?_type=menuData&_tag=topo_lua.lua&_=${System.currentTimeMillis()}")
        logDiag("LOGIN stage=topology complete: hasController=${topology.has("controllerData")}, hasAgentLayer1=${topology.has("agentlay1")}, cookieStore=${cookieStoreSummary()}")
        if (!topology.has("controllerData") && !topology.has("agentlay1")) {
            val err = result.optString("loginErrMsg").ifBlank { "Router chưa xác nhận đăng nhập qua API Topology" }
            throw IllegalStateException(err)
        }
        return result
    }

    /** F6601P VT9.3.10P2N7 uses a different login sequence and topology payload. */
    /** F6601P F6600P-compatible challenge login, following the working Go tracker client. */
    private fun loginF6601p(username: String, password: String): JSONObject {
        logDiag("F6601P LOGIN begin: host=${safeHost(baseUrl)}")
        ensureCookieSupportMarker()

        // Reference flow in zte-tracker-bot 1.0.3:
        // 1) GET login_entry to obtain the initial sess_token.
        // 2) GET login_token to obtain the challenge.
        // 3) POST login_entry with SHA256(password + challenge) and initial sess_token.
        // 4) If login_need_refresh=true, GET "/" before opening the topology view.
        val initial = getJson("/?_type=loginData&_tag=login_entry", "F6601P initial login_entry")
        val initialToken = initial.optString("sess_token")
        if (initialToken.isBlank()) {
            throw IllegalStateException("F6601P không cấp sess_token khởi tạo")
        }
        logDiag("F6601P LOGIN stage=initial-session complete: tokenPresent=true cookieStore=${cookieStoreSummary()}")

        val tokenXml = getText("/?_type=loginData&_tag=login_token&_=${System.currentTimeMillis()}")
        val challenge = extractXmlValue(tokenXml)
        if (challenge.isBlank()) throw IllegalStateException("F6601P không cấp login_token")
        logDiag("F6601P LOGIN stage=challenge complete: challengePresent=true")

        val hash = sha256(password + challenge)
        val result = postJson("/?_type=loginData&_tag=login_entry", linkedMapOf(
            "action" to "login",
            "Username" to username,
            "Password" to hash,
            "_sessionTOKEN" to initialToken
        ))
        val err = result.optString("loginErrMsg")
        val lock = result.optInt("lockingTime", 0)
        if (lock > 0) throw IllegalStateException("F6601P khóa đăng nhập $lock giây")
        if (err.isNotBlank() && err.contains("password", true)) {
            throw IllegalStateException("F6601P từ chối tên đăng nhập hoặc mật khẩu")
        }

        sessionToken = result.optString("sess_token").ifBlank { initialToken }
        if (sessionToken.isBlank()) throw IllegalStateException("F6601P không trả về sess_token sau đăng nhập")
        logDiag("F6601P LOGIN stage=login-submit complete: tokenPresent=true refresh=${result.optBoolean("login_need_refresh", false)} cookieStore=${cookieStoreSummary()}")

        // The Go reference client uses the router-requested root refresh here.
        // Do not send wizardData/closeaction: it is a browser Easy Setup navigation
        // action, not part of the working F6600P-compatible API login flow.
        if (result.optBoolean("login_need_refresh", false)) {
            val refreshed = getText("/")
            if (refreshed.isBlank()) throw IllegalStateException("F6601P không tải được trang sau login refresh")
        }

        // Match GetTopologyRaw() in the Go reference: enter the Topology view first,
        // then request topo_lua.lua. The page request initializes the menu/session context.
        val topologyPage = getText("/?_type=menuView&_tag=mmTopology&Menu3Location=0&_=${System.currentTimeMillis()}")
        if (topologyPage.isBlank()) throw IllegalStateException("F6601P không tải được trang Topology")
        val topology = getJson("/?_type=menuData&_tag=topo_lua.lua&_=${System.currentTimeMillis()}", "F6601P topology")
        if (!topology.has("controllerData")) {
            throw IllegalStateException("F6601P đăng nhập chưa xác thực được controllerData")
        }

        // This endpoint is a topology status probe, not a prerequisite for authentication.
        try {
            getText("/?_type=menuData&_tag=topo_save_lua.lua&_=${System.currentTimeMillis()}")
        } catch (e: Exception) {
            logDiag("F6601P topo_save status unavailable: ${e.javaClass.simpleName}")
        }
        logDiag("F6601P LOGIN success: controllerDataPresent=true cookieStore=${cookieStoreSummary()}")
        return result
    }

    private fun readF6601pMesh(): JSONObject {
        val topology = getJson("/?_type=menuData&_tag=topo_lua.lua&_=${System.currentTimeMillis()}", "F6601P topology")
        val controller = topology.optJSONObject("controllerData")
            ?: throw IllegalStateException("F6601P không có controllerData trong Topology")
        val aps = JSONArray()
        val clients = JSONArray()
        val perNode = JSONArray()
        val controllerAp = normalizeAp(controller, true)
        controllerAp.put("parent", "")
        aps.put(controllerAp)

        // F6601P topology uses object maps agentlay1..agentlay8, not JSON arrays.
        for (layer in 1..8) {
            val layerObj = topology.optJSONObject("agentlay$layer") ?: continue
            val count = layerObj.optInt("MGET_INST_NUM", 0)
            for (index in 0 until count) {
                val node = layerObj.optJSONObject(index.toString()) ?: continue
                val parentInst = node.optString("instID_Parent").ifBlank { "MESH.CONTROLLER" }
                val ap = normalizeAp(node, false).put("parent", parentInst)
                aps.put(ap)
            }
        }

        // GetALLAP is the firmware's dedicated AP inventory. Merge only fields that
        // exist; preserve topology's explicit instID_Parent relationship.
        try {
            val all = getJson("/?_type=menuData&_tag=topo_lua.lua&Action=GetALLAP&_=${System.currentTimeMillis()}", "F6601P GetALLAP")
                .optJSONObject("apData")
            if (all != null) {
                val keys = all.keys().asSequence().filter { it != "MGET_INST_NUM" }.toList().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                for (key in keys) {
                    val item = all.optJSONObject(key) ?: continue
                    val mac = normalizeMac(item.optString("MacAddr"))
                    val target = (0 until aps.length()).mapNotNull { aps.optJSONObject(it) }
                        .firstOrNull { normalizeMac(it.optString("mac")) == mac }
                    if (target != null) mergeApFields(target, item)
                    else if (mac.isNotBlank()) aps.put(normalizeAp(item, item.optString("role") == "controller"))
                }
            }
        } catch (e: Exception) { logDiag("F6601P GetALLAP unavailable: ${e.javaClass.simpleName}") }

        // GetALLClients currently reports MGET_INST_NUM=0 on the supplied capture.
        // Still query it and accept rows if the firmware returns them; per-AP popup
        // remains the source for associated clients when that inventory is empty.
        val globalClients = try {
            getJson("/?_type=menuData&_tag=topo_lua.lua&Action=GetALLClients&_=${System.currentTimeMillis()}", "F6601P GetALLClients")
        } catch (e: Exception) { JSONObject() }
        val globalRows = mutableListOf<JSONObject>()
        collectClientObjects(globalClients, globalRows)

        for (i in 0 until aps.length()) {
            val ap = aps.optJSONObject(i) ?: continue
            val mac = ap.optString("mac")
            val detail = try {
                getJson("/?_type=menuData&_tag=topo_lua.lua&ParentID=${URLEncoder.encode(mac, "UTF-8")}&Action=GetPopData&_=${System.currentTimeMillis()}", "F6601P GetPopData")
            } catch (e: Exception) { JSONObject().put("readError", e.message ?: "Không đọc được chi tiết AP") }
            appendNodeDetail(detail, ap, clients, perNode)
        }

        // Add global inventory rows only when they contain a MAC not already seen.
        val seen = HashSet<String>()
        for (i in 0 until clients.length()) seen.add(normalizeMac(clients.optJSONObject(i)?.optString("mac").orEmpty()))
        for (raw in globalRows) {
            val mac = normalizeMac(raw.optString("MacAddr", raw.optString("MACAddress")))
            if (mac.isBlank() || !seen.add(mac)) continue
            clients.put(JSONObject().put("name", raw.optString("HostName", raw.optString("DeviceName", "Thiết bị không tên")))
                .put("ip", raw.optString("IpAddr", raw.optString("IPAddress"))).put("mac", raw.optString("MacAddr", raw.optString("MACAddress")))
                .put("kind", "Wi-Fi/LAN").put("band", raw.optString("AccessType")).put("upstream", "Chưa xác định AP"))
        }
        val controllerMac = controller.optString("MacAddr")
        return JSONObject().put("url", baseUrl).put("capturedAt", System.currentTimeMillis())
            .put("controllerMac", controllerMac).put("aps", aps).put("clients", clients).put("perNode", perNode)
            .put("inventory", JSONObject().put("wlan", clients.length()).put("lan", 0))
            .put("topology", topology).put("allClientsRaw", globalClients).put("model", "F6601P_VT9310P2N7")
    }

    private fun mergeApFields(target: JSONObject, source: JSONObject) {
        val mapping = mapOf("IpAddr" to "ip", "DeviceName" to "name", "AccessType" to "backhaul", "LinkSpeed" to "linkSpeed",
            "BhStaRssi" to "rssi", "BhStaSnr" to "snr", "SoftwareVer" to "software", "accdevCount" to "clientsCount")
        mapping.forEach { (from, to) -> if (source.has(from)) target.put(to, source.optString(from)) }
    }

    private fun collectClientObjects(value: Any?, out: MutableList<JSONObject>, depth: Int = 0) {
        if (depth > 6 || value == null || value == JSONObject.NULL) return
        when (value) {
            is JSONArray -> for (i in 0 until value.length()) collectClientObjects(value.opt(i), out, depth + 1)
            is JSONObject -> {
                if (value.has("MacAddr") || value.has("MACAddress")) out.add(value)
                else { val keys = value.keys(); while (keys.hasNext()) collectClientObjects(value.opt(keys.next()), out, depth + 1) }
            }
        }
    }

    private fun ensureCookieSupportMarker() {
        val uri = URL(baseUrl.trimEnd('/') + "/").toURI()
        val marker = HttpCookie("_TESTCOOKIESUPPORT_HTTPS_", "1").apply {
            // The router browser sends this as a legacy Netscape cookie pair:
            // _TESTCOOKIESUPPORT_HTTPS_=1. HttpCookie defaults to Version 1,
            // which adds $Version/$Path attributes and does not match Chrome.
            // Force Version 0 so CookieManager emits the exact name=value form.
            version = 0
            path = "/"
            setSecure(uri.scheme.equals("https", ignoreCase = true))
        }
        cookies.cookieStore.cookies
            .filter { it.name == marker.name }
            .forEach { cookies.cookieStore.remove(uri, it) }
        cookies.cookieStore.add(uri, marker)
    }

    /**
     * Requests the Router to preempt an already active administrator session.
     * The preempt_sessid must be obtained dynamically from the Router's conflict
     * flow; never persist or hard-code a captured session ID.
     */
    /** Read the router conflict page and extract the currently offered admin sessions. */
    fun getPreemptCandidates(): List<PreemptCandidate> {
        val html = getText("/")
        if (html.isBlank()) return emptyList()
        val inputRegex = Regex("(?is)<input\\b([^>]*\\bname\\s*=\\s*['\"]preempt_sessid['\"][^>]*)>")
        val result = mutableListOf<PreemptCandidate>()
        for (match in inputRegex.findAll(html)) {
            val attrs = match.groupValues[1]
            val id = Regex("(?is)\\bvalue\\s*=\\s*(['\"])(.*?)\\1").find(attrs)?.groupValues?.get(2)
                ?: Regex("(?is)\\bvalue\\s*=\\s*([^\\s>]+)").find(attrs)?.groupValues?.get(1)
                ?: continue
            if (id.isBlank()) continue
            val radioId = Regex("(?is)\\bid\\s*=\\s*(['\"])(.*?)\\1").find(attrs)?.groupValues?.get(2).orEmpty()
            val label = if (radioId.isNotBlank()) {
                Regex("(?is)<label\\b[^>]*\\bfor\\s*=\\s*['\"]" + Regex.escape(radioId) + "['\"][^>]*>(.*?)</label>")
                    .find(html)?.groupValues?.get(1).orEmpty()
            } else ""
            val nearby = if (label.isNotBlank()) label else html.substring(match.range.last + 1, (match.range.last + 300).coerceAtMost(html.length))
                .replace(Regex("(?is)<[^>]+>"), " ")
            val description = decodeHtml(nearby).replace(Regex("\\s+"), " ").trim().take(160)
                .ifBlank { "Phiên quản trị đang hoạt động" }
            if (result.none { it.sessionId == id }) result += PreemptCandidate(id, description)
        }
        return result
    }

    private fun decodeHtml(value: String): String = value
        .replace("&nbsp;", " ", true).replace("&amp;", "&", true)
        .replace("&lt;", "<", true).replace("&gt;", ">", true)
        .replace("&quot;", "\"", true).replace("&#39;", "'", true)

    fun preemptLoginSession(preemptSessionId: String): JSONObject {
        val id = preemptSessionId.trim()
        require(id.isNotBlank()) { "F6601P không cung cấp preempt_sessid hợp lệ" }
        val result = postJson("/?_type=loginData&_tag=login_preempt", linkedMapOf(
            "_type" to "loginData",
            "_tag" to "login_preempt",
            "preempt_sessid" to id,
            "action" to "preempt"
        ))
        if (!result.optBoolean("login_need_refresh", false)) {
            throw IllegalStateException("Router chưa xác nhận login_preempt: ${result}")
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
        if (modelKey == "F6601P_VT9310P2N7") return readF6601pMesh()
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

    private fun request(method: String, path: String, body: String?, extraHeaders: Map<String, String> = emptyMap()): String {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection)
        val apiName = path.substringAfter("_tag=", path.substringBefore('?')).substringBefore('&').ifBlank { "root" }
        val diagnosticName = apiName
        val uri = url.toURI()
        val sidBefore = cookieValue("SID_HTTPS_")
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
            extraHeaders.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val outgoingCookies = cookies.get(uri, emptyMap())
            outgoingCookies.forEach { (k, values) -> values.forEach { connection.addRequestProperty(k, it) } }
            logDiag("HTTP request: api=$diagnosticName method=$method path=${safePath(path)} cookieNames=${cookieNames(outgoingCookies)} markerPresent=${hasCookie(outgoingCookies, "_TESTCOOKIESUPPORT_HTTPS_")} sidPresent=${hasCookie(outgoingCookies, "SID_HTTPS_")} sidInStore=${sidBefore.isNotBlank()} accept=${connection.getRequestProperty("Accept")} xRequestedWith=${connection.getRequestProperty("X-Requested-With")} contentType=${if (body == null) "none" else "application/x-www-form-urlencoded"} bodyLength=${body?.length ?: 0}")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val responseHeaders = connection.headerFields.filterKeys { it != null }
            val setCookieDetails = responseCookieDetails(responseHeaders)
            cookies.put(uri, responseHeaders)
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val contentType = connection.contentType.orEmpty().substringBefore(';')
            val sessionTimeout = text.contains("SessionTimeout", ignoreCase = true)
            val xmlLike = text.trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith("<")
            val sidAfter = cookieValue("SID_HTTPS_")
            logDiag("HTTP response: api=$diagnosticName http=$code contentType=$contentType bodyLength=${text.length} xmlLike=$xmlLike sessionTimeout=$sessionTimeout setCookie=$setCookieDetails sidStoredAfter=${sidAfter.isNotBlank()} sidChanged=${sidBefore.isNotBlank() && sidAfter.isNotBlank() && sidBefore != sidAfter} cookieStore=${cookieStoreSummary()}")
            if (xmlLike || sessionTimeout || code !in 200..299) {
                logDiag("HTTP safeBody: api=$diagnosticName body=${safeResponsePreview(text)}")
            }
            if (code !in 200..299) throw IllegalStateException("Router HTTP $code: ${text.take(250)}")
            return text
        } catch (e: Exception) {
            // Never log exception messages: some implementations include URLs, headers or payloads.
            logDiag("HTTP failure: api=$diagnosticName exception=${e.javaClass.simpleName}")
            throw e
        } finally { connection.disconnect() }
    }

    private fun logDiag(message: String) { Log.i(DIAGNOSTIC_TAG, message) }

    private fun safeHost(value: String): String = try { URL(value).host } catch (_: Exception) { "unknown" }

    private fun safePath(path: String): String = path.substringBefore('?').take(160)

    private fun cookieValue(name: String): String = cookies.cookieStore.cookies.firstOrNull { it.name.equals(name, true) }?.value.orEmpty()

    private fun cookieStoreSummary(): String = cookies.cookieStore.cookies.joinToString(prefix = "[", postfix = "]") { cookie ->
        "${cookie.name}(secure=${cookie.secure},httpOnly=${cookie.isHttpOnly},path=${cookie.path ?: ""},domain=${cookie.domain ?: ""},version=${cookie.version},expired=${cookie.hasExpired()},maxAge=${cookie.maxAge})"
    }

    /** Preserve XML structure/status fields while redacting all potentially sensitive values. */
    private fun safeResponsePreview(raw: String): String {
        var safe = raw.replace(Regex("(?is)(<(?:ParaValue|Password|Username|sess_token|_sessionTOKEN|login_token|SID_HTTPS_)[^>]*>).*?(</(?:ParaValue|Password|Username|sess_token|_sessionTOKEN|login_token|SID_HTTPS_)>)"), "$1[REDACTED]$2")
        safe = safe.replace(Regex("""(?i)("(?:password|username|sess_token|token|sid|cookie)"\s*:\s*")[^"]*(")"""), "$1[REDACTED]$2")
        safe = safe.replace(Regex("""(?i)(password|username|sess_token|token|sid|cookie)=([^&\s]+)"""), "$1=[REDACTED]")
        return safe.replace('\n', ' ').replace('\r', ' ').take(1200)
    }

    private fun responseCookieDetails(headers: Map<String?, List<String>>): String {
        val details = headers.entries.filter { it.key.equals("Set-Cookie", true) || it.key.equals("Set-Cookie2", true) }
            .flatMap { it.value.orEmpty() }.mapNotNull { raw ->
                try {
                    val parsed = HttpCookie.parse(raw).firstOrNull() ?: return@mapNotNull null
                    "${parsed.name}(secure=${parsed.secure},httpOnly=${parsed.isHttpOnly},path=${parsed.path ?: ""},domain=${parsed.domain ?: ""},version=${parsed.version},discard=${parsed.discard},maxAge=${parsed.maxAge})"
                } catch (_: Exception) {
                    raw.substringBefore('=').trim().takeIf { it.isNotBlank() }?.let { "$it(attributes=parseFailed)" }
                }
            }
        return details.distinct().toString()
    }

    /** Log cookie names/presence only. Never log cookie values, SID, credentials or tokens. */
    private fun cookieNames(headers: Map<String, List<String>>): List<String> = headers.entries
        .filter { it.key.equals("Cookie", true) || it.key.equals("Cookie2", true) }
        .flatMap { it.value }
        .flatMap { header -> header.split(';') }
        .map { it.trim().substringBefore('=').trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun hasCookie(headers: Map<String, List<String>>, name: String): Boolean =
        cookieNames(headers).any { it.equals(name, true) }

    private fun responseCookieNames(headers: Map<String?, List<String>>): List<String> = headers.entries
        .filter { it.key.equals("Set-Cookie", true) || it.key.equals("Set-Cookie2", true) }
        .flatMap { it.value.orEmpty() }
        .map { it.substringBefore(';').substringBefore('=').trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun cookieStoreHas(name: String): Boolean =
        cookies.cookieStore.cookies.any { it.name.equals(name, true) }

    private companion object {
        const val DIAGNOSTIC_TAG = "ZteApiDiag"
    }

    private fun extractXmlValue(xml: String): String = Regex("<ajax_response_xml_root>\\s*(.*?)\\s*</ajax_response_xml_root>", RegexOption.DOT_MATCHES_ALL)
        .find(xml)?.groupValues?.getOrNull(1)?.trim().orEmpty()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
