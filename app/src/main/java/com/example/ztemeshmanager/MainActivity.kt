package com.example.ztemeshmanager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var dashboard: WebView
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    @Volatile private var busy = false
    @Volatile private var authInProgress = false
    private fun refreshIntervalMs(): Long {
        val seconds = getSharedPreferences("zte", MODE_PRIVATE)
            .getInt("refresh_interval_seconds", 180).coerceIn(10, 3600)
        return seconds * 1000L
    }
    private val refreshTask = object : Runnable {
        override fun run() {
            if (isFinishing) return
            refreshFromRouter()
            handler.postDelayed(this, refreshIntervalMs())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboard = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(DashboardBridge(), "AndroidApp")
        }
        val root = FrameLayout(this)
        root.addView(dashboard, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
        dashboard.loadUrl("file:///android_asset/index.html")
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        // First launch is configured inside the WebView's combined Language + Router
        // setup screen. Existing installations keep their saved model and go straight
        // to Dashboard without seeing the onboarding screen again.
        if (!prefs.getString("router_model", null).isNullOrBlank()) {
            startForSelectedRouter()
        }
    }


    private fun startForSelectedRouter() {
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        val model = prefs.getString("router_model", RouterDefaults.HV3601P)
        ZteSession.routerUrl = prefs.getString("router", RouterDefaults.urlFor(model)) ?: RouterDefaults.urlFor(model)
        clearSnapshot(if (model == "F6601P_VT9310P2N7") "Đang khởi tạo hồ sơ F6601P · kiểm tra phiên và đồng bộ dữ liệu…" else "Đang kiểm tra phiên Router…")
        attemptSecureAutoLogin()
    }

    private fun showRouterModelPicker(firstRun: Boolean) {
        val labels = arrayOf(
            "ZTE HV3601P · v9.3 · firmware FPT",
            "ZTE F6601P · VT9.3.10P2N7"
        )
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        val current = if (prefs.getString("router_model", "HV3601P_V93_FPT") == "F6601P_VT9310P2N7") 1 else 0
        AlertDialog.Builder(this)
            .setTitle(if (firstRun) "Chọn loại Router" else "Loại Router đang sử dụng")
            .setSingleChoiceItems(labels, if (firstRun) -1 else current) { dialog, which ->
                val selected = if (which == 1) "F6601P_VT9310P2N7" else "HV3601P_V93_FPT"
                val old = prefs.getString("router_model", null)
                if (old == selected) {
                    dialog.dismiss()
                } else if (firstRun) {
                    prefs.edit().putString("router_model", selected)
                        .putString("router", RouterDefaults.urlFor(selected)).apply()
                    dialog.dismiss()
                    startForSelectedRouter()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Xác nhận đổi Router")
                        .setMessage("Ứng dụng sẽ đóng phiên hiện tại, làm mới Dashboard và mở lại theo cấu hình đã chọn. Thông tin đăng nhập đã lưu không bị xóa.")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Đổi và khởi động lại") { _, _ ->
                            val oldUrl = prefs.getString("router", RouterDefaults.urlFor(old))
                            val edit = prefs.edit().putString("router_model", selected)
                            // Move only a model-default address; preserve a manually configured custom URL.
                            if (oldUrl.isNullOrBlank() || RouterDefaults.isDefaultUrl(oldUrl, old)) {
                                edit.putString("router", RouterDefaults.urlFor(selected))
                            }
                            edit.remove("snapshot_json").remove("snapshot").remove("snapshot_time")
                                .putBoolean("snapshot_stale", false).apply()
                            ZteSession.client = null
                            ZteSession.authenticated = false
                            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                        }.showZteStyled()
                    dialog.dismiss()
                }
            }
            .setNegativeButton(if (firstRun) "Thoát" else "Đóng") { _, _ -> if (firstRun) finish() }
            .setCancelable(!firstRun)
            .showZteStyled()
    }

    private fun clearSnapshot(status: String) {
        getSharedPreferences("zte", MODE_PRIVATE).edit()
            .remove("snapshot_json")
            .remove("snapshot")
            .remove("snapshot_time")
            .putBoolean("snapshot_stale", false)
            .putString("auto_status", status)
            .apply()
    }

    private fun stamp(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    private fun attemptSecureAutoLogin() {
        if (ZteSession.authenticated || authInProgress) return
        val saved = CredentialVault.load(this)
        if (saved == null) {
            clearSnapshot("Chưa đăng nhập Router")
            refreshSnapshotUi()
            return
        }
        authInProgress = true
        clearSnapshot("Đang tự đăng nhập và đồng bộ Router…")
        refreshSnapshotUi()
        val base = getSharedPreferences("zte", MODE_PRIVATE)
            .getString("router", RouterDefaults.urlFor(getSharedPreferences("zte", MODE_PRIVATE).getString("router_model", RouterDefaults.HV3601P)))
            ?: RouterDefaults.urlFor(getSharedPreferences("zte", MODE_PRIVATE).getString("router_model", RouterDefaults.HV3601P))
        io.execute {
            try {
                val modelKey = getSharedPreferences("zte", MODE_PRIVATE).getString("router_model", "HV3601P_V93_FPT") ?: "HV3601P_V93_FPT"
                val client = ZteApiClient(base, modelKey)
                client.login(saved.first, saved.second)
                val fresh = client.readMesh()
                val prefs = getSharedPreferences("zte", MODE_PRIVATE)
                val newMac = fresh.optString("controllerMac").uppercase().filter { it.isLetterOrDigit() }
                val stamp = stamp()
                prefs.edit()
                    .putString("snapshot_json", fresh.toString())
                    .putString("snapshot_time", stamp)
                    .putLong("last_sync_epoch", System.currentTimeMillis())
                    .putString("router_controller_mac", newMac)
                    .putString("auto_status", "Tự đăng nhập thành công · dữ liệu vừa đồng bộ")
                    .putBoolean("snapshot_stale", false)
                    .apply()
                ZteSession.client = client
                ZteSession.routerUrl = base
                ZteSession.authenticated = true
            } catch (e: Exception) {
                ZteSession.authenticated = false
                ZteSession.client = null
                val reason = e.message ?: "UNKNOWN_ERROR"
                // Keep credentials for network/session/topology failures; clear only on
                // an explicit Router authentication rejection.
                if (reason.startsWith("AUTH_REJECTED:")) CredentialVault.clear(this)
                clearSnapshot(if (reason.startsWith("AUTH_REJECTED:"))
                    "Tự đăng nhập thất bại: sai tài khoản hoặc mật khẩu"
                else "Chưa hoàn tất tự đăng nhập (có thể lỗi phiên/mạng/Topology): $reason")
            } finally {
                authInProgress = false
                runOnUiThread { if (!isFinishing) refreshSnapshotUi() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshTask)
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        if (prefs.getBoolean("navigate_dashboard_after_login", false)) {
            prefs.edit().putBoolean("navigate_dashboard_after_login", false).apply()
            dashboard.post { dashboard.evaluateJavascript("if(window.showPage)showPage('home');", null) }
        }
        // Resume must respect the configured sync interval. Returning from Home or
        // another Activity does not itself justify a Router request or a new login.
        // AdminActivity writes last_sync_epoch after its successful login/read, so this
        // also avoids immediately repeating that fresh read when returning from login.
        val interval = refreshIntervalMs()
        val lastSync = prefs.getLong("last_sync_epoch", 0L)
        val elapsed = if (lastSync > 0L) (System.currentTimeMillis() - lastSync).coerceAtLeast(0L) else interval
        val remaining = (interval - elapsed).coerceAtLeast(0L)
        if (ZteSession.authenticated && !authInProgress && remaining == 0L) {
            refreshFromRouter()
            handler.postDelayed(refreshTask, interval)
        } else {
            handler.postDelayed(refreshTask, if (ZteSession.authenticated) remaining else interval)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        super.onPause()
    }

    private fun refreshFromRouter() {
        if (authInProgress || busy || !ZteSession.authenticated) {
            runOnUiThread { if (!isFinishing) refreshSnapshotUi() }
            return
        }
        val client = ZteSession.client
        if (client == null) {
            ZteSession.authenticated = false
            clearSnapshot("Chưa có phiên Router")
            refreshSnapshotUi()
            return
        }
        val prefs = getSharedPreferences("zte", MODE_PRIVATE)
        // AdminActivity already performed a full read immediately before returning here.
        val lastSync = prefs.getLong("last_sync_epoch", 0L)
        if (System.currentTimeMillis() - lastSync in 0..10_000L) {
            refreshSnapshotUi()
            return
        }
        busy = true
        io.execute {
            try {
                val data = client.readMesh()
                val stamp = stamp()
                prefs.edit()
                    .putString("snapshot_json", data.toString())
                    .putString("snapshot_time", stamp)
                    .putLong("last_sync_epoch", System.currentTimeMillis())
                    .putString("auto_status", "Online · dữ liệu vừa đồng bộ")
                    .putBoolean("snapshot_stale", false)
                    .apply()
            } catch (e: Exception) {
                val message = e.message ?: "lỗi không rõ"
                if (message.contains("SessionTimeout", ignoreCase = true)) {
                    // A stale session is not an online router. Clear the old view and re-authenticate once.
                    ZteSession.authenticated = false
                    ZteSession.client = null
                    clearSnapshot("Phiên Router hết hạn · đang thử đăng nhập lại…")
                } else {
                    // Keep a truthful empty state rather than showing an old snapshot as live.
                    clearSnapshot("Không đồng bộ được Router: $message")
                }
            } finally {
                busy = false
                runOnUiThread {
                    if (!isFinishing) {
                        refreshSnapshotUi()
                        if (!ZteSession.authenticated && !authInProgress) attemptSecureAutoLogin()
                    }
                }
            }
        }
    }

    inner class DashboardBridge {
        @JavascriptInterface fun openProjectGithub() {
            runOnUiThread {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/baolich2/zte-mesh-manager")))
            }
        }
        @JavascriptInterface fun openAdmin() {
            runOnUiThread { startActivity(Intent(this@MainActivity, AdminActivity::class.java)) }
        }
        @JavascriptInterface fun selectRouterModel() {
            runOnUiThread { showRouterModelPicker(firstRun = false) }
        }
        @JavascriptInterface fun showMessage(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show() }
        }
        @JavascriptInterface fun getSnapshot(): String =
            getSharedPreferences("zte", MODE_PRIVATE).getString("snapshot", "") ?: ""
        @JavascriptInterface fun getSnapshotTime(): String =
            getSharedPreferences("zte", MODE_PRIVATE).getString("snapshot_time", "Chưa có dữ liệu") ?: "Chưa có dữ liệu"
        @JavascriptInterface fun getSnapshotJson(): String =
            getSharedPreferences("zte", MODE_PRIVATE).getString("snapshot_json", "") ?: ""
        @JavascriptInterface fun getAutoStatus(): String =
            getSharedPreferences("zte", MODE_PRIVATE).getString("auto_status", "Chưa đăng nhập Router") ?: "Chưa đăng nhập Router"
        @JavascriptInterface fun getAutoUrl(): String = ZteSession.routerUrl
        @JavascriptInterface fun getRouterDisplayName(): String {
            val key = getSharedPreferences("zte", MODE_PRIVATE).getString("router_model", RouterDefaults.HV3601P)
            return RouterDefaults.displayName(key)
        }
        @JavascriptInterface fun setUiLanguage(language: String) {
            val safe = if (language in setOf("vi", "en", "zh-CN")) language else "vi"
            getSharedPreferences("zte", MODE_PRIVATE).edit().putString("ui_language", safe).apply()
        }
        @JavascriptInterface fun isInitialSetupRequired(): Boolean =
            getSharedPreferences("zte", MODE_PRIVATE).getString("router_model", null).isNullOrBlank()

        @JavascriptInterface fun completeInitialSetup(language: String, modelKey: String): Boolean {
            val safeLanguage = if (language in setOf("vi", "en", "zh-CN")) language else "vi"
            val safeModel = if (modelKey == "F6601P_VT9310P2N7") "F6601P_VT9310P2N7" else RouterDefaults.HV3601P
            val prefs = getSharedPreferences("zte", MODE_PRIVATE)
            prefs.edit()
                .putString("ui_language", safeLanguage)
                .putString("router_model", safeModel)
                .putString("router", RouterDefaults.urlFor(safeModel))
                .apply()
            ZteSession.routerUrl = RouterDefaults.urlFor(safeModel)
            runOnUiThread { startForSelectedRouter() }
            return true
        }
        @JavascriptInterface fun isSnapshotStale(): Boolean =
            getSharedPreferences("zte", MODE_PRIVATE).getBoolean("snapshot_stale", false)
        @JavascriptInterface fun isLoggedIn(): Boolean = ZteSession.authenticated
        @JavascriptInterface fun getRefreshIntervalSeconds(): Int =
            getSharedPreferences("zte", MODE_PRIVATE).getInt("refresh_interval_seconds", 180).coerceIn(10, 3600)
        @JavascriptInterface fun setRefreshIntervalSeconds(seconds: Int): Boolean {
            if (seconds !in 10..3600) return false
            getSharedPreferences("zte", MODE_PRIVATE).edit().putInt("refresh_interval_seconds", seconds).apply()
            runOnUiThread {
                handler.removeCallbacks(refreshTask)
                handler.postDelayed(refreshTask, refreshIntervalMs())
            }
            return true
        }
        @JavascriptInterface fun refreshNow() { runOnUiThread { refreshFromRouter() } }
        @JavascriptInterface fun logout() {
            io.execute {
                try { ZteSession.client?.logout() } catch (_: Exception) {}
                ZteSession.authenticated = false
                ZteSession.client = null
                CredentialVault.clear(this@MainActivity)
                clearSnapshot("Đã đăng xuất Router")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Đã đăng xuất Router", Toast.LENGTH_SHORT).show()
                    refreshSnapshotUi()
                }
            }
        }
    }

    private fun refreshSnapshotUi() {
        if (::dashboard.isInitialized) dashboard.evaluateJavascript("window.updateRouterIdentity && updateRouterIdentity(); window.refreshSnapshot && refreshSnapshot();", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { super.onBackPressed() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        io.shutdownNow()
        dashboard.stopLoading()
        dashboard.destroy()
        super.onDestroy()
    }
}
