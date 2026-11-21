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
    private var busy = false
    private val refreshMs = 30_000L
    private val refreshTask = object : Runnable {
        override fun run() {
            if (isFinishing) return
            refreshFromRouter()
            handler.postDelayed(this, refreshMs)
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
        attemptSecureAutoLogin()
    }

    private fun attemptSecureAutoLogin() {
        if (ZteSession.authenticated) return
        val saved = CredentialVault.load(this) ?: return
        val base = getSharedPreferences("zte", MODE_PRIVATE).getString("router", "https://192.168.1.11/") ?: "https://192.168.1.11/"
        io.execute {
            try {
                val client = ZteApiClient(base)
                client.login(saved.first, saved.second)
                ZteSession.client = client
                ZteSession.routerUrl = base
                ZteSession.authenticated = true
                getSharedPreferences("zte", MODE_PRIVATE).edit().putString("auto_status", "Tự đăng nhập thành công · cập nhật mỗi 30 giây").apply()
                runOnUiThread { if (!isFinishing) dashboard.evaluateJavascript("window.refreshSnapshot && refreshSnapshot();", null) }
            } catch (e: Exception) {
                CredentialVault.clear(this)
                getSharedPreferences("zte", MODE_PRIVATE).edit().putString("auto_status", "Phiên hết hạn hoặc xác thực lại thất bại: ${e.message ?: "lỗi"}").apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshTask)
        refreshFromRouter()
        handler.postDelayed(refreshTask, refreshMs)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        super.onPause()
    }

    private fun refreshFromRouter() {
        val client = ZteSession.client
        if (!ZteSession.authenticated || client == null || busy) {
            runOnUiThread { dashboard.evaluateJavascript("window.refreshSnapshot && refreshSnapshot();", null) }
            return
        }
        busy = true
        io.execute {
            try {
                val data = client.readMesh()
                val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                getSharedPreferences("zte", MODE_PRIVATE).edit()
                    .putString("snapshot_json", data.toString())
                    .putString("snapshot_time", stamp)
                    .putString("auto_status", "Đang kết nối · cập nhật mỗi 30 giây")
                    .apply()
            } catch (e: Exception) {
                getSharedPreferences("zte", MODE_PRIVATE).edit()
                    .putString("auto_status", "Lỗi đọc Router: ${e.message ?: "không rõ"}")
                    .apply()
            } finally {
                busy = false
                runOnUiThread { if (!isFinishing) dashboard.evaluateJavascript("window.refreshSnapshot && refreshSnapshot();", null) }
            }
        }
    }

    inner class DashboardBridge {
        @JavascriptInterface fun openAdmin() { runOnUiThread { startActivity(Intent(this@MainActivity, AdminActivity::class.java)) } }
        @JavascriptInterface fun showMessage(message: String) { runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show() } }
        @JavascriptInterface fun getSnapshot(): String = getSharedPreferences("zte", MODE_PRIVATE).getString("snapshot", "") ?: ""
        @JavascriptInterface fun getSnapshotTime(): String = getSharedPreferences("zte", MODE_PRIVATE).getString("snapshot_time", "Chưa có dữ liệu") ?: "Chưa có dữ liệu"
        @JavascriptInterface fun getSnapshotJson(): String = getSharedPreferences("zte", MODE_PRIVATE).getString("snapshot_json", "") ?: ""
        @JavascriptInterface fun getAutoStatus(): String = getSharedPreferences("zte", MODE_PRIVATE).getString("auto_status", "Chưa đăng nhập Router") ?: "Chưa đăng nhập Router"
        @JavascriptInterface fun getAutoUrl(): String = ZteSession.routerUrl
        @JavascriptInterface fun isLoggedIn(): Boolean = ZteSession.authenticated
        @JavascriptInterface fun refreshNow() { runOnUiThread { refreshFromRouter() } }
        @JavascriptInterface fun logout() {
            io.execute {
                try { ZteSession.client?.logout() } catch (_: Exception) {}
                ZteSession.authenticated = false
                ZteSession.client = null
                CredentialVault.clear(this@MainActivity)
                runOnUiThread { Toast.makeText(this@MainActivity, "Đã đăng xuất Router", Toast.LENGTH_SHORT).show(); refreshSnapshotUi() }
            }
        }
    }

    private fun refreshSnapshotUi() { dashboard.evaluateJavascript("window.refreshSnapshot && refreshSnapshot();", null) }

    @Deprecated("Deprecated in Java") override fun onBackPressed() { super.onBackPressed() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        io.shutdownNow()
        dashboard.stopLoading()
        dashboard.destroy()
        super.onDestroy()
    }
}
