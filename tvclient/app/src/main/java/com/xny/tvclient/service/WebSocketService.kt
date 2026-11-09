package com.xny.tvclient.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.JsonObject
import com.xny.tvclient.MainActivity
import com.xny.tvclient.R
import com.xny.tvclient.manager.ConfigManager
import com.xny.tvclient.model.DeviceConfig
import com.xny.tvclient.model.WsMessage
import com.xny.tvclient.utils.DeviceUtils
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * WebSocket 前台服务
 * 负责与服务端保持长连接、发送心跳、处理指令
 */
class WebSocketService : Service() {

    companion object {
        private const val TAG = "WebSocketService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tv_ws_service"

        // 广播 Action
        const val ACTION_COMMAND = "com.xny.tvclient.COMMAND"
        const val ACTION_CONNECTED = "com.xny.tvclient.CONNECTED"
        const val ACTION_DISCONNECTED = "com.xny.tvclient.DISCONNECTED"
        const val ACTION_SCREENSHOT_REQ = "com.xny.tvclient.SCREENSHOT_REQ"

        // 广播 Extra Key
        const val EXTRA_COMMAND_TYPE = "command_type"
        const val EXTRA_COMMAND_ID = "command_id"
        const val EXTRA_URL = "url"
        const val EXTRA_CONFIG = "config_json"
        const val EXTRA_SCREENSHOT_ID = "screenshot_id"

        private var instance: WebSocketService? = null

        fun getInstance(): WebSocketService? = instance

        fun sendCommandResult(context: Context, commandId: String, success: Boolean, msg: String) {
            instance?.sendCommandResult(commandId, success, msg)
        }
    }

    private var webSocket: WebSocket? = null
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)  // WebSocket 不超时
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0
    private var isConnected = false
    private var heartbeatRunnable: Runnable? = null
    private var deviceId: String = ""

    override fun onCreate() {
        super.onCreate()
        instance = this
        deviceId = DeviceUtils.getDeviceId(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("启动中..."))
        Log.d(TAG, "WebSocketService created, deviceId=$deviceId")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val wsUrl = ConfigManager.getWsUrl(this)
        if (wsUrl.isNotBlank()) {
            connect(wsUrl)
        } else {
            Log.w(TAG, "WebSocket URL not configured")
        }
        return START_STICKY
    }

    // ===================== 连接管理 =====================

    fun connect(url: String? = null) {
        val wsUrl = url ?: ConfigManager.getWsUrl(this)
        if (wsUrl.isBlank()) return
        if (isConnected) return

        Log.d(TAG, "Connecting to $wsUrl")
        updateNotification("连接中...")

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                isConnected = true
                reconnectAttempts = 0
                updateNotification("已连接")
                sendBroadcast(Intent(ACTION_CONNECTED).setPackage(packageName))
                // 注册设备
                sendRegister()
                // 启动心跳
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closed: $code $reason")
                onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                onDisconnected()
            }
        })
    }

    private fun onDisconnected() {
        isConnected = false
        stopHeartbeat()
        updateNotification("已断开，重连中...")
        sendBroadcast(Intent(ACTION_DISCONNECTED).setPackage(packageName))
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        reconnectAttempts++
        // 指数退避：1s, 2s, 4s, 8s ... 最大 60s
        val delay = min(60_000L, 1000L * (1L shl minOf(reconnectAttempts - 1, 6)))
        Log.i(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempts)")
        handler.postDelayed({
            if (!isConnected) connect()
        }, delay)
    }

    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "User closed")
        webSocket = null
        isConnected = false
    }

    fun isConnected() = isConnected

    // ===================== 消息发送 =====================

    private fun sendRegister() {
        val data = JsonObject().apply {
            addProperty("deviceId", deviceId)
            addProperty("deviceName", ConfigManager.getDeviceName(this@WebSocketService).ifBlank {
                DeviceUtils.getDeviceModel()
            })
            addProperty("deviceModel", DeviceUtils.getDeviceModel())
            addProperty("androidVersion", DeviceUtils.getAndroidVersion())
            addProperty("appVersion", getAppVersion())
            addProperty("macAddress", DeviceUtils.getMacAddress())
            addProperty("ipAddress", DeviceUtils.getIpAddress(this@WebSocketService))
        }
        sendMessage(WsMessage(WsMessage.TYPE_REGISTER, data = data))
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        val interval = ConfigManager.getHeartbeatInterval(this) * 1000L
        heartbeatRunnable = object : Runnable {
            override fun run() {
                if (isConnected) {
                    sendHeartbeat()
                    handler.postDelayed(this, interval)
                }
            }
        }
        handler.postDelayed(heartbeatRunnable!!, interval)
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { handler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    private fun sendHeartbeat() {
        val data = JsonObject().apply {
            addProperty("deviceId", deviceId)
            addProperty("status", "NORMAL")
            addProperty("currentUrl", ConfigManager.getDisplayUrl(this@WebSocketService))
            addProperty("webviewStatus", "RUNNING")
        }
        sendMessage(WsMessage(WsMessage.TYPE_HEARTBEAT, data = data))
    }

    private fun requestScreenshot(data: JsonObject) {
        val screenshotId = data.get("screenshotId")?.asString ?: System.currentTimeMillis().toString()
        sendBroadcast(Intent(ACTION_SCREENSHOT_REQ).apply {
            setPackage(packageName)
            putExtra(EXTRA_SCREENSHOT_ID, screenshotId)
        })
        Log.d(TAG, "Screenshot requested: id=$screenshotId")
    }

    fun sendScreenshotResult(screenshotId: String, base64Jpeg: String) {
        val data = JsonObject().apply {
            addProperty("screenshotId", screenshotId)
            addProperty("imageBase64", base64Jpeg)
            addProperty("deviceId", deviceId)
        }
        sendMessage(WsMessage(WsMessage.TYPE_SCREENSHOT_RESULT, data = data))
        Log.d(TAG, "Screenshot sent: id=$screenshotId, size=${base64Jpeg.length}")
    }

    fun sendCommandResult(commandId: String, success: Boolean, message: String) {
        val data = JsonObject().apply {
            addProperty("commandId", commandId)
            addProperty("success", success)
            addProperty("message", message)
        }
        sendMessage(WsMessage(WsMessage.TYPE_COMMAND_RESULT, data = data))
    }

    private fun sendMessage(msg: WsMessage) {
        try {
            webSocket?.send(msg.toJson())
        } catch (e: Exception) {
            Log.e(TAG, "Send failed: ${e.message}")
        }
    }

    // ===================== 消息处理 =====================

    private fun handleMessage(text: String) {
        val msg = WsMessage.fromJson(text) ?: return
        val data = msg.getDataAsJsonObject() ?: JsonObject()

        when (msg.type) {
            WsMessage.TYPE_REGISTER_ACK -> handleRegisterAck(data)
            WsMessage.TYPE_HEARTBEAT_ACK -> { /* ignore */ }
            WsMessage.TYPE_CHANGE_URL -> dispatchCommand(WsMessage.TYPE_CHANGE_URL, data)
            WsMessage.TYPE_REFRESH -> dispatchCommand(WsMessage.TYPE_REFRESH, data)
            WsMessage.TYPE_REBUILD_WEBVIEW -> dispatchCommand(WsMessage.TYPE_REBUILD_WEBVIEW, data)
            WsMessage.TYPE_UPDATE_CONFIG -> dispatchCommand(WsMessage.TYPE_UPDATE_CONFIG, data)
            WsMessage.TYPE_RESTART_APP -> restartApp()
            WsMessage.TYPE_SCREENSHOT_REQ -> requestScreenshot(data)
            else -> Log.w(TAG, "Unknown message type: ${msg.type}")
        }
    }

    private fun handleRegisterAck(data: JsonObject) {
        val success = data.get("success")?.asBoolean == true
        if (!success) {
            Log.w(TAG, "Register failed: ${data.get("message")?.asString}")
            return
        }
        Log.i(TAG, "Register success")

        val configJson = data.getAsJsonObject("config") ?: return
        val config = DeviceConfig(
            displayUrl = configJson.get("displayUrl")?.asString ?: "about:blank",
            refreshInterval = configJson.get("refreshInterval")?.asInt ?: 0,
            rebuildInterval = configJson.get("rebuildInterval")?.asInt ?: 3600,
            heartbeatInterval = configJson.get("heartbeatInterval")?.asInt ?: 30,
            enableKioskMode = configJson.get("enableKioskMode")?.asInt ?: 1,
            enableWhiteScreenCheck = configJson.get("enableWhiteScreenCheck")?.asInt ?: 1
        )
        ConfigManager.applyServerConfig(this, config)

        // 重新启动心跳（可能间隔变了）
        startHeartbeat()

        // 通知 MainActivity 应用新配置
        sendBroadcast(Intent(ACTION_COMMAND).apply {
            setPackage(packageName)
            putExtra(EXTRA_COMMAND_TYPE, "APPLY_CONFIG")
            putExtra(EXTRA_URL, config.displayUrl)
        })
    }

    private fun dispatchCommand(type: String, data: JsonObject) {
        val commandId = data.get("commandId")?.asString ?: ""
        sendBroadcast(Intent(ACTION_COMMAND).apply {
            setPackage(packageName)
            putExtra(EXTRA_COMMAND_TYPE, type)
            putExtra(EXTRA_COMMAND_ID, commandId)
            if (WsMessage.TYPE_CHANGE_URL == type) {
                putExtra(EXTRA_URL, data.get("url")?.asString ?: "")
            } else if (WsMessage.TYPE_UPDATE_CONFIG == type) {
                putExtra(EXTRA_CONFIG, data.getAsJsonObject("config")?.toString() ?: "{}")
            }
        })
    }

    private fun restartApp() {
        Log.i(TAG, "Restarting app via AlarmManager...")
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } ?: run {
            Log.e(TAG, "No launch intent found, cannot restart")
            return
        }
        // 用 AlarmManager 在进程杀死后重新启动，避免 startActivity 和 killProcess 竞争
        val pi = android.app.PendingIntent.getActivity(
            this, 9999, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val am = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 1500, pi)
        // 短暂延迟后杀进程，AlarmManager 会在进程外调度
        handler.postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 300)
    }

    // ===================== 通知 =====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "TV Client Service", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps TV Client running in background" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TV Client")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(status: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        disconnect()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "WebSocketService destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 应用从后台任务列表移除时，重启 Service
        val restartIntent = Intent(applicationContext, WebSocketService::class.java)
        val pi = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pi)
        super.onTaskRemoved(rootIntent)
    }
}
