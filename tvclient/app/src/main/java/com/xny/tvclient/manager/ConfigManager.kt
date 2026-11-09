package com.xny.tvclient.manager

import android.content.Context
import android.content.SharedPreferences

/**
 * 设备配置持久化管理（SharedPreferences）
 */
object ConfigManager {

    private const val PREF_NAME = "tv_client_config"

    private const val KEY_WS_URL = "ws_server_url"
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_LOCATION = "location"
    private const val KEY_DISPLAY_URL = "display_url"
    private const val KEY_REFRESH_INTERVAL = "refresh_interval"
    private const val KEY_REBUILD_INTERVAL = "rebuild_interval"
    private const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval"
    private const val KEY_ENABLE_KIOSK = "enable_kiosk_mode"
    private const val KEY_WHITE_SCREEN_CHECK = "enable_white_screen_check"
    private const val KEY_SETUP_DONE = "setup_done"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // WebSocket 服务器地址
    fun getWsUrl(context: Context): String =
        prefs(context).getString(KEY_WS_URL, "") ?: ""

    fun setWsUrl(context: Context, url: String) =
        prefs(context).edit().putString(KEY_WS_URL, url).apply()

    // 设备名称
    fun getDeviceName(context: Context): String =
        prefs(context).getString(KEY_DEVICE_NAME, "") ?: ""

    fun setDeviceName(context: Context, name: String) =
        prefs(context).edit().putString(KEY_DEVICE_NAME, name).apply()

    // 设备位置
    fun getLocation(context: Context): String =
        prefs(context).getString(KEY_LOCATION, "") ?: ""

    fun setLocation(context: Context, loc: String) =
        prefs(context).edit().putString(KEY_LOCATION, loc).apply()

    // 展示 URL
    fun getDisplayUrl(context: Context): String =
        prefs(context).getString(KEY_DISPLAY_URL, "about:blank") ?: "about:blank"

    fun setDisplayUrl(context: Context, url: String) =
        prefs(context).edit().putString(KEY_DISPLAY_URL, url).apply()

    // 刷新间隔(秒)
    fun getRefreshInterval(context: Context): Int =
        prefs(context).getInt(KEY_REFRESH_INTERVAL, 0)

    fun setRefreshInterval(context: Context, interval: Int) =
        prefs(context).edit().putInt(KEY_REFRESH_INTERVAL, interval).apply()

    // 重建间隔(秒)
    fun getRebuildInterval(context: Context): Int =
        prefs(context).getInt(KEY_REBUILD_INTERVAL, 3600)

    fun setRebuildInterval(context: Context, interval: Int) =
        prefs(context).edit().putInt(KEY_REBUILD_INTERVAL, interval).apply()

    // 心跳间隔(秒)
    fun getHeartbeatInterval(context: Context): Int =
        prefs(context).getInt(KEY_HEARTBEAT_INTERVAL, 30)

    fun setHeartbeatInterval(context: Context, interval: Int) =
        prefs(context).edit().putInt(KEY_HEARTBEAT_INTERVAL, interval).apply()

    // Kiosk 模式
    fun isKioskEnabled(context: Context): Boolean =
        prefs(context).getInt(KEY_ENABLE_KIOSK, 1) == 1

    fun setKioskEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putInt(KEY_ENABLE_KIOSK, if (enabled) 1 else 0).apply()

    // 白屏检测
    fun isWhiteScreenCheckEnabled(context: Context): Boolean =
        prefs(context).getInt(KEY_WHITE_SCREEN_CHECK, 1) == 1

    // 初始设置是否完成
    fun isSetupDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_DONE, false)

    fun setSetupDone(context: Context, done: Boolean) =
        prefs(context).edit().putBoolean(KEY_SETUP_DONE, done).apply()

    /**
     * 从服务端下发的配置更新本地配置
     */
    fun applyServerConfig(context: Context, config: com.xny.tvclient.model.DeviceConfig) {
        prefs(context).edit().apply {
            if (config.displayUrl.isNotBlank() && config.displayUrl != "about:blank") {
                putString(KEY_DISPLAY_URL, config.displayUrl)
            }
            putInt(KEY_REFRESH_INTERVAL, config.refreshInterval)
            putInt(KEY_REBUILD_INTERVAL, config.rebuildInterval)
            putInt(KEY_HEARTBEAT_INTERVAL, config.heartbeatInterval)
            putInt(KEY_ENABLE_KIOSK, config.enableKioskMode)
            putInt(KEY_WHITE_SCREEN_CHECK, config.enableWhiteScreenCheck)
            apply()
        }
    }
}
