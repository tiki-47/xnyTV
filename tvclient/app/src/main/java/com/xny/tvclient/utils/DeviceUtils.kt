package com.xny.tvclient.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface

object DeviceUtils {

    /**
     * 获取设备唯一标识（Android ID）
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: Build.SERIAL.takeIf { it != Build.UNKNOWN }
            ?: "TV_${Build.MODEL.replace(" ", "_")}_${System.currentTimeMillis()}"
    }

    /**
     * 获取设备型号
     */
    fun getDeviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    /**
     * 获取 Android 版本
     */
    fun getAndroidVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    /**
     * 获取 MAC 地址
     */
    fun getMacAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                ?.firstOrNull { it.name.equals("wlan0", ignoreCase = true) }
                ?.hardwareAddress
                ?.joinToString(":") { "%02X".format(it) }
                ?: "00:00:00:00:00:00"
        } catch (e: Exception) {
            "00:00:00:00:00:00"
        }
    }

    /**
     * 获取 WiFi IP 地址
     */
    fun getIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            if (ip == 0) {
                // fallback: 通过 NetworkInterface 获取
                NetworkInterface.getNetworkInterfaces()
                    ?.toList()
                    ?.flatMap { it.inetAddresses.toList() }
                    ?.firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
                    ?.hostAddress ?: "0.0.0.0"
            } else {
                "%d.%d.%d.%d".format(ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
            }
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }
}
