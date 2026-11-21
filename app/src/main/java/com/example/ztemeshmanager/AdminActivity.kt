package com.example.ztemeshmanager

import android.app.Activity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bg = 0xFF0B1220.toInt(); val panel = 0xFF151F30.toInt(); val text = 0xFFEEF4FF.toInt(); val muted = 0xFF91A4BD.toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 26, 22, 22); setBackgroundColor(bg) }
        val title = TextView(this).apply { this.text = "Kết nối Router ZTE"; textSize = 23f; setTextColor(text) }
        root.addView(title)
        root.addView(TextView(this).apply { this.text = "Đăng nhập native qua API HTTPS của Router. Không mở WebView quản trị."; setTextColor(muted); textSize = 13f; setPadding(0, 8, 0, 18) })
        fun field(hint: String, value: String = ""): EditText = EditText(this).apply {
            this.hint = hint; setText(value); setSingleLine(true); setTextColor(text); setHintTextColor(muted)
            setPadding(14, 10, 14, 10); setBackgroundColor(panel)
        }
        routerInput = field("Địa chỉ Router", getSharedPreferences("zte", MODE_PRIVATE).getString("router", "https://192.168.1.11/") ?: "https://192.168.1.11/")
        userInput = field("Tên đăng nhập", getSharedPreferences("zte", MODE_PRIVATE).getString("username", "admin") ?: "admin")
        passwordInput = field("Mật khẩu")
        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        listOf(routerInput, userInput, passwordInput).forEach { v -> root.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }) }
        status = TextView(this).apply { setTextColor(muted); textSize = 13f; this.text = "Nhập thông tin tài khoản để kết nối."; setPadding(0, 10, 0, 14) }
        root.addView(status)
        loginButton = Button(this).apply { this.text = "ĐĂNG NHẬP"; setOnClickListener { doLogin() } }
        root.addView(loginButton, LinearLayout.LayoutParams(-1, -2))
        val logout = Button(this).apply { this.text = "Đăng xuất"; setOnClickListener { doLogout() } }
        root.addView(logout, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(this).apply { this.text = "← Quay lại Dashboard"; setOnClickListener { finish() } }, LinearLayout.LayoutParams(-1, -2))
        setContentView(ScrollView(this).apply { addView(root) })
        if (ZteSession.authenticated) status.text = "Đã có phiên Router đang hoạt động."
    }

    private fun doLogin() {
        val base = routerInput.text.toString().trim().let { if (it.endsWith("/")) it else "$it/" }
        val user = userInput.text.toString().trim()
        val pass = passwordInput.text.toString()
        val host = try { java.net.URI(base).host ?: "" } catch (_: Exception) { "" }
        val privateHost = host.startsWith("192.168.") || host.startsWith("10.") ||
            (host.startsWith("172.") && host.split(".").getOrNull(1)?.toIntOrNull() in 16..31)
        if (!base.startsWith("https://") || !privateHost || user.isBlank() || pass.isBlank()) {
            status.text = "Hãy nhập địa chỉ HTTPS của Router trong mạng nội bộ (ví dụ https://192.168.1.11/), tên đăng nhập và mật khẩu."; return
        }
        loginButton.isEnabled = false
        status.text = "Đang khởi tạo phiên và xác thực với Router…"
        io.execute {
            try {
                val client = ZteApiClient(base)
                client.login(user, pass)
                ZteSession.client = client
                ZteSession.routerUrl = base
                ZteSession.authenticated = true
                getSharedPreferences("zte", MODE_PRIVATE).edit().putString("router", base).putString("username", user)
                    .putString("auto_status", "Đăng nhập thành công · đang đồng bộ Mesh").apply()
                CredentialVault.save(this, user, pass)
                runOnUiThread {
                    status.text = "Đăng nhập thành công. Dashboard sẽ tự đọc Topology và từng node mỗi 30 giây."
                    passwordInput.setText("")
                    Toast.makeText(this, "Đã kết nối Router ZTE", Toast.LENGTH_LONG).show()
                    loginButton.isEnabled = true
                }
            } catch (e: Exception) {
                ZteSession.authenticated = false
                runOnUiThread { status.text = "Đăng nhập chưa thành công: ${e.message ?: "Lỗi không xác định"}\nKiểm tra lại mật khẩu hoặc thuật toán mã hóa Password của firmware."; loginButton.isEnabled = true }
            }
        }
    }

    private fun doLogout() {
        status.text = "Đang đăng xuất…"
        io.execute {
            try { ZteSession.client?.logout() } catch (e: Exception) { runOnUiThread { status.text = "Đã gửi đăng xuất; phản hồi: ${e.message}" } }
            ZteSession.client = null; ZteSession.authenticated = false
            CredentialVault.clear(this)
            runOnUiThread { status.text = "Đã kết thúc phiên Router."; Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onDestroy() { io.shutdownNow(); super.onDestroy() }
}
