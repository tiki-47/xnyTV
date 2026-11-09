package com.xny.tvclient.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * WebSocket 消息格式（与服务端保持一致）
 */
data class WsMessage(
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: Any? = null
) {
    companion object {
        private val gson = Gson()

        // 消息类型常量
        const val TYPE_REGISTER = "REGISTER"
        const val TYPE_REGISTER_ACK = "REGISTER_ACK"
        const val TYPE_HEARTBEAT = "HEARTBEAT"
        const val TYPE_HEARTBEAT_ACK = "HEARTBEAT_ACK"
        const val TYPE_CHANGE_URL = "CHANGE_URL"
        const val TYPE_REFRESH = "REFRESH"
        const val TYPE_REBUILD_WEBVIEW = "REBUILD_WEBVIEW"
        const val TYPE_UPDATE_CONFIG = "UPDATE_CONFIG"
        const val TYPE_RESTART_APP = "RESTART_APP"
        const val TYPE_COMMAND_RESULT = "COMMAND_RESULT"
        const val TYPE_SCREENSHOT_REQ = "SCREENSHOT_REQ"
        const val TYPE_SCREENSHOT_RESULT = "SCREENSHOT_RESULT"

        fun fromJson(json: String): WsMessage? {
            return try {
                val obj = JsonParser.parseString(json).asJsonObject
                val type = obj.get("type")?.asString ?: return null
                val timestamp = obj.get("timestamp")?.asLong ?: System.currentTimeMillis()
                val data = obj.get("data")
                WsMessage(type, timestamp, data)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String = gson.toJson(this)

    fun getDataAsJsonObject(): JsonObject? {
        return when (data) {
            is JsonObject -> data
            is String -> try { JsonParser.parseString(data).asJsonObject } catch (e: Exception) { null }
            else -> try { JsonParser.parseString(gson.toJson(data)).asJsonObject } catch (e: Exception) { null }
        }
    }
}
