package com.example.ztemeshmanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
            .getInt("refresh_interval_seconds", 30).coerceIn(10, 3600)
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
        setContentView(dashboard)
        dashboard.loadUrl("file:///android_asset/index.html")
        // Never present a previous router's snapshot as current data after process restart.
        clearSnapshot("Đang kiểm tra phiên Router…")
        attemptSecureAutoLogin()
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
            .getString("router", "https://192.168.1.11/") ?: "https://192.168.1.11/"
        io.execute {
            try {
                val client = ZteApiClient(base)
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
                CredentialVault.clear(this)
                clearSnapshot("Tự đăng nhập thất bại: ${e.message ?: "lỗi không rõ"}")
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
        // Do not race startup auto-login or repeat the fresh read just completed by AdminActivity.
        if (ZteSession.authenticated && !authInProgress) refreshFromRouter()
        handler.postDelayed(refreshTask, refreshIntervalMs())
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
        @JavascriptInterface fun openAdmin() {
            runOnUiThread { startActivity(Intent(this@MainActivity, AdminActivity::class.java)) }
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
        @JavascriptInterface fun isSnapshotStale(): Boolean =
            getSharedPreferences("zte", MODE_PRIVATE).getBoolean("snapshot_stale", false)
        @JavascriptInterface fun isLoggedIn(): Boolean = ZteSession.authenticated
        @JavascriptInterface fun getRefreshIntervalSeconds(): Int =
            getSharedPreferences("zte", MODE_PRIVATE).getInt("refresh_interval_seconds", 30).coerceIn(10, 3600)
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
        if (::dashboard.isInitialized) dashboard.evaluateJavascript("window.refreshSnapshot && refreshSnapshot();", null)
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
