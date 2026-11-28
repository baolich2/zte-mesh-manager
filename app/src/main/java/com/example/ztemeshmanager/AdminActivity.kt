package com.example.ztemeshmanager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class AdminActivity : Activity() {
    private lateinit var routerInput: EditText
    private lateinit var userInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var status: TextView
    private lateinit var loginButton: Button
    private val io = Executors.newSingleThreadExecutor()
    private val language: String by lazy { getSharedPreferences("zte", MODE_PRIVATE).getString("ui_language", "vi") ?: "vi" }
    private fun tr(vi: String, en: String, zh: String): String = when (language) { "en" -> en; "zh-CN" -> zh; else -> vi }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bg = 0xFF0B1220.toInt(); val panel = 0xFF151F30.toInt(); val text = 0xFFEEF4FF.toInt(); val muted = 0xFF91A4BD.toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 26, 22, 22); setBackgroundColor(bg) }
        val title = TextView(this).apply { this.text = tr("Kết nối Router ZTE", "Connect to ZTE Router", "连接 ZTE 路由器"); textSize = 23f; setTextColor(text) }
        root.addView(title)
        val modelKey = getSharedPreferences("zte", MODE_PRIVATE).getString("router_model", "HV3601P_V93_FPT") ?: "HV3601P_V93_FPT"
        val modelLabel = if (modelKey == "F6601P_VT9310P2N7") "ZTE F6601P · VT9.3.10P2N7" else "ZTE HV3601P · v9.3 FPT"
        root.addView(Button(this).apply {
            this.text = tr("Loại Router: $modelLabel · Thay đổi", "Router model: $modelLabel · Change", "路由器型号：$modelLabel · 更改")
            setOnClickListener { showModelPicker() }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
        root.addView(TextView(this).apply { this.text = tr("Đăng nhập native qua API HTTPS của Router. Không mở WebView quản trị.", "Sign in through the Router HTTPS API. The admin WebView is not opened.", "通过路由器 HTTPS API 登录，不打开管理 WebView。"); setTextColor(muted); textSize = 13f; setPadding(0, 8, 0, 18) })
        fun field(hint: String, value: String = ""): EditText = EditText(this).apply {
            this.hint = hint; setText(value); setSingleLine(true); setTextColor(text); setHintTextColor(muted)
            setPadding(14, 10, 14, 10); setBackgroundColor(panel)
        }
        val savedRouterUrl = getSharedPreferences("zte", MODE_PRIVATE).getString("router", null)
        routerInput = field(tr("Địa chỉ Router", "Router address", "路由器地址"), savedRouterUrl ?: RouterDefaults.urlFor(modelKey))
        userInput = field(tr("Tên đăng nhập", "Username", "用户名"), getSharedPreferences("zte", MODE_PRIVATE).getString("username", "admin") ?: "admin")
        passwordInput = field(tr("Mật khẩu", "Password", "密码"))
        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        listOf(routerInput, userInput, passwordInput).forEach { v -> root.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }) }
        status = TextView(this).apply { setTextColor(muted); textSize = 13f; this.text = tr("Nhập thông tin tài khoản để kết nối.", "Enter your account details to connect.", "请输入账户信息以连接。"); setPadding(0, 10, 0, 14) }
        root.addView(status)
        loginButton = Button(this).apply { this.text = tr("ĐĂNG NHẬP", "LOG IN", "登录"); setOnClickListener { doLogin() } }
        root.addView(loginButton, LinearLayout.LayoutParams(-1, -2))
        val logout = Button(this).apply { this.text = tr("Đăng xuất", "Log out", "退出登录"); setOnClickListener { doLogout() } }
        root.addView(logout, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(this).apply { this.text = tr("← Quay lại Dashboard", "← Back to Dashboard", "← 返回总览"); setOnClickListener { finish() } }, LinearLayout.LayoutParams(-1, -2))
        setContentView(ScrollView(this).apply { addView(root) })
        if (ZteSession.authenticated) status.text = tr("Đã có phiên Router đang hoạt động.", "An active Router session already exists.", "路由器会话已处于活动状态。")
        else if (modelKey == "F6601P_VT9310P2N7") status.text = "F6601P · dùng quy trình đăng nhập, Topology và API riêng cho firmware VT9.3.10P2N7."
    }


    private fun showModelPicker() {
        val labels = arrayOf(
            "ZTE HV3601P · v9.3 · firmware FPT",
            "ZTE F6601P · VT9.3.10P2N7"
        )
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        val selected = if (prefs.getString("router_model", "HV3601P_V93_FPT") == "F6601P_VT9310P2N7") 1 else 0
        AlertDialog.Builder(this).setTitle(tr("Chọn loại Router", "Select Router model", "选择路由器型号"))
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                val value = if (which == 1) "F6601P_VT9310P2N7" else "HV3601P_V93_FPT"
                if (prefs.getString("router_model", "HV3601P_V93_FPT") == value) {
                    dialog.dismiss()
                } else {
                    AlertDialog.Builder(this).setTitle("Xác nhận đổi Router")
                        .setMessage("Dashboard sẽ khởi động lại theo loại Router mới. Tài khoản/mật khẩu đã lưu vẫn được giữ.")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Đổi và khởi động lại") { _, _ ->
                            val oldModel = prefs.getString("router_model", RouterDefaults.HV3601P)
                            val oldUrl = prefs.getString("router", RouterDefaults.urlFor(oldModel))
                            val edit = prefs.edit().putString("router_model", value)
                            if (oldUrl.isNullOrBlank() || RouterDefaults.isDefaultUrl(oldUrl, oldModel)) {
                                edit.putString("router", RouterDefaults.urlFor(value))
                            }
                            edit.remove("snapshot_json").remove("snapshot").remove("snapshot_time").putBoolean("snapshot_stale", false).apply()
                            ZteSession.client = null; ZteSession.authenticated = false
                            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            finish()
                        }.showZteStyled()
                    dialog.dismiss()
                }
            }.setNegativeButton("Đóng", null).showZteStyled()
    }

    private fun doLogin() {
        val base = routerInput.text.toString().trim().let { if (it.endsWith("/")) it else "$it/" }
        val user = userInput.text.toString().trim()
        val pass = passwordInput.text.toString()
        // Resolve the selected Router model before validation so the localized
        // example URL and the API client use the same model configuration.
        val modelKey = getSharedPreferences("zte", MODE_PRIVATE)
            .getString("router_model", RouterDefaults.HV3601P) ?: RouterDefaults.HV3601P
        val host = try { java.net.URI(base).host ?: "" } catch (_: Exception) { "" }
        val privateHost = host.startsWith("192.168.") || host.startsWith("10.") ||
            (host.startsWith("172.") && ((host.split(".").getOrNull(1)?.toIntOrNull() ?: -1) in 16..31))
        if (!base.startsWith("https://") || !privateHost || user.isBlank() || pass.isBlank()) {
            status.text = tr("Hãy nhập địa chỉ HTTPS của Router trong mạng nội bộ (ví dụ ${RouterDefaults.urlFor(modelKey)}), tên đăng nhập và mật khẩu.", "Enter the Router HTTPS address on your local network (for example, ${RouterDefaults.urlFor(modelKey)}), username and password.", "请输入局域网内路由器的 HTTPS 地址（例如 ${RouterDefaults.urlFor(modelKey)}）、用户名和密码。"); return
        }
        loginButton.isEnabled = false
        status.text = tr("Đang khởi tạo phiên và xác thực với Router…", "Creating a session and authenticating with Router…", "正在创建会话并验证路由器…")
        io.execute {
            // Keep the exact client instance that attempted login. Its CookieManager
            // contains the Router-issued SID/cookies needed to retrieve the conflict
            // page and its dynamic preempt_sessid values. A fresh probe loses that session.
            var attemptedClient: ZteApiClient? = null
            try {
                val client = ZteApiClient(base, modelKey)
                attemptedClient = client
                client.login(user, pass)
                val fresh = client.readMesh()
                completeLogin(client, base, user, pass, fresh)
            } catch (e: Exception) {
                ZteSession.authenticated = false
                if (modelKey == "F6601P_VT9310P2N7") {
                    try {
                        // Do not create a new API client here: the Router conflict
                        // response is tied to the cookie/session from the failed attempt.
                        val probe = attemptedClient ?: ZteApiClient(base, modelKey)
                        val candidates = probe.getPreemptCandidates()
                        if (candidates.isNotEmpty()) {
                            runOnUiThread {
                                val selected = intArrayOf(0)
                                AlertDialog.Builder(this)
                                    .setTitle(tr("Router đang có phiên quản trị khác", "Another admin session is active", "路由器上已有其他管理员会话"))
                                    .setMessage(tr("Chọn phiên muốn ngắt. Chỉ thực hiện khi bạn xác nhận; phiên được chọn sẽ bị đăng xuất khỏi Router.", "Choose the session to disconnect. This only happens after confirmation; the selected session will be logged out.", "选择要断开的会话。只有确认后才会执行，被选中的会话将退出路由器。"))
                                    .setSingleChoiceItems(candidates.map { it.description }.toTypedArray(), 0) { _, which -> selected[0] = which }
                                    .setNegativeButton(tr("Hủy", "Cancel", "取消"), null)
                                    .setPositiveButton(tr("Ngắt phiên và đăng nhập", "Disconnect and log in", "断开会话并登录")) { _, _ ->
                                        val candidate = candidates[selected[0].coerceIn(candidates.indices)]
                                        loginButton.isEnabled = false
                                        status.text = tr("Đang xin Router ngắt phiên đã chọn…", "Asking Router to disconnect the selected session…", "正在请求路由器断开所选会话…")
                                        io.execute {
                                            try {
                                                probe.preemptLoginSession(candidate.sessionId)
                                                val retry = ZteApiClient(base, modelKey)
                                                retry.login(user, pass)
                                                completeLogin(retry, base, user, pass, retry.readMesh())
                                            } catch (retryError: Exception) {
                                                runOnUiThread { status.text = tr("Không thể tiếp tục đăng nhập: ${retryError.message}", "Could not continue login: ${retryError.message}", "无法继续登录：${retryError.message}"); loginButton.isEnabled = true }
                                            }
                                        }
                                    }.showZteStyled()
                            }
                            return@execute
                        }
                    } catch (_: Exception) { /* No conflict page; show the original login error. */ }
                }
                runOnUiThread {
                    val raw = e.message ?: "UNKNOWN_ERROR"
                    status.text = if (raw.startsWith("AUTH_REJECTED:")) {
                        tr("Sai tài khoản hoặc mật khẩu. Router đã từ chối thông tin đăng nhập.", "Incorrect username or password. The Router rejected the credentials.", "用户名或密码错误，路由器拒绝了登录信息。")
                    } else {
                        tr("Đăng nhập chưa hoàn tất. Lỗi tại bước xử lý Router: $raw", "Login did not complete. Router processing error: $raw", "登录未完成。路由器处理错误：$raw")
                    }
                    loginButton.isEnabled = true
                }
            }
        }
    }


    private fun completeLogin(client: ZteApiClient, base: String, user: String, pass: String, fresh: org.json.JSONObject) {
        val newControllerMac = fresh.optString("controllerMac").uppercase().filter { it.isLetterOrDigit() }
        // Router Information is best-effort and model-specific; never block a
        // successful login or Mesh snapshot if this optional API is unavailable.
        val routerInfo = try { client.readRouterInfo() } catch (e: Exception) {
            org.json.JSONObject().put("error", e.message ?: "Router Information không khả dụng (${e.javaClass.simpleName})")
        }
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        val oldControllerMac = prefs.getString("router_controller_mac", "")?.uppercase()?.filter { it.isLetterOrDigit() } ?: ""
        val changedSystem = newControllerMac.isNotBlank() && oldControllerMac.isNotBlank() && newControllerMac != oldControllerMac
        val stamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        prefs.edit().putString("router", base).putString("username", user)
            .putString("router_controller_mac", newControllerMac).putString("snapshot_json", fresh.toString())
            .putString("router_info_json", routerInfo.toString())
            .putString("snapshot_time", stamp).putLong("last_sync_epoch", System.currentTimeMillis())
            .putString("auto_status", if (changedSystem) "Đã chuyển sang hệ Router mới · dữ liệu vừa đồng bộ" else "Đăng nhập thành công · dữ liệu vừa đồng bộ")
            .putBoolean("snapshot_stale", false).apply()
        ZteSession.client = client; ZteSession.routerUrl = base; ZteSession.authenticated = true
        CredentialVault.save(this, user, pass)
        prefs.edit().putBoolean("navigate_dashboard_after_login", true).apply()
        runOnUiThread {
            Toast.makeText(this, tr("Đã kết nối Router ZTE", "Connected to ZTE Router", "已连接 ZTE 路由器"), Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK); finish()
        }
    }

    private fun doLogout() {
        status.text = tr("Đang đăng xuất…", "Logging out…", "正在退出登录…")
        io.execute {
            try { ZteSession.client?.logout() } catch (e: Exception) { runOnUiThread { status.text = tr("Đã gửi đăng xuất; phản hồi: ${e.message}", "Logout sent; response: ${e.message}", "已发送退出请求；响应：${e.message}") } }
            ZteSession.client = null; ZteSession.authenticated = false
            CredentialVault.clear(this)
            getSharedPreferences("zte", MODE_PRIVATE).edit()
                .remove("snapshot_json").remove("snapshot").remove("snapshot_time").remove("router_info_json")
                .putBoolean("snapshot_stale", false)
                .putString("auto_status", "Đã đăng xuất Router")
                .apply()
            runOnUiThread {
                Toast.makeText(this, tr("Đã đăng xuất", "Logged out", "已退出登录"), Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onDestroy() { io.shutdownNow(); super.onDestroy() }
}
