package com.xny.tvclient.model

/**
 * 设备配置（从服务端下发）
 */
data class DeviceConfig(
    val displayUrl: String = "about:blank",
    val refreshInterval: Int = 0,        // 秒，0=不刷新
    val rebuildInterval: Int = 3600,     // 秒，0=不重建
    val heartbeatInterval: Int = 30,     // 秒
    val enableKioskMode: Int = 1,        // 1=启用
    val enableWhiteScreenCheck: Int = 1  // 1=启用
)
