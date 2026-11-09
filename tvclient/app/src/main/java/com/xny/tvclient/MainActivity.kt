package com.xny.tvclient

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xny.tvclient.manager.ConfigManager
import com.xny.tvclient.manager.WebViewManager
import com.xny.tvclient.model.DeviceConfig
import com.xny.tvclient.service.WebSocketService
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_NOTIFICATION = 1001
    }

    private lateinit var webViewContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvDeviceId: TextView
    private lateinit var statusBar: View

    private var webViewManager: WebViewManager? = null
    private val handler = Handler(Looper.getMainLooper())

    // 快速按键计数（5 次 OK/BACK 键进入设置）
    private var settingsKeyCount = 0
    private val resetSettingsKeyCount = Runnable { settingsKeyCount = 0 }

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WebSocketService.ACTION_COMMAND -> handleCommand(intent)
                WebSocketService.ACTION_CONNECTED -> onWsConnected()
                WebSocketService.ACTION_DISCONNECTED -> onWsDisconnected()
                WebSocketService.ACTION_SCREENSHOT_REQ -> handleScreenshotReq(intent)
            }
        }
    }

    // ===================== 生命周期 =====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ConfigManager.isSetupDone(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setupFullscreen()
        setContentView(R.layout.activity_main)
        bindViews()
        requestNotificationPermission()
        startWebSocketService()

        val filter = IntentFilter().apply {
            addAction(WebSocketService.ACTION_COMMAND)
            addAction(WebSocketService.ACTION_CONNECTED)
            addAction(WebSocketService.ACTION_DISCONNECTED)
            addAction(WebSocketService.ACTION_SCREENSHOT_REQ)
        }
        ContextCompat.registerReceiver(
            this, commandReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        initWebView()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        webViewManager?.getWebView()?.onResume()
    }

    override fun onPause() {
        super.onPause()
        webViewManager?.getWebView()?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        webViewManager?.stopAll()
        handler.removeCallbacksAndMessages(null)
    }

    // ===================== 初始化 =====================

    private fun bindViews() {
        progressBar = findViewById(R.id.progressBar)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvDeviceId = findViewById(R.id.tvDeviceId)
        statusBar = findViewById(R.id.statusBar)
        webViewContainer = (findViewById<android.webkit.WebView>(R.id.webView).parent) as FrameLayout

        if (BuildConfig.DEBUG) {
            statusBar.visibility = View.VISIBLE
            tvDeviceId.text = com.xny.tvclient.utils.DeviceUtils.getDeviceId(this).take(8)
        }

        // 长按状态栏区域进入设置（开发/运维用）
        statusBar.setOnLongClickListener {
            openSettingsDialog()
            true
        }
    }

    private fun initWebView() {
        val staticWv = findViewById<android.webkit.WebView>(R.id.webView)
        webViewContainer.removeView(staticWv)

        webViewManager = WebViewManager(
            context = this,
            container = webViewContainer,
            progressBar = progressBar
        )
        webViewManager!!.init(ConfigManager.getDisplayUrl(this))
        applySchedules()
    }

    private fun applySchedules() {
        val wvm = webViewManager ?: return
        wvm.scheduleRefresh(ConfigManager.getRefreshInterval(this))
        wvm.scheduleRebuild(ConfigManager.getRebuildInterval(this))
        wvm.startWhiteScreenCheck(ConfigManager.isWhiteScreenCheckEnabled(this))
    }

    // ===================== 通知权限（Android 13+）=====================

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION
                )
            }
        }
    }

    // ===================== WebSocket 服务 =====================

    private fun startWebSocketService() {
        val intent = Intent(this, WebSocketService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // ===================== 指令处理 =====================

    private fun handleCommand(intent: Intent) {
        val type = intent.getStringExtra(WebSocketService.EXTRA_COMMAND_TYPE) ?: return
        val commandId = intent.getStringExtra(WebSocketService.EXTRA_COMMAND_ID) ?: ""

        Log.i(TAG, "Handling command: type=$type, commandId=$commandId")

        when (type) {
            "APPLY_CONFIG" -> {
                val url = intent.getStringExtra(WebSocketService.EXTRA_URL) ?: return
                if (url.isNotBlank() && url != "about:blank") {
                    webViewManager?.loadUrl(url)
                    applySchedules()
                }
            }
            "CHANGE_URL" -> {
                val url = intent.getStringExtra(WebSocketService.EXTRA_URL) ?: return
                if (url.isNotBlank()) {
                    ConfigManager.setDisplayUrl(this, url)
                    webViewManager?.loadUrl(url)
                    WebSocketService.sendCommandResult(this, commandId, true, "URL changed")
                }
            }
            "REFRESH" -> {
                webViewManager?.reload()
                WebSocketService.sendCommandResult(this, commandId, true, "Page refreshed")
            }
            "REBUILD_WEBVIEW" -> {
                webViewManager?.rebuildAndLoad()
                WebSocketService.sendCommandResult(this, commandId, true, "WebView rebuilt")
            }
            "UPDATE_CONFIG" -> {
                val configJson = intent.getStringExtra(WebSocketService.EXTRA_CONFIG) ?: return
                try {
                    val jo = JSONObject(configJson)
                    val config = DeviceConfig(
                        refreshInterval = jo.optInt("refreshInterval", 0),
                        rebuildInterval = jo.optInt("rebuildInterval", 3600),
                        heartbeatInterval = jo.optInt("heartbeatInterval", 30),
                        enableKioskMode = jo.optInt("enableKioskMode", 1)
                    )
                    ConfigManager.applyServerConfig(this, config)
                    applySchedules()
                    WebSocketService.sendCommandResult(this, commandId, true, "Config updated")
                } catch (e: Exception) {
                    WebSocketService.sendCommandResult(this, commandId, false, e.message ?: "Parse error")
                }
            }
        }
    }

    private fun handleScreenshotReq(intent: Intent) {
        val screenshotId = intent.getStringExtra(WebSocketService.EXTRA_SCREENSHOT_ID) ?: return
        webViewManager?.captureScreenshot { base64 ->
            if (base64 != null) {
                WebSocketService.getInstance()?.sendScreenshotResult(screenshotId, base64)
            } else {
                Log.w(TAG, "Screenshot capture failed for id=$screenshotId")
            }
        }
    }

    private fun onWsConnected() {
        handler.post {
            tvConnectionStatus.setTextColor(android.graphics.Color.parseColor("#68d391"))
        }
    }

    private fun onWsDisconnected() {
        handler.post {
            tvConnectionStatus.setTextColor(android.graphics.Color.parseColor("#fc8181"))
        }
    }

    // ===================== Kiosk 模式 & 设置入口 =====================

    /**
     * dispatchKeyEvent 在事件分发给 WebView 之前拦截，确保遥控器按键一定生效
     * onKeyDown 在 WebView 已消费按键后才调用，WebView 会消费 PAGE_UP/DOWN 等
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                // ===== 放大（频道+、ZoomIn） =====
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_ZOOM_IN -> {
                    webViewManager?.adjustZoom(+10)
                    return true
                }
                // ===== 缩小（频道-、ZoomOut） =====
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_ZOOM_OUT -> {
                    webViewManager?.adjustZoom(-10)
                    return true
                }
                // ===== 刷新（F5 / Refresh 键） =====
                KeyEvent.KEYCODE_F5,
                KeyEvent.KEYCODE_REFRESH -> {
                    webViewManager?.reload()
                    return true
                }
                // ===== 设置入口：MENU 键 =====
                KeyEvent.KEYCODE_MENU -> {
                    openSettingsDialog()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!ConfigManager.isKioskEnabled(this)) {
            return super.onKeyDown(keyCode, event)
        }

        // Kiosk 模式：连按 5 次 BACK 键进入设置
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            settingsKeyCount++
            handler.removeCallbacks(resetSettingsKeyCount)
            if (settingsKeyCount >= 5) {
                settingsKeyCount = 0
                openSettingsDialog()
            } else {
                handler.postDelayed(resetSettingsKeyCount, 2000)
            }
            return true
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun openSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("TV Client 设置")
            .setMessage("进入设置页面可修改展示网址和服务器地址")
            .setPositiveButton("进入设置") { _, _ ->
                startActivity(Intent(this, SetupActivity::class.java))
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun setupFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(
                    android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                )
                systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
}
